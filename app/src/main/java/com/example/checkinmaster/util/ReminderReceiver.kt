package com.alosir.task.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.alosir.task.data.CheckinDatabase
import com.alosir.task.data.entity.CheckinType
import com.alosir.task.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val itemId = intent.getIntExtra(EXTRA_ITEM_ID, 0)
        val itemName = intent.getStringExtra(EXTRA_ITEM_NAME) ?: return
        val itemDescription = intent.getStringExtra(EXTRA_ITEM_DESCRIPTION) ?: ""
        val itemType = intent.getIntExtra(EXTRA_ITEM_TYPE, CheckinType.OTHER)
        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME)

        if (itemId == 0) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = CheckinDatabase.getDatabase(context)
                val item = database.checkinItemDao().getById(itemId) ?: return@launch

                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val isCheckinedToday = database.checkinRecordDao().getByDate(itemId, today) != null

                if (!isCheckinedToday && CycleCalculator.isCheckinAvailable(item)) {
                    NotificationHelper.showReminderNotification(
                        context,
                        itemId,
                        itemName,
                        itemDescription,
                        itemType,
                        packageName
                    )
                }

                ReminderScheduler.schedule(context, item)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        const val ACTION_REMIND = "com.alosir.task.ACTION_REMIND"
        const val EXTRA_ITEM_ID = "item_id"
        const val EXTRA_ITEM_NAME = "item_name"
        const val EXTRA_ITEM_DESCRIPTION = "item_description"
        const val EXTRA_ITEM_TYPE = "item_type"
        const val EXTRA_PACKAGE_NAME = "package_name"
    }
}
