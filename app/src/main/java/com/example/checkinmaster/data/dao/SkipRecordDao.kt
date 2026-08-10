package com.alosir.task.data.dao

import androidx.room.*
import com.alosir.task.data.entity.SkipRecord

@Dao
interface SkipRecordDao {

    @Query("SELECT * FROM skip_records WHERE item_id = :itemId AND skip_date = :date")
    suspend fun getByItemIdAndDate(itemId: Int, date: String): SkipRecord?

    @Query("SELECT * FROM skip_records WHERE skip_date >= :startDate AND skip_date <= :endDate ORDER BY skip_date ASC")
    suspend fun getBetween(startDate: String, endDate: String): List<SkipRecord>

    @Query("SELECT * FROM skip_records ORDER BY skip_date DESC")
    suspend fun getAll(): List<SkipRecord>

    @Insert
    suspend fun insert(record: SkipRecord)

    @Query("DELETE FROM skip_records WHERE item_id = :itemId AND skip_date = :date")
    suspend fun deleteByItemIdAndDate(itemId: Int, date: String)

    @Query("DELETE FROM skip_records WHERE item_id = :itemId")
    suspend fun deleteByItemId(itemId: Int)
}
