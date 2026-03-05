package com.aura.ai.utils

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

class PermissionHelper(private val activity: Activity) {
    
    fun hasRequiredPermissions(): Boolean {
        return Constants.REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    fun requestPermissions(launcher: androidx.activity.result.ActivityResultLauncher<Array<String>>) {
        if (!hasRequiredPermissions()) {
            launcher.launch(Constants.REQUIRED_PERMISSIONS)
        }
    }
}
