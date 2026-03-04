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
    
    private val _isAccessibilityEnabled = MutableStateFlow(false)
    val isAccessibilityEnabled: StateFlow<Boolean> = _isAccessibilityEnabled
    
    private val _lastOpenedApp = MutableStateFlow<String?>(null)
    val lastOpenedApp: StateFlow<String?> = _lastOpenedApp
    
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    
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
            e.printStackTrace()
            false
        }
    }
    
    fun openAppByName(appName: String): Boolean {
        // Check common apps first
        Constants.COMMON_APP_PACKAGES[appName.lowercase()]?.let { packageName ->
            return openApp(packageName)
        }
        
        // Search through installed apps
        val packageName = findAppPackage(appName)
        return if (packageName != null) {
            openApp(packageName)
        } else {
            false
        }
    }
    
    private fun findAppPackage(appName: String): String? {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        
        // First try exact match
        apps.find { app ->
            pm.getApplicationLabel(app).toString().equals(appName, ignoreCase = true)
        }?.let { return it.packageName }
        
        // Then try contains match
        apps.find { app ->
            pm.getApplicationLabel(app).toString().contains(appName, ignoreCase = true)
        }?.let { return it.packageName }
        
        return null
    }
    
    fun openUrl(url: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    fun searchQuery(query: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_WEB_SEARCH)
            intent.putExtra(SearchManager.QUERY, query)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            e.printStackTrace()
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
    
    fun wakeDevice(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val wakeLock = powerManager.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "AuraAI:WakeLock"
                )
                wakeLock.acquire(5000)
                wakeLock.release()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    fun requestAccessibilityPermission() {
        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }
    
    fun isAccessibilityServiceEnabled(serviceClass: Class<*>): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return enabledServices?.contains(context.packageName + "/" + serviceClass.name) == true
    }
    
    fun getInstalledApps(): List<Map<String, String>> {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        
        return apps.map { app ->
            mapOf(
                "name" to pm.getApplicationLabel(app).toString(),
                "packageName" to app.packageName
            )
        }.sortedBy { it["name"] }
    }
}
