package com.devfutech.chatqiscus.ui.groupchatcreation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.devfutech.chatqiscus.data.repository.ChatRoomRepositoryImpl
import com.devfutech.chatqiscus.utils.Action
import com.devfutech.chatqiscus.utils.ResultOf
import com.qiscus.sdk.chat.core.data.model.QiscusAccount
import com.qiscus.sdk.chat.core.data.model.QiscusChatRoom
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class GroupChatCreationViewModel @Inject constructor(private val chatRoomRepository: ChatRoomRepositoryImpl) :
    ViewModel() {

    private val _chatRoom = MutableLiveData<ResultOf<QiscusChatRoom>>()
    val chatRoom: LiveData<ResultOf<QiscusChatRoom>>
        get() = _chatRoom

    fun createGroup(name: String?, members: List<QiscusAccount?>) {
        _chatRoom.postValue(ResultOf.Progress(true))
        chatRoomRepository.createGroupChatRoom(
            name = name,
            members = members,
            onSuccess = object : Action<QiscusChatRoom?> {
                override fun call(t: QiscusChatRoom?) {
                    _chatRoom.postValue(ResultOf.Success(t!!))
                }
            },
            onError = object : Action<Throwable?> {
                override fun call(t: Throwable?) {
                    _chatRoom.postValue(ResultOf.Failure(throwable = t, message = null))
                }
            }
        )
    }
}