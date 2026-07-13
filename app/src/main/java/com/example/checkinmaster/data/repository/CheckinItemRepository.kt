package com.alosir.task.data.repository

import androidx.lifecycle.LiveData
import com.alosir.task.data.dao.CheckinItemDao
import com.alosir.task.data.dao.CheckinRecordDao
import com.alosir.task.data.entity.CheckinItem
import com.alosir.task.data.entity.CheckinRecord
import com.alosir.task.data.entity.CheckinType
import java.text.SimpleDateFormat
import java.util.*

class CheckinItemRepository(private val itemDao: CheckinItemDao, private val recordDao: CheckinRecordDao) {
    
    suspend fun getItemById(id: Int): CheckinItem? {
        return itemDao.getById(id)
    }
    
    suspend fun insertItem(item: CheckinItem): Long {
        return itemDao.insert(item)
    }
    
    suspend fun updateItem(item: CheckinItem) {
        itemDao.update(item)
    }
    
    suspend fun deleteItem(item: CheckinItem) {
        itemDao.delete(item)
    }
    
    suspend fun clearHistoryByItemId(itemId: Int) {
        recordDao.deleteByItemId(itemId)
    }
    
    suspend fun checkinToday(itemId: Int, isAuto: Boolean = false): Boolean {
        return try {
            val today = getCurrentDate()
            val existing = recordDao.getByDate(itemId, today)
            
            if (existing == null) {
                val record = CheckinRecord(
                    itemId = itemId,
                    checkinDate = today,
                    isAuto = isAuto,
                    status = CheckinRecord.STATUS_SUCCESS
                )
                recordDao.insert(record)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun isCheckinedToday(itemId: Int): Boolean {
        val today = getCurrentDate()
        return recordDao.getByDate(itemId, today) != null
    }
    
    suspend fun getCheckinCount(itemId: Int): Int {
        return recordDao.getByItem(itemId).size
    }
    
    suspend fun resetOldRecords() {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, -7)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = dateFormat.format(calendar.time)
        recordDao.deleteOldRecords(date)
    }
    
    private fun getCurrentDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
}
