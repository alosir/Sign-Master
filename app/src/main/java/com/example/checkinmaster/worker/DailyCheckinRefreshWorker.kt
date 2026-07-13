package com.alosir.task.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.alosir.task.data.CheckinDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DailyCheckinRefreshWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    
    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                val database = CheckinDatabase.getDatabase(applicationContext)
                val recordDao = database.checkinRecordDao()
                
                // 清理 365 天前的签到记录，以支持年度统计与连续签到展示
                val calendar = java.util.Calendar.getInstance()
                calendar.add(java.util.Calendar.DAY_OF_MONTH, -365)
                val cutoffDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    .format(calendar.time)
                recordDao.deleteOldRecords(cutoffDate)
                
                Result.success()
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure()
            }
        }
    }
}
