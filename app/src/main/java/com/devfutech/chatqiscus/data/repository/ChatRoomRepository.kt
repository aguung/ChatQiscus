package com.devfutech.chatqiscus.data.repository

import com.devfutech.chatqiscus.utils.Action
import com.qiscus.sdk.chat.core.data.model.QiscusAccount
import com.qiscus.sdk.chat.core.data.model.QiscusChatRoom

interface ChatRoomRepository {
    fun getChatRooms(onSuccess: Action<List<QiscusChatRoom?>?>, onError: Action<Throwable?>)

    fun createChatRoom(
        user: QiscusAccount?,
        onSuccess: Action<QiscusChatRoom?>,
        onError: Action<Throwable?>
    )

    fun createGroupChatRoom(
        name: String?,
        members: List<QiscusAccount?>,
        onSuccess: Action<QiscusChatRoom?>,
        onError: Action<Throwable?>
    )

    fun addParticipant(
        roomId: Long,
        members: List<QiscusAccount?>,
        onSuccess: Action<Void?>,
        onError: Action<Throwable?>
    )

    fun removeParticipant(
        roomId: Long,
        members: List<QiscusAccount?>,
        onSuccess: Action<Void?>,
        onError: Action<Throwable?>
    )
}