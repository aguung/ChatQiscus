package com.devfutech.chatqiscus.ui.chatroom

import android.app.Activity
import android.content.*
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import coil.load
import coil.transform.CircleCropTransformation
import com.devfutech.chatqiscus.R
import com.devfutech.chatqiscus.data.adapter.CommentAdapter
import com.devfutech.chatqiscus.databinding.ChatRoomFragmentBinding
import com.devfutech.chatqiscus.ui.base.BaseFragment
import com.devfutech.chatqiscus.ui.sendpohoto.SendPhotoFragment
import com.devfutech.chatqiscus.ui.view.QiscusChatScrollListener
import com.devfutech.chatqiscus.utils.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.qiscus.sdk.chat.core.QiscusCore
import com.qiscus.sdk.chat.core.data.local.QiscusCacheManager
import com.qiscus.sdk.chat.core.data.model.QiscusComment
import com.qiscus.sdk.chat.core.data.model.QiscusPhoto
import com.qiscus.sdk.chat.core.data.model.QiscusRoomMember
import com.qiscus.sdk.chat.core.data.remote.QiscusPusherApi
import com.qiscus.sdk.chat.core.event.QiscusCommentReceivedEvent
import com.qiscus.sdk.chat.core.util.QiscusFileUtil
import dagger.hilt.android.AndroidEntryPoint
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import rx.Observable
import java.io.File
import java.io.IOException
import java.util.*


@AndroidEntryPoint
open class ChatRoomFragment : BaseFragment(), CommentAdapter.OnItemClickListener,
    QiscusPermissionsUtil.PermissionCallbacks,
    QiscusChatScrollListener.Listener {

    companion object {
        const val RC_CAMERA_PERMISSION = 128
        const val RC_FILE_PERMISSION = 130
        const val TAKE_PICTURE_REQUEST = 3
        const val SEND_PICTURE_CONFIRMATION_REQUEST = 4
        const val REQUEST_PICK_IMAGE = 1
        const val REQUEST_FILE_PERMISSION = 2
        const val CAPTION = "caption"
    }

    private lateinit var binding: ChatRoomFragmentBinding
    private val viewModels by viewModels<ChatRoomViewModel>()
    private val args: ChatRoomFragmentArgs by navArgs()
    private var opponentEmail: String? = null
    private var selectedComment: QiscusComment? = null
    private lateinit var commentAdapter: CommentAdapter
    private val filePermission = arrayOf(
        "android.permission.WRITE_EXTERNAL_STORAGE",
        "android.permission.READ_EXTERNAL_STORAGE"
    )
    private val cameraPermission = arrayOf(
        "android.permission.CAMERA",
        "android.permission.WRITE_EXTERNAL_STORAGE",
        "android.permission.READ_EXTERNAL_STORAGE"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ChatRoomFragmentBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupAction()
        setupViewModel()
        setupObservable()
        getOpponentIfNotGroupEmail()
        QiscusPusherApi.getInstance().subscribeUserOnlinePresence(opponentEmail)
        QiscusPusherApi.getInstance().subscribeChatRoom(args.qiscusChatRoom)
    }

    override fun onDestroy() {
        super.onDestroy()
        notifyLatestRead()
        viewModels.detachView()
        EventBus.getDefault().unregister(this@ChatRoomFragment)
        QiscusPusherApi.getInstance().unsubscribeUserOnlinePresence(opponentEmail)
        QiscusPusherApi.getInstance().unsubsribeChatRoom(args.qiscusChatRoom)
    }

    override fun onResume() {
        super.onResume()
        EventBus.getDefault().register(this@ChatRoomFragment)
        if (commentAdapter.itemCount > 0) {
            QiscusCacheManager.getInstance().setLastChatActivity(true, args.qiscusChatRoom?.id!!)
            notifyLatestRead()
        }
    }

    override fun onPause() {
        super.onPause()
        EventBus.getDefault().unregister(this@ChatRoomFragment)
        if (commentAdapter.itemCount > 0) {
            QiscusCacheManager.getInstance().setLastChatActivity(false, args.qiscusChatRoom?.id!!)
        }
    }


    private fun setupUI() {
        commentAdapter = CommentAdapter()
        val linearLayoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.VERTICAL,
            false
        )
        binding.apply {
            avatarProfil.load(args.qiscusChatRoom?.avatarUrl!!) {
                crossfade(true)
                error(R.drawable.ic_qiscus_avatar)
                transformations(CircleCropTransformation())
            }
            tvTitle.text = args.qiscusChatRoom?.name
            rvChat.apply {
                layoutManager = linearLayoutManager
                addOnScrollListener(
                    QiscusChatScrollListener(
                        linearLayoutManager = LinearLayoutManager(
                            requireContext()
                        ), listener = this@ChatRoomFragment
                    )
                )
                adapter = commentAdapter
            }
        }
        commentAdapter.registerAdapterDataObserver(object : AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                val friendlyMessageCount: Int = commentAdapter.itemCount
                val lastVisiblePosition: Int =
                    linearLayoutManager.findLastCompletelyVisibleItemPosition()
                if (lastVisiblePosition == -1 ||
                    positionStart >= friendlyMessageCount - 1 &&
                    lastVisiblePosition == positionStart - 1
                ) {
                    binding.rvChat.scrollToPosition(positionStart)
                }
            }
        })
    }

    @Subscribe
    fun onReceiveComment(event: QiscusCommentReceivedEvent) {
        event.qiscusComment
        viewModels.setCommentLive(event.qiscusComment)
    }

    private fun setupAction() {
        binding.apply {
            back.back()
            toolbarSelectedComment.btnActionCopy.setOnClickListener { copyComment() }
            toolbarSelectedComment.btnActionDelete.setOnClickListener { deleteComment() }
            toolbarSelectedComment.btnActionReply.setOnClickListener { replyComment() }
            toolbarSelectedComment.btnActionReplyCancel.setOnClickListener { clearSelectedComment() }

            buttonSend.setOnClickListener {
                if (!TextUtils.isEmpty(fieldMessage.text)) {
                    if (rootViewSender.visibility == View.VISIBLE) {
                        if (selectedComment != null) {
                            viewModels.sendReplyComment(
                                fieldMessage.text.toString(),
                                selectedComment
                            )
                        }
                        rootViewSender.gone()
                        selectedComment = null
                    } else {
                        viewModels.sendComment(fieldMessage.text.toString())
                    }
                    fieldMessage.setText("")
                }
            }
            btnCancelReply.setOnClickListener { rootViewSender.gone() }
            buttonAddImage.setOnClickListener {
                if (linAttachment.isShown) {
                    linAttachment.gone()
                } else {
                    linAttachment.visible()
                }
            }
            linTakePhoto.setOnClickListener {
                if (QiscusPermissionsUtil.hasPermissions(
                        activity,
                        *cameraPermission
                    )
                ) {
                    val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    var photoFile: File? = null
                    try {
                        photoFile = QiscusImageUtil.createImageFile()
                    } catch (ex: IOException) {
                        requireContext().toast(resources.getString(R.string.qiscus_chat_error_failed_write))
                    } catch (ex: ActivityNotFoundException) {
                        requireContext().toast(resources.getString(R.string.application_not_found))
                    }
                    if (photoFile != null) {
                        intent.putExtra(
                            MediaStore.EXTRA_OUTPUT,
                            FileProvider.getUriForFile(
                                requireActivity(),
                                requireActivity().packageName + ".provider",
                                photoFile
                            )
                        )
                        startActivityForResult(intent, TAKE_PICTURE_REQUEST)
                    }
                    binding.linAttachment.gone()
                } else {
                    requestCameraPermission()
                }
            }
            linImageGallery.setOnClickListener {
                if (QiscusPermissionsUtil.hasPermissions(
                        activity,
                        *filePermission
                    )
                ) {
                    pickImage()
                    binding.linAttachment.gone()
                } else {
                    requestFilePermission()
                }
            }
            linCancel.setOnClickListener { linAttachment.gone() }
        }
    }

    private fun setupViewModel() {
        viewModels.startChat(args.qiscusChatRoom)
        viewModels.loadComments(20)
    }

    private fun setupObservable() {
        viewModels.listQiscusComment.observe(viewLifecycleOwner, { result ->
            when (result) {
                is ResultOf.Success -> result.value.let {
                    dialogProgress.dismiss()
                    if (it.isEmpty()) {
                        binding.emptyChat.visible()
                        binding.rvChat.gone()
                    } else {
                        binding.emptyChat.gone()
                        binding.rvChat.visible()
                        commentAdapter.submitList(it.reversed())
                        binding.rvChat.smoothScrollToPosition(commentAdapter.itemCount)
                    }
                }
                is ResultOf.Failure -> {
                    dialogProgress.dismiss()
                    requireContext().toast(result.throwable?.message)
                }
                else -> dialogProgress.show()
            }
        })

        viewModels.qiscusComment.observe(viewLifecycleOwner, { result ->
            when (result) {
                is ResultOf.Success -> result.value.let {
                    dialogProgress.dismiss()
                    val list: MutableList<QiscusComment> =
                        commentAdapter.currentList.toMutableList()
                    list.add(it)
                    if (list.size == 1) {
                        binding.emptyChat.gone()
                        binding.rvChat.visible()
                    }
                    commentAdapter.submitList(list)
                }
                is ResultOf.Failure -> {
                    dialogProgress.dismiss()
                    requireContext().toast(result.throwable?.message)
                }
                else -> println("Nothing")
            }
        })
    }

    private fun getOpponentIfNotGroupEmail() {
        if (!args.qiscusChatRoom?.isGroup!!) {
            opponentEmail = Observable.from(args.qiscusChatRoom?.member)
                .map { obj: QiscusRoomMember -> obj.email }
                .filter { email: String ->
                    email != QiscusCore.getQiscusAccount().email
                }
                .first()
                .toBlocking()
                .single()
        }
    }

    private fun copyComment() {
        clearSelectedComment()
        val commentSelected = selectedComment
        val textCopied: String?
        textCopied = when (commentSelected?.type) {
            QiscusComment.Type.FILE -> {
                commentSelected.attachmentName
            }
            QiscusComment.Type.IMAGE -> {
                commentSelected.caption
            }
            else -> {
                commentSelected?.message
            }
        }
        val clipboard =
            requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(
            "Message",
            textCopied
        )
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "messages copied!", Toast.LENGTH_SHORT).show()
    }

    private fun clearSelectedComment() {
        binding.toolbarSelectedComment.container.visible()
    }

    private fun replyComment() {
        clearSelectedComment()
        if (selectedComment == null) {
            binding.rootViewSender.gone()
        } else {
            binding.rootViewSender.visible()
        }
        if (selectedComment != null) {
            bindReplyView(selectedComment)
        }
    }

    private fun deleteComment() {
        clearSelectedComment()
        val selectedComment = selectedComment
        if (selectedComment != null) {
            showDialogDeleteComment(selectedComment)
        }
    }

    private fun requestCameraPermission() {
        if (!QiscusPermissionsUtil.hasPermissions(activity, *cameraPermission)) {
            QiscusPermissionsUtil.requestPermissions(
                this, getString(R.string.qiscus_permission_request_title),
                RC_CAMERA_PERMISSION, cameraPermission
            )
        }
    }

    private fun pickImage() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_PICK_IMAGE)
    }


    private fun requestFilePermission() {
        if (!QiscusPermissionsUtil.hasPermissions(activity, *filePermission)) {
            QiscusPermissionsUtil.requestPermissions(
                this, getString(R.string.qiscus_permission_request_title),
                RC_FILE_PERMISSION, filePermission
            )
        }
    }

    private fun toggleSelectedComment(comment: QiscusComment) {
        if (comment.type != QiscusComment.Type.SYSTEM_EVENT || comment.type != QiscusComment.Type.CARD) {
            comment.isSelected = true
            selectedComment = comment
            onCommentSelected(comment)
        }
    }

    private fun onCommentSelected(selectedComment: QiscusComment) {
        if (binding.toolbarSelectedComment.container.visibility == View.VISIBLE) {
            binding.toolbarSelectedComment.container.gone()
        } else {
            if (selectedComment.isMyComment) {
                binding.toolbarSelectedComment.btnActionDelete.visible()
            } else {
                binding.toolbarSelectedComment.btnActionDelete.gone()
            }
            binding.toolbarSelectedComment.container.visible()
        }
    }

    private fun bindReplyView(origin: QiscusComment?) {
        binding.originSender.text = origin?.sender
        when (origin?.type) {
            QiscusComment.Type.IMAGE -> {
                binding.originImage.visible()
                binding.originImage.load(origin.caption) {
                    crossfade(true)
                    error(R.drawable.ic_qiscus_avatar)
                    transformations(CircleCropTransformation())
                }
            }
            QiscusComment.Type.FILE -> {
                binding.originContent.text = origin.attachmentName
                binding.originImage.gone()
            }
            else -> {
                binding.originImage.gone()
                binding.originContent.text = origin?.message
            }
        }
    }

    private fun showDialogDeleteComment(qiscusComment: QiscusComment) {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage("Sure to delete this message??")
            .setCancelable(true)
            .setPositiveButton(
                "Yes"
            ) { dialog, _ ->
                viewModels.deleteComment(qiscusComment)
                dialog.dismiss()
            }.setNegativeButton(
                "Cancel"
            ) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun loadMoreComments() {
        if (binding.progressBar.visibility == View.GONE && commentAdapter.itemCount > 0) {
            val comment: QiscusComment = commentAdapter.currentList[commentAdapter.itemCount - 1]
            if (comment.id == -1L || comment.commentBeforeId > 0) {
                viewModels.loadOlderCommentThan(comment)
            }
        }
    }

    private fun notifyLatestRead() {
        if (commentAdapter.itemCount > 0) {
            val comment: QiscusComment = commentAdapter.currentList[commentAdapter.itemCount - 1]
            QiscusPusherApi.getInstance().markAsRead(args.qiscusChatRoom?.id!!, comment.id)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            try {
                val imageFile = QiscusFileUtil.from(data!!.data)
                val qiscusPhotos = arrayListOf<QiscusPhoto>()
                qiscusPhotos.add(QiscusPhoto(imageFile))
                val fragment = SendPhotoFragment(
                    qiscusChatRoom = args.qiscusChatRoom,
                    qiscusPhotos = qiscusPhotos
                ).apply {
                    setTargetFragment(this@ChatRoomFragment, SEND_PICTURE_CONFIRMATION_REQUEST)
                }
                fragment.show(parentFragmentManager, fragment.tag)
            } catch (e: Exception) {
                e.printStackTrace()
                requireContext().toast("Failed to open image file!")
            }
        } else if (requestCode == SEND_PICTURE_CONFIRMATION_REQUEST && resultCode == Activity.RESULT_OK) {
            if (data == null) {
                requireContext().toast(getString(R.string.qiscus_chat_error_failed_open_picture))
                return
            }
            val captions = data.getStringExtra(CAPTION)
            val qiscusPhotos: ArrayList<QiscusPhoto>? =
                data.getParcelableArrayListExtra(SendPhotoFragment.PHOTO)
            if (qiscusPhotos != null) {
                viewModels.sendFile(qiscusPhotos[0].photoFile, captions)
            } else {
                requireContext().toast(getString(R.string.qiscus_chat_error_failed_read_picture))
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        QiscusPermissionsUtil.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults,
            this
        )
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        if (requestCode == REQUEST_FILE_PERMISSION) {
            pickImage()
        }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        QiscusPermissionsUtil.checkDeniedPermissionsNeverAskAgain(
            this, getString(R.string.qiscus_permission_message),
            R.string.qiscus_grant, R.string.qiscus_denny, perms
        )
    }

    override fun onTopOffListMessage() {
        loadMoreComments()
    }

    override fun onMiddleOffListMessage() {

    }

    override fun onBottomOffListMessage() {

    }

    override fun onItemClick(qiscusComment: QiscusComment) {

    }

    override fun onItemLongClick(qiscusComment: QiscusComment) {
        toggleSelectedComment(qiscusComment)
    }

}