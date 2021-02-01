package com.devfutech.chatqiscus.utils

interface Action<T> {
    fun call(t: T)
}