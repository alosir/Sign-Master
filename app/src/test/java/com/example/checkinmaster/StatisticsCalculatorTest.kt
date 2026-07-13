package com.alosir.task

import com.alosir.task.data.entity.CheckinItem
import com.alosir.task.data.entity.CheckinRecord
import com.alosir.task.data.entity.CheckinType
import com.alosir.task.util.StatisticsCalculator
import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.*

class StatisticsCalculatorTest {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private fun record(itemId: Int, dateStr: String, isAuto: Boolean = false): CheckinRecord {
        return CheckinRecord(
            itemId = itemId,
            checkinDate = dateStr,
            checkinTime = "$dateStr 08:00:00",
            isAuto = isAuto
        )
    }

    private fun item(id: Int, name: String, type: Int): CheckinItem {
        return CheckinItem(
            id = id,
            name = name,
            type = type,
            iconPath = ""
        )
    }

    @Test
    fun currentStreak_emptyRecords_returnsZero() {
        val result = StatisticsCalculator.calculateCurrentStreak(emptyList())
        assertEquals(0, result)
    }

    @Test
    fun currentStreak_todayOnly_returnsOne() {
        val today = StatisticsCalculator.formatDate(Date())
        val records = listOf(record(1, today))
        val result = StatisticsCalculator.calculateCurrentStreak(records)
        assertEquals(1, result)
    }

    @Test
    fun currentStreak_threeConsecutiveDays_returnsThree() {
        val calendar = Calendar.getInstance()
        val records = (0..2).map {
            val date = StatisticsCalculator.formatDate(calendar.time)
            calendar.add(Calendar.DAY_OF_MONTH, -1)
            record(1, date)
        }
        val result = StatisticsCalculator.calculateCurrentStreak(records)
        assertEquals(3, result)
    }

    @Test
    fun currentStreak_gapToday_returnsZero() {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        val yesterday = StatisticsCalculator.formatDate(calendar.time)
        val records = listOf(record(1, yesterday))
        val result = StatisticsCalculator.calculateCurrentStreak(records)
        assertEquals(0, result)
    }

    @Test
    fun longestStreak_emptyRecords_returnsZero() {
        val result = StatisticsCalculator.calculateLongestStreak(emptyList())
        assertEquals(0, result)
    }

    @Test
    fun longestStreak_multipleStreaks_returnsMax() {
        val calendar = Calendar.getInstance()
        val records = mutableListOf<CheckinRecord>()

        // 最近 2 天连续
        repeat(2) {
            val date = StatisticsCalculator.formatDate(calendar.time)
            records.add(record(1, date))
            calendar.add(Calendar.DAY_OF_MONTH, -1)
        }

        // 跳过 1 天
        calendar.add(Calendar.DAY_OF_MONTH, -1)

        // 之前 5 天连续
        repeat(5) {
            val date = StatisticsCalculator.formatDate(calendar.time)
            records.add(record(1, date))
            calendar.add(Calendar.DAY_OF_MONTH, -1)
        }

        val result = StatisticsCalculator.calculateLongestStreak(records)
        assertEquals(5, result)
    }

    @Test
    fun completionRate_emptyRecords_returnsZero() {
        val result = StatisticsCalculator.calculateCompletionRate(emptyList(), 7)
        assertEquals(0, result)
    }

    @Test
    fun completionRate_threeOutOfSeven_returnsCorrectRate() {
        val calendar = Calendar.getInstance()
        val records = (0..2).map {
            val date = StatisticsCalculator.formatDate(calendar.time)
            calendar.add(Calendar.DAY_OF_MONTH, -1)
            record(1, date)
        }
        val result = StatisticsCalculator.calculateCompletionRate(records, 7)
        assertEquals(42, result) // 3 / 7 = 0.428 -> 42%
    }

    @Test
    fun typeDistribution_calculatesCorrectly() {
        val items = listOf(
            item(1, "App1", CheckinType.APP),
            item(2, "Web1", CheckinType.WEBSITE),
            item(3, "Other1", CheckinType.OTHER)
        )
        val records = listOf(
            record(1, "2026-06-01"),
            record(1, "2026-06-02"),
            record(2, "2026-06-01"),
            record(3, "2026-06-01"),
            record(3, "2026-06-02")
        )

        val result = StatisticsCalculator.calculateTypeDistribution(records, items)
        assertEquals(2, result.appCount)
        assertEquals(1, result.websiteCount)
        assertEquals(2, result.otherCount)
        assertEquals(5, result.totalCount)
        assertEquals(40, result.appPercent())
        assertEquals(20, result.websitePercent())
        assertEquals(40, result.otherPercent())
    }

    @Test
    fun typeDistribution_unknownItemType_ignored() {
        val items = listOf(item(1, "App1", CheckinType.APP))
        val records = listOf(
            record(1, "2026-06-01"),
            record(99, "2026-06-01")
        )
        val result = StatisticsCalculator.calculateTypeDistribution(records, items)
        assertEquals(1, result.appCount)
        assertEquals(0, result.websiteCount)
        assertEquals(0, result.otherCount)
        assertEquals(1, result.totalCount)
    }

    @Test
    fun streakRanking_returnsTopN() {
        val items = listOf(
            item(1, "App1", CheckinType.APP),
            item(2, "Web1", CheckinType.WEBSITE),
            item(3, "Other1", CheckinType.OTHER)
        )

        val records = mutableListOf<CheckinRecord>()

        // Item 1: 3 天连续
        val cal1 = Calendar.getInstance()
        repeat(3) {
            records.add(record(1, StatisticsCalculator.formatDate(cal1.time)))
            cal1.add(Calendar.DAY_OF_MONTH, -1)
        }

        // Item 2: 5 天连续
        val cal2 = Calendar.getInstance()
        repeat(5) {
            records.add(record(2, StatisticsCalculator.formatDate(cal2.time)))
            cal2.add(Calendar.DAY_OF_MONTH, -1)
        }

        // Item 3: 无记录

        val result = StatisticsCalculator.calculateStreakRanking(records, items, topN = 2)
        assertEquals(2, result.size)
        assertEquals(2, result[0].item.id)
        assertEquals(5, result[0].streak)
        assertEquals(1, result[1].item.id)
        assertEquals(3, result[1].streak)
    }

    @Test
    fun getMonthStartEnd_returnsCorrectRange() {
        val (start, end) = StatisticsCalculator.getMonthStartEnd(2026, 2)
        assertEquals("2026-02-01", start)
        assertEquals("2026-02-28", end)
    }

    @Test
    fun getDaysInMonth_leapYearFebruary_returns29() {
        val result = StatisticsCalculator.getDaysInMonth(2024, 2)
        assertEquals(29, result)
    }
}
