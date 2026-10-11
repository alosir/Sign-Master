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
import com.alosir.task.data.dao.SkipRecordDao
import com.alosir.task.data.entity.AutomationScript
import com.alosir.task.data.entity.CheckinItem
import com.alosir.task.data.entity.CheckinRecord
import com.alosir.task.data.entity.SkipRecord

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

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE checkin_items ADD COLUMN end_type INTEGER NOT NULL DEFAULT 0"
        )
        database.execSQL(
            "ALTER TABLE checkin_items ADD COLUMN end_count INTEGER NOT NULL DEFAULT 0"
        )
        database.execSQL(
            "ALTER TABLE checkin_items ADD COLUMN end_date TEXT"
        )
        database.execSQL(
            "ALTER TABLE checkin_items ADD COLUMN terminated INTEGER NOT NULL DEFAULT 0"
        )
        database.execSQL(
            "ALTER TABLE checkin_items ADD COLUMN terminated_date TEXT"
        )
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS skip_records (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "item_id INTEGER NOT NULL, " +
                "skip_date TEXT NOT NULL, " +
                "created_at TEXT NOT NULL, " +
                "FOREIGN KEY(item_id) REFERENCES checkin_items(id) ON DELETE CASCADE" +
                ")"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_skip_records_item_id ON skip_records(item_id)"
        )
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 旧版本（v1.2.4 之前）的「跳过」只更新 last_checkin_date 而不留记录，
        // 导致这些日期在统计日历中被算作「计划了但没完成」（黄色）。
        // 将「有 last_checkin_date 但当天无签到记录」的历史数据补录为跳过记录。
        database.execSQL(
            "INSERT INTO skip_records (item_id, skip_date, created_at) " +
                "SELECT id, last_checkin_date, datetime('now', 'localtime') " +
                "FROM checkin_items " +
                "WHERE last_checkin_date IS NOT NULL " +
                "AND NOT EXISTS (SELECT 1 FROM checkin_records r " +
                "WHERE r.item_id = checkin_items.id AND r.checkin_date = checkin_items.last_checkin_date) " +
                "AND NOT EXISTS (SELECT 1 FROM skip_records s " +
                "WHERE s.item_id = checkin_items.id AND s.skip_date = checkin_items.last_checkin_date)"
        )
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 「每周」周期不再支持「跳过双休日」（若签到日就是周末，二者冲突会导致任务永不出现）。
        // 清除存量每周任务上的该配置。
        database.execSQL(
            "UPDATE checkin_items SET skip_weekends = 0 WHERE cycle_type = 1"
        )
    }
}

@Database(
    entities = [CheckinItem::class, CheckinRecord::class, SkipRecord::class, AutomationScript::class],
    version = 8,
    exportSchema = false
)
abstract class CheckinDatabase : RoomDatabase() {
    
    abstract fun checkinItemDao(): CheckinItemDao
    abstract fun checkinRecordDao(): CheckinRecordDao
    abstract fun skipRecordDao(): SkipRecordDao
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
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
