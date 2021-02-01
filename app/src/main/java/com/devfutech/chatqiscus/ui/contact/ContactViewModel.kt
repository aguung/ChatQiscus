package com.devfutech.chatqiscus.ui.contact

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.devfutech.chatqiscus.data.repository.ChatRoomRepositoryImpl
import com.devfutech.chatqiscus.data.repository.UserRepositoryImpl
import com.devfutech.chatqiscus.utils.Action
import com.devfutech.chatqiscus.utils.Event
import com.devfutech.chatqiscus.utils.ResultOf
import com.qiscus.sdk.chat.core.data.model.QiscusAccount
import com.qiscus.sdk.chat.core.data.model.QiscusChatRoom
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ContactViewModel @Inject constructor(
    private val userRepository: UserRepositoryImpl,
    private val chatRoomRepository: ChatRoomRepositoryImpl
) : ViewModel() {

    private val _contact = MutableLiveData<ResultOf<List<QiscusAccount?>?>>()
    val contact: LiveData<ResultOf<List<QiscusAccount?>?>>
        get() = _contact

    private val _room = MutableLiveData<Event<ResultOf<QiscusChatRoom?>>>()
    val room: LiveData<Event<ResultOf<QiscusChatRoom?>>>
        get() = _room

    fun loadContacts(page: Long, limit: Int, query: String?) {
        _contact.postValue(ResultOf.Progress(true))
        userRepository.getUsers(page = page, limit = limit, query = query, onSuccess = object :
            Action<List<QiscusAccount?>?> {
            override fun call(t: List<QiscusAccount?>?) {
                _contact.postValue(ResultOf.Success(t))
            }
        },
            onError = object : Action<Throwable?> {
                override fun call(t: Throwable?) {
                    _contact.postValue(ResultOf.Failure(throwable = t, message = null))
                }
            })
    }

    fun createRoom(qiscusAccount: QiscusAccount?) {
        _room.postValue(Event(ResultOf.Progress(true)))
        chatRoomRepository.createChatRoom(user = qiscusAccount, onSuccess = object :
            Action<QiscusChatRoom?> {
            override fun call(t: QiscusChatRoom?) {
                _room.postValue(Event(ResultOf.Success(t)))
            }
        },
            onError = object : Action<Throwable?> {
                override fun call(t: Throwable?) {
                    _room.postValue(Event(ResultOf.Failure(throwable = t, message = null)))
                }
            })
    }

}