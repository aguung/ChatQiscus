package com.devfutech.chatqiscus.data.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.devfutech.chatqiscus.R
import com.devfutech.chatqiscus.databinding.ItemChatRoomBinding
import com.devfutech.chatqiscus.utils.getLastMessageTimestamp
import com.devfutech.chatqiscus.utils.gone
import com.devfutech.chatqiscus.utils.visible
import com.qiscus.sdk.chat.core.data.model.QiscusChatRoom
import com.qiscus.sdk.chat.core.data.model.QiscusComment

class ChatRoomAdapter(val listener: OnItemClickListener) :
    ListAdapter<QiscusChatRoom, ChatRoomAdapter.ChatRoomViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatRoomViewHolder {
        val binding =
            ItemChatRoomBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatRoomViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatRoomViewHolder, position: Int) {
        val currentItem = getItem(position)
        holder.bind(qiscusChatRoom = currentItem)
    }

    inner class ChatRoomViewHolder(private val binding: ItemChatRoomBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(qiscusChatRoom: QiscusChatRoom) {
            binding.apply {
                roomName.text = qiscusChatRoom.name
                avatar.load(qiscusChatRoom.avatarUrl){
                    crossfade(true)
                    error(R.drawable.ic_qiscus_avatar)
                    transformations(CircleCropTransformation())
                }
                val lastComment: QiscusComment? = qiscusChatRoom.lastComment
                if (lastComment != null && lastComment.id > 0) {
                    if (lastComment.sender != null) {
                        var lastMessageText: String? =
                            if (lastComment.isMyComment) "You: " else lastComment.sender.split(" ")
                                .toTypedArray()[0] + ": "
                        lastMessageText += if (qiscusChatRoom.lastComment.type == QiscusComment.Type.IMAGE) "\uD83D\uDCF7 send an image" else lastComment.message
                        tvLastMessage.text = lastMessageText
                    } else {
                        var lastMessageText: String? = ""
                        lastMessageText += if (qiscusChatRoom.lastComment.type == QiscusComment.Type.IMAGE) "\uD83D\uDCF7 send an image" else lastComment.message
                        tvLastMessage.text = lastMessageText
                    }
                    tvTime.text = lastComment.time.getLastMessageTimestamp()
                } else {
                    tvLastMessage.text = ""
                    tvTime.text = ""
                }
                tvUnreadCount.text = String.format("%d", qiscusChatRoom.unreadCount)
                if (qiscusChatRoom.unreadCount == 0) {
                    layoutUnreadCount.gone()
                } else {
                    layoutUnreadCount.visible()
                }
                root.setOnClickListener {
                    listener.onItemClick(qiscusChatRoom = qiscusChatRoom)
                }
            }
        }
    }

    interface OnItemClickListener {
        fun onItemClick(qiscusChatRoom: QiscusChatRoom)
    }

    class DiffCallback : DiffUtil.ItemCallback<QiscusChatRoom>() {
        override fun areItemsTheSame(oldItem: QiscusChatRoom, newItem: QiscusChatRoom) =
            oldItem.lastComment == newItem.lastComment

        override fun areContentsTheSame(oldItem: QiscusChatRoom, newItem: QiscusChatRoom) =
            oldItem == newItem

    }


}