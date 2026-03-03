package com.aura.ai.utils

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.aura.ai.utils.Constants.REQUIRED_PERMISSIONS

class PermissionHelper(private val activity: Activity) {
    
    fun hasRequiredPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            REQUIRED_PERMISSIONS.all {
                ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
            }
        } else {
            // For older versions, check what's available
            REQUIRED_PERMISSIONS.filter { permission ->
                // Filter out permissions that don't exist in older APIs
                when (permission) {
                    android.Manifest.permission.POST_NOTIFICATIONS -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    else -> true
                }
            }.all {
                ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
            }
        }
    }
    
    fun requestPermissions(launcher: androidx.activity.result.ActivityResultLauncher<Array<String>>) {
        if (!hasRequiredPermissions()) {
            val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                REQUIRED_PERMISSIONS
            } else {
                REQUIRED_PERMISSIONS.filter {
                    it != android.Manifest.permission.POST_NOTIFICATIONS
                }.toTypedArray()
            }
            launcher.launch(permissionsToRequest)
        }
    }
    
    fun checkPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
    }
}
