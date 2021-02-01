package com.devfutech.chatqiscus

import androidx.multidex.MultiDexApplication
import com.qiscus.sdk.chat.core.QiscusCore
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.one.EmojiOneProvider
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ChatQiscus: MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        QiscusCore.setup(this, BuildConfig.APP_ID_SAMPLE)
        EmojiManager.install(EmojiOneProvider())
    }
}