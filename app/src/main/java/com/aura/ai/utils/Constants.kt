package com.aura.ai.utils

import android.Manifest

object Constants {
    const val APP_NAME = "Aura AI"
    const val AURA_DIR = "AuraAI"
    const val MODELS_DIR = "models"
    
    // Model configuration
    const val MAX_SEQUENCE_LENGTH = 1024
    const val MAX_BATCH_SIZE = 1
    const val TEMPERATURE = 0.7f
    const val TOP_P = 0.9f
    const val TOP_K = 50
    
    // Permissions
    val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.POST_NOTIFICATIONS
    )
    
    // Command keywords for automation
    val APP_OPEN_COMMANDS = listOf("open", "launch", "start", "run")
    val APP_CLOSE_COMMANDS = listOf("close", "exit", "kill", "stop")
    val SCROLL_COMMANDS = listOf("scroll", "slide", "move", "swipe")
    val SEARCH_COMMANDS = listOf("search", "find", "lookup", "google")
    val CLICK_COMMANDS = listOf("click", "tap", "press", "select")
    val BACK_COMMANDS = listOf("back", "go back", "previous")
    val HOME_COMMANDS = listOf("home", "go home", "main screen")
    
    // Common app packages for quick launch
    val COMMON_APP_PACKAGES = mapOf(
        "chrome" to "com.android.chrome",
        "youtube" to "com.google.android.youtube",
        "gmail" to "com.google.android.gm",
        "maps" to "com.google.android.apps.maps",
        "photos" to "com.google.android.apps.photos",
        "play store" to "com.android.vending",
        "settings" to "com.android.settings",
        "calculator" to "com.android.calculator2",
        "calendar" to "com.android.calendar",
        "clock" to "com.android.deskclock",
        "spotify" to "com.spotify.music",
        "whatsapp" to "com.whatsapp",
        "facebook" to "com.facebook.katana",
        "instagram" to "com.instagram.android",
        "twitter" to "com.twitter.android"
    )
}
