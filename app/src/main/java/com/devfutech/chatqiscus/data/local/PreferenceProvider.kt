package com.devfutech.chatqiscus.data.local

import android.content.Context
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.qiscus.sdk.chat.core.QiscusCore
import com.qiscus.sdk.chat.core.data.model.QiscusAccount
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class PreferenceProvider @Inject constructor(@ApplicationContext context: Context) {
    private val gson: Gson =
        GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create()
    private val sharedPreferences = context.getSharedPreferences("user", Context.MODE_PRIVATE)


    fun getCurrentUserLogin(): QiscusAccount {
        return gson.fromJson(
            sharedPreferences.getString("current_user", ""),
            QiscusAccount::class.java
        )
    }

    fun setUser(qiscusAccount: QiscusAccount) {
        sharedPreferences.edit()
            .putString("current_user", gson.toJson(qiscusAccount))
            .apply()
    }

    private fun getCurrentDeviceToken(): String? {
        return sharedPreferences.getString("current_device_token", "")
    }

    fun setCurrentDeviceToken(token: String) {
        sharedPreferences.edit()
            .putString("current_device_token", token)
            .apply()
    }

    fun logout() {
        QiscusCore.removeDeviceToken(getCurrentDeviceToken())
        QiscusCore.clearUser()
        sharedPreferences.edit().clear().apply()
    }

}