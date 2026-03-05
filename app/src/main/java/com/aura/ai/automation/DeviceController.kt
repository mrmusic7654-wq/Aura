package com.aura.ai.automation

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import com.aura.ai.utils.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class DeviceController(private val context: Context) {
    
    private val _lastOpenedApp = MutableStateFlow<String?>(null)
    val lastOpenedApp: StateFlow<String?> = _lastOpenedApp
    
    fun openApp(packageName: String): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                _lastOpenedApp.value = packageName
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    fun openAppByName(appName: String): Boolean {
        Constants.COMMON_APP_PACKAGES[appName.lowercase()]?.let { packageName ->
            return openApp(packageName)
        }
        
        val packageName = findAppPackage(appName)
        return packageName?.let { openApp(it) } ?: false
    }
    
    private fun findAppPackage(appName: String): String? {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        
        return apps.find { app ->
            pm.getApplicationLabel(app).toString().equals(appName, ignoreCase = true)
        }?.packageName
    }
    
    fun searchQuery(query: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_WEB_SEARCH)
            intent.putExtra(SearchManager.QUERY, query)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun goHome(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return true
    }
}
