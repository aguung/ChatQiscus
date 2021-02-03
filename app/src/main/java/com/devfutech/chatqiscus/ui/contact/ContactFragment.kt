package com.devfutech.chatqiscus.ui.contact


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.devfutech.chatqiscus.data.adapter.ContactAdapter
import com.devfutech.chatqiscus.databinding.ContactFragmentBinding
import com.devfutech.chatqiscus.ui.base.BaseFragment
import com.devfutech.chatqiscus.utils.*
import com.qiscus.sdk.chat.core.data.model.QiscusAccount
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ContactFragment : BaseFragment(), ContactAdapter.OnItemClickListener {

    private lateinit var binding: ContactFragmentBinding
    private val viewModels by viewModels<ContactViewModel>()
    private lateinit var contactAdapter: ContactAdapter
    private val selectedContact = mutableListOf<QiscusAccount>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ContactFragmentBinding.inflate(layoutInflater)
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
            rvContact.apply {
                layoutManager = LinearLayoutManager(
                    requireContext(),
                    LinearLayoutManager.VERTICAL,
                    false
                )
                adapter = contactAdapter
            }
            imgNext.invisible()
        }
    }

    private fun setupAction() {
        binding.apply {
            back.back()
            imgNext.setOnClickListener {
                if (selectedContact.size > 0)
                    findNavController().navigate(
                        ContactFragmentDirections.actionContactFragmentToGroupChatCreationFragment(
                            selectedContact.toTypedArray()
                        )
                    )
            }
        }
    }

    private fun setupObservable() {
        viewModels.contact.observe(viewLifecycleOwner, { result ->
            when (result) {
                is ResultOf.Success -> {
                    dialogProgress.dismiss()
                    result.value?.let { contactAdapter.submitList(it) }
                }
                is ResultOf.Failure -> {
                    dialogProgress.dismiss()
                    requireContext().toast(result.throwable?.message)
                }
                else -> dialogProgress.show()
            }
        })
        viewModels.room.observe(viewLifecycleOwner, {
            it.getContentIfNotHandled()?.let { result ->
                when (result) {
                    is ResultOf.Success -> {
                        dialogProgress.dismiss()
                        findNavController().navigate(
                            ContactFragmentDirections.actionContactFragmentToChatRoomFragment(
                                qiscusChatRoom = result.value
                            )
                        )
                    }
                    is ResultOf.Failure -> {
                        requireContext().toast(result.throwable?.message)
                    }
                    else -> dialogProgress.show()
                }
            }
        })
    }


    override fun onItemClick(qiscusAccount: QiscusAccount) {
        viewModels.createRoom(qiscusAccount)
    }

    override fun onItemLongClick(qiscusAccount: QiscusAccount, removed: Boolean) {
        if (removed) {
            selectedContact.remove(qiscusAccount)
        } else {
            selectedContact.add(qiscusAccount)
        }
        if (selectedContact.size > 0) {
            binding.imgNext.visible()
        } else {
            binding.imgNext.invisible()
        }
    }

}