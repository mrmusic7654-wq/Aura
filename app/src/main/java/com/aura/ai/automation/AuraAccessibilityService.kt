package com.aura.ai.automation

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class AuraAccessibilityService : AccessibilityService() {
    
    companion object {
        var instance: AuraAccessibilityService? = null
    }
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Handle events if needed
    }
    
    override fun onInterrupt() {
        instance = null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }
}
