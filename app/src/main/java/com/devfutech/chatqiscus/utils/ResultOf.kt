package com.devfutech.chatqiscus.utils

sealed class ResultOf<out T> {
    data class Success<out R>(val value: R) : ResultOf<R>()
    data class Progress(val boolean: Boolean? = false) : ResultOf<Nothing>()
    data class Failure(val throwable: Throwable?, val message:String?) : ResultOf<Nothing>()
}