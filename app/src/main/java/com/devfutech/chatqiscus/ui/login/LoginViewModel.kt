package com.devfutech.chatqiscus.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.devfutech.chatqiscus.data.repository.UserRepositoryImpl
import com.devfutech.chatqiscus.utils.Action
import com.devfutech.chatqiscus.utils.ResultOf
import com.qiscus.sdk.chat.core.data.model.QiscusAccount
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(private val userRepository: UserRepositoryImpl) :
    ViewModel() {

    private val _login = MutableLiveData<ResultOf<QiscusAccount?>>()
    val login: LiveData<ResultOf<QiscusAccount?>>
        get() = _login

    private val _currentUser = MutableLiveData<ResultOf<QiscusAccount?>>()
    val currentUser: LiveData<ResultOf<QiscusAccount?>>
        get() = _currentUser

    init {
        start()
    }

    fun login(
        email: String?,
        password: String?,
        name: String?
    ) {
        _login.postValue(ResultOf.Progress(true))
        userRepository.login(
            email = email,
            password = password,
            name = name,
            onSuccess = object : Action<QiscusAccount?> {
                override fun call(t: QiscusAccount?) {
                    _login.postValue(ResultOf.Success(t))
                }
            },
            onError = object : Action<Throwable?> {
                override fun call(t: Throwable?) {
                    _login.postValue(ResultOf.Failure(throwable = t, message = null))
                }
            })
    }

    private fun start() {
        _currentUser.postValue(ResultOf.Progress(true))
        userRepository.getCurrentUser(onSuccess = object : Action<QiscusAccount?> {
            override fun call(t: QiscusAccount?) {
                _currentUser.postValue(ResultOf.Success(t))
            }
        }, onError = object : Action<Throwable?> {
            override fun call(t: Throwable?) {
                _currentUser.postValue(ResultOf.Failure(throwable = t, message = null))
            }
        })
    }
}