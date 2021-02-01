package com.devfutech.chatqiscus.utils

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.devfutech.chatqiscus.R
import java.lang.reflect.InvocationTargetException
import java.util.*

object QiscusPermissionsUtil {
    private const val TAG = "QiscusPermissionsUtil"
    fun hasPermissions(context: Context?, vararg perms: String?): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Log.w(TAG, "hasPermissions: API version < M, returning true by default")
            return true
        }
        for (perm in perms) {
            val hasPerm = ContextCompat.checkSelfPermission(
                context!!,
                perm!!
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPerm) {
                return false
            }
        }
        return true
    }

    fun requestPermissions(
        `object`: Any, rationale: String?,
        requestCode: Int, perms: Array<String>
    ) {
        requestPermissions(
            `object`, rationale,
            R.string.ok,
            R.string.cancel,
            requestCode, perms
        )
    }


    private fun requestPermissions(
        `object`: Any, rationale: String?,
        @StringRes positiveButton: Int,
        @StringRes negativeButton: Int,
        requestCode: Int, perms: Array<String>
    ) {
        checkCallingObjectSuitability(`object`)
        val callbacks = `object` as PermissionCallbacks
        var shouldShowRationale = false
        for (perm in perms) {
            shouldShowRationale =
                shouldShowRationale || shouldShowRequestPermissionRationale(`object`, perm)
        }
        if (shouldShowRationale) {
            val activity = getActivity(`object`) ?: return
            val dialog = AlertDialog.Builder(activity)
                .setMessage(rationale)
                .setPositiveButton(
                    positiveButton
                ) { _: DialogInterface?, _: Int ->
                    executePermissionsRequest(
                        `object`,
                        perms,
                        requestCode
                    )
                }
                .setNegativeButton(
                    negativeButton
                ) { _: DialogInterface?, _: Int ->
                    callbacks.onPermissionsDenied(
                        requestCode,
                        listOf(*perms)
                    )
                }.create()
            dialog.show()
        } else {
            executePermissionsRequest(`object`, perms, requestCode)
        }
    }

    fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray, `object`: Any
    ) {
        checkCallingObjectSuitability(`object`)
        val callbacks = `object` as PermissionCallbacks

        val granted = ArrayList<String>()
        val denied = ArrayList<String>()
        for (i in permissions.indices) {
            val perm = permissions[i]
            if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                granted.add(perm)
            } else {
                denied.add(perm)
            }
        }

        if (granted.isNotEmpty()) {
            callbacks.onPermissionsGranted(requestCode, granted)
        }
        if (denied.isNotEmpty()) {
            callbacks.onPermissionsDenied(requestCode, denied)
        }
        if (granted.isNotEmpty() && denied.isEmpty()) {
            runAnnotatedMethods(`object`, requestCode)
        }
    }

    fun checkDeniedPermissionsNeverAskAgain(
        `object`: Any, rationale: String?,
        @StringRes positiveButton: Int,
        @StringRes negativeButton: Int,
        deniedPerms: List<String>
    ): Boolean {
        var shouldShowRationale: Boolean
        for (perm in deniedPerms) {
            shouldShowRationale = shouldShowRequestPermissionRationale(`object`, perm)
            if (!shouldShowRationale) {
                val activity = getActivity(`object`) ?: return true
                val dialog = AlertDialog.Builder(activity)
                    .setMessage(rationale)
                    .setPositiveButton(positiveButton) { dialog1: DialogInterface?, which: Int ->
                        val intent =
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        val uri =
                            Uri.fromParts("package", activity.packageName, null)
                        intent.data = uri
                        activity.startActivity(intent)
                    }
                    .setNegativeButton(negativeButton, null)
                    .create()
                dialog.show()
                return true
            }
        }
        return false
    }

    @TargetApi(23)
    private fun shouldShowRequestPermissionRationale(`object`: Any, perm: String): Boolean {
        return when (`object`) {
            is Activity -> {
                ActivityCompat.shouldShowRequestPermissionRationale(`object`, perm)
            }
            is Fragment -> {
                `object`.shouldShowRequestPermissionRationale(perm)
            }
            else -> {
                false
            }
        }
    }

    @TargetApi(23)
    private fun executePermissionsRequest(`object`: Any, perms: Array<String>, requestCode: Int) {
        checkCallingObjectSuitability(`object`)
        if (`object` is Activity) {
            ActivityCompat.requestPermissions(`object`, perms, requestCode)
        } else if (`object` is Fragment) {
            `object`.requestPermissions(perms, requestCode)
        }
    }

    @TargetApi(11)
    private fun getActivity(`object`: Any): Activity? {
        return when (`object`) {
            is Activity -> {
                `object`
            }
            is Fragment -> {
                `object`.activity
            }
            else -> {
                null
            }
        }
    }

    private fun runAnnotatedMethods(`object`: Any, requestCode: Int) {
        val clazz: Class<*> = `object`.javaClass
        for (method in clazz.declaredMethods) {
            if (method.isAnnotationPresent(AfterPermissionGranted::class.java)) {
                val ann: AfterPermissionGranted = method.getAnnotation(AfterPermissionGranted::class.java)!!
                if (ann.value == requestCode) {
                    if (method.parameterTypes.isNotEmpty()) {
                        throw RuntimeException(
                            "Cannot execute non-void method " + method.name
                        )
                    }
                    try {
                        if (!method.isAccessible) {
                            method.isAccessible = true
                        }
                        method.invoke(`object`)
                    } catch (e: IllegalAccessException) {
                        Log.e(TAG, "runDefaultMethod:IllegalAccessException", e)
                    } catch (e: InvocationTargetException) {
                        Log.e(TAG, "runDefaultMethod:InvocationTargetException", e)
                    }
                }
            }
        }
    }

    private fun checkCallingObjectSuitability(`object`: Any) {
        val isActivity = `object` is Activity
        val isSupportFragment = `object` is Fragment
        val isAppFragment = `object` is Fragment
        val isMinSdkM = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
        if (!(isSupportFragment || isActivity || isAppFragment && isMinSdkM)) {
            require(!isAppFragment) { "Target SDK needs to be greater than 23 if caller is android.app.Fragment" }
            throw IllegalArgumentException("Caller must be an Activity or a Fragment.")
        }
        require(`object` is PermissionCallbacks) { "Caller must implement PermissionCallbacks." }
    }

    interface PermissionCallbacks : OnRequestPermissionsResultCallback {
        fun onPermissionsGranted(requestCode: Int, perms: List<String>)
        fun onPermissionsDenied(requestCode: Int, perms: List<String>)
    }
}