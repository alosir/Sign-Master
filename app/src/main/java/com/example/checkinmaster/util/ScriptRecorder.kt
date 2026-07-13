package com.alosir.task.util

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.alosir.task.service.ActionInfo
import com.alosir.task.service.TargetInfo

class ScriptRecorder {
    
    val actions = mutableListOf<ActionInfo>()
    private var lastTimestamp = System.currentTimeMillis()
    
    fun recordEvent(event: AccessibilityEvent, source: AccessibilityNodeInfo?) {
        val action = when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_CLICKED -> createClickAction(source)
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> createFocusAction(source)
            else -> null
        }
        
        action?.let {
            val delay = System.currentTimeMillis() - lastTimestamp
            actions.add(it.copy(delay = delay))
            lastTimestamp = System.currentTimeMillis()
        }
    }
    
    private fun createClickAction(source: AccessibilityNodeInfo?): ActionInfo? {
        if (source == null) return null
        val target = findBestTarget(source) ?: return null
        return ActionInfo(type = "click", target = target)
    }
    
    private fun createFocusAction(source: AccessibilityNodeInfo?): ActionInfo? {
        if (source == null) return null
        val target = findBestTarget(source) ?: return null
        return ActionInfo(type = "click", target = target)
    }
    
    private fun findBestTarget(source: AccessibilityNodeInfo): TargetInfo? {
        source.viewIdResourceName?.let { id ->
            if (id.isNotEmpty()) {
                return TargetInfo(type = "id", value = id)
            }
        }
        
        source.text?.let { text ->
            if (text.isNotEmpty()) {
                return TargetInfo(type = "text", value = text.toString())
            }
        }
        
        source.contentDescription?.let { desc ->
            if (desc.isNotEmpty()) {
                return TargetInfo(type = "desc", value = desc.toString())
            }
        }
        
        return null
    }
}
