package com.alosir.task.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.alosir.task.data.CheckinDatabase
import com.alosir.task.data.entity.CheckinItem
import com.alosir.task.data.entity.CheckinRecord
import com.alosir.task.util.CycleCalculator
import com.alosir.task.util.ReminderScheduler
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CheckinListViewModel(application: Application) : AndroidViewModel(application) {

    private val database = CheckinDatabase.getDatabase(application)
    private val itemDao = database.checkinItemDao()
    private val recordDao = database.checkinRecordDao()

    private val appItems: LiveData<List<CheckinItem>> = itemDao.getAllByType(0)
    private val websiteItems: LiveData<List<CheckinItem>> = itemDao.getAllByType(1)
    private val otherItems: LiveData<List<CheckinItem>> = itemDao.getAllByType(2)

    private val _pendingItems = MutableLiveData<List<PendingItemUiModel>>()
    val pendingItems: LiveData<List<PendingItemUiModel>> = _pendingItems

    private val _allPendingItems = MutableLiveData<List<PendingItemUiModel>>()
    val allPendingItems: LiveData<List<PendingItemUiModel>> = _allPendingItems

    private val _completedRecords = MutableLiveData<List<CompletedRecordUiModel>>()
    val completedRecords: LiveData<List<CompletedRecordUiModel>> = _completedRecords

    private var refreshJob: Job? = null
    private var hasLoadedOnce = false

    private var currentCompletedPage = 0
    private var hasMoreCompleted = true
    private val completedPageSize = 30

    fun getItems(type: Int): LiveData<List<CheckinItem>> {
        return when (type) {
            0 -> appItems
            1 -> websiteItems
            else -> otherItems
        }
    }

    fun loadCheckinStatus() {
        if (hasLoadedOnce) {
            refreshAll()
            return
        }
        hasLoadedOnce = true
        refreshAll()
    }

    private fun refreshAll() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            try {
                refreshPendingItems()
                refreshAllPendingItems()
                refreshCompletedRecords(reset = true)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun refreshPendingItems() {
        val allItems = itemDao.getAll()
        val todayStr = todayString()

        val pending = allItems.mapNotNull { item ->
            val nextDate = CycleCalculator.getNextCheckinDate(item) ?: return@mapNotNull null
            if (dateOnly(nextDate) != todayStr) return@mapNotNull null
            PendingItemUiModel(item, nextDate)
        }.sortedWith(pendingComparator())

        _pendingItems.postValue(pending)
    }

    private suspend fun refreshAllPendingItems() {
        val allItems = itemDao.getAll()
        val todayStr = todayString()

        val pending = allItems.mapNotNull { item ->
            val nextDate = CycleCalculator.getNextCheckinDate(item) ?: return@mapNotNull null
            if (dateOnly(nextDate) < todayStr) return@mapNotNull null
            PendingItemUiModel(item, nextDate)
        }.sortedWith(pendingComparator())

        _allPendingItems.postValue(pending)
    }

    private fun dateOnly(date: Date): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)

    private fun pendingComparator(): Comparator<PendingItemUiModel> {
        return compareBy<PendingItemUiModel> { it.nextDate }
            .thenBy { reminderTimeToMinutes(it.item.reminderTime) }
            .thenBy { it.item.name }
    }

    private fun reminderTimeToMinutes(time: String?): Int {
        if (time.isNullOrBlank()) return Int.MAX_VALUE
        val parts = time.split(":")
        if (parts.size < 2) return Int.MAX_VALUE
        val hours = parts[0].toIntOrNull() ?: return Int.MAX_VALUE
        val minutes = parts[1].toIntOrNull() ?: return Int.MAX_VALUE
        if (hours !in 0..23 || minutes !in 0..59) return Int.MAX_VALUE
        return hours * 60 + minutes
    }

    private suspend fun refreshCompletedRecords(reset: Boolean) {
        if (reset) {
            currentCompletedPage = 0
            hasMoreCompleted = true
        }
        if (!hasMoreCompleted && !reset) return

        val offset = currentCompletedPage * completedPageSize
        val records = recordDao.getRecordsPaged(completedPageSize + 1, offset)
        hasMoreCompleted = records.size > completedPageSize
        val pageRecords = if (records.size > completedPageSize) records.take(completedPageSize) else records

        val itemMap = itemDao.getAll().associateBy { it.id }
        val uiModels = pageRecords.mapNotNull { record ->
            val item = itemMap[record.itemId] ?: return@mapNotNull null
            CompletedRecordUiModel(record, item)
        }

        if (reset) {
            _completedRecords.postValue(uiModels)
        } else {
            val current = _completedRecords.value ?: emptyList()
            _completedRecords.postValue(current + uiModels)
        }
    }

    fun loadMoreCompletedRecords() {
        if (!hasMoreCompleted) return
        currentCompletedPage++
        viewModelScope.launch {
            try {
                refreshCompletedRecords(reset = false)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun markCheckined(itemId: Int) {
        viewModelScope.launch {
            try {
                val today = todayString()
                val existing = recordDao.getByDate(itemId, today)
                if (existing == null) {
                    recordDao.insert(
                        CheckinRecord(
                            itemId = itemId,
                            checkinDate = today
                        )
                    )
                    itemDao.updateLastCheckinDate(itemId, today)
                    refreshPendingItems()
                    refreshAllPendingItems()
                    refreshCompletedRecords(reset = true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun resetCheckinTime(itemId: Int) {
        viewModelScope.launch {
            try {
                val today = todayString()
                val existing = recordDao.getByDate(itemId, today)
                if (existing != null) {
                    recordDao.deleteByItemIdAndDate(itemId, today)
                    itemDao.clearLastCheckinDate(itemId)
                    refreshPendingItems()
                    refreshAllPendingItems()
                    refreshCompletedRecords(reset = true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteItem(item: CheckinItem) {
        viewModelScope.launch {
            try {
                ReminderScheduler.cancel(getApplication(), item.id)
                itemDao.delete(item)
                refreshPendingItems()
                refreshAllPendingItems()
                refreshCompletedRecords(reset = true)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteRecord(record: CheckinRecord) {
        viewModelScope.launch {
            try {
                recordDao.delete(record)
                val today = todayString()
                if (record.checkinDate == today) {
                    val item = itemDao.getById(record.itemId)
                    if (item?.lastCheckinDate == today) {
                        itemDao.clearLastCheckinDate(record.itemId)
                    }
                }
                refreshPendingItems()
                refreshAllPendingItems()
                refreshCompletedRecords(reset = true)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun restoreTodayRecord(record: CheckinRecord) {
        viewModelScope.launch {
            try {
                recordDao.delete(record)
                val today = todayString()
                val item = itemDao.getById(record.itemId)
                if (item?.lastCheckinDate == today) {
                    itemDao.clearLastCheckinDate(record.itemId)
                }
                refreshPendingItems()
                refreshAllPendingItems()
                refreshCompletedRecords(reset = true)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun todayString(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    data class PendingItemUiModel(
        val item: CheckinItem,
        val nextDate: Date
    )

    data class CompletedRecordUiModel(
        val record: CheckinRecord,
        val item: CheckinItem
    )
}
