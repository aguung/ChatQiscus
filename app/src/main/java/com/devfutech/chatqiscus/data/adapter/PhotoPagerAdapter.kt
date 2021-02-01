package com.devfutech.chatqiscus.data.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.devfutech.chatqiscus.ui.sendpohoto.ContentPhotoFragment
import com.qiscus.sdk.chat.core.data.model.QiscusPhoto

class PhotoPagerAdapter(
    fragment: Fragment,
    val content: List<QiscusPhoto?>
) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = content.size

    override fun createFragment(position: Int): Fragment {
        return ContentPhotoFragment(content[0])
    }
}