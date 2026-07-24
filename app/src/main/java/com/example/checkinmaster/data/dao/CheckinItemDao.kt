package com.alosir.task.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.alosir.task.data.entity.CheckinItem
import com.alosir.task.data.entity.CheckinRecord

@Dao
interface CheckinItemDao {
    
    @Query("SELECT * FROM checkin_items WHERE type = :type ORDER BY sort_order, name")
    fun getAllByType(type: Int): LiveData<List<CheckinItem>>
    
    @Query("SELECT * FROM checkin_items WHERE type = :type ORDER BY sort_order, name")
    suspend fun getAllByTypeSync(type: Int): List<CheckinItem>
    
    @Query("SELECT * FROM checkin_items WHERE id = :id")
    suspend fun getById(id: Int): CheckinItem?
    
    @Query("SELECT * FROM checkin_items WHERE id = :id")
    suspend fun getByIdSync(id: Int): CheckinItem?
    
    @Query("SELECT * FROM checkin_items WHERE id = :id")
    fun getByIdLiveData(id: Int): LiveData<CheckinItem?>
    
    @Insert
    suspend fun insert(item: CheckinItem): Long
    
    @Update
    suspend fun update(item: CheckinItem)
    
    @Delete
    suspend fun delete(item: CheckinItem)
    
    @Query("UPDATE checkin_items SET sort_order = :order WHERE id = :id")
    suspend fun updateSortOrder(id: Int, order: Int)
    
    @Query("UPDATE checkin_items SET last_checkin_date = :date WHERE id = :id")
    suspend fun updateLastCheckinDate(id: Int, date: String)
    
    @Query("UPDATE checkin_items SET last_checkin_date = NULL WHERE id = :id")
    suspend fun clearLastCheckinDate(id: Int)

    @Query("UPDATE checkin_items SET terminated = :terminated, terminated_date = :terminatedDate WHERE id = :id")
    suspend fun updateTerminated(id: Int, terminated: Int, terminatedDate: String?)

    @Query("SELECT COUNT(*) FROM checkin_items")
    suspend fun getCount(): Int
    
    @Query("SELECT * FROM checkin_items ORDER BY sort_order, name")
    suspend fun getAll(): List<CheckinItem>

    @Query("SELECT * FROM checkin_items ORDER BY sort_order, name")
    suspend fun getAllSync(): List<CheckinItem>
    
    @Query("SELECT * FROM checkin_items ORDER BY cycle_type, type, name")
    fun getAllOrderByCycleType(): LiveData<List<CheckinItem>>
    
    @Query("SELECT * FROM checkin_items ORDER BY last_checkin_date DESC, name")
    fun getAllOrderByLastCheckin(): LiveData<List<CheckinItem>>
}
