package com.devfutech.chatqiscus.utils

@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
annotation class AfterPermissionGranted(val value: Int)
