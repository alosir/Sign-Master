package com.alosir.task.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.alosir.task.util.ScriptRecorder
import com.google.gson.Gson
import kotlinx.coroutines.*
import java.util.*

class AutomationService : AccessibilityService() {
    
    companion object {
        var isRunning: Boolean = false
            private set
        
        var isRecording: Boolean = false
            private set
        
        var isPlaying: Boolean = false
            private set
        
        private var recorder: ScriptRecorder? = null
        
        fun startRecording() {
            recorder = ScriptRecorder()
            isRecording = true
        }
        
        fun stopRecording(): List<ActionInfo>? {
            isRecording = false
            return recorder?.actions?.toList()
        }
        
        fun cancelRecording() {
            isRecording = false
            recorder = null
        }
    }
    
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var playJob: Job? = null
    
    val recordedActions = mutableListOf<ActionInfo>()
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true
        
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_VIEW_CLICKED or
                        AccessibilityEvent.TYPE_VIEW_FOCUSED or
                        AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                   AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                   AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
        }
        serviceInfo = info
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        
        if (isRecording) {
            recorder?.recordEvent(event, rootInActiveWindow)
        }
    }
    
    override fun onInterrupt() {
        isRecording = false
        isPlaying = false
        recorder = null
    }
    
    fun performAction(action: ActionInfo): Boolean {
        return when (action.type) {
            "click" -> performClick(action)
            "long_click" -> performLongClick(action)
            "swipe" -> performSwipe(action)
            "text" -> performTextInput(action)
            "back" -> performBack()
            "home" -> performHome()
            "wait" -> {
                Thread.sleep(action.duration ?: 1000)
                true
            }
            else -> false
        }
    }
    
    private fun performClick(action: ActionInfo): Boolean {
        val node = findNodeByTarget(action.target) ?: return false
        
        return try {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    private fun performLongClick(action: ActionInfo): Boolean {
        val node = findNodeByTarget(action.target) ?: return false
        
        return try {
            node.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    private fun performSwipe(action: ActionInfo): Boolean {
        return try {
            val startX = action.startX ?: 0
            val startY = action.startY ?: 0
            val endX = action.endX ?: 0
            val endY = action.endY ?: 0
            
            val gestureBuilder = android.accessibilityservice.GestureDescription.StrokeDescription(
                android.graphics.Path().apply {
                    moveTo(startX.toFloat(), startY.toFloat())
                    lineTo(endX.toFloat(), endY.toFloat())
                },
                0,
                action.duration ?: 300
            )
            
            val gesture = android.accessibilityservice.GestureDescription.Builder()
                .addStroke(gestureBuilder)
                .build()
            
            dispatchGesture(gesture, null, null)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    private fun performTextInput(action: ActionInfo): Boolean {
        val node = findNodeByTarget(action.target) ?: return false
        
        return try {
            val arguments = android.os.Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, action.text)
            }
            node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    private fun performBack(): Boolean {
        return try {
            performGlobalAction(GLOBAL_ACTION_BACK)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    private fun performHome(): Boolean {
        return try {
            performGlobalAction(GLOBAL_ACTION_HOME)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    private fun findNodeByTarget(target: TargetInfo?): AccessibilityNodeInfo? {
        if (target == null) return null
        
        return try {
            when (target.type) {
                "id" -> rootInActiveWindow?.findAccessibilityNodeInfosByViewId(target.value)?.firstOrNull()
                "text" -> rootInActiveWindow?.findAccessibilityNodeInfosByText(target.value)?.firstOrNull()
                "desc" -> rootInActiveWindow?.findAccessibilityNodeInfosByText(target.value)?.firstOrNull()
                "focus" -> findFocus()
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun findFocus(): AccessibilityNodeInfo? {
        return try {
            rootInActiveWindow?.findAccessibilityNodeInfosByText("")?.firstOrNull {
                it.isFocused
            } ?: rootInActiveWindow
        } catch (e: Exception) {
            null
        }
    }
    
    fun playScript(actions: List<ActionInfo>, callback: (Boolean) -> Unit) {
        if (isPlaying) {
            callback(false)
            return
        }
        
        isPlaying = true
        
        playJob = serviceScope.launch {
            try {
                for ((_, action) in actions.withIndex()) {
                    if (!isPlaying) break
                    
                    performAction(action)
                    delay(action.delay ?: 500L)
                }
                callback(true)
            } catch (e: Exception) {
                callback(false)
            } finally {
                isPlaying = false
            }
        }
    }
    
    fun stopPlayback() {
        isPlaying = false
        playJob?.cancel()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        isRecording = false
        isPlaying = false
        serviceScope.cancel()
    }
}
