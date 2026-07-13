package com.alosir.task.data.dao

import androidx.room.*
import com.alosir.task.data.entity.AutomationScript

@Dao
interface ScriptDao {
    
    @Query("SELECT * FROM automation_scripts WHERE item_id = :itemId")
    suspend fun getByItemId(itemId: Int): AutomationScript?
    
    @Query("SELECT * FROM automation_scripts WHERE id = :id")
    suspend fun getById(id: Int): AutomationScript?
    
    @Insert
    suspend fun insert(script: AutomationScript): Long
    
    @Update
    suspend fun update(script: AutomationScript)
    
    @Delete
    suspend fun delete(script: AutomationScript)
    
    @Query("DELETE FROM automation_scripts WHERE item_id = :itemId")
    suspend fun deleteByItemId(itemId: Int)
}
