package com.alosir.task.util

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.alosir.task.R
import com.alosir.task.ui.adapter.PendingCheckinAdapter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PendingSwipeCallback(
    private val adapter: PendingCheckinAdapter,
    private val onComplete: (position: Int) -> Unit,
    private val onSkip: (position: Int) -> Unit,
    private val onEdit: (position: Int) -> Unit,
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
        val isToday = dateFormat.format(model.nextDate) == dateFormat.format(Date())

        when (direction) {
            ItemTouchHelper.RIGHT -> {
                if (isToday) onComplete(position) else onEdit(position)
            }
            ItemTouchHelper.LEFT -> {
                if (isToday) onSkip(position) else onDelete(position)
            }
        }
        // 数据变更由 ViewModel/LiveData 驱动 Adapter 刷新；
        // 编辑/删除取消等不修改数据的操作由 Fragment 自行 notifyItemChanged 复位。
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
            val isToday = model?.let { dateFormat.format(it.nextDate) == dateFormat.format(Date()) } ?: false

            val background = RectF(
                itemView.left.toFloat(),
                itemView.top.toFloat(),
                itemView.right.toFloat(),
                itemView.bottom.toFloat()
            )

            val (text, color) = when {
                dX > 0 && isToday -> "完成" to itemView.context.getColor(R.color.checkin_success)
                dX > 0 -> "编辑" to itemView.context.getColor(R.color.md_primary)
                dX < 0 && isToday -> "跳过" to itemView.context.getColor(R.color.md_tertiary)
                else -> "删除" to itemView.context.getColor(R.color.md_error)
            }

            val paint = Paint().apply {
                this.color = color
                isAntiAlias = true
            }

            if (dX > 0) {
                background.right = itemView.left + dX
            } else {
                background.left = itemView.right + dX
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
            c.drawText(text, textX, textY, textPaint)
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }
}
