package com.alosir.task.util

import android.content.Context
import androidx.work.*
import com.alosir.task.worker.DailyCheckinRefreshWorker
import java.util.concurrent.TimeUnit

object CheckinResetScheduler {
    
    private const val WORK_NAME = "daily_checkin_refresh"
    
    fun schedule(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiresCharging(false)
            .setRequiresDeviceIdle(false)
            .build()
        
        val request = PeriodicWorkRequestBuilder<DailyCheckinRefreshWorker>(
            1, TimeUnit.DAYS
        )
            .setConstraints(constraints)
            .setInitialDelay(getInitialDelay(), TimeUnit.MILLISECONDS)
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            request
        )
    }
    
    private fun getInitialDelay(): Long {
        val calendar = java.util.Calendar.getInstance()
        val tomorrow = calendar.apply {
            add(java.util.Calendar.DAY_OF_MONTH, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        
        return tomorrow.timeInMillis - System.currentTimeMillis()
    }
    
    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
