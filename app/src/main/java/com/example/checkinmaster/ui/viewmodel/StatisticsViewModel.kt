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
import com.alosir.task.util.StatisticsCalculator
import kotlinx.coroutines.launch
import java.util.*

class StatisticsViewModel(application: Application) : AndroidViewModel(application) {

    private val database = CheckinDatabase.getDatabase(application)
    private val itemDao = database.checkinItemDao()
    private val recordDao = database.checkinRecordDao()
    private val skipDao = database.skipRecordDao()

    private val _uiState = MutableLiveData(StatisticsUiState())
    val uiState: LiveData<StatisticsUiState> = _uiState

    private val calendar = Calendar.getInstance()

    init {
        loadStatistics()
    }

    fun loadStatistics() {
        viewModelScope.launch {
            try {
                val year = calendar.get(Calendar.YEAR)
                val month = calendar.get(Calendar.MONTH) + 1
                val items = itemDao.getAll()
                val allRecords = recordDao.getAllRecords()

                val (monthStart, monthEnd) = StatisticsCalculator.getMonthStartEnd(year, month)
                val monthRecords = recordDao.getRecordsBetween(monthStart, monthEnd)
                val monthSkips = skipDao.getBetween(monthStart, monthEnd)

                val typeDistribution = StatisticsCalculator.calculateTypeDistribution(allRecords, items)
                val monthlyRanking = StatisticsCalculator.calculateMonthlyRanking(monthRecords, items)
                val recordCountByItem = allRecords.groupBy { it.itemId }.mapValues { it.value.size }
                val calendarDays = buildCalendarDays(year, month, monthRecords, monthSkips, items, recordCountByItem)

                val (earliestYear, earliestMonth) = computeEarliestRecordMonth(allRecords)

                val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1

                _uiState.postValue(
                    StatisticsUiState(
                        year = year,
                        month = month,
                        monthDisplay = getMonthDisplay(year, month),
                        monthlyCheckins = monthRecords.size,
                        currentStreak = StatisticsCalculator.calculateOngoingStreak(allRecords),
                        typeDistribution = typeDistribution,
                        monthlyRanking = monthlyRanking,
                        calendarDays = calendarDays,
                        earliestYear = earliestYear,
                        earliestMonth = earliestMonth,
                        currentYear = currentYear,
                        currentMonth = currentMonth,
                        selectedDateRecords = emptyList()
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun computeEarliestRecordMonth(records: List<CheckinRecord>): Pair<Int, Int> {
        if (records.isEmpty()) {
            val now = Calendar.getInstance()
            return now.get(Calendar.YEAR) to (now.get(Calendar.MONTH) + 1)
        }

        val minDateStr = records.minByOrNull { it.checkinDate }?.checkinDate ?: return run {
            val now = Calendar.getInstance()
            now.get(Calendar.YEAR) to (now.get(Calendar.MONTH) + 1)
        }

        return try {
            val parts = minDateStr.split("-")
            parts[0].toInt() to parts[1].toInt()
        } catch (e: Exception) {
            val now = Calendar.getInstance()
            now.get(Calendar.YEAR) to (now.get(Calendar.MONTH) + 1)
        }
    }

    fun goToPreviousMonth() {
        val current = _uiState.value ?: return
        if (!canGoPrevious(current)) return
        calendar.add(Calendar.MONTH, -1)
        loadStatistics()
    }

    fun goToNextMonth() {
        val current = _uiState.value ?: return
        if (!canGoNext(current)) return
        calendar.add(Calendar.MONTH, 1)
        loadStatistics()
    }

    fun resetToCurrentMonth() {
        calendar.time = Date()
        loadStatistics()
    }

    private fun canGoPrevious(state: StatisticsUiState): Boolean {
        return state.year > state.earliestYear ||
                (state.year == state.earliestYear && state.month > state.earliestMonth)
    }

    private fun canGoNext(state: StatisticsUiState): Boolean {
        return state.year < state.currentYear ||
                (state.year == state.currentYear && state.month < state.currentMonth)
    }

    fun selectDate(dateStr: String) {
        viewModelScope.launch {
            try {
                val records = recordDao.getAllByDate(dateStr)
                val items = itemDao.getAll()
                val itemMap = items.associateBy { it.id }
                val recordItems = records.mapNotNull { record ->
                    itemMap[record.itemId]?.let { item -> RecordWithItem(record, item) }
                }
                val current = _uiState.value ?: return@launch
                _uiState.postValue(current.copy(selectedDate = dateStr, selectedDateRecords = recordItems))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearSelectedDate() {
        val current = _uiState.value ?: return
        _uiState.value = current.copy(selectedDate = null, selectedDateRecords = emptyList())
    }

    private fun buildCalendarDays(
        year: Int,
        month: Int,
        records: List<CheckinRecord>,
        skips: List<com.alosir.task.data.entity.SkipRecord>,
        items: List<CheckinItem>,
        recordCountByItem: Map<Int, Int>
    ): List<CalendarDay> {
        val daysInMonth = StatisticsCalculator.getDaysInMonth(year, month)
        val recordsByDate = records.groupBy { it.checkinDate }
        val skipsByDate = skips.groupBy { it.skipDate }
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        return (1..daysInMonth).map { day ->
            val dateStr = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month, day)
            val date = dateFormat.parse(dateStr) ?: Date()
            val dayRecords = recordsByDate[dateStr].orEmpty()
            val completedItemIds = dayRecords.map { it.itemId }.toSet()
            val skippedItemIds = skipsByDate[dateStr].orEmpty().map { it.itemId }.toSet()
            // 已终止或已过结束日期的任务，自终止/结束之日起不再计入应签到
            val scheduledItems = items.filter {
                CycleCalculator.isScheduledForDate(it, date) &&
                    !CycleCalculator.isTerminatedOnDate(it, recordCountByItem[it.id] ?: 0, date)
            }
            // 跳过的任务当天无需签到，从统计中排除；但跳过后又完成签到的仍按完成计
            val countableItems = scheduledItems.filter {
                !skippedItemIds.contains(it.id) || completedItemIds.contains(it.id)
            }
            val countableCount = countableItems.size
            val completedCount = countableItems.count { completedItemIds.contains(it.id) }

            // 全部完成、或部分完成+其余跳过 → 全部完成（绿色）；
            // 全部跳过且无任何完成 → 无记录
            val status = when {
                countableCount == 0 -> DayStatus.NONE
                completedCount >= countableCount -> DayStatus.FULL
                completedCount > 0 -> DayStatus.PARTIAL
                else -> DayStatus.NONE
            }
            CalendarDay(dateStr, day, status)
        }
    }

    private fun getMonthDisplay(year: Int, month: Int): String {
        return String.format(Locale.getDefault(), "%d年%d月", year, month)
    }

    data class StatisticsUiState(
        val year: Int = 0,
        val month: Int = 0,
        val monthDisplay: String = "",
        val monthlyCheckins: Int = 0,
        val currentStreak: Int = 0,
        val typeDistribution: StatisticsCalculator.TypeDistribution = StatisticsCalculator.TypeDistribution(0, 0, 0, 0),
        val monthlyRanking: List<StatisticsCalculator.MonthlyRankInfo> = emptyList(),
        val calendarDays: List<CalendarDay> = emptyList(),
        val earliestYear: Int = 0,
        val earliestMonth: Int = 1,
        val currentYear: Int = 0,
        val currentMonth: Int = 1,
        val selectedDate: String? = null,
        val selectedDateRecords: List<RecordWithItem> = emptyList()
    )

    data class CalendarDay(
        val date: String,
        val dayOfMonth: Int,
        val status: DayStatus
    )

    enum class DayStatus {
        NONE,
        PARTIAL,
        FULL
    }

    data class RecordWithItem(
        val record: CheckinRecord,
        val item: CheckinItem
    )
}
