package com.devfutech.chatqiscus.data.repository

import com.devfutech.chatqiscus.utils.Action
import com.devfutech.chatqiscus.utils.ObjectUtil
import com.qiscus.sdk.chat.core.QiscusCore
import com.qiscus.sdk.chat.core.data.model.QiscusAccount
import com.qiscus.sdk.chat.core.data.model.QiscusChatRoom
import com.qiscus.sdk.chat.core.data.remote.QiscusApi
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRoomRepositoryImpl @Inject constructor() : ChatRoomRepository {
    override fun getChatRooms(
        onSuccess: Action<List<QiscusChatRoom?>?>,
        onError: Action<Throwable?>
    ) {
        Observable.from(QiscusCore.getDataStore().getChatRooms(100))
            .filter { chatRoom: QiscusChatRoom -> chatRoom.lastComment != null }
            .toList()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(onSuccess::call, onError::call)

        QiscusApi.getInstance()
            .getAllChatRooms(true, false, true, 1, 100)
            .flatMap { iterable: List<QiscusChatRoom?>? ->
                Observable.from(
                    iterable
                )
            }
            .doOnNext { qiscusChatRoom: QiscusChatRoom? ->
                QiscusCore.getDataStore().addOrUpdate(qiscusChatRoom)
            }
            .filter { chatRoom: QiscusChatRoom? ->
                chatRoom?.lastComment?.id != 0L
            }
            .toList()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(onSuccess::call, onError::call)
    }

    override fun createChatRoom(
        user: QiscusAccount?,
        onSuccess: Action<QiscusChatRoom?>,
        onError: Action<Throwable?>
    ) {
        val savedChatRoom = QiscusCore.getDataStore().getChatRoom(user?.email.toString())
        if (savedChatRoom != null) {
            onSuccess.call(savedChatRoom)
            return
        }
        QiscusApi.getInstance()
            .chatUser(user?.email.toString(), null)
            .doOnNext { chatRoom: QiscusChatRoom? ->
                QiscusCore.getDataStore().addOrUpdate(chatRoom)
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(onSuccess::call, onError::call)
    }

    override fun createGroupChatRoom(
        name: String?,
        members: List<QiscusAccount?>,
        onSuccess: Action<QiscusChatRoom?>,
        onError: Action<Throwable?>
    ) {
        val ids = members.map { it?.email }
        QiscusApi.getInstance()
            .createGroupChat(name, ids, ObjectUtil.generateAvatar(name), null)
            .doOnNext { chatRoom: QiscusChatRoom? ->
                QiscusCore.getDataStore().addOrUpdate(chatRoom)
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(onSuccess::call, onError::call)
    }

    override fun addParticipant(
        roomId: Long,
        members: List<QiscusAccount?>,
        onSuccess: Action<Void?>,
        onError: Action<Throwable?>
    ) {
        val ids: MutableList<String> = ArrayList()
        for (member in members) {
            ids.add(member?.id.toString())
        }
        QiscusApi.getInstance().addParticipants(roomId, ids)
            .doOnNext { chatRoom: QiscusChatRoom? ->
                QiscusCore.getDataStore().addOrUpdate(chatRoom)
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ onSuccess.call(null) }, onError::call)
    }

    override fun removeParticipant(
        roomId: Long,
        members: List<QiscusAccount?>,
        onSuccess: Action<Void?>,
        onError: Action<Throwable?>
    ) {
        val ids: MutableList<String> = ArrayList()
        for (member in members) {
            ids.add(member?.id.toString())
        }
        QiscusApi.getInstance().addParticipants(roomId, ids)
            .doOnNext { chatRoom: QiscusChatRoom? ->
                QiscusCore.getDataStore().addOrUpdate(chatRoom)
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ onSuccess.call(null) }, onError::call)
    }
}