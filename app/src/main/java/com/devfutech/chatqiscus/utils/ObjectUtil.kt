package com.devfutech.chatqiscus.utils

object ObjectUtil {
    fun generateAvatar(s: String?): String {
        val url = s?.replace(" ".toRegex(), "")
        return "https://robohash.org/$url/bgset_bg2/3.14160?set=set4"
    }
}