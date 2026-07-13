package com.alosir.task.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.NumberPicker
import com.alosir.task.R
import com.alosir.task.data.entity.CheckinCycleType
import com.alosir.task.databinding.ViewCyclePickerBinding
import com.google.android.material.button.MaterialButton
import org.json.JSONArray

class CyclePickerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding = ViewCyclePickerBinding.inflate(LayoutInflater.from(context), this, true)

    private var currentType: Int = CheckinCycleType.DAY
    private var currentValue: Int = 1
    private val selectedWeekDays = mutableSetOf<Int>()
    private val selectedMonthDays = mutableSetOf<Int>()

    private var onCycleChangedListener: ((type: Int, value: Int, weekDays: Set<Int>, monthDays: Set<Int>, skipHolidays: Boolean, skipWeekends: Boolean) -> Unit)? = null

    private val unitValues = arrayOf(
        context.getString(R.string.cycle_unit_day),
        context.getString(R.string.cycle_unit_week),
        context.getString(R.string.cycle_unit_month)
    )

    init {
        orientation = VERTICAL

        setupQuickChips()
        setupCustomPickers()
        setupWeekDayChips()
        setupMonthDayChips()
        setupSkipCheckBoxes()
        refreshDescription()
    }

    private fun setupQuickChips() {
        binding.quickCycleChipGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            when (checkedId) {
                R.id.chipDaily -> switchMode(CheckinCycleType.DAY, 1)
                R.id.chipWeekly -> switchMode(CheckinCycleType.WEEK, 1)
                R.id.chipMonthly -> switchMode(CheckinCycleType.MONTH, 1)
                R.id.chipCustom -> switchMode(CheckinCycleType.DAY, currentValue.coerceAtLeast(1), true)
            }
        }
        binding.quickCycleChipGroup.check(R.id.chipDaily)
    }

    private fun switchMode(type: Int, value: Int, isCustom: Boolean = false) {
        currentType = type
        currentValue = value.coerceAtLeast(1)

        binding.customCycleContainer.visibility = if (isCustom) VISIBLE else GONE
        binding.weekDayChipGroup.visibility = if (type == CheckinCycleType.WEEK) VISIBLE else GONE
        binding.monthDayChipGroup.visibility = if (type == CheckinCycleType.MONTH) VISIBLE else GONE

        if (isCustom) {
            updateCustomPickersFromCurrent()
        }

        refreshDescription()
        notifyChanged()
    }

    private fun setupCustomPickers() {
        binding.cycleValuePicker.minValue = 1
        binding.cycleValuePicker.maxValue = 31
        binding.cycleValuePicker.wrapSelectorWheel = false

        binding.cycleValuePicker.setOnValueChangedListener { _, _, newVal ->
            if (isCustomMode()) {
                currentValue = newVal
                refreshDescription()
                notifyChanged()
            }
        }

        binding.cycleUnitPicker.minValue = 0
        binding.cycleUnitPicker.maxValue = unitValues.size - 1
        binding.cycleUnitPicker.displayedValues = unitValues
        binding.cycleUnitPicker.wrapSelectorWheel = false

        binding.cycleUnitPicker.setOnValueChangedListener { _, _, newVal ->
            if (isCustomMode()) {
                currentType = when (newVal) {
                    1 -> CheckinCycleType.WEEK
                    2 -> CheckinCycleType.MONTH
                    else -> CheckinCycleType.DAY
                }
                updateValuePickerMax()
                refreshDescription()
                notifyChanged()
            }
        }
    }

    private fun setupWeekDayChips() {
        val chipToDay = mapOf(
            R.id.chipMon to 1,
            R.id.chipTue to 2,
            R.id.chipWed to 3,
            R.id.chipThu to 4,
            R.id.chipFri to 5,
            R.id.chipSat to 6,
            R.id.chipSun to 7
        )

        chipToDay.forEach { (chipId, day) ->
            val chip = binding.weekDayChipGroup.findViewById<MaterialButton>(chipId)
            chip?.addOnCheckedChangeListener { _, isChecked ->
                if (isChecked) selectedWeekDays.add(day) else selectedWeekDays.remove(day)
                refreshDescription()
                notifyChanged()
            }
        }
    }

    private fun setupMonthDayChips() {
        binding.monthDayChipGroup.removeAllViews()

        for (day in 1..31) {
            val chip = createMonthDayButton(day.toString(), day, 1)
            binding.monthDayChipGroup.addView(chip)
        }

        val lastDayChip = createMonthDayButton(context.getString(R.string.cycle_last_day), LAST_DAY_OF_MONTH, 2)
        binding.monthDayChipGroup.addView(lastDayChip)
    }

    private fun createMonthDayButton(text: String, tag: Int, columnSpan: Int): MaterialButton {
        val button = MaterialButton(context, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = ViewGroup.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, columnSpan, 1f)
                setMargins(4, 4, 4, 4)
            }
            this.text = text
            this.tag = tag
            isCheckable = true
            setPadding(0, 0, 0, 0)
            textSize = 13f
        }
        button.addOnCheckedChangeListener { _, isChecked ->
            val dayValue = button.tag as? Int ?: return@addOnCheckedChangeListener
            if (isChecked) selectedMonthDays.add(dayValue) else selectedMonthDays.remove(dayValue)
            refreshDescription()
            notifyChanged()
        }
        return button
    }

    private fun setupSkipCheckBoxes() {
        binding.checkSkipHolidays.setOnCheckedChangeListener { _, _ ->
            refreshDescription()
            notifyChanged()
        }
        binding.checkSkipWeekends.setOnCheckedChangeListener { _, _ ->
            refreshDescription()
            notifyChanged()
        }
    }

    private fun updateCustomPickersFromCurrent() {
        updateValuePickerMax()
        binding.cycleValuePicker.value = currentValue.coerceIn(
            binding.cycleValuePicker.minValue,
            binding.cycleValuePicker.maxValue
        )

        binding.cycleUnitPicker.value = when (currentType) {
            CheckinCycleType.WEEK -> 1
            CheckinCycleType.MONTH -> 2
            else -> 0
        }
    }

    private fun updateValuePickerMax() {
        val max = when (currentType) {
            CheckinCycleType.DAY -> 31
            CheckinCycleType.WEEK -> 52
            CheckinCycleType.MONTH -> 36
            else -> 31
        }
        binding.cycleValuePicker.maxValue = max
        if (currentValue > max) {
            currentValue = max
            binding.cycleValuePicker.value = max
        }
    }

    private fun isCustomMode(): Boolean {
        return binding.quickCycleChipGroup.checkedButtonId == R.id.chipCustom
    }

    fun setCycleRules(
        type: Int,
        value: Int,
        weekDaysJson: String?,
        monthDaysJson: String?,
        skipHolidays: Boolean,
        skipWeekends: Boolean
    ) {
        currentType = type
        currentValue = value.coerceAtLeast(1)

        selectedWeekDays.clear()
        selectedWeekDays.addAll(parseJsonIntArray(weekDaysJson))

        selectedMonthDays.clear()
        selectedMonthDays.addAll(parseJsonIntArray(monthDaysJson))

        binding.checkSkipHolidays.isChecked = skipHolidays
        binding.checkSkipWeekends.isChecked = skipWeekends

        when (type) {
            CheckinCycleType.DAY -> {
                if (value == 1) {
                    binding.quickCycleChipGroup.check(R.id.chipDaily)
                    switchMode(type, value)
                } else {
                    binding.quickCycleChipGroup.check(R.id.chipCustom)
                    switchMode(type, value, true)
                }
            }
            CheckinCycleType.WEEK -> {
                if (value == 1 && selectedWeekDays.isNotEmpty()) {
                    binding.quickCycleChipGroup.check(R.id.chipWeekly)
                    switchMode(type, value)
                } else {
                    binding.quickCycleChipGroup.check(R.id.chipCustom)
                    switchMode(type, value, true)
                }
            }
            CheckinCycleType.MONTH -> {
                if (value == 1 && selectedMonthDays.isNotEmpty()) {
                    binding.quickCycleChipGroup.check(R.id.chipMonthly)
                    switchMode(type, value)
                } else {
                    binding.quickCycleChipGroup.check(R.id.chipCustom)
                    switchMode(type, value, true)
                }
            }
            else -> {
                binding.quickCycleChipGroup.check(R.id.chipCustom)
                switchMode(type, value, true)
            }
        }

        updateWeekDayChips()
        updateMonthDayChips()
        refreshDescription()
    }

    private fun updateWeekDayChips() {
        val chipToDay = mapOf(
            R.id.chipMon to 1,
            R.id.chipTue to 2,
            R.id.chipWed to 3,
            R.id.chipThu to 4,
            R.id.chipFri to 5,
            R.id.chipSat to 6,
            R.id.chipSun to 7
        )
        chipToDay.forEach { (chipId, day) ->
            val chip = binding.weekDayChipGroup.findViewById<MaterialButton>(chipId)
            chip?.isChecked = day in selectedWeekDays
        }
    }

    private fun updateMonthDayChips() {
        for (i in 0 until binding.monthDayChipGroup.childCount) {
            val chip = binding.monthDayChipGroup.getChildAt(i) as? MaterialButton ?: continue
            val tag = chip.tag
            val day = when (tag) {
                is Int -> tag
                else -> chip.text.toString().toIntOrNull() ?: LAST_DAY_OF_MONTH
            }
            chip.isChecked = day in selectedMonthDays
        }
    }

    fun getCycleRules(): CycleRules {
        return CycleRules(
            type = currentType,
            value = currentValue,
            weekDays = selectedWeekDays.toSortedSet(),
            monthDays = selectedMonthDays.toSortedSet(),
            weekDaysJson = selectedWeekDays.toSortedSet().toJsonArrayString(),
            monthDaysJson = selectedMonthDays.toSortedSet().toJsonArrayString(),
            skipHolidays = binding.checkSkipHolidays.isChecked,
            skipWeekends = binding.checkSkipWeekends.isChecked
        )
    }

    fun setOnCycleChangedListener(
        listener: (type: Int, value: Int, weekDays: Set<Int>, monthDays: Set<Int>, skipHolidays: Boolean, skipWeekends: Boolean) -> Unit
    ) {
        onCycleChangedListener = listener
    }

    private fun notifyChanged() {
        onCycleChangedListener?.invoke(
            currentType,
            currentValue,
            selectedWeekDays.toSet(),
            selectedMonthDays.toSet(),
            binding.checkSkipHolidays.isChecked,
            binding.checkSkipWeekends.isChecked
        )
    }

    private fun refreshDescription() {
        val desc = when (currentType) {
            CheckinCycleType.DAY -> context.getString(R.string.cycle_desc_days, currentValue)
            CheckinCycleType.WEEK -> {
                if (selectedWeekDays.isNotEmpty()) {
                    val dayNames = selectedWeekDays.sorted().map { getDayName(it) }.joinToString("、")
                    context.getString(R.string.cycle_weekly_desc, dayNames)
                } else {
                    context.getString(R.string.cycle_desc_weeks, currentValue)
                }
            }
            CheckinCycleType.MONTH -> {
                if (selectedMonthDays.isNotEmpty()) {
                    val dayLabels = selectedMonthDays.sorted().map {
                        if (it == LAST_DAY_OF_MONTH) context.getString(R.string.cycle_last_day) else it.toString()
                    }.joinToString("、")
                    context.getString(R.string.cycle_monthly_desc, dayLabels)
                } else {
                    context.getString(R.string.cycle_desc_months, currentValue)
                }
            }
            else -> context.getString(R.string.cycle_desc_days, currentValue)
        }

        val suffix = buildString {
            if (binding.checkSkipHolidays.isChecked) append("，跳过法定节假日")
            if (binding.checkSkipWeekends.isChecked) append("，跳过双休日")
        }

        binding.cycleDescriptionText.text = desc + suffix
    }

    private fun getDayName(day: Int): String {
        return when (day) {
            1 -> context.getString(R.string.cycle_monday)
            2 -> context.getString(R.string.cycle_tuesday)
            3 -> context.getString(R.string.cycle_wednesday)
            4 -> context.getString(R.string.cycle_thursday)
            5 -> context.getString(R.string.cycle_friday)
            6 -> context.getString(R.string.cycle_saturday)
            7 -> context.getString(R.string.cycle_sunday)
            else -> ""
        }
    }

    private fun parseJsonIntArray(json: String?): List<Int> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { array.getInt(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun Set<Int>.toJsonArrayString(): String {
        val array = JSONArray()
        sorted().forEach { array.put(it) }
        return array.toString()
    }

    data class CycleRules(
        val type: Int,
        val value: Int,
        val weekDays: Set<Int>,
        val monthDays: Set<Int>,
        val weekDaysJson: String,
        val monthDaysJson: String,
        val skipHolidays: Boolean,
        val skipWeekends: Boolean
    )

    companion object {
        const val LAST_DAY_OF_MONTH = 32
    }
}
