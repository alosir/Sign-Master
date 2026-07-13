package com.alosir.task.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.alosir.task.R
import com.alosir.task.data.entity.CheckinType
import com.alosir.task.databinding.ItemCompletedCheckinBinding
import com.alosir.task.ui.viewmodel.CheckinListViewModel
import com.alosir.task.util.CycleCalculator
import com.alosir.task.util.IconManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CompletedCheckinAdapter(
    private val onItemClick: (CheckinListViewModel.CompletedRecordUiModel) -> Unit
) : ListAdapter<CheckinListViewModel.CompletedRecordUiModel, CompletedCheckinAdapter.ViewHolder>(DiffCallback()) {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCompletedCheckinBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun getItemAt(position: Int): CheckinListViewModel.CompletedRecordUiModel = getItem(position)

    inner class ViewHolder(
        private val binding: ItemCompletedCheckinBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(pos))
                }
            }
        }

        fun bind(model: CheckinListViewModel.CompletedRecordUiModel) {
            val item = model.item
            val record = model.record
            val context = binding.root.context

            binding.nameText.text = item.name

            val dateStr = CycleCalculator.formatRelativeRecordDate(
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(record.checkinDate) ?: Date()
            )
            val timeStr = try {
                timeFormat.format(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(record.checkinTime) ?: Date())
            } catch (e: Exception) {
                ""
            }
            binding.dateText.text = if (timeStr.isEmpty()) dateStr else "$dateStr $timeStr"

            if (item.description.isNullOrEmpty()) {
                binding.descText.visibility = android.view.View.GONE
            } else {
                binding.descText.text = item.description
                binding.descText.visibility = android.view.View.VISIBLE
            }

            when (item.type) {
                CheckinType.APP -> {
                    val icon = IconManager.loadAppIcon(context, item.packageName ?: "")
                    if (icon != null) {
                        binding.iconImage.setImageDrawable(icon)
                    } else {
                        binding.iconImage.setImageResource(R.drawable.ic_default_app)
                    }
                }
                CheckinType.WEBSITE -> {
                    binding.iconImage.setImageResource(R.drawable.ic_default_website)
                }
                CheckinType.OTHER -> {
                    binding.iconImage.setImageResource(R.drawable.ic_default_other)
                }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<CheckinListViewModel.CompletedRecordUiModel>() {
        override fun areItemsTheSame(
            oldItem: CheckinListViewModel.CompletedRecordUiModel,
            newItem: CheckinListViewModel.CompletedRecordUiModel
        ): Boolean {
            return oldItem.record.id == newItem.record.id
        }

        override fun areContentsTheSame(
            oldItem: CheckinListViewModel.CompletedRecordUiModel,
            newItem: CheckinListViewModel.CompletedRecordUiModel
        ): Boolean {
            return oldItem == newItem
        }
    }
}
