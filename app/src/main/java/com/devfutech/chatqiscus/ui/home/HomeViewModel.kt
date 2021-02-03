package com.devfutech.chatqiscus.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.devfutech.chatqiscus.data.repository.ChatRoomRepositoryImpl
import com.devfutech.chatqiscus.utils.Action
import com.devfutech.chatqiscus.utils.ResultOf
import com.qiscus.sdk.chat.core.data.model.QiscusChatRoom
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(private val chatRoomRepository: ChatRoomRepositoryImpl) :
    ViewModel() {

    private val _room = MutableLiveData<ResultOf<List<QiscusChatRoom?>?>>()
    val room: LiveData<ResultOf<List<QiscusChatRoom?>?>>
        get() = _room


    fun loadChatRooms() {
        _room.postValue(ResultOf.Progress(true))
        chatRoomRepository.getChatRooms(onSuccess = object :
            Action<List<QiscusChatRoom?>?> {
            override fun call(t: List<QiscusChatRoom?>?) {
                _room.postValue(ResultOf.Success(t))
            }
        },
            onError = object : Action<Throwable?> {
                override fun call(t: Throwable?) {
                    _room.postValue(ResultOf.Failure(throwable = t, message = null))
                }
            })
    }
}