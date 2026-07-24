package com.alosir.task.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.alosir.task.data.entity.CheckinRecord

@Dao
interface CheckinRecordDao {
    
    @Query("SELECT * FROM checkin_records WHERE item_id = :itemId AND checkin_date = :date")
    suspend fun getByDate(itemId: Int, date: String): CheckinRecord?

    @Query("SELECT * FROM checkin_records WHERE item_id = :itemId AND checkin_date = :date")
    suspend fun getByItemIdAndDateSync(itemId: Int, date: String): CheckinRecord?
    
    @Query("SELECT * FROM checkin_records WHERE checkin_date = :date")
    suspend fun getAllByDate(date: String): List<CheckinRecord>
    
    @Query("SELECT * FROM checkin_records WHERE item_id = :itemId AND checkin_date = :date ORDER BY checkin_time DESC LIMIT 1")
    fun getLatestByDate(itemId: Int, date: String): CheckinRecord?
    
    @Query("SELECT * FROM checkin_records WHERE item_id = :itemId AND checkin_date = :date ORDER BY checkin_time DESC LIMIT 1")
    fun getLatestByDateLiveData(itemId: Int, date: String): LiveData<CheckinRecord?>
    
    @Query("SELECT * FROM checkin_records WHERE item_id = :itemId ORDER BY checkin_time DESC")
    fun getByItem(itemId: Int): List<CheckinRecord>
    
    @Query("SELECT DISTINCT checkin_date FROM checkin_records ORDER BY checkin_date DESC")
    fun getAllDates(): List<String>
    
    @Insert
    suspend fun insert(record: CheckinRecord)
    
    @Query("DELETE FROM checkin_records WHERE checkin_date < :date")
    suspend fun deleteOldRecords(date: String)
    
    @Query("DELETE FROM checkin_records WHERE item_id = :itemId")
    suspend fun deleteByItemId(itemId: Int)
    
    @Query("DELETE FROM checkin_records WHERE item_id = :itemId AND checkin_date = :date")
    suspend fun deleteByItemIdAndDate(itemId: Int, date: String)
    
    @Query("SELECT * FROM checkin_records ORDER BY checkin_date DESC, checkin_time DESC")
    suspend fun getAllRecords(): List<CheckinRecord>

    @Query("SELECT * FROM checkin_records ORDER BY checkin_time DESC LIMIT :limit OFFSET :offset")
    suspend fun getRecordsPaged(limit: Int, offset: Int): List<CheckinRecord>

    @Query("SELECT * FROM checkin_records WHERE checkin_date >= :startDate AND checkin_date <= :endDate ORDER BY checkin_date ASC, checkin_time ASC")
    suspend fun getRecordsBetween(startDate: String, endDate: String): List<CheckinRecord>

    @Query("SELECT COUNT(*) FROM checkin_records WHERE item_id = :itemId")
    suspend fun getCountByItemId(itemId: Int): Int

    @Query("SELECT COUNT(*) FROM checkin_records")
    suspend fun getTotalCount(): Int

    @Query("SELECT COUNT(*) FROM checkin_records WHERE checkin_date >= :startDate AND checkin_date <= :endDate")
    suspend fun getCountBetween(startDate: String, endDate: String): Int

    @Query("SELECT DISTINCT checkin_date FROM checkin_records WHERE checkin_date >= :startDate AND checkin_date <= :endDate ORDER BY checkin_date ASC")
    suspend fun getDistinctDatesBetween(startDate: String, endDate: String): List<String>

    @Delete
    suspend fun delete(record: CheckinRecord)
}
