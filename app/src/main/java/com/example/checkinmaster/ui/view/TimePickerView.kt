package com.alosir.task.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.alosir.task.databinding.ViewTimePickerBinding

class TimePickerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding = ViewTimePickerBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        orientation = HORIZONTAL
        gravity = android.view.Gravity.CENTER_VERTICAL

        val hours = Array(24) { String.format("%02d", it) }
        binding.hourPicker.apply {
            minValue = 0
            maxValue = 23
            displayedValues = hours
            descendantFocusability = FOCUS_BLOCK_DESCENDANTS
            wrapSelectorWheel = true
        }

        val minutes = Array(60) { String.format("%02d", it) }
        binding.minutePicker.apply {
            minValue = 0
            maxValue = 59
            displayedValues = minutes
            descendantFocusability = FOCUS_BLOCK_DESCENDANTS
            wrapSelectorWheel = true
        }
    }

    fun getTime(): String {
        val hour = binding.hourPicker.value
        val minute = binding.minutePicker.value
        return String.format("%02d:%02d", hour, minute)
    }

    fun setTime(time: String?) {
        if (time.isNullOrBlank()) return
        val parts = time.split(":")
        if (parts.size != 2) return
        val hour = parts[0].toIntOrNull() ?: return
        val minute = parts[1].toIntOrNull() ?: return
        if (hour in 0..23 && minute in 0..59) {
            binding.hourPicker.value = hour
            binding.minutePicker.value = minute
        }
    }
}
