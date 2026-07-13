package com.alosir.task.data.repository

import com.alosir.task.data.dao.ScriptDao
import com.alosir.task.data.entity.AutomationScript

class ScriptRepository(private val scriptDao: ScriptDao) {
    
    suspend fun getScriptByItemId(itemId: Int): AutomationScript? {
        return scriptDao.getByItemId(itemId)
    }
    
    suspend fun getScriptById(id: Int): AutomationScript? {
        return scriptDao.getById(id)
    }
    
    suspend fun insertScript(script: AutomationScript): Long {
        return scriptDao.insert(script)
    }
    
    suspend fun updateScript(script: AutomationScript) {
        scriptDao.update(script)
    }
    
    suspend fun deleteScript(script: AutomationScript) {
        scriptDao.delete(script)
    }
    
    suspend fun deleteScriptByItemId(itemId: Int) {
        scriptDao.deleteByItemId(itemId)
    }
}
