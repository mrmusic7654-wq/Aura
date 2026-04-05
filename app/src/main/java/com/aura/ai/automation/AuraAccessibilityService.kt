package com.aura.ai.automation

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
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
        // Service interrupted
    }
    
    fun performClick(x: Float, y: Float) {
        val path = Path()
        path.moveTo(x, y)
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()
        dispatchGesture(gesture, null, null)
    }
    
    fun performSwipe(fromX: Float, fromY: Float, toX: Float, toY: Float) {
        val path = Path()
        path.moveTo(fromX, fromY)
        path.lineTo(toX, toY)
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 300))
            .build()
        dispatchGesture(gesture, null, null)
    }
    
    fun performScroll(direction: String) {
        val displayWidth = resources.displayMetrics.widthPixels
        val displayHeight = resources.displayMetrics.heightPixels
        
        val startX = displayWidth / 2f
        val startY: Float
        val endX = displayWidth / 2f
        val endY: Float
        
        when (direction.lowercase()) {
            "up" -> {
                startY = displayHeight * 0.7f
                endY = displayHeight * 0.3f
            }
            "down" -> {
                startY = displayHeight * 0.3f
                endY = displayHeight * 0.7f
            }
            else -> return
        }
        
        val path = Path()
        path.moveTo(startX, startY)
        path.lineTo(endX, endY)
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 500))
            .build()
        dispatchGesture(gesture, null, null)
    }
    
    fun performBack(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_BACK)
    }
    
    fun performHome(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_HOME)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }
}
