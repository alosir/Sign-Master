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
     * 计算进行中的连续签到天数（用于统计页概览）。
     * 规则：有签到记录的日期（无论全部完成或部分完成）视为签过到。
     * 截至当天向前追溯，中断则停止；今天还没有记录时，从昨天开始计（当天尚未结束，不算中断）。
     */
    fun calculateOngoingStreak(records: List<CheckinRecord>, reference: Date = Date()): Int {
        val dates = getDistinctDates(records)
        if (dates.isEmpty()) return 0

        val calendar = Calendar.getInstance().apply {
            time = reference
            clearTime()
        }

        // 今天还没有签到记录时，从昨天开始计
        if (formatDate(calendar.time) !in dates) {
            calendar.add(Calendar.DAY_OF_MONTH, -1)
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
     * 计算当月签到排行：按各任务在给定记录集合（通常为当月记录）中的
     * 累计签到次数降序排列，无记录的任务不列入。
     */
    fun calculateMonthlyRanking(
        records: List<CheckinRecord>,
        items: List<CheckinItem>
    ): List<MonthlyRankInfo> {
        val itemMap = items.associateBy { it.id }
        return records.groupBy { it.itemId }
            .mapNotNull { (itemId, itemRecords) ->
                itemMap[itemId]?.let { MonthlyRankInfo(it, itemRecords.size) }
            }
            .sortedWith(compareByDescending<MonthlyRankInfo> { it.count }.thenBy { it.item.name })
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

    data class MonthlyRankInfo(
        val item: CheckinItem,
        val count: Int
    )
}
