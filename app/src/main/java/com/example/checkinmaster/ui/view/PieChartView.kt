package com.alosir.task.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.withStyledAttributes
import com.alosir.task.R
import kotlin.math.min

class PieChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    data class Slice(
        val value: Float,
        val color: Int
    )

    private val slices = mutableListOf<Slice>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val rect = RectF()

    private var holeRadiusRatio = 0.55f
    private var strokeWidth = 0f
    private var strokeColor = 0

    init {
        context.withStyledAttributes(attrs, R.styleable.PieChartView) {
            holeRadiusRatio = getFloat(R.styleable.PieChartView_holeRadiusRatio, 0.55f)
            strokeWidth = getDimension(R.styleable.PieChartView_sliceStrokeWidth, 0f)
            strokeColor = getColor(R.styleable.PieChartView_sliceStrokeColor, 0)
        }
    }

    fun setSlices(newSlices: List<Slice>) {
        slices.clear()
        slices.addAll(newSlices)
        invalidate()
    }

    fun setHoleRadiusRatio(ratio: Float) {
        holeRadiusRatio = ratio.coerceIn(0f, 0.95f)
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val defaultSize = context.resources.getDimensionPixelSize(com.google.android.material.R.dimen.mtrl_min_touch_target_size)

        val width = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> widthSize.coerceAtMost(defaultSize)
            else -> defaultSize
        }
        val height = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> heightSize.coerceAtMost(defaultSize)
            else -> defaultSize
        }
        setMeasuredDimension(width.coerceAtLeast(1), height.coerceAtLeast(1))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val viewWidth = measuredWidth
        val viewHeight = measuredHeight
        if (viewWidth <= 0 || viewHeight <= 0) return

        val total = slices.sumOf { it.value.toDouble() }.toFloat()
        if (total <= 0f) {
            drawEmptyState(canvas)
            return
        }

        val width = viewWidth.toFloat()
        val height = viewHeight.toFloat()
        val diameter = min(width, height)
        val radius = diameter / 2f
        if (radius <= 0f) return

        val centerX = width / 2f
        val centerY = height / 2f
        val padding = strokeWidth / 2f

        rect.set(
            centerX - radius + padding,
            centerY - radius + padding,
            centerX + radius - padding,
            centerY + radius - padding
        )

        var startAngle = -90f
        slices.forEach { slice ->
            val sweepAngle = slice.value / total * 360f
            paint.color = slice.color
            canvas.drawArc(rect, startAngle, sweepAngle, true, paint)

            if (strokeWidth > 0f && strokeColor != 0) {
                strokePaint.color = strokeColor
                strokePaint.strokeWidth = strokeWidth
                canvas.drawArc(rect, startAngle, sweepAngle, true, strokePaint)
            }
            startAngle += sweepAngle
        }

        // 中心透明圆孔
        paint.color = getBackgroundColor()
        canvas.drawCircle(centerX, centerY, radius * holeRadiusRatio, paint)
    }

    private fun drawEmptyState(canvas: Canvas) {
        val width = measuredWidth.toFloat()
        val height = measuredHeight.toFloat()
        val diameter = min(width, height)
        val radius = diameter / 2f
        if (radius <= 0f) return

        val centerX = width / 2f
        val centerY = height / 2f

        paint.color = getBackgroundColor()
        rect.set(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )
        paint.color = getEmptyColor()
        canvas.drawArc(rect, -90f, 360f, true, paint)
        paint.color = getBackgroundColor()
        canvas.drawCircle(centerX, centerY, radius * holeRadiusRatio, paint)
    }

    private fun getBackgroundColor(): Int {
        val typedArray = context.obtainStyledAttributes(intArrayOf(android.R.attr.colorBackground))
        val color = typedArray.getColor(0, 0xFFFFFFFF.toInt())
        typedArray.recycle()
        return color
    }

    private fun getEmptyColor(): Int {
        val typedArray = context.obtainStyledAttributes(intArrayOf(com.google.android.material.R.attr.colorSurfaceVariant))
        val color = typedArray.getColor(0, 0xFFE0E0E0.toInt())
        typedArray.recycle()
        return color
    }
}
