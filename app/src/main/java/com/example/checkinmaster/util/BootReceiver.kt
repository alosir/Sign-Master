package com.alosir.task.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.alosir.task.data.CheckinDatabase

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val database = CheckinDatabase.getDatabase(context)
                    val items = database.checkinItemDao().getAll()
                    ReminderScheduler.rescheduleAll(context, items)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
