package com.alosir.task.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import java.text.SimpleDateFormat
import java.util.*

@Entity(tableName = "checkin_items")
data class CheckinItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "type")
    val type: Int,
    
    @ColumnInfo(name = "package_name")
    val packageName: String? = null,
    
    @ColumnInfo(name = "url")
    val url: String? = null,
    
    @ColumnInfo(name = "description")
    val description: String? = null,
    
    @ColumnInfo(name = "icon_path")
    val iconPath: String,
    
    @ColumnInfo(name = "script_path")
    val scriptPath: String? = null,
    
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,
    
    @ColumnInfo(name = "cycle_type")
    val cycleType: Int = 0,
    
    @ColumnInfo(name = "cycle_value")
    val cycleValue: Int = 1,
    
    // 每周选中的星期几：1-7，JSON 数组字符串，如 "[1,3,5]"
    @ColumnInfo(name = "cycle_week_days")
    val cycleWeekDays: String? = null,
    
    // 每月选中的日期：1-31，32 表示最后一天，JSON 数组字符串，如 "[10,17,32]"
    @ColumnInfo(name = "cycle_month_days")
    val cycleMonthDays: String? = null,
    
    @ColumnInfo(name = "skip_holidays")
    val skipHolidays: Boolean = false,
    
    @ColumnInfo(name = "skip_weekends")
    val skipWeekends: Boolean = false,
    
    // 提醒时间，格式 HH:mm
    @ColumnInfo(name = "reminder_time")
    val reminderTime: String? = null,
    
    @ColumnInfo(name = "last_checkin_date")
    val lastCheckinDate: String? = null,
    
    @ColumnInfo(name = "created_at")
    val createdAt: String = getCurrentTimestamp(),
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: String = getCurrentTimestamp()
) {
    companion object {
        fun getCurrentTimestamp(): String {
            return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(Date())
        }
    }
}

object CheckinType {
    const val APP = 0
    const val WEBSITE = 1
    const val OTHER = 2
}

object CheckinCycleType {
    const val DAY = 0
    const val WEEK = 1
    const val MONTH = 2
}
