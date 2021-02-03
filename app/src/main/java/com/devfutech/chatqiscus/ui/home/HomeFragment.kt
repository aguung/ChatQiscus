package com.devfutech.chatqiscus.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import coil.transform.CircleCropTransformation
import com.devfutech.chatqiscus.R
import com.devfutech.chatqiscus.data.adapter.ChatRoomAdapter
import com.devfutech.chatqiscus.databinding.HomeFragmentBinding
import com.devfutech.chatqiscus.ui.base.BaseFragment
import com.devfutech.chatqiscus.utils.*
import com.qiscus.sdk.chat.core.QiscusCore
import com.qiscus.sdk.chat.core.data.model.QiscusChatRoom
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : BaseFragment(), ChatRoomAdapter.OnItemClickListener {

    private lateinit var binding: HomeFragmentBinding
    private val viewModels by viewModels<HomeViewModel>()
    private lateinit var chatRoomAdapter: ChatRoomAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =  HomeFragmentBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupAction()
        setupObservable()
    }

    private fun setupUI() {
        chatRoomAdapter = ChatRoomAdapter(this@HomeFragment)
        binding.apply {
            avatarProfil.load(QiscusCore.getQiscusAccount().avatar){
                crossfade(true)
                error(R.drawable.ic_qiscus_avatar)
                transformations(CircleCropTransformation())
            }
            rvRoom.apply {
                layoutManager = LinearLayoutManager(
                    requireContext(),
                    LinearLayoutManager.VERTICAL,
                    false
                )
                adapter = chatRoomAdapter
            }
        }
    }

    private fun setupAction() {
        binding.apply {
            createChat.setOnClickListener {
                findNavController().navigate(R.id.action_homeFragment_to_contactFragment)
            }
            btnStartChat.setOnClickListener {
                findNavController().navigate(R.id.action_homeFragment_to_contactFragment)
            }
        }
    }

    private fun setupObservable() {
        viewModels.room.observe(viewLifecycleOwner, { result ->
            when (result) {
                is ResultOf.Success -> {
                    dialogProgress.dismiss()
                    result.value?.let { showChatRooms(it) }
                }
                is ResultOf.Failure -> {
                    dialogProgress.dismiss()
                    requireContext().toast(result.throwable?.message)
                }
                else -> dialogProgress.show()
            }
        })
    }

    private fun showChatRooms(chatRooms: List<QiscusChatRoom?>) {
        binding.apply {
            if (chatRooms.isEmpty()) {
                rvRoom.gone()
                linEmptyChatRooms.visible()
            } else {
                linEmptyChatRooms.gone()
                rvRoom.visible()
                chatRoomAdapter.submitList(chatRooms)
            }
        }
    }

    override fun onItemClick(qiscusChatRoom: QiscusChatRoom) {
        findNavController().navigate(
            HomeFragmentDirections.actionHomeFragmentToChatRoomFragment(
                qiscusChatRoom
            )
        )
    }

}