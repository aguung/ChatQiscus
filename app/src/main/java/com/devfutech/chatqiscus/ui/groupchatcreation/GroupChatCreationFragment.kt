package com.devfutech.chatqiscus.ui.groupchatcreation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.devfutech.chatqiscus.R
import com.devfutech.chatqiscus.data.adapter.ContactAdapter
import com.devfutech.chatqiscus.databinding.GroupChatCreationFragmentBinding
import com.devfutech.chatqiscus.ui.base.BaseFragment
import com.devfutech.chatqiscus.utils.ResultOf
import com.devfutech.chatqiscus.utils.back
import com.devfutech.chatqiscus.utils.toast
import com.qiscus.sdk.chat.core.data.model.QiscusAccount
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GroupChatCreationFragment : BaseFragment(), ContactAdapter.OnItemClickListener {
    private lateinit var binding: GroupChatCreationFragmentBinding
    private val viewModels by viewModels<GroupChatCreationViewModel>()
    private val args: GroupChatCreationFragmentArgs by navArgs()
    private lateinit var contactAdapter: ContactAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = GroupChatCreationFragmentBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupAction()
        setupObservable()
    }

    private fun setupUI() {
        contactAdapter = ContactAdapter(this)
        binding.apply {
            rvParticipant.apply {
                layoutManager = LinearLayoutManager(
                    requireContext(),
                    LinearLayoutManager.VERTICAL,
                    false
                )
                adapter = contactAdapter
            }
        }
        contactAdapter.submitList(args.participants?.toMutableList())
    }

    private fun setupAction() {
        binding.apply {
            back.back()
            imgDone.setOnClickListener { proceedCreateGroup() }
        }
    }

    private fun setupObservable() {
        viewModels.chatRoom.observe(viewLifecycleOwner, { result ->
            when (result) {
                is ResultOf.Success -> {
                    dialogProgress.dismiss()
                    findNavController().navigate(
                        GroupChatCreationFragmentDirections.actionGroupChatCreationFragmentToChatRoomFragment(
                            result.value
                        )
                    )
                }
                is ResultOf.Failure -> {
                    dialogProgress.dismiss()
                    result.throwable?.printStackTrace()
                    requireContext().toast(result.throwable?.message)
                }
                else -> dialogProgress.show()
            }
        })
    }

    private fun proceedCreateGroup() {
        val groupName: String = binding.groupNameInput.text.toString()
        val groupNameInputted = groupName.trim { it <= ' ' }.isNotEmpty()
        if (groupNameInputted) {
            viewModels.createGroup(name = groupName, members = contactAdapter.currentList)
        } else {
            binding.groupNameInput.error = resources.getString(
                R.string.empty_fields,
                resources.getString(R.string.group_name)
            )
        }
    }

    override fun onItemClick(qiscusAccount: QiscusAccount) {

    }

    override fun onItemLongClick(qiscusAccount: QiscusAccount, removed: Boolean) {

    }
}