package com.devfutech.chatqiscus.ui.chatroom

import android.content.Context
import androidx.annotation.CheckResult
import androidx.core.util.Pair
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.devfutech.chatqiscus.R
import com.devfutech.chatqiscus.utils.QiscusEvent
import com.devfutech.chatqiscus.utils.QiscusImageUtil
import com.devfutech.chatqiscus.utils.ResultOf
import com.qiscus.sdk.chat.core.QiscusCore
import com.qiscus.sdk.chat.core.data.model.QiscusChatRoom
import com.qiscus.sdk.chat.core.data.model.QiscusComment
import com.qiscus.sdk.chat.core.data.remote.QiscusApi
import com.qiscus.sdk.chat.core.data.remote.QiscusResendCommentHelper
import com.qiscus.sdk.chat.core.util.QiscusAndroidUtil
import com.qiscus.sdk.chat.core.util.QiscusFileUtil
import com.qiscus.sdk.chat.core.util.QiscusTextUtil
import com.trello.rxlifecycle.LifecycleTransformer
import com.trello.rxlifecycle.RxLifecycle
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONException
import retrofit2.HttpException
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.functions.Func2
import rx.schedulers.Schedulers
import rx.subjects.BehaviorSubject
import java.io.File
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ChatRoomViewModel @Inject constructor(@ApplicationContext val context: Context) :
    ViewModel() {
    private lateinit var room: QiscusChatRoom
    private val commentComparator =
        Func2 { lhs: QiscusComment, rhs: QiscusComment ->
            rhs.time.compareTo(lhs.time)
        }
    private var pendingTask = HashMap<QiscusComment, Subscription>()
    private val lifecycleSubject: BehaviorSubject<QiscusEvent> = BehaviorSubject.create()
    private val _listQiscusComment = MutableLiveData<ResultOf<List<QiscusComment?>>>()

    val listQiscusComment: LiveData<ResultOf<List<QiscusComment?>>>
        get() = _listQiscusComment
    private val _qiscusComment = MutableLiveData<ResultOf<QiscusComment>>()
    val qiscusComment: LiveData<ResultOf<QiscusComment>>
        get() = _qiscusComment

    fun setCommentLive(qiscusComment: QiscusComment) {
        _qiscusComment.postValue(ResultOf.Success(qiscusComment))
    }

    fun startChat(qiscusChatRoom: QiscusChatRoom?){
        this.room = qiscusChatRoom!!
    }
    fun loadComments(count: Int) {
        _listQiscusComment.postValue(ResultOf.Progress(true))
        Observable.merge<Pair<QiscusChatRoom?, List<QiscusComment?>?>>(
            getInitRoomData(),
            getLocalComments(count).map { comments: List<QiscusComment> ->
                Pair.create(
                    room,
                    comments
                )
            }
        )
            .filter { qiscusChatRoomListPair: Pair<QiscusChatRoom?, List<QiscusComment?>?>? -> qiscusChatRoomListPair != null }
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .compose(bindToLifecycle())
            .subscribe({
                _listQiscusComment.postValue(ResultOf.Success(it.second!!))
            }, { _listQiscusComment.postValue(ResultOf.Failure(throwable = it, message = null)) })
    }

    private fun getInitRoomData(): Observable<Pair<QiscusChatRoom?, List<QiscusComment?>?>?>? {
        return QiscusApi.getInstance().getChatRoomWithMessages(room.id)
            .doOnError { throwable: Throwable ->
                throwable.printStackTrace()
                QiscusAndroidUtil.runOnUIThread {
                    _listQiscusComment.postValue(
                        ResultOf.Failure(
                            throwable = throwable,
                            message = null
                        )
                    )
                }
            }
            .doOnNext { roomData: Pair<QiscusChatRoom?, MutableList<QiscusComment?>?> ->
//                roomEventHandler.setChatRoom(roomData.first)
                roomData.second?.sortWith { lhs: QiscusComment?, rhs: QiscusComment? ->
                    rhs?.time?.compareTo(lhs?.time)!!
                }
                QiscusCore.getDataStore().addOrUpdate(roomData.first)
            }
            .doOnNext { roomData: Pair<QiscusChatRoom?, List<QiscusComment?>> ->
                for (qiscusComment in roomData.second!!) {
                    QiscusCore.getDataStore().addOrUpdate(qiscusComment)
                }
            }
            .subscribeOn(Schedulers.io())
            .onErrorReturn { null }
    }

    fun loadOlderCommentThan(qiscusComment: QiscusComment) {
        _listQiscusComment.postValue(ResultOf.Progress(true))
        QiscusCore.getDataStore().getObservableOlderCommentsThan(qiscusComment, room.id, 40)
            .flatMap { iterable: MutableList<QiscusComment?>? ->
                Observable.from(
                    iterable
                )
            }
            .filter { result -> qiscusComment.id == -1L || result!!.id < qiscusComment.id }
            .toSortedList(commentComparator)
            .map { comments: MutableList<QiscusComment?> ->
                if (comments.size >= 20) {
                    return@map comments.subList(0, 20)
                }
                comments
            }
            .doOnNext(this::updateRepliedSender)
            .flatMap { comments: MutableList<QiscusComment?> ->
                if (isValidOlderComments(comments, qiscusComment)) Observable.from(
                    comments
                )
                    .toSortedList(commentComparator) else getCommentsFromNetwork(qiscusComment.id).map { comments1: MutableList<QiscusComment?> ->
                    for (localComment in comments) {
                        if (localComment != null && localComment.state <= QiscusComment.STATE_SENDING) {
                            comments1.add(localComment)
                        }
                    }
                    comments1
                }
            }
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .compose(bindToLifecycle())
            .subscribe({ comments ->
                _listQiscusComment.postValue(ResultOf.Success(comments))
            }, { throwable ->
                _listQiscusComment.postValue(
                    ResultOf.Failure(
                        throwable = throwable,
                        message = null
                    )
                )
            })
    }

    private fun updateRepliedSender(comments: MutableList<QiscusComment?>) {
        for (comment in comments) {
            if (comment?.type == QiscusComment.Type.REPLY) {
                val repliedComment = comment.replyTo
                if (repliedComment != null) {
                    for (qiscusRoomMember in room.member) {
                        if (repliedComment.senderEmail == qiscusRoomMember.email) {
                            repliedComment.sender = qiscusRoomMember.username
                            comment.replyTo = repliedComment
                            break
                        }
                    }
                }
            }
        }
    }

    private fun isValidOlderComments(
        qiscusComments: MutableList<QiscusComment?>,
        lastQiscusComment: QiscusComment
    ): Boolean {
        if (qiscusComments.isEmpty()) return false
        val result = cleanFailedComments(qiscusComments)
        var containsLastValidComment = result.isEmpty() || lastQiscusComment.id == -1L
        val size = result.size
        if (size == 1) {
            return result[0].commentBeforeId == 0L && lastQiscusComment.commentBeforeId == result[0].id
        }
        for (i in 0 until size - 1) {
            if (!containsLastValidComment && result[i].id == lastQiscusComment.commentBeforeId) {
                containsLastValidComment = true
            }
            if (result[i].commentBeforeId != result[i + 1].id) {
                return false
            }
        }
        return containsLastValidComment
    }

    private fun cleanFailedComments(qiscusComments: List<QiscusComment?>): List<QiscusComment> {
        val comments: MutableList<QiscusComment> = ArrayList()
        for (qiscusComment in qiscusComments) {
            if (qiscusComment != null && qiscusComment.id != -1L) {
                comments.add(qiscusComment)
            }
        }
        return comments
    }

    private fun getCommentsFromNetwork(lastCommentId: Long): Observable<MutableList<QiscusComment?>> {
        return QiscusApi.getInstance().getPreviousMessagesById(room.id, 20, lastCommentId)
            .doOnNext { qiscusComment: QiscusComment ->
                QiscusCore.getDataStore().addOrUpdate(qiscusComment)
                qiscusComment.roomId = room.id
            }
            .toSortedList(commentComparator)
            .subscribeOn(Schedulers.io())
    }

    private fun getLocalComments(
        count: Int
    ): Observable<List<QiscusComment>> {
        return QiscusCore.getDataStore().getObservableComments(room.id, 2 * count)
            .flatMap { iterable: List<QiscusComment> ->
                Observable.from(
                    iterable
                )
            }
            .toSortedList(commentComparator)
            .map { comments: List<QiscusComment> ->
                if (comments.size > count) {
                    return@map comments.subList(0, count)
                }
                comments
            }
            .subscribeOn(Schedulers.io())
    }

    fun sendReplyComment(content: String?, originComment: QiscusComment?) {
        val qiscusComment = QiscusComment.generateReplyMessage(room.id, content, originComment)
        sendComment(qiscusComment)
    }

    fun sendComment(content: String?) {
        val qiscusComment = QiscusComment.generateMessage(room.id, content)
        sendComment(qiscusComment)
    }

    fun deleteComment(qiscusComment: QiscusComment) {
        _listQiscusComment.postValue(ResultOf.Progress(true))
        cancelPendingComment(qiscusComment)
        QiscusResendCommentHelper.cancelPendingComment(qiscusComment)
        QiscusAndroidUtil.runOnBackgroundThread {
            QiscusCore.getDataStore().delete(qiscusComment)
        }
        Observable.from(arrayOf(qiscusComment))
            .map { obj: QiscusComment -> obj.uniqueId }
            .toList()
            .flatMap { uniqueIds: List<String>? ->
                QiscusApi.getInstance().deleteMessages(uniqueIds)
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .compose(bindToLifecycle())
            .subscribe({ deletedComments ->
                _listQiscusComment.postValue(ResultOf.Success(deletedComments))
            }, { throwable ->
                _listQiscusComment.postValue(
                    ResultOf.Failure(
                        throwable = throwable,
                        message = null
                    )
                )
            })
    }

    fun sendFile(file: File, caption: String?) {
        var compressedFile = file
        compressedFile = if (QiscusImageUtil.isImage(file) && !file.name.endsWith(".gif")) {
            try {
                QiscusImageUtil.compressImage(context, file)
            } catch (e: NullPointerException) {
                _qiscusComment.postValue(
                    ResultOf.Failure(
                        throwable = null,
                        message = QiscusTextUtil.getString(R.string.qiscus_corrupted_file)
                    )
                )
                return
            }
        } else {
            QiscusFileUtil.saveFile(compressedFile)
        }
        if (!file.exists()) {
            _qiscusComment.postValue(
                ResultOf.Failure(
                    throwable = null,
                    message = QiscusTextUtil.getString(R.string.qiscus_corrupted_file)
                )
            )
            return
        }
        val qiscusComment = QiscusComment.generateFileAttachmentMessage(
            room.id,
            compressedFile.path, caption, file.name
        )
        qiscusComment.isDownloading = true

        val finalCompressedFile = compressedFile
        val subscription = QiscusApi.getInstance().sendFileMessage(
            qiscusComment, finalCompressedFile
        ) { percentage: Long ->
            qiscusComment.progress = percentage.toInt()
        }.doOnSubscribe { QiscusCore.getDataStore().addOrUpdate(qiscusComment) }
            .doOnNext { result -> commentSuccess(result) }
            .doOnError { throwable: Throwable? ->
                commentFail(
                    throwable!!,
                    qiscusComment
                )
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .compose(bindToLifecycle())
            .subscribe({ commentSend ->
                if (commentSend.roomId == room.id) {
                    commentSend.isDownloading = false
                    QiscusCore.getDataStore()
                        .addOrUpdateLocalPath(
                            commentSend.roomId,
                            commentSend.id, finalCompressedFile.absolutePath
                        )
                }
            }, { throwable ->
                throwable.printStackTrace()
                if (qiscusComment.roomId == room.id) {
                    _qiscusComment.postValue(ResultOf.Success(qiscusComment))
                }
            })
        pendingTask[qiscusComment] = subscription
    }

    private fun sendComment(qiscusComment: QiscusComment) {
        _qiscusComment.postValue(ResultOf.Progress(true))
        val subscription = QiscusApi.getInstance().sendMessage(qiscusComment)
            .doOnSubscribe { QiscusCore.getDataStore().addOrUpdate(qiscusComment) }
            .doOnNext(this::commentSuccess)
            .doOnError { throwable ->
                commentFail(
                    throwable,
                    qiscusComment
                )
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .compose(bindToLifecycle())
            .subscribe(
                {},
                {
                    if (qiscusComment.roomId == room.id) {
                        _qiscusComment.postValue(ResultOf.Success(qiscusComment))
                    }
                },
            )
        pendingTask[qiscusComment] = subscription
    }

    private fun cancelPendingComment(qiscusComment: QiscusComment?) {
        if (pendingTask.containsKey(qiscusComment)) {
            val subscription = pendingTask[qiscusComment]
            if (!subscription!!.isUnsubscribed) {
                subscription.unsubscribe()
            }
            pendingTask.remove(qiscusComment)
        }
    }

    private fun commentSuccess(qiscusComment: QiscusComment) {
        pendingTask.remove(qiscusComment)
        qiscusComment.state = QiscusComment.STATE_ON_QISCUS
        val savedQiscusComment = QiscusCore.getDataStore().getComment(qiscusComment.uniqueId)
        if (savedQiscusComment != null && savedQiscusComment.state > qiscusComment.state) {
            qiscusComment.state = savedQiscusComment.state
        }
        QiscusCore.getDataStore().addOrUpdate(qiscusComment)
    }

    private fun commentFail(throwable: Throwable, qiscusComment: QiscusComment) {
        pendingTask.remove(qiscusComment)
        if (!QiscusCore.getDataStore().isContains(qiscusComment)) { //Have been deleted
            return
        }
        var state = QiscusComment.STATE_PENDING
        if (mustFailed(throwable, qiscusComment)) {
            qiscusComment.isDownloading = false
            state = QiscusComment.STATE_FAILED
        }
        val savedQiscusComment = QiscusCore.getDataStore().getComment(qiscusComment.uniqueId)
        if (savedQiscusComment != null && savedQiscusComment.state > QiscusComment.STATE_SENDING) {
            return
        }
        qiscusComment.state = state
        QiscusCore.getDataStore().addOrUpdate(qiscusComment)
    }

    private fun mustFailed(throwable: Throwable, qiscusComment: QiscusComment): Boolean {
        return throwable is HttpException && throwable.code() >= 400 || throwable is JSONException || qiscusComment.isAttachment
    }

    private fun clearUnreadCount() {
        room.unreadCount = 0
        room.lastComment = null
        QiscusCore.getDataStore().addOrUpdate(room)
    }

    fun detachView() {
        clearUnreadCount()
    }

    @CheckResult
    fun <T> bindToLifecycle(): LifecycleTransformer<T> {
        return RxLifecycle.bindUntilEvent(lifecycleSubject, QiscusEvent.DETACH)
    }
}