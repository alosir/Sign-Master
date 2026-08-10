package com.alosir.task.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * 跳过记录：表示某任务在某天被用户标记为「当天无需签到」。
 * 跳过不等于完成，不计入签到记录，仅用于将当天从完成情况统计中排除。
 */
@Entity(
    tableName = "skip_records",
    foreignKeys = [
        ForeignKey(
            entity = CheckinItem::class,
            parentColumns = ["id"],
            childColumns = ["item_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["item_id"])]
)
data class SkipRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "item_id")
    val itemId: Int,

    @ColumnInfo(name = "skip_date")
    val skipDate: String,

    @ColumnInfo(name = "created_at")
    val createdAt: String = CheckinItem.getCurrentTimestamp()
)
