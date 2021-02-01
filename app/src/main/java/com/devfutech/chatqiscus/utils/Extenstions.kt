package com.devfutech.chatqiscus.utils

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.navigation.findNavController
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

fun Context.toast(message: String?) {
    Toast.makeText(this, message!!, Toast.LENGTH_SHORT).show()
}

fun ImageView.back() {
    setOnClickListener {
        findNavController().navigateUp()
    }
}

fun String.isEmailValid(): Boolean {
    val expression = "^[\\w.-]+@([\\w\\-]+\\.)+[A-Z]{2,4}$"
    val pattern: Pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE)
    val matcher: Matcher = pattern.matcher(this)
    return matcher.matches()
}

fun View.visible() {
    visibility = View.VISIBLE
}

fun View.gone() {
    visibility = View.GONE
}

fun View.invisible() {
    visibility = View.INVISIBLE
}

fun TextInputLayout.inputError(data: String, message: String?): Boolean {
    return if (data.isEmpty()) {
        error = message
        false
    } else {
        error = null
        true
    }
}

fun TextInputEditText.clearInput(
    inputLayout: TextInputLayout
) {
    this.setOnFocusChangeListener { _, hasFoccus ->
        if (hasFoccus) {
            inputLayout.error = null
        }
    }
}

fun Date?.getLastMessageTimestamp(): String? {
    return if (this != null) {
        val todayCalendar = Calendar.getInstance()
        val localCalendar = Calendar.getInstance()
        localCalendar.time = this
        when {
            todayCalendar.time.getDateStringFromDate()
                    == localCalendar.time.getDateStringFromDate() -> {
                this.getTimeStringFromDate()
            }
            todayCalendar[Calendar.DATE] - localCalendar[Calendar.DATE] == 1 -> {
                "Yesterday"
            }
            else -> {
                this.getDateStringFromDate()
            }
        }
    } else {
        null
    }
}

fun Date?.getTimeStringFromDate(): String? {
    val dateFormat: DateFormat = SimpleDateFormat("HH:mm", Locale.US)
    return dateFormat.format(this!!)
}

fun Date?.getDateStringFromDate(): String? {
    val dateFormat: DateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US)
    return dateFormat.format(this!!)
}

fun Date?.toFullDate(): String? {
    val fullDateFormat = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.US)
    return fullDateFormat.format(this!!)
}