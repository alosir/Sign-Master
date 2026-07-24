package com.alosir.task.util

import com.alosir.task.data.entity.CheckinCycleType
import com.alosir.task.data.entity.CheckinEndType
import com.alosir.task.data.entity.CheckinItem
import java.text.SimpleDateFormat
import java.util.*

object CycleCalculator {

    fun isCheckinAvailable(
        cycleType: Int,
        cycleValue: Int,
        lastCheckinDate: String?,
        currentDate: Date = Date()
    ): Boolean {
        return isCheckinAvailableInternal(cycleType, cycleValue, null, null, false, false, lastCheckinDate, currentDate)
    }

    fun isCheckinAvailable(
        item: CheckinItem,
        currentDate: Date = Date()
    ): Boolean {
        // 已终止任务不可签到
        if (item.terminated == 1) return false

        // 超过结束日期不可签到
        val endDate = parseDateOnly(item.endDate)
        if (endDate != null && currentDate.after(endDate)) return false

        return isCheckinAvailableInternal(
            item.cycleType,
            item.cycleValue,
            parseJsonIntArray(item.cycleWeekDays),
            parseJsonIntArray(item.cycleMonthDays),
            item.skipHolidays,
            item.skipWeekends,
            item.lastCheckinDate,
            currentDate
        )
    }

    private fun isCheckinAvailableInternal(
        cycleType: Int,
        cycleValue: Int,
        cycleWeekDays: List<Int>?,
        cycleMonthDays: List<Int>?,
        skipHolidays: Boolean,
        skipWeekends: Boolean,
        lastCheckinDate: String?,
        currentDate: Date
    ): Boolean {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val currentCal = Calendar.getInstance().apply {
            time = currentDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (shouldSkipDate(currentCal, skipHolidays, skipWeekends)) {
            return false
        }

        val todayStr = dateFormat.format(currentDate)
        if (lastCheckinDate == todayStr) {
            return false
        }

        if (lastCheckinDate == null) {
            return matchesCycleDate(cycleType, cycleWeekDays, cycleMonthDays, currentCal)
        }

        val lastDate = dateFormat.parse(lastCheckinDate) ?: return true
        val lastCal = Calendar.getInstance().apply {
            time = lastDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        when (cycleType) {
            CheckinCycleType.DAY -> {
                if (cycleValue == 1) {
                    return !isSameDay(currentCal, lastCal)
                } else {
                    val diffDays = ((currentCal.timeInMillis - lastCal.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
                    return diffDays >= cycleValue
                }
            }

            CheckinCycleType.WEEK -> {
                if (!cycleWeekDays.isNullOrEmpty()) {
                    return matchesWeekDays(cycleWeekDays, currentCal)
                }

                val isMonday = currentCal.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY
                val baseCal = Calendar.getInstance().apply { time = lastDate }
                while (baseCal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
                    baseCal.add(Calendar.DAY_OF_MONTH, -1)
                }
                baseCal.set(Calendar.HOUR_OF_DAY, 0)
                baseCal.set(Calendar.MINUTE, 0)
                baseCal.set(Calendar.SECOND, 0)
                baseCal.set(Calendar.MILLISECOND, 0)

                val currentWeekMonday = Calendar.getInstance().apply { time = currentCal.time }
                while (currentWeekMonday.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
                    currentWeekMonday.add(Calendar.DAY_OF_MONTH, -1)
                }
                currentWeekMonday.set(Calendar.HOUR_OF_DAY, 0)
                currentWeekMonday.set(Calendar.MINUTE, 0)
                currentWeekMonday.set(Calendar.SECOND, 0)
                currentWeekMonday.set(Calendar.MILLISECOND, 0)

                val weeksPassed = ((currentWeekMonday.timeInMillis - baseCal.timeInMillis) / (1000L * 60 * 60 * 24 * 7)).toInt()
                return isMonday && weeksPassed >= cycleValue && weeksPassed % cycleValue == 0
            }

            CheckinCycleType.MONTH -> {
                if (!cycleMonthDays.isNullOrEmpty()) {
                    return matchesMonthDays(cycleMonthDays, currentCal)
                }

                val isFirstDay = currentCal.get(Calendar.DAY_OF_MONTH) == 1
                val baseCal = Calendar.getInstance().apply {
                    time = lastDate
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val currentMonthFirst = Calendar.getInstance().apply {
                    time = currentCal.time
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val diffMonths = (currentMonthFirst.get(Calendar.YEAR) - baseCal.get(Calendar.YEAR)) * 12 +
                        (currentMonthFirst.get(Calendar.MONTH) - baseCal.get(Calendar.MONTH))
                return isFirstDay && diffMonths >= cycleValue && diffMonths % cycleValue == 0
            }

            else -> return true
        }
    }

    /**
     * 判断某任务在某日期是否处于“应签到”状态（只看周期和创建时间，不看是否已完成）。
     */
    fun isScheduledForDate(item: CheckinItem, date: Date): Boolean {
        val cal = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (shouldSkipDate(cal, item.skipHolidays, item.skipWeekends)) {
            return false
        }

        val createdAtDate = try {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(item.createdAt)
        } catch (e: Exception) {
            null
        }
        if (createdAtDate != null && date.before(createdAtDate)) {
            return false
        }

        return matchesCycleDate(
            item.cycleType,
            parseJsonIntArray(item.cycleWeekDays),
            parseJsonIntArray(item.cycleMonthDays),
            cal
        )
    }

    /**
     * 判断任务是否已终止（手动终止或自动达到结束条件）。
     */
    fun isTerminated(item: CheckinItem, recordCount: Int, currentDate: Date = Date()): Boolean {
        if (item.terminated == 1) return true

        return when (item.endType) {
            CheckinEndType.BY_COUNT -> item.endCount > 0 && recordCount >= item.endCount
            CheckinEndType.BY_DATE -> {
                val endDate = parseDateOnly(item.endDate) ?: return false
                val today = Calendar.getInstance().apply {
                    time = currentDate
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                today.after(endDate)
            }
            else -> false
        }
    }

    /**
     * 判断任务在指定日期是否已终止（用于历史日历统计）。
     */
    fun isTerminatedOnDate(item: CheckinItem, recordCount: Int, date: Date): Boolean {
        if (item.terminated == 1) {
            val terminatedDate = parseDateOnly(item.terminatedDate)
            return terminatedDate != null && !date.before(terminatedDate)
        }

        return when (item.endType) {
            CheckinEndType.BY_COUNT -> item.endCount > 0 && recordCount >= item.endCount
            CheckinEndType.BY_DATE -> {
                val endDate = parseDateOnly(item.endDate) ?: return false
                date.after(endDate)
            }
            else -> false
        }
    }

    /**
     * 获取任务的实际结束日期（按次数时推算最后一次签到日）。
     */
    fun getEffectiveEndDate(item: CheckinItem, recordCount: Int): Date? {
        return when (item.endType) {
            CheckinEndType.BY_DATE -> parseDateOnly(item.endDate)
            CheckinEndType.BY_COUNT -> {
                if (item.endCount <= 0) return null
                // 从最近一个待签到日期开始推算第 N 次签到日期
                val startDate = getNextCheckinDate(item) ?: parseDateOnly(item.createdAt) ?: return null
                var count = recordCount
                var current = startDate
                val maxIterations = item.endCount + 10
                var iterations = 0

                while (count < item.endCount && iterations < maxIterations) {
                    if (isScheduledForDate(item, current)) {
                        count++
                    }
                    if (count >= item.endCount) break

                    val next = getNextCheckinDateAfter(item, current)
                    if (next == null || next.before(current) || isSameDay(
                            Calendar.getInstance().apply { time = next },
                            Calendar.getInstance().apply { time = current }
                        )
                    ) {
                        break
                    }
                    current = next
                    iterations++
                }
                current
            }
            else -> null
        }
    }

    private fun getNextCheckinDateAfter(item: CheckinItem, date: Date): Date? {
        val next = getNextCheckinDate(item, date) ?: return null
        return if (isSameDay(
                Calendar.getInstance().apply { time = next },
                Calendar.getInstance().apply { time = date }
            )
        ) {
            // 如果算出同一天，往后推一天再算
            val tomorrow = Calendar.getInstance().apply {
                time = date
                add(Calendar.DAY_OF_MONTH, 1)
            }
            getNextCheckinDate(item, tomorrow.time)
        } else next
    }

    private fun parseDateOnly(dateStr: String?): Date? {
        if (dateStr.isNullOrEmpty()) return null
        return try {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)
        } catch (e: Exception) {
            try {
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(dateStr)
            } catch (e2: Exception) {
                null
            }
        }
    }

    fun getCheckinStatus(item: CheckinItem, currentDate: Date = Date()): CheckinStatus {
        return if (isCheckinAvailable(item, currentDate)) CheckinStatus.PENDING else CheckinStatus.COMPLETED
    }

    fun getCheckinStatus(
        cycleType: Int,
        cycleValue: Int,
        lastCheckinDate: String?,
        currentDate: Date = Date()
    ): CheckinStatus {
        return if (isCheckinAvailable(cycleType, cycleValue, lastCheckinDate, currentDate)) {
            CheckinStatus.PENDING
        } else {
            CheckinStatus.COMPLETED
        }
    }

    fun getCycleDescription(item: CheckinItem): String {
        return getCycleShortDescription(item)
    }

    fun getCycleShortDescription(item: CheckinItem): String {
        val weekDays = parseJsonIntArray(item.cycleWeekDays)
        val monthDays = parseJsonIntArray(item.cycleMonthDays)

        return when (item.cycleType) {
            CheckinCycleType.DAY -> {
                if (item.cycleValue == 1) "每天" else "每${item.cycleValue}天"
            }
            CheckinCycleType.WEEK -> {
                if (weekDays.isNotEmpty()) {
                    val sorted = weekDays.sorted()
                    if (sorted.size <= 4) {
                        "每周 ${sorted.map { getDayShortName(it) }.joinToString("、")}"
                    } else {
                        "每周 ${sorted.take(3).map { getDayShortName(it) }.joinToString("、")}…等${sorted.size}天"
                    }
                } else {
                    if (item.cycleValue == 1) "每周一" else "每${item.cycleValue}周"
                }
            }
            CheckinCycleType.MONTH -> {
                if (monthDays.isNotEmpty()) {
                    val sorted = monthDays.sorted()
                    val labels = sorted.map { if (it == LAST_DAY_OF_MONTH) "最后一天" else "${it}号" }
                    if (labels.size <= 4) {
                        "每月 ${labels.joinToString("、")}"
                    } else {
                        "每月 ${labels.take(3).joinToString("、")}…等${labels.size}天"
                    }
                } else {
                    if (item.cycleValue == 1) "每月1号" else "每${item.cycleValue}个月"
                }
            }
            else -> "每天"
        }
    }

    fun getNextCheckinDate(item: CheckinItem, from: Date = Date()): Date? {
        // 已终止的任务不再返回下次签到日期
        if (item.terminated == 1) return null

        val endDate = parseDateOnly(item.endDate)
        val cal = Calendar.getInstance().apply {
            time = from
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val weekDays = parseJsonIntArray(item.cycleWeekDays)
        val monthDays = parseJsonIntArray(item.cycleMonthDays)

        repeat(366) {
            if (isCheckinAvailableInternal(
                    item.cycleType,
                    item.cycleValue,
                    weekDays,
                    monthDays,
                    item.skipHolidays,
                    item.skipWeekends,
                    item.lastCheckinDate,
                    cal.time
                )
            ) {
                // 如果找到可签到日期，需同时满足未超过结束日期
                if (endDate == null || !cal.time.after(endDate)) {
                    return cal.time
                }
                return null
            }
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        return null
    }

    fun formatRelativeDate(date: Date, base: Date = Date()): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateStr = dateFormat.format(date)
        val baseStr = dateFormat.format(base)
        if (dateStr == baseStr) return "今天"

        val baseCal = Calendar.getInstance().apply { time = base; clearTime() }
        val targetCal = Calendar.getInstance().apply { time = date; clearTime() }

        val diffMillis = targetCal.timeInMillis - baseCal.timeInMillis
        val diffDays = (diffMillis / (1000L * 60 * 60 * 24)).toInt()

        when (diffDays) {
            1 -> return "明天"
            2 -> return "后天"
        }

        val baseWeekday = baseCal.get(Calendar.DAY_OF_WEEK)
        // 将周日(1)视为7，周一=1...周六=6
        val baseWeekdayAdjusted = if (baseWeekday == Calendar.SUNDAY) 7 else baseWeekday - 1
        val daysToThisSunday = 7 - baseWeekdayAdjusted

        if (diffDays in 0..daysToThisSunday) {
            val targetWeekday = targetCal.get(Calendar.DAY_OF_WEEK)
            return when (targetWeekday) {
                Calendar.SATURDAY -> "本周六"
                Calendar.SUNDAY -> "本周日"
                else -> "${diffDays}天后"
            }
        }

        if (diffDays <= daysToThisSunday + 7) {
            return "下周${getDayShortName(getAdjustedWeekday(targetCal))}"
        }

        return "${diffDays}天后"
    }

    fun formatRelativeRecordDate(date: Date, base: Date = Date()): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateStr = dateFormat.format(date)
        val baseStr = dateFormat.format(base)
        if (dateStr == baseStr) return "今天"

        val baseCal = Calendar.getInstance().apply { time = base; clearTime() }
        val targetCal = Calendar.getInstance().apply { time = date; clearTime() }
        val diffDays = ((baseCal.timeInMillis - targetCal.timeInMillis) / (1000L * 60 * 60 * 24)).toInt()

        return when (diffDays) {
            1 -> "昨天"
            2 -> "前天"
            else -> "${diffDays}天前"
        }
    }

    private fun Calendar.clearTime() {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private fun getAdjustedWeekday(cal: Calendar): Int {
        val day = cal.get(Calendar.DAY_OF_WEEK)
        return if (day == Calendar.SUNDAY) 7 else day - 1
    }

    private fun matchesCycleDate(
        cycleType: Int,
        cycleWeekDays: List<Int>?,
        cycleMonthDays: List<Int>?,
        currentCal: Calendar
    ): Boolean {
        return when (cycleType) {
            CheckinCycleType.WEEK -> {
                if (!cycleWeekDays.isNullOrEmpty()) matchesWeekDays(cycleWeekDays, currentCal)
                else currentCal.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY
            }
            CheckinCycleType.MONTH -> {
                if (!cycleMonthDays.isNullOrEmpty()) matchesMonthDays(cycleMonthDays, currentCal)
                else currentCal.get(Calendar.DAY_OF_MONTH) == 1
            }
            else -> true
        }
    }

    private fun matchesWeekDays(weekDays: List<Int>, currentCal: Calendar): Boolean {
        val dayOfWeek = currentCal.get(Calendar.DAY_OF_WEEK)
        val adjustedDay = when (dayOfWeek) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }
        return adjustedDay in weekDays
    }

    private fun matchesMonthDays(monthDays: List<Int>, currentCal: Calendar): Boolean {
        val dayOfMonth = currentCal.get(Calendar.DAY_OF_MONTH)
        val lastDay = currentCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        return monthDays.any { it == dayOfMonth || (it == LAST_DAY_OF_MONTH && dayOfMonth == lastDay) }
    }

    private fun shouldSkipDate(calendar: Calendar, skipHolidays: Boolean, skipWeekends: Boolean): Boolean {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateStr = dateFormat.format(calendar.time)

        if (skipWeekends) {
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
                return true
            }
        }

        if (skipHolidays && HolidayHelper.isHoliday(dateStr)) {
            return true
        }

        return false
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
    }

    private fun parseJsonIntArray(json: String?): List<Int> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val array = org.json.JSONArray(json)
            (0 until array.length()).map { array.getInt(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun getDayShortName(day: Int): String {
        return when (day) {
            1 -> "一"
            2 -> "二"
            3 -> "三"
            4 -> "四"
            5 -> "五"
            6 -> "六"
            7 -> "日"
            else -> ""
        }
    }

    private const val LAST_DAY_OF_MONTH = 32
}

enum class CheckinStatus {
    PENDING,
    COMPLETED,
    WAITING
}
