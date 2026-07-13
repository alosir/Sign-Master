package com.alosir.task.service

data class ActionInfo(
    val type: String,
    val target: TargetInfo? = null,
    val delay: Long? = null,
    val duration: Long? = null,
    val text: String? = null,
    val startX: Int? = null,
    val startY: Int? = null,
    val endX: Int? = null,
    val endY: Int? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class TargetInfo(
    val type: String,
    val value: String
)
