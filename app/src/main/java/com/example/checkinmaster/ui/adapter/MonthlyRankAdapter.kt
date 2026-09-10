package com.alosir.task.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.alosir.task.R
import com.alosir.task.databinding.ItemStreakRankBinding
import com.alosir.task.util.StatisticsCalculator

/**
 * 当月签到排行：按任务在当月的累计签到次数降序展示。
 */
class MonthlyRankAdapter : RecyclerView.Adapter<MonthlyRankAdapter.ViewHolder>() {

    private val items = mutableListOf<StatisticsCalculator.MonthlyRankInfo>()

    fun submitList(newItems: List<StatisticsCalculator.MonthlyRankInfo>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStreakRankBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], position + 1)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(
        private val binding: ItemStreakRankBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(info: StatisticsCalculator.MonthlyRankInfo, rank: Int) {
            binding.rankNumber.text = rank.toString()
            binding.itemName.text = info.item.name

            if (info.item.description.isNullOrEmpty()) {
                binding.itemDesc.visibility = android.view.View.GONE
            } else {
                binding.itemDesc.text = info.item.description
                binding.itemDesc.visibility = android.view.View.VISIBLE
            }

            binding.streakDays.text =
                binding.root.context.getString(R.string.statistics_count_unit, info.count)
        }
    }
}
