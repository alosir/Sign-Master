package com.alosir.task.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "automation_scripts",
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
data class AutomationScript(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "item_id")
    val itemId: Int,
    
    @ColumnInfo(name = "actions")
    val actions: String,
    
    @ColumnInfo(name = "created_at")
    val createdAt: String = CheckinItem.getCurrentTimestamp()
)
