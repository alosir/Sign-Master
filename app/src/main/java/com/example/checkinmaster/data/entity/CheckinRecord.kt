package com.alosir.task.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "checkin_records",
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
data class CheckinRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    
    @ColumnInfo(name = "item_id")
    val itemId: Int,
    
    @ColumnInfo(name = "checkin_date")
    val checkinDate: String,
    
    @ColumnInfo(name = "checkin_time")
    val checkinTime: String = CheckinItem.getCurrentTimestamp(),
    
    @ColumnInfo(name = "is_auto")
    val isAuto: Boolean = false,
    
    @ColumnInfo(name = "status")
    val status: Int = STATUS_SUCCESS
) {
    companion object {
        const val STATUS_SUCCESS = 1
        const val STATUS_FAILED = 0
    }
}
