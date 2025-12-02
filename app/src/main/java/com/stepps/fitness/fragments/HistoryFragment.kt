package com.stepps.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.stepps.DatabaseHelper
import com.stepps.R
import com.stepps.adapters.HistoryAdapter
import com.stepps.databinding.FragmentHistoryBinding
import com.stepps.fitness.models.DailyStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var historyAdapter: HistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbHelper = DatabaseHelper(requireContext())
        setupRecyclerView()
        loadHistoryData()

        // Setup click listeners
        binding.btnExport.setOnClickListener {
            exportData()
        }

        binding.btnRefresh.setOnClickListener {
            loadHistoryData()
        }
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter(emptyList())
        binding.rvHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historyAdapter
            setHasFixedSize(true)
        }
    }

    private fun loadHistoryData() {
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE

            val weeklyStats = withContext(Dispatchers.IO) {
                // Convert the database result to the correct type
                dbHelper.getWeeklyStats().map { stats ->
                    DailyStats(
                        date = stats.date,
                        steps = stats.steps,
                        distance = stats.distance,
                        calories = stats.calories,
                        time = stats.time
                    )
                }
            }

            // Calculate statistics
            val totalSteps = weeklyStats.sumOf { it.steps }
            val averageSteps = if (weeklyStats.isNotEmpty()) totalSteps / weeklyStats.size else 0
            val bestDay = weeklyStats.maxByOrNull { it.steps }?.steps ?: 0

            // Update statistics UI
            withContext(Dispatchers.Main) {
                binding.tvTotalSteps.text = getString(R.string.total_steps, totalSteps)
                binding.tvAverageSteps.text = getString(R.string.average_steps, averageSteps)
                binding.tvBestDay.text = getString(R.string.best_day, bestDay)

                // Update RecyclerView
                historyAdapter.updateData(weeklyStats)
                binding.progressBar.visibility = View.GONE

                // Show/hide empty state
                if (weeklyStats.all { it.steps == 0 }) {
                    binding.tvEmptyState.visibility = View.VISIBLE
                    binding.rvHistory.visibility = View.GONE
                } else {
                    binding.tvEmptyState.visibility = View.GONE
                    binding.rvHistory.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun exportData() {
        lifecycleScope.launch {
            val weeklyStats = withContext(Dispatchers.IO) {
                // Convert the database result to the correct type
                dbHelper.getWeeklyStats().map { stats ->
                    DailyStats(
                        date = stats.date,
                        steps = stats.steps,
                        distance = stats.distance,
                        calories = stats.calories,
                        time = stats.time
                    )
                }
            }

            // Create CSV content
            val csvContent = buildString {
                appendLine("Date,Steps,Calories,Distance (m),Time (ms)")
                weeklyStats.forEach { stats ->
                    appendLine("${stats.date},${stats.steps},${stats.calories},${stats.distance},${stats.time}")
                }
            }

            // Share via implicit intent
            val shareIntent = android.content.Intent().apply {
                action = android.content.Intent.ACTION_SEND
                type = "text/csv"
                putExtra(android.content.Intent.EXTRA_TEXT, csvContent)
                putExtra(android.content.Intent.EXTRA_SUBJECT, "Stepps_History_Export.csv")
            }

            // Check if there's an app that can handle this intent
            if (shareIntent.resolveActivity(requireContext().packageManager) != null) {
                startActivity(android.content.Intent.createChooser(shareIntent, "Export Data"))
            } else {
                android.widget.Toast.makeText(
                    requireContext(),
                    "No app available to export data",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}