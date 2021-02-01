package com.devfutech.chatqiscus.ui.view

import android.view.View
import androidx.annotation.IntDef

interface QiscusProgressView {

    fun setProgress(progress: Int)

    fun setVisibility(@Visibility visibility: Int)

    @IntDef(View.VISIBLE, View.INVISIBLE, View.GONE)
    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    annotation class Visibility
}