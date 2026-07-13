package com.alosir.task.util

import com.alosir.task.data.entity.CheckinItem
import com.alosir.task.data.entity.CheckinRecord
import com.alosir.task.data.entity.CheckinType
import java.text.SimpleDateFormat
import java.util.*

object StatisticsCalculator {

    private const val DATE_FORMAT = "yyyy-MM-dd"

    private fun dateFormat(): SimpleDateFormat = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())

    fun formatDate(date: Date): String = dateFormat().format(date)

    fun parseDate(dateStr: String): Date? = try {
        dateFormat().parse(dateStr)
    } catch (e: Exception) {
        null
    }

    fun getDaysInMonth(year: Int, month: Int): Int {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    fun getMonthStartEnd(year: Int, month: Int): Pair<String, String> {
        val days = getDaysInMonth(year, month)
        val start = String.format(Locale.getDefault(), "%04d-%02d-01", year, month)
        val end = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month, days)
        return start to end
    }

    fun getWeekStartEnd(reference: Date = Date()): Pair<String, String> {
        val calendar = Calendar.getInstance().apply { time = reference }
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val diffToMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
        calendar.add(Calendar.DAY_OF_MONTH, -diffToMonday)
        calendar.clearTime()
        val start = formatDate(calendar.time)
        calendar.add(Calendar.DAY_OF_MONTH, 6)
        val end = formatDate(calendar.time)
        return start to end
    }

    fun getDistinctDates(records: List<CheckinRecord>): Set<String> {
        return records.map { it.checkinDate }.toSortedSet()
    }

    /**
     * 计算当前连续签到天数。
     * 规则：从今天开始向前追溯，每一天都必须至少有一条签到记录，中断则停止。
     * 今天若无记录，则当前连续签到为 0。
     */
    fun calculateCurrentStreak(records: List<CheckinRecord>, reference: Date = Date()): Int {
        val dates = getDistinctDates(records)
        if (dates.isEmpty()) return 0

        val calendar = Calendar.getInstance().apply {
            time = reference
            clearTime()
        }

        var streak = 0
        while (true) {
            val dateStr = formatDate(calendar.time)
            if (dateStr in dates) {
                streak++
                calendar.add(Calendar.DAY_OF_MONTH, -1)
            } else {
                break
            }
        }
        return streak
    }

    /**
     * 计算历史最长连续签到天数。
     */
    fun calculateLongestStreak(records: List<CheckinRecord>): Int {
        val dates = getDistinctDates(records)
        if (dates.isEmpty()) return 0

        var maxStreak = 1
        var currentStreak = 1
        val sorted = dates.sorted()

        for (i in 1 until sorted.size) {
            if (isConsecutive(sorted[i - 1], sorted[i])) {
                currentStreak++
                maxStreak = maxOf(maxStreak, currentStreak)
            } else {
                currentStreak = 1
            }
        }
        return maxStreak
    }

    private fun isConsecutive(prev: String, next: String): Boolean {
        val prevDate = parseDate(prev) ?: return false
        val nextDate = parseDate(next) ?: return false
        val diffMillis = nextDate.time - prevDate.time
        val diffDays = diffMillis / (1000L * 60 * 60 * 24)
        return diffDays == 1L
    }

    /**
     * 计算日期区间内的完成率。
     * 规则：区间内至少有一条签到记录的天数 / 区间总天数。
     * 返回 0..100 的整数。
     */
    fun calculateCompletionRate(records: List<CheckinRecord>, totalDays: Int): Int {
        if (totalDays <= 0) return 0
        val distinctDays = getDistinctDates(records).size
        return (distinctDays * 100 / totalDays).coerceIn(0, 100)
    }

    /**
     * 按类型统计签到次数。
     */
    fun calculateTypeDistribution(
        records: List<CheckinRecord>,
        items: List<CheckinItem>
    ): TypeDistribution {
        val itemTypeMap = items.associateBy({ it.id }, { it.type })
        var app = 0
        var website = 0
        var other = 0

        records.forEach { record ->
            when (itemTypeMap[record.itemId]) {
                CheckinType.APP -> app++
                CheckinType.WEBSITE -> website++
                CheckinType.OTHER -> other++
            }
        }

        return TypeDistribution(
            appCount = app,
            websiteCount = website,
            otherCount = other,
            totalCount = app + website + other
        )
    }

    /**
     * 计算每个签到项的当前连续签到天数。
     */
    fun calculateStreakRanking(
        records: List<CheckinRecord>,
        items: List<CheckinItem>,
        reference: Date = Date()
    ): List<StreakInfo> {
        val recordsByItem = records.groupBy { it.itemId }
        return items.mapNotNull { item ->
            val itemRecords = recordsByItem[item.id] ?: return@mapNotNull null
            val streak = calculateCurrentStreak(itemRecords, reference)
            if (streak > 0) StreakInfo(item, streak) else null
        }
            .sortedByDescending { it.streak }
    }

    private fun Calendar.clearTime() {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    data class TypeDistribution(
        val appCount: Int,
        val websiteCount: Int,
        val otherCount: Int,
        val totalCount: Int
    ) {
        fun appPercent(): Int = if (totalCount == 0) 0 else appCount * 100 / totalCount
        fun websitePercent(): Int = if (totalCount == 0) 0 else websiteCount * 100 / totalCount
        fun otherPercent(): Int = if (totalCount == 0) 0 else otherCount * 100 / totalCount
    }

    data class StreakInfo(
        val item: CheckinItem,
        val streak: Int
    )
}
