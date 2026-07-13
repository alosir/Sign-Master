package com.alosir.task.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.GridLayout
import android.widget.TextView
import com.alosir.task.R
import com.alosir.task.ui.viewmodel.StatisticsViewModel
import com.google.android.material.card.MaterialCardView
import java.util.*

class CheckinCalendarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : GridLayout(context, attrs, defStyleAttr) {

    var days: List<StatisticsViewModel.CalendarDay> = emptyList()
        set(value) {
            field = value
            renderDays()
        }

    var selectedDate: String? = null
        set(value) {
            field = value
            updateSelection()
        }

    var onDateClickListener: ((String) -> Unit)? = null

    private val fullColor: Int
    private val partialColor: Int
    private val noneTextColor: Int
    private val filledTextColor: Int
    private val selectedStrokeColor: Int

    init {
        columnCount = 7
        rowCount = 6

        fullColor = resolveColor(com.google.android.material.R.attr.colorSecondary)
        partialColor = resolveColor(com.google.android.material.R.attr.colorTertiary)
        noneTextColor = resolveColor(com.google.android.material.R.attr.colorOnSurface)
        filledTextColor = resolveColor(android.R.attr.colorBackground)
        selectedStrokeColor = resolveColor(com.google.android.material.R.attr.colorPrimary)
    }

    private fun resolveColor(attr: Int): Int {
        val typedArray = context.obtainStyledAttributes(intArrayOf(attr))
        val color = typedArray.getColor(0, 0xFF000000.toInt())
        typedArray.recycle()
        return color
    }

    private fun renderDays() {
        removeAllViews()
        if (days.isEmpty()) return

        val calendar = Calendar.getInstance()
        calendar.set(days.first().date.substring(0, 4).toInt(), days.first().date.substring(5, 7).toInt() - 1, 1)
        val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val startOffset = if (firstDayOfWeek == Calendar.SUNDAY) 6 else firstDayOfWeek - Calendar.MONDAY

        for (i in 0 until startOffset) {
            addView(createEmptyCell())
        }

        days.forEach { day ->
            addView(createDayCell(day))
        }

        updateSelection()
    }

    private fun createEmptyCell(): View {
        return View(context).apply {
            layoutParams = LayoutParams().apply {
                width = 0
                height = 0
                columnSpec = spec(UNDEFINED, 1f)
                rowSpec = spec(UNDEFINED, 1f)
            }
        }
    }

    private fun createDayCell(day: StatisticsViewModel.CalendarDay): View {
        val inflater = LayoutInflater.from(context)
        val card = inflater.inflate(R.layout.item_calendar_day, this, false) as MaterialCardView
        val textView = card.findViewById<TextView>(R.id.dayText)
        val statusDot = card.findViewById<View>(R.id.statusDot)

        card.layoutParams = LayoutParams().apply {
            width = 0
            height = LayoutParams.WRAP_CONTENT
            columnSpec = spec(UNDEFINED, 1f)
            rowSpec = spec(UNDEFINED, 1f)
        }

        textView.text = day.dayOfMonth.toString()
        card.setCardBackgroundColor(context.getColor(android.R.color.transparent))
        textView.setTextColor(noneTextColor)

        when (day.status) {
            StatisticsViewModel.DayStatus.FULL -> {
                statusDot.visibility = View.VISIBLE
                statusDot.backgroundTintList = android.content.res.ColorStateList.valueOf(fullColor)
            }
            StatisticsViewModel.DayStatus.PARTIAL -> {
                statusDot.visibility = View.VISIBLE
                statusDot.backgroundTintList = android.content.res.ColorStateList.valueOf(partialColor)
            }
            StatisticsViewModel.DayStatus.NONE -> {
                statusDot.visibility = View.INVISIBLE
            }
        }

        card.tag = day.date
        card.isClickable = true
        card.isFocusable = true
        card.setOnClickListener {
            onDateClickListener?.invoke(day.date)
        }

        return card
    }

    private fun updateSelection() {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val dateTag = child.tag as? String ?: continue
            val card = child as? MaterialCardView ?: continue
            val isSelected = dateTag == selectedDate
            card.strokeWidth = if (isSelected) dpToPx(2) else 0
            card.strokeColor = if (isSelected) selectedStrokeColor else 0
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
}
