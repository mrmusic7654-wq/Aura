package com.aura.ai.automation

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.*

class AuraAccessibilityService : AccessibilityService() {
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var isServiceActive = false
    
    companion object {
        var instance: AuraAccessibilityService? = null
    }
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        isServiceActive = true
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Handle accessibility events if needed
    }
    
    override fun onInterrupt() {
        isServiceActive = false
    }
    
    override fun onDestroy() {
        super.onDestroy()
        instance = null
        serviceScope.cancel()
    }
    
    fun performScroll(direction: String) {
        val displayWidth = resources.displayMetrics.widthPixels
        val displayHeight = resources.displayMetrics.heightPixels
        
        val scrollData = when (direction.lowercase()) {
            "up" -> ScrollData(
                displayWidth / 2f,
                displayHeight * 0.7f,
                displayWidth / 2f,
                displayHeight * 0.3f
            )
            "down" -> ScrollData(
                displayWidth / 2f,
                displayHeight * 0.3f,
                displayWidth / 2f,
                displayHeight * 0.7f
            )
            "left" -> ScrollData(
                displayWidth * 0.7f,
                displayHeight / 2f,
                displayWidth * 0.3f,
                displayHeight / 2f
            )
            "right" -> ScrollData(
                displayWidth * 0.3f,
                displayHeight / 2f,
                displayWidth * 0.7f,
                displayHeight / 2f
            )
            else -> return
        }
        
        val path = Path()
        path.moveTo(scrollData.startX, scrollData.startY)
        path.lineTo(scrollData.endX, scrollData.endY)
        
        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, 500))
        
        dispatchGesture(gestureBuilder.build(), null, null)
    }
    
    fun performClick(x: Float, y: Float) {
        val path = Path()
        path.moveTo(x, y)
        
        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, 100))
        
        dispatchGesture(gestureBuilder.build(), null, null)
    }
    
    fun performSwipe(fromX: Float, fromY: Float, toX: Float, toY: Float) {
        val path = Path()
        path.moveTo(fromX, fromY)
        path.lineTo(toX, toY)
        
        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, 300))
        
        dispatchGesture(gestureBuilder.build(), null, null)
    }
    
    fun performBack(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            performGlobalAction(GLOBAL_ACTION_BACK)
            true
        } else {
            false
        }
    }
    
    fun getRootNodeInfo(): AccessibilityNodeInfo? {
        return if (isServiceActive) {
            rootInActiveWindow
        } else {
            null
        }
    }
    
    fun findViewByText(text: String): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        return findNodeByText(root, text)
    }
    
    private fun findNodeByText(node: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        if (node.text?.toString()?.contains(text, ignoreCase = true) == true) {
            return node
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            val result = findNodeByText(child, text)
            if (result != null) {
                return result
            }
            child.recycle()
        }
        
        return null
    }
    
    private data class ScrollData(
        val startX: Float,
        val startY: Float,
        val endX: Float,
        val endY: Float
    )
}
