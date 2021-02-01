package com.devfutech.chatqiscus.data.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.devfutech.chatqiscus.R
import com.devfutech.chatqiscus.databinding.ItemContactBinding
import com.devfutech.chatqiscus.utils.gone
import com.devfutech.chatqiscus.utils.visible
import com.qiscus.sdk.chat.core.data.model.QiscusAccount

class ContactAdapter(val listener: OnItemClickListener) :
    ListAdapter<QiscusAccount, ContactAdapter.ContactViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding =
            ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ContactViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val currentItem = getItem(position)
        holder.bind(qiscusAccount = currentItem)
    }

    inner class ContactViewHolder(private val binding: ItemContactBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(qiscusAccount: QiscusAccount) {
            binding.apply {
                avatarProfil.load(qiscusAccount.avatar){
                    crossfade(true)
                    error(R.drawable.ic_qiscus_avatar)
                    transformations(CircleCropTransformation())
                }
                name.text = qiscusAccount.username
                root.setOnClickListener {
                    listener.onItemClick(qiscusAccount = qiscusAccount)
                }
                root.setOnLongClickListener {
                    if (imgViewCheck.visibility == View.GONE){
                        listener.onItemLongClick(qiscusAccount = qiscusAccount, removed = false)
                        imgViewCheck.visible()
                        container.setBackgroundColor(Color.GRAY)
                    }else{
                        listener.onItemLongClick(qiscusAccount = qiscusAccount, removed = true)
                        imgViewCheck.gone()
                        container.setBackgroundColor(Color.WHITE)
                    }
                    return@setOnLongClickListener true
                }
            }
        }
    }

    interface OnItemClickListener {
        fun onItemClick(qiscusAccount: QiscusAccount)
        fun onItemLongClick(qiscusAccount: QiscusAccount, removed: Boolean)
    }

    class DiffCallback : DiffUtil.ItemCallback<QiscusAccount>() {
        override fun areItemsTheSame(oldItem: QiscusAccount, newItem: QiscusAccount) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: QiscusAccount, newItem: QiscusAccount) =
            oldItem == newItem
    }
}