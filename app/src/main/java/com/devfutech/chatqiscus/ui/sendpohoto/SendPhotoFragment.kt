package com.devfutech.chatqiscus.ui.sendpohoto

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import com.devfutech.chatqiscus.data.adapter.PhotoPagerAdapter
import com.devfutech.chatqiscus.databinding.SendPhotoFragmentBinding
import com.devfutech.chatqiscus.ui.chatroom.ChatRoomFragment
import com.devfutech.chatqiscus.utils.DepthPageTransformer
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.tabs.TabLayoutMediator
import com.qiscus.sdk.chat.core.data.model.QiscusChatRoom
import com.qiscus.sdk.chat.core.data.model.QiscusPhoto
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@AndroidEntryPoint
class SendPhotoFragment(val qiscusChatRoom: QiscusChatRoom?,val qiscusPhotos:ArrayList<QiscusPhoto>) : BottomSheetDialogFragment() {
    companion object {
        const val PHOTO = "photo"
    }
    private lateinit var binding: SendPhotoFragmentBinding
    private lateinit var mBehavior: BottomSheetBehavior<*>

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        binding = SendPhotoFragmentBinding.inflate(layoutInflater)
        dialog.setContentView(binding.root)

        setupUI()
        setupAction()

        return dialog
    }

    private fun setupUI() {
        val pagerAdapter = PhotoPagerAdapter(this, qiscusPhotos)
        mBehavior = BottomSheetBehavior.from(binding.root.parent as View)
        mBehavior.state = BottomSheetBehavior.STATE_EXPANDED

        binding.apply {
            tvTitle.text = qiscusChatRoom?.name
            viewPagerImage.adapter = pagerAdapter
            viewPagerImage.setPageTransformer(DepthPageTransformer())
            TabLayoutMediator(tabLayout, viewPagerImage)
            { _, _ -> }.attach()
            lytSpacer.minimumHeight = Resources.getSystem().displayMetrics.heightPixels
        }
    }

    private fun setupAction() {
        binding.apply {
            back.setOnClickListener {
                dismiss()
            }
            buttonSend.setOnClickListener {
                sendPhoto()
            }
            fieldMessage.setOnEditorActionListener(OnEditorActionListener { _: TextView?, actionId: Int, _: KeyEvent? ->
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    sendPhoto()
                    return@OnEditorActionListener true
                }
                false
            })
        }
    }

    private fun sendPhoto() {
        targetFragment?.onActivityResult(
            targetRequestCode,
            Activity.RESULT_OK,
            Intent().apply {
                putExtra(ChatRoomFragment.CAPTION, binding.fieldMessage.text.toString())
                putParcelableArrayListExtra(PHOTO, qiscusPhotos)
            }
        )
        dismiss()
    }

}