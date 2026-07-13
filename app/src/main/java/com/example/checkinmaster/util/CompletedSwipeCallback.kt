package com.alosir.task.util

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.alosir.task.R
import com.alosir.task.ui.adapter.CompletedCheckinAdapter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CompletedSwipeCallback(
    private val adapter: CompletedCheckinAdapter,
    private val onRestore: (position: Int) -> Unit,
    private val onDelete: (position: Int) -> Unit
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean = false

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.bindingAdapterPosition
        if (position == RecyclerView.NO_POSITION) return
        val model = adapter.getItemAt(position)
        val isToday = model.record.checkinDate == dateFormat.format(Date())

        when (direction) {
            ItemTouchHelper.RIGHT -> {
                if (isToday) onRestore(position) else onDelete(position)
            }
            ItemTouchHelper.LEFT -> onDelete(position)
        }
        // 数据变更由 ViewModel/LiveData 驱动 Adapter 刷新。
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            val itemView = viewHolder.itemView
            val position = viewHolder.bindingAdapterPosition
            val model = if (position != RecyclerView.NO_POSITION) adapter.getItemAt(position) else null
            val isToday = model?.record?.checkinDate == dateFormat.format(Date())

            val background = RectF(
                itemView.left.toFloat(),
                itemView.top.toFloat(),
                itemView.right.toFloat(),
                itemView.bottom.toFloat()
            )

            val (text, color) = when {
                dX > 0 && isToday -> "恢复" to itemView.context.getColor(R.color.md_tertiary)
                dX > 0 -> "删除" to itemView.context.getColor(R.color.md_error)
                dX < 0 -> "删除" to itemView.context.getColor(R.color.md_error)
                else -> "" to itemView.context.getColor(R.color.md_error)
            }

            if (dX > 0) {
                background.right = itemView.left + dX
            } else {
                background.left = itemView.right + dX
            }

            val paint = Paint().apply {
                this.color = color
                isAntiAlias = true
            }
            c.drawRect(background, paint)

            val textPaint = Paint().apply {
                this.color = android.graphics.Color.WHITE
                isAntiAlias = true
                textSize = itemView.context.resources.displayMetrics.density * 16
                textAlign = Paint.Align.CENTER
            }
            val textX = if (dX > 0) itemView.left + dX / 2 else itemView.right + dX / 2
            val textY = itemView.top + itemView.height / 2f - (textPaint.descent() + textPaint.ascent()) / 2
            if (text.isNotEmpty()) {
                c.drawText(text, textX, textY, textPaint)
            }
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }
}
