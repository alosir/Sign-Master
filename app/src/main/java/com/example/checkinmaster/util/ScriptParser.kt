package com.alosir.task.util

import com.alosir.task.service.ActionInfo
import com.alosir.task.service.TargetInfo
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object ScriptParser {
    
    private val gson = Gson()
    
    fun parse(json: String): ScriptData? {
        return try {
            gson.fromJson(json, ScriptData::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
    fun toJson(scriptData: ScriptData): String {
        return gson.toJson(scriptData)
    }
    
    fun actionsToJson(actions: List<ActionInfo>): String {
        return gson.toJson(actions)
    }
    
    fun jsonToActions(json: String): List<ActionInfo>? {
        return try {
            val type = object : TypeToken<List<ActionInfo>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            null
        }
    }
}

data class ScriptData(
    val version: String = "1.0",
    val appName: String = "",
    val packageName: String = "",
    val actions: List<ActionInfo> = emptyList()
)
