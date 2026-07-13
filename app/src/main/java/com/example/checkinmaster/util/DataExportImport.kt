package com.alosir.task.util

import android.content.Context
import com.alosir.task.data.CheckinDatabase
import com.alosir.task.data.entity.CheckinItem
import com.alosir.task.data.entity.CheckinRecord
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*

object DataExportImport {

    private const val EXPORT_VERSION = "2.0.1"

    data class ExportData(
        val items: List<CheckinItem>,
        val records: List<CheckinRecord>,
        val exportTime: Long,
        val version: String
    )

    data class ImportResult(
        val success: Boolean,
        val importedItems: Int = 0,
        val importedRecords: Int = 0,
        val skippedItems: Int = 0,
        val skippedRecords: Int = 0,
        val errorMessage: String? = null
    ) {
        val totalImported: Int get() = importedItems + importedRecords
        val totalSkipped: Int get() = skippedItems + skippedRecords
    }

    suspend fun exportToJson(context: Context, file: File): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val json = exportToJsonString(context)
                file.writeText(json, Charsets.UTF_8)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun exportToJsonString(context: Context): String {
        return withContext(Dispatchers.IO) {
            val database = CheckinDatabase.getDatabase(context)
            val items = database.checkinItemDao().getAll()
            val records = database.checkinRecordDao().getAllRecords()

            val exportData = ExportData(
                items = items,
                records = records,
                exportTime = System.currentTimeMillis(),
                version = EXPORT_VERSION
            )

            Gson().toJson(exportData)
        }
    }

    suspend fun importFromJson(context: Context, file: File): ImportResult {
        return withContext(Dispatchers.IO) {
            try {
                val json = file.readText(Charsets.UTF_8)
                if (json.isBlank()) {
                    return@withContext ImportResult(success = false, errorMessage = "文件内容为空")
                }

                val gson = Gson()
                val exportDataType = object : TypeToken<ExportData>() {}.type
                val exportData = try {
                    gson.fromJson<ExportData>(json, exportDataType)
                } catch (e: JsonSyntaxException) {
                    return@withContext ImportResult(success = false, errorMessage = "JSON 格式错误：${e.message}")
                } catch (e: Exception) {
                    return@withContext ImportResult(success = false, errorMessage = "解析失败：${e.message}")
                }

                if (exportData == null) {
                    return@withContext ImportResult(success = false, errorMessage = "文件内容为空或格式不正确")
                }

                val database = CheckinDatabase.getDatabase(context)
                var importedItems = 0
                var importedRecords = 0
                var skippedItems = 0
                var skippedRecords = 0

                val existingItems = database.checkinItemDao().getAllSync().associateBy { it.id }

                for (item in exportData.items) {
                    try {
                        if (item.id == 0 || existingItems.containsKey(item.id)) {
                            skippedItems++
                        } else {
                            database.checkinItemDao().insert(item)
                            importedItems++
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        skippedItems++
                    }
                }

                for (record in exportData.records) {
                    try {
                        val existing = database.checkinRecordDao().getByItemIdAndDateSync(record.itemId, record.checkinDate)
                        if (existing != null) {
                            skippedRecords++
                        } else {
                            database.checkinRecordDao().insert(record)
                            importedRecords++
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        skippedRecords++
                    }
                }

                ImportResult(
                    success = true,
                    importedItems = importedItems,
                    importedRecords = importedRecords,
                    skippedItems = skippedItems,
                    skippedRecords = skippedRecords
                )
            } catch (e: IOException) {
                e.printStackTrace()
                ImportResult(success = false, errorMessage = "读取文件失败：${e.message}")
            } catch (e: Exception) {
                e.printStackTrace()
                ImportResult(success = false, errorMessage = "导入失败：${e.message}")
            }
        }
    }
}
