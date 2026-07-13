package com.alosir.task.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.alosir.task.R
import com.alosir.task.data.entity.CheckinType
import com.alosir.task.ui.MainActivity

object NotificationHelper {

    private const val CHANNEL_ID = "checkin_reminder"
    private const val CHANNEL_NAME = "签到提醒"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.reminder_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.reminder_channel_desc)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showReminderNotification(
        context: Context,
        itemId: Int,
        itemName: String,
        itemDescription: String,
        itemType: Int,
        packageName: String?
    ) {
        createNotificationChannel(context)

        val contentIntent = createMainPendingIntent(context, itemId)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_check_circle)
            .setContentTitle(itemName)
            .setContentText(itemDescription.ifEmpty { context.getString(R.string.reminder_tap_to_checkin) })
            .setStyle(NotificationCompat.BigTextStyle().bigText(itemDescription.ifEmpty { context.getString(R.string.reminder_tap_to_checkin) }))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)

        loadNotificationIcon(context, itemType, packageName)?.let {
            builder.setLargeIcon(it)
        }

        val notification = builder.build()
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(itemId, notification)
    }

    private fun createMainPendingIntent(context: Context, itemId: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(ReminderReceiver.EXTRA_ITEM_ID, itemId)
            putExtra("navigate_to_today", true)
        }
        return PendingIntent.getActivity(
            context,
            itemId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun loadNotificationIcon(
        context: Context,
        itemType: Int,
        packageName: String?
    ): android.graphics.Bitmap? {
        return try {
            when (itemType) {
                CheckinType.APP -> {
                    val drawable = packageName?.let { IconManager.loadAppIcon(context, it) }
                        ?: context.getDrawable(R.drawable.ic_default_app)
                    drawable?.toBitmap()
                }
                CheckinType.WEBSITE -> {
                    context.getDrawable(R.drawable.ic_notification_website)?.toBitmap()
                }
                else -> {
                    context.getDrawable(R.drawable.ic_notification_other)?.toBitmap()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun android.graphics.drawable.Drawable.toBitmap(): android.graphics.Bitmap {
        val bitmap = android.graphics.Bitmap.createBitmap(intrinsicWidth.coerceAtLeast(1), intrinsicHeight.coerceAtLeast(1), android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        setBounds(0, 0, canvas.width, canvas.height)
        draw(canvas)
        return bitmap
    }
}
