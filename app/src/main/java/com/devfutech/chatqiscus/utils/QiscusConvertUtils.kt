package com.devfutech.chatqiscus.utils

import android.content.res.Resources
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object QiscusConverterUtil {
    fun dp2px(resources: Resources, dp: Float): Float {
        val scale = resources.displayMetrics.density
        return dp * scale + 0.5f
    }

    fun sp2px(resources: Resources, sp: Float): Float {
        val scale = resources.displayMetrics.scaledDensity
        return sp * scale
    }
}