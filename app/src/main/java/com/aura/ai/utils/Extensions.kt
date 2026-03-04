package com.aura.ai.utils

import android.content.Context
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.*

fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

fun Context.showLongToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

fun Long.formatDate(): String {
    val format = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return format.format(Date(this))
}

fun String.isValidCommand(): Boolean {
    val commandKeywords = listOf("open", "close", "scroll", "search", "click", "back", "home")
    return commandKeywords.any { this.contains(it, ignoreCase = true) }
}

fun String.extractAppName(): String {
    val keywords = listOf("open", "launch", "start", "run")
    for (keyword in keywords) {
        if (this.startsWith(keyword, ignoreCase = true)) {
            return this.substring(keyword.length).trim()
        }
    }
    return this
}

fun Long.toReadableSize(): String {
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var size = this.toDouble()
    var unitIndex = 0
    
    while (size >= 1024 && unitIndex < units.size - 1) {
        size /= 1024
        unitIndex++
    }
    
    return String.format("%.2f %s", size, units[unitIndex])
}
