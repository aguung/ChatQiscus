package com.devfutech.chatqiscus.data.adapter

import android.annotation.SuppressLint
import android.net.Uri
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ClickableSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.core.util.PatternsCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.devfutech.chatqiscus.R
import com.devfutech.chatqiscus.databinding.*
import com.devfutech.chatqiscus.ui.view.QiscusProgressView
import com.devfutech.chatqiscus.utils.*
import com.qiscus.sdk.chat.core.QiscusCore
import com.qiscus.sdk.chat.core.data.model.QiscusComment
import com.qiscus.sdk.chat.core.data.remote.QiscusApi
import com.qiscus.sdk.chat.core.util.QiscusDateUtil
import org.json.JSONObject
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.io.File


class CommentAdapter : ListAdapter<QiscusComment, CommentAdapter.CommentViewHolder>(DiffCallback()) {
    companion object {
        const val TYPE_MY_TEXT = 1
        const val TYPE_OPPONENT_TEXT = 2
        const val TYPE_MY_IMAGE = 3
        const val TYPE_OPPONENT_IMAGE = 4
        const val TYPE_MY_FILE = 5
        const val TYPE_OPPONENT_FILE = 6
        const val TYPE_MY_REPLY = 7
        const val TYPE_OPPONENT_REPLY = 8
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CommentAdapter.CommentViewHolder {
        return when (viewType) {
            TYPE_MY_TEXT -> TextCommentViewHolder(
                ItemMyTextCommentBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )
            TYPE_OPPONENT_TEXT -> OpponentTextCommentViewHolder(
                ItemOpponentTextCommentBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )
            TYPE_MY_IMAGE -> ImageCommentViewHolder(
                ItemMyImageCommentBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )
            TYPE_OPPONENT_IMAGE -> OpponentImageCommentViewHolder(
                ItemOpponentImageCommentBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )
            TYPE_MY_FILE, TYPE_OPPONENT_FILE -> FileCommentViewHolder(
                ItemOpponentFileCommentBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )
            TYPE_MY_REPLY, TYPE_OPPONENT_REPLY -> ReplyCommentViewHolder(
                ItemOpponentReplyMcBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )
            else -> TextCommentViewHolder(
                ItemMyTextCommentBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )
        }
    }

    override fun onBindViewHolder(holder: CommentAdapter.CommentViewHolder, position: Int) {
        if (position == currentList.size - 1) {
            holder.setNeedToShowDate(true)
        } else {
            holder.setNeedToShowDate(
                !QiscusDateUtil.isDateEqualIgnoreTime(
                    getItem(position).time,
                    getItem(position + 1).time
                )
            )
        }
        with(getItem(position)) {
            when (holder) {
                is TextCommentViewHolder -> holder.bind(this)
                is OpponentTextCommentViewHolder -> holder.bind(this)
                is ImageCommentViewHolder -> holder.bind(this)
                is OpponentImageCommentViewHolder -> holder.bind(this)
                is FileCommentViewHolder -> holder.bind(this)
                is ReplyCommentViewHolder -> holder.bind(this)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        val comment: QiscusComment = getItem(position)
        return when (comment.type) {
            QiscusComment.Type.TEXT -> if (comment.isMyComment) TYPE_MY_TEXT else TYPE_OPPONENT_TEXT
            QiscusComment.Type.IMAGE -> if (comment.isMyComment) TYPE_MY_IMAGE else TYPE_OPPONENT_IMAGE
            QiscusComment.Type.FILE -> if (comment.isMyComment) TYPE_MY_FILE else TYPE_OPPONENT_FILE
            QiscusComment.Type.REPLY -> if (comment.isMyComment) TYPE_MY_REPLY else TYPE_OPPONENT_REPLY
            else -> if (comment.isMyComment) TYPE_MY_TEXT else TYPE_OPPONENT_TEXT
        }
    }


    open inner class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val avatar: ImageView = itemView.findViewById(R.id.avatar)
        private val sender: TextView? = itemView.findViewById(R.id.sender)
        private val date: TextView = itemView.findViewById(R.id.date)
        private val dateOfMessage: TextView = itemView.findViewById(R.id.dateOfMessage)
        private val state: ImageView? = itemView.findViewById(R.id.state)
        private val pendingStateColor: Int = ContextCompat.getColor(
            itemView.context,
            R.color.pending_message
        )
        private val readStateColor: Int = ContextCompat.getColor(
            itemView.context,
            R.color.read_message
        )
        private val failedStateColor: Int = ContextCompat.getColor(
            itemView.context,
            R.color.qiscus_red
        )

        open fun bind(comment: QiscusComment) {
            avatar.load(comment.senderAvatar) {
                crossfade(true)
                error(R.drawable.ic_qiscus_avatar)
                transformations(CircleCropTransformation())
            }
            sender?.text = comment.sender
            date.text = comment.time.getTimeStringFromDate()
            dateOfMessage.text = comment.time.toFullDate()
            renderState(comment)
        }

        open fun setNeedToShowDate(showDate: Boolean) {
            if (showDate) {
                dateOfMessage.visible()
            } else {
                dateOfMessage.gone()
            }
        }

        private fun renderState(comment: QiscusComment) {
            when (comment.state) {
                QiscusComment.STATE_PENDING, QiscusComment.STATE_SENDING -> {
                    state?.setColorFilter(pendingStateColor)
                    state?.setImageResource(R.drawable.ic_qiscus_info_time)
                }
                QiscusComment.STATE_ON_QISCUS -> {
                    state?.setColorFilter(pendingStateColor)
                    state?.setImageResource(R.drawable.ic_qiscus_sending)
                }
                QiscusComment.STATE_DELIVERED -> {
                    state?.setColorFilter(pendingStateColor)
                    state?.setImageResource(R.drawable.ic_qiscus_read)
                }
                QiscusComment.STATE_READ -> {
                    state?.setColorFilter(readStateColor)
                    state?.setImageResource(R.drawable.ic_qiscus_read)
                }
                QiscusComment.STATE_FAILED -> {
                    state?.setColorFilter(failedStateColor)
                    state?.setImageResource(R.drawable.ic_qiscus_sending_failed)
                }
            }
        }
    }

    inner class TextCommentViewHolder(binding: ItemMyTextCommentBinding) :
        CommentAdapter.CommentViewHolder(binding.root) {
        private val sender: TextView? = itemView.findViewById(R.id.sender)
        private val message: TextView = itemView.findViewById(R.id.message)
        private val dateOfMessage: TextView = itemView.findViewById(R.id.dateOfMessage)
        override fun bind(comment: QiscusComment) {
            super.bind(comment)
            message.text = comment.message
            val chatRoom = QiscusCore.getDataStore().getChatRoom(comment.roomId)
            if (chatRoom != null) {
                if (!chatRoom.isGroup) {
                    sender?.visibility = View.GONE
                } else {
                    sender?.visibility = View.VISIBLE
                }
            }
            dateOfMessage.text = comment.time.toFullDate()
        }

        override fun setNeedToShowDate(showDate: Boolean) {
            if (showDate) {
                dateOfMessage.visible()
            } else {
                dateOfMessage.gone()
            }
        }
    }

    inner class OpponentTextCommentViewHolder(binding: ItemOpponentTextCommentBinding) :
        CommentAdapter.CommentViewHolder(binding.root) {
        private val sender: TextView = itemView.findViewById(R.id.sender)
        private val message: TextView = itemView.findViewById(R.id.message)
        private val dateOfMessage: TextView = itemView.findViewById(R.id.dateOfMessage)
        override fun bind(comment: QiscusComment) {
            super.bind(comment)
            message.text = comment.message
            val chatRoom = QiscusCore.getDataStore().getChatRoom(comment.roomId)
            if (chatRoom != null) {
                if (!chatRoom.isGroup) {
                    sender.visibility = View.GONE
                } else {
                    sender.visibility = View.VISIBLE
                }
            }
            dateOfMessage.text = comment.time.toFullDate()
        }

        override fun setNeedToShowDate(showDate: Boolean) {
            if (showDate) {
                dateOfMessage.visible()
            } else {
                dateOfMessage.gone()
            }
        }
    }

    inner class ImageCommentViewHolder(binding: ItemMyImageCommentBinding) :
        CommentAdapter.CommentViewHolder(binding.root) {
        private val sender: TextView? = itemView.findViewById(R.id.sender)
        private val thumbnail: ImageView = itemView.findViewById(R.id.thumbnail)
        private val messageCaption: TextView = itemView.findViewById(R.id.messageCaption)
        private val dateOfMessage: TextView = itemView.findViewById(R.id.dateOfMessage)
        override fun bind(comment: QiscusComment) {
            super.bind(comment)
            try {
                val obj = JSONObject(comment.extraPayload)
                val url = obj.getString("url")
                val caption = obj.getString("caption")
                val filename = obj.getString("file_name")
                showSendingImage(url)

                if (caption.isEmpty()) {
                    messageCaption.visibility = View.GONE
                } else {
                    messageCaption.visibility = View.VISIBLE
                    messageCaption.text = caption
                }
                val chatRoom = QiscusCore.getDataStore().getChatRoom(comment.roomId)
                if (!chatRoom.isGroup) {
                    sender?.visibility = View.GONE
                } else {
                    sender?.visibility = View.VISIBLE
                }
                dateOfMessage.text = comment.time.toFullDate()
                thumbnail.setOnClickListener {
                    val localPath = QiscusCore.getDataStore().getLocalPath(comment.id)
                    if (localPath != null) {
                        Toast.makeText(
                            itemView.context,
                            "Image already in the gallery",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    } else {
                        downloadFile(comment, filename, url)
                    }
                }
            } catch (t: Throwable) {
                Log.e(
                    "SampleCore",
                    "Could not parse malformed JSON: \"" + comment.extraPayload + "\""
                )
            }
        }

        private fun downloadFile(
            qiscusComment: QiscusComment,
            fileName: String?,
            URLImage: String?
        ) {
            QiscusApi.getInstance()
                .downloadFile(URLImage, fileName) { }
                .doOnNext { file: File ->
                    // here we update the local path of file
                    QiscusCore.getDataStore()
                        .addOrUpdateLocalPath(
                            qiscusComment.roomId,
                            qiscusComment.id,
                            file.absolutePath
                        )
                    QiscusImageUtil.addImageToGallery(file)
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Toast.makeText(
                        itemView.context,
                        "success save image to gallery",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                ) { }
        }

        override fun setNeedToShowDate(showDate: Boolean) {
            if (showDate) {
                dateOfMessage.visible()
            } else {
                dateOfMessage.gone()
            }
        }

        private fun showSendingImage(url: String) {
            val localPath = File(url)
            showLocalImage(localPath)
        }

        private fun showLocalImage(localPath: File) {
            thumbnail.load(localPath.path) {
                crossfade(true)
                error(R.drawable.ic_qiscus_avatar)
                transformations(CircleCropTransformation())
            }
        }
    }

    inner class OpponentImageCommentViewHolder(binding: ItemOpponentImageCommentBinding) :
        CommentAdapter.CommentViewHolder(binding.root) {
        private val sender: TextView = itemView.findViewById(R.id.sender)
        private val thumbnail: ImageView = itemView.findViewById(R.id.thumbnail)
        private val messageCaption: TextView = itemView.findViewById(R.id.messageCaption)
        private val dateOfMessage: TextView = itemView.findViewById(R.id.dateOfMessage)
        override fun bind(comment: QiscusComment) {
            super.bind(comment)
            try {
                val obj = JSONObject(comment.extraPayload)
                val url = obj.getString("url")
                val caption = obj.getString("caption")
                val filename = obj.getString("file_name")
                showSendingImage(url)

                if (caption.isEmpty()) {
                    messageCaption.visibility = View.GONE
                } else {
                    messageCaption.visibility = View.VISIBLE
                    messageCaption.text = caption
                }
                val chatRoom = QiscusCore.getDataStore().getChatRoom(comment.roomId)
                if (!chatRoom.isGroup) {
                    sender.visibility = View.GONE
                } else {
                    sender.visibility = View.VISIBLE
                }
                dateOfMessage.text = comment.time.toFullDate()
                thumbnail.setOnClickListener {
                    val localPath = QiscusCore.getDataStore().getLocalPath(comment.id)
                    if (localPath != null) {
                        Toast.makeText(
                            itemView.context,
                            "Image already in the gallery",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    } else {
                        downloadFile(comment, filename, url)
                    }
                }
            } catch (t: Throwable) {
                Log.e(
                    "SampleCore",
                    "Could not parse malformed JSON: \"" + comment.extraPayload + "\""
                )
            }
        }

        private fun downloadFile(
            qiscusComment: QiscusComment,
            fileName: String?,
            URLImage: String?
        ) {
            QiscusApi.getInstance()
                .downloadFile(URLImage, fileName) { }
                .doOnNext { file: File ->
                    // here we update the local path of file
                    QiscusCore.getDataStore()
                        .addOrUpdateLocalPath(
                            qiscusComment.roomId,
                            qiscusComment.id,
                            file.absolutePath
                        )
                    QiscusImageUtil.addImageToGallery(file)
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Toast.makeText(
                        itemView.context,
                        "success save image to gallery",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                ) { }
        }

        override fun setNeedToShowDate(showDate: Boolean) {
            if (showDate) {
                dateOfMessage.visible()
            } else {
                dateOfMessage.gone()
            }
        }

        private fun showSendingImage(url: String) {
            val localPath = File(url)
            showLocalImage(localPath)
        }

        private fun showLocalImage(localPath: File) {
            thumbnail.load(localPath.path) {
                crossfade(true)
                error(R.drawable.ic_qiscus_avatar)
                transformations(CircleCropTransformation())
            }
        }
    }

    inner class FileCommentViewHolder(binding: ItemOpponentFileCommentBinding) :
        CommentAdapter.CommentViewHolder(binding.root),
        QiscusComment.ProgressListener, QiscusComment.DownloadingListener {
        private val sender: TextView = itemView.findViewById(R.id.sender)
        private val dateOfMessage: TextView = itemView.findViewById(R.id.dateOfMessage)
        private val fileName: TextView = itemView.findViewById(R.id.file_name)
        private val progress: QiscusProgressView = itemView.findViewById(R.id.progress)
        private val icFile: ImageView = itemView.findViewById(R.id.ic_file)
        override fun bind(comment: QiscusComment) {
            super.bind(comment)
            comment.setProgressListener(this)
            comment.setDownloadingListener(this)
            val chatRoom = QiscusCore.getDataStore().getChatRoom(comment.roomId)
            if (!chatRoom.isGroup) {
                sender.gone()
            } else {
                sender.visible()
            }
            try {
                val obj = JSONObject(comment.extraPayload)
                val url = obj.getString("url")
                val filename = obj.getString("file_name")
                fileName.text = filename
                dateOfMessage.text = comment.time.toFullDate()
                fileName.setOnClickListener {
                    val localPath = QiscusCore.getDataStore().getLocalPath(comment.id)
                    if (localPath != null) {
                        QiscusImageUtil.addImageToGallery(localPath)
                        Toast.makeText(itemView.context, "File already save", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        downloadFile(comment, filename, url)
                    }
                }
            } catch (t: Throwable) {
                Log.e(
                    "SampleCore",
                    "Could not parse malformed JSON: \"" + comment.extraPayload + "\""
                )
            }
        }

        override fun setNeedToShowDate(showDate: Boolean) {
            if (showDate) {
                dateOfMessage.visible()
            } else {
                dateOfMessage.gone()
            }
        }

        override fun onProgress(qiscusComment: QiscusComment?, percentage: Int) {
            progress.setProgress(percentage)
            icFile.gone()
            if (percentage == 100) {
                progress.setVisibility(View.GONE)
                icFile.visible()
            } else {
                progress.setVisibility(View.VISIBLE)
                icFile.gone()
            }
        }

        override fun onDownloading(qiscusComment: QiscusComment?, downloading: Boolean) {
            progress.setVisibility(if (downloading) View.VISIBLE else View.GONE)
        }

        private fun downloadFile(
            qiscusComment: QiscusComment,
            fileName: String?,
            URLFile: String?
        ) {
            QiscusApi.getInstance()
                .downloadFile(URLFile, fileName) { }
                .doOnNext { file: File ->
                    QiscusCore.getDataStore()
                        .addOrUpdateLocalPath(
                            qiscusComment.roomId,
                            qiscusComment.id,
                            file.absolutePath
                        )
                    QiscusImageUtil.addImageToGallery(file)
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        Toast.makeText(itemView.context, "Success save file", Toast.LENGTH_SHORT)
                            .show()
                    }
                ) { }
        }
    }


    inner class ReplyCommentViewHolder(binding: ItemOpponentReplyMcBinding) :
        CommentAdapter.CommentViewHolder(binding.root) {
        private val qiscusAccount = QiscusCore.getQiscusAccount()
        private val sender: TextView = itemView.findViewById(R.id.sender)
        private val dateOfMessage: TextView = itemView.findViewById(R.id.dateOfMessage)
        private val message: TextView = itemView.findViewById(R.id.message)
        private val originComment: TextView = itemView.findViewById(R.id.origin_comment)
        private val icon: ImageView = itemView.findViewById(R.id.icon)
        private val originImage: ImageView = itemView.findViewById(R.id.origin_image)
        override fun bind(comment: QiscusComment) {
            super.bind(comment)
            val origin = comment.replyTo
            if (qiscusAccount.email == origin.senderEmail) {
                sender.text = itemView.resources.getString(R.string.you)
            } else {
                sender.text = origin.sender
            }
            originComment.text = origin.message
            message.text = comment.message
            icon.visibility = View.VISIBLE
            setUpLinks()
            when (origin.type) {
                QiscusComment.Type.TEXT -> {
                    originImage.visible()
                    icon.visibility = View.GONE
                }
                QiscusComment.Type.IMAGE -> {
                    originImage.visible()
                    icon.setImageResource(R.drawable.ic_gallery)
                    if (origin.caption === "") {
                        originComment.text = itemView.resources.getString(R.string.image)
                    } else {
                        originComment.text = origin.caption
                    }
                    originImage.load(origin.attachmentUri.path!!) {
                        crossfade(true)
                        error(R.drawable.ic_qiscus_avatar)
                        transformations(CircleCropTransformation())
                    }
                }
                QiscusComment.Type.FILE -> {
                    originImage.gone()
                    icon.visibility = View.VISIBLE
                    originComment.text = origin.attachmentName
                    icon.setImageResource(R.drawable.ic_file)
                }
                else -> {
                    originImage.gone()
                    icon.gone()
                    originComment.text = origin.message
                }
            }
        }

        @SuppressLint("RestrictedApi")
        private fun setUpLinks() {
            val messageData = message.text.toString()
            val matcher = PatternsCompat.AUTOLINK_WEB_URL.matcher(messageData)
            while (matcher.find()) {
                val start = matcher.start()
                if (start > 0 && messageData[start - 1] == '@') {
                    continue
                }
                val end = matcher.end()
                clickify(start, end, object : ClickSpan.OnClickListener {
                    override fun onClick() {
                        var url = messageData.substring(start, end)
                        if (!url.startsWith("http")) {
                            url = "http://$url"
                        }
                        val params = CustomTabColorSchemeParams.Builder()
                            .setToolbarColor(
                                ContextCompat.getColor(
                                    QiscusCore.getApps(),
                                    R.color.white
                                )
                            )
                            .build()
                        CustomTabsIntent.Builder()
                            .setColorSchemeParams(CustomTabsIntent.COLOR_SCHEME_DARK, params)
                            .setShowTitle(true)
                            .setShareState(CustomTabsIntent.SHARE_STATE_ON)
                            .setUrlBarHidingEnabled(true)
                            .build()
                            .launchUrl(message.context, Uri.parse(url))
                    }

                })
            }
        }

        private fun clickify(start: Int, end: Int, listener: ClickSpan.OnClickListener) {
            val text = message.text
            val span = ClickSpan(listener)
            if (start == -1) {
                return
            }
            if (text is Spannable) {
                text.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            } else {
                val s = SpannableString.valueOf(text)
                s.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                message.text = s
            }
        }

        override fun setNeedToShowDate(showDate: Boolean) {
            if (showDate) {
                dateOfMessage.visible()
            } else {
                dateOfMessage.gone()
            }
        }
    }

    class ClickSpan(private val listener: OnClickListener?) : ClickableSpan() {
        override fun onClick(widget: View) {
            listener?.onClick()
        }

        interface OnClickListener {
            fun onClick()
        }
    }

    interface OnItemClickListener {
        fun onItemClick(qiscusComment: QiscusComment)
        fun onItemLongClick(qiscusComment: QiscusComment)
    }

    class DiffCallback : DiffUtil.ItemCallback<QiscusComment>() {
        override fun areItemsTheSame(oldItem: QiscusComment, newItem: QiscusComment) =
            oldItem.time == newItem.time

        override fun areContentsTheSame(oldItem: QiscusComment, newItem: QiscusComment) =
            oldItem == newItem
    }

}