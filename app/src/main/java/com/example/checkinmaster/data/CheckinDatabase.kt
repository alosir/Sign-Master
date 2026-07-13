package com.alosir.task.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.alosir.task.data.dao.CheckinItemDao
import com.alosir.task.data.dao.CheckinRecordDao
import com.alosir.task.data.dao.ScriptDao
import com.alosir.task.data.entity.AutomationScript
import com.alosir.task.data.entity.CheckinItem
import com.alosir.task.data.entity.CheckinRecord

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE checkin_items ADD COLUMN cycle_type INTEGER NOT NULL DEFAULT 0"
        )
        database.execSQL(
            "ALTER TABLE checkin_items ADD COLUMN cycle_value INTEGER NOT NULL DEFAULT 1"
        )
        database.execSQL(
            "ALTER TABLE checkin_items ADD COLUMN last_checkin_date TEXT"
        )
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE checkin_items ADD COLUMN cycle_week_days TEXT"
        )
        database.execSQL(
            "ALTER TABLE checkin_items ADD COLUMN cycle_month_days TEXT"
        )
        database.execSQL(
            "ALTER TABLE checkin_items ADD COLUMN skip_holidays INTEGER NOT NULL DEFAULT 0"
        )
        database.execSQL(
            "ALTER TABLE checkin_items ADD COLUMN skip_weekends INTEGER NOT NULL DEFAULT 0"
        )
        database.execSQL(
            "ALTER TABLE checkin_items ADD COLUMN reminder_time TEXT"
        )
    }
}

@Database(
    entities = [CheckinItem::class, CheckinRecord::class, AutomationScript::class],
    version = 4,
    exportSchema = false
)
abstract class CheckinDatabase : RoomDatabase() {
    
    abstract fun checkinItemDao(): CheckinItemDao
    abstract fun checkinRecordDao(): CheckinRecordDao
    abstract fun scriptDao(): ScriptDao
    
    companion object {
        @Volatile
        private var INSTANCE: CheckinDatabase? = null
        
        fun getDatabase(context: Context): CheckinDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CheckinDatabase::class.java,
                    "checkin_database"
                )
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
