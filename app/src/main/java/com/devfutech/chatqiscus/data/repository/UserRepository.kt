package com.devfutech.chatqiscus.data.repository

import com.devfutech.chatqiscus.utils.Action
import com.qiscus.sdk.chat.core.data.model.QiscusAccount

interface UserRepository {
    fun login(
        email: String?,
        password: String?,
        name: String?,
        onSuccess: Action<QiscusAccount?>,
        onError: Action<Throwable?>
    )

    fun getCurrentUser(onSuccess: Action<QiscusAccount?>, onError: Action<Throwable?>)

    fun getUsers(
        page: Long,
        limit: Int,
        query: String?,
        onSuccess: Action<List<QiscusAccount?>?>,
        onError: Action<Throwable?>
    )

    fun updateProfile(name: String?, onSuccess: Action<QiscusAccount?>, onError: Action<Throwable?>)

    fun logout()

    fun setDeviceToken(token: String?)
}