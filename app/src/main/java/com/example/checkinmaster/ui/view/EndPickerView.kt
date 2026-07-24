package com.alosir.task.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.alosir.task.R
import com.alosir.task.data.entity.CheckinEndType
import com.alosir.task.databinding.ViewEndPickerBinding
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.*

/**
 * 结束签到选择器：永不结束 / 按次数 / 按日期
 */
class EndPickerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding = ViewEndPickerBinding.inflate(LayoutInflater.from(context), this, true)

    private var currentEndType: Int = CheckinEndType.NEVER
    private var currentEndCount: Int = 10
    private var selectedEndDate: String? = null

    private var calendarYear: Int
    private var calendarMonth: Int
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val monthFormat = SimpleDateFormat("yyyy年MM月", Locale.getDefault())

    init {
        orientation = VERTICAL

        val today = Calendar.getInstance()
        calendarYear = today.get(Calendar.YEAR)
        calendarMonth = today.get(Calendar.MONTH)

        setupToggleGroup()
        setupCountPicker()
        setupCalendar()
        refreshDescription()
    }

    private fun setupToggleGroup() {
        binding.endTypeToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            currentEndType = when (checkedId) {
                R.id.btnEndCount -> CheckinEndType.BY_COUNT
                R.id.btnEndDate -> CheckinEndType.BY_DATE
                else -> CheckinEndType.NEVER
            }
            updateVisibility()
            refreshDescription()
        }
        binding.endTypeToggleGroup.check(R.id.btnEndNever)
    }

    private fun setupCountPicker() {
        binding.endCountPicker.minValue = 1
        binding.endCountPicker.maxValue = 200
        binding.endCountPicker.value = currentEndCount
        binding.endCountPicker.wrapSelectorWheel = false

        binding.endCountPicker.setOnValueChangedListener { _, _, newVal ->
            currentEndCount = newVal
            refreshDescription()
        }
    }

    private fun setupCalendar() {
        binding.btnEndDatePrev.setOnClickListener {
            calendarMonth--
            if (calendarMonth < 0) {
                calendarMonth = 11
                calendarYear--
            }
            renderCalendar()
        }

        binding.btnEndDateNext.setOnClickListener {
            calendarMonth++
            if (calendarMonth > 11) {
                calendarMonth = 0
                calendarYear++
            }
            renderCalendar()
        }

        renderCalendar()
    }

    private fun renderCalendar() {
        binding.endDateCalendar.removeAllViews()

        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, calendarYear)
            set(Calendar.MONTH, calendarMonth)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        binding.endDateMonthText.text = monthFormat.format(cal.time)

        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val startOffset = if (firstDayOfWeek == Calendar.SUNDAY) 6 else firstDayOfWeek - Calendar.MONDAY

        val todayStr = dateFormat.format(Date())

        // 星期标题
        val weekDayNames = arrayOf("一", "二", "三", "四", "五", "六", "日")
        weekDayNames.forEach { name ->
            binding.endDateCalendar.addView(createTextCell(name, R.color.md_on_surface_variant))
        }

        // 前置空白
        for (i in 0 until startOffset) {
            binding.endDateCalendar.addView(createEmptyCell())
        }

        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (day in 1..daysInMonth) {
            cal.set(Calendar.DAY_OF_MONTH, day)
            val dateStr = dateFormat.format(cal.time)
            val isToday = dateStr == todayStr
            val isSelected = dateStr == selectedEndDate
            val isPast = dateStr < todayStr

            val cell = createDayCell(day, dateStr, isToday, isSelected, isPast)
            binding.endDateCalendar.addView(cell)
        }
    }

    private fun createEmptyCell(): TextView {
        return TextView(context).apply {
            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = 0
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            }
        }
    }

    private fun createTextCell(text: String, colorRes: Int): TextView {
        return TextView(context).apply {
            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = 32
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            }
            this.text = text
            textSize = 12f
            setTextColor(context.getColor(colorRes))
            gravity = android.view.Gravity.CENTER
        }
    }

    private fun createDayCell(
        day: Int,
        dateStr: String,
        isToday: Boolean,
        isSelected: Boolean,
        isPast: Boolean
    ): TextView {
        return TextView(context).apply {
            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = 36
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(2, 2, 2, 2)
            }
            this.text = day.toString()
            textSize = 13f
            gravity = android.view.Gravity.CENTER

            when {
                isSelected -> {
                    setBackgroundResource(R.drawable.bg_status_indicator)
                    backgroundTintList = android.content.res.ColorStateList.valueOf(
                        context.getColor(R.color.md_primary)
                    )
                    setTextColor(context.getColor(R.color.md_on_primary))
                }
                isToday -> {
                    setTextColor(context.getColor(R.color.md_primary))
                }
                isPast -> {
                    setTextColor(context.getColor(R.color.md_on_surface_variant))
                    alpha = 0.4f
                }
                else -> {
                    setTextColor(context.getColor(R.color.md_on_surface))
                }
            }

            if (!isPast) {
                setOnClickListener {
                    selectedEndDate = dateStr
                    renderCalendar()
                    refreshDescription()
                }
            }
        }
    }

    private fun updateVisibility() {
        binding.endCountContainer.visibility = if (currentEndType == CheckinEndType.BY_COUNT) VISIBLE else GONE
        binding.endDateContainer.visibility = if (currentEndType == CheckinEndType.BY_DATE) VISIBLE else GONE
    }

    private fun refreshDescription() {
        binding.endDescriptionText.text = when (currentEndType) {
            CheckinEndType.BY_COUNT -> context.getString(R.string.end_desc_count, currentEndCount)
            CheckinEndType.BY_DATE -> {
                selectedEndDate?.let {
                    val date = dateFormat.parse(it)
                    val display = if (date != null) {
                        SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault()).format(date)
                    } else it
                    context.getString(R.string.end_desc_date, display)
                } ?: context.getString(R.string.end_type_date)
            }
            else -> context.getString(R.string.end_desc_never)
        }
    }

    fun getEndRules(): EndRules {
        return EndRules(
            endType = currentEndType,
            endCount = if (currentEndType == CheckinEndType.BY_COUNT) currentEndCount else 0,
            endDate = if (currentEndType == CheckinEndType.BY_DATE) selectedEndDate else null
        )
    }

    fun setEndRules(endType: Int, endCount: Int, endDate: String?) {
        currentEndType = endType
        currentEndCount = endCount.coerceIn(1, 200)
        selectedEndDate = endDate

        binding.endTypeToggleGroup.check(
            when (endType) {
                CheckinEndType.BY_COUNT -> R.id.btnEndCount
                CheckinEndType.BY_DATE -> R.id.btnEndDate
                else -> R.id.btnEndNever
            }
        )

        binding.endCountPicker.value = currentEndCount
        updateVisibility()
        renderCalendar()
        refreshDescription()
    }

    data class EndRules(
        val endType: Int,
        val endCount: Int,
        val endDate: String?
    )
}
