package com.aura.ai.utils

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

class PermissionHelper(private val activity: Activity) {
    
    fun hasRequiredPermissions(): Boolean {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
        
        return permissions.all {
            ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    fun requestPermissions(launcher: androidx.activity.result.ActivityResultLauncher<Array<String>>) {
        if (!hasRequiredPermissions()) {
            val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.POST_NOTIFICATIONS
                )
            } else {
                arrayOf(
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                )
            }
            launcher.launch(permissions)
        }
    }
    
    fun checkPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
    }
}
