package com.stepps.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.stepps.R
import com.stepps.fitness.models.DailyStats

class HistoryAdapter(private var data: List<DailyStats>) :
    RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvSteps: TextView = itemView.findViewById(R.id.tvSteps)
        val tvCalories: TextView = itemView.findViewById(R.id.tvCalories)
        val tvDistance: TextView = itemView.findViewById(R.id.tvDistance)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]

        holder.tvDate.text = item.date
        holder.tvSteps.text = item.steps.toString()
        holder.tvCalories.text = String.format("%.1f kcal", item.calories)
        holder.tvDistance.text = String.format("%.2f m", item.distance)

        // Highlight today's entry
        if (position == data.size - 1) { // Last item is today
            holder.itemView.setBackgroundResource(R.color.primary_light)
        } else {
            holder.itemView.setBackgroundResource(android.R.color.transparent)
        }
    }

    override fun getItemCount(): Int = data.size

    fun updateData(newData: List<DailyStats>) {
        data = newData
        notifyDataSetChanged()
    }
}