package com.devfutech.chatqiscus.ui.sendpohoto

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import coil.load
import com.devfutech.chatqiscus.R
import com.devfutech.chatqiscus.databinding.ContentPhotoFragmentBinding
import com.qiscus.sdk.chat.core.data.model.QiscusPhoto
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ContentPhotoFragment(private val qiscusPhoto: QiscusPhoto?) : Fragment() {

    private lateinit var binding:ContentPhotoFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ContentPhotoFragmentBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.imgContentTake.load(qiscusPhoto?.photoFile){
            crossfade(true)
            error(R.drawable.logo)
        }
    }
}