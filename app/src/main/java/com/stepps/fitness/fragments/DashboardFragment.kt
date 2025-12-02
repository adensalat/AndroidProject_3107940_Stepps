package com.stepps.fragments

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.stepps.DatabaseHelper
import com.stepps.R
import com.stepps.StepCounterService
import com.stepps.databinding.FragmentDashboardBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var dbHelper: DatabaseHelper
    private var isTracking = false

    // Permission request codes
    companion object {
        private const val PERMISSION_REQUEST_ACTIVITY_RECOGNITION = 100
        private const val PERMISSION_REQUEST_LOCATION = 101
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbHelper = DatabaseHelper(requireContext())

        setupUI()
        checkPermissions()
        loadDashboardData()
        startDataRefreshLoop()
    }

    private fun setupUI() {
        // Progress ring setup (simplified for milestone 2)
        binding.progressRing.max = 100
        binding.progressRing.progress = 0

        // Start/Stop tracking button
        binding.btnStartStop.setOnClickListener {
            toggleTracking()
        }

        // Share button - Implicit Intent
        binding.btnShare.setOnClickListener {
            shareProgress()
        }

        // Request permissions automatically when fragment starts
        if (!checkPermissionsGranted()) {
            requestPermissions()
        }

        // Settings button - Navigate to settings (for milestone 3)
        binding.btnSettings.setOnClickListener {
            Toast.makeText(context, "Settings will be available in Milestone 3", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleTracking() {
        isTracking = !isTracking

        if (isTracking) {
            binding.btnStartStop.text = getString(R.string.stop_tracking)
            binding.tvTrackingStatus.text = getString(R.string.tracking_active)
            binding.tvTrackingStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.success))

            // Start service
            val serviceIntent = Intent(requireContext(), StepCounterService::class.java)
            serviceIntent.putExtra("start_tracking", true)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requireContext().startForegroundService(serviceIntent)
            } else {
                requireContext().startService(serviceIntent)
            }
        } else {
            binding.btnStartStop.text = getString(R.string.start_tracking)
            binding.tvTrackingStatus.text = getString(R.string.tracking_paused)
            binding.tvTrackingStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark))
            // Stop service
            requireContext().stopService(Intent(requireContext(), StepCounterService::class.java))
        }
    }

    private fun loadDashboardData() {
        lifecycleScope.launch {
            val todayStats = withContext(Dispatchers.IO) {
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                dbHelper.getTodayStats(date)
            }

            val sharedPrefs = requireContext().getSharedPreferences("SteppsPrefs", Context.MODE_PRIVATE)
            val dailyGoal = sharedPrefs.getInt("daily_goal", 10000)

            // Update UI
            binding.tvStepCount.text = todayStats.steps.toString()
            binding.tvGoalProgress.text = getString(R.string.daily_goal, todayStats.steps, dailyGoal)
            binding.tvCalories.text = String.format("%.1f kcal", todayStats.calories)
            binding.tvDistance.text = String.format("%.2f m", todayStats.distance)
            binding.tvTime.text = formatTime(todayStats.time)

            // Update progress ring
            val progress = if (dailyGoal > 0) {
                (todayStats.steps * 100 / dailyGoal).coerceAtMost(100)
            } else {
                0
            }
            binding.progressRing.progress = progress

            // Update streak
            val streak = sharedPrefs.getInt("current_streak", 0)
            binding.tvStreak.text = getString(R.string.streak, streak)

            // Update motivational message
            updateMotivationalMessage(todayStats.steps, dailyGoal)
        }
    }

    private fun updateMotivationalMessage(steps: Int, goal: Int) {
        val message = when {
            steps == 0 -> "Let's get moving!"
            steps < goal / 4 -> "Great start! Keep going!"
            steps < goal / 2 -> "You're making progress!"
            steps < goal * 3 / 4 -> "Halfway there! You can do it!"
            steps < goal -> "Almost there! Keep pushing!"
            else -> "Goal achieved! Amazing work!"
        }

        binding.tvMotivation.text = message
    }

    private fun formatTime(milliseconds: Long): String {
        val seconds = milliseconds / 1000
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60

        return String.format("%02d:%02d:%02d", hours, minutes, secs)
    }

    private fun startDataRefreshLoop() {
        val handler = Handler(Looper.getMainLooper())

        val refreshRunnable = object : Runnable {
            override fun run() {
                if (isAdded) {
                    loadDashboardData()
                    handler.postDelayed(this, 5000) // Refresh every 5 seconds
                }
            }
        }

        handler.post(refreshRunnable)
    }

    private fun checkPermissions(): Boolean {
        val permissionsNeeded = mutableListOf<String>()

        // Check activity recognition permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsNeeded.add(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        }

        // Check location permission
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        return permissionsNeeded.isEmpty()
    }

    private fun checkPermissionsGranted(): Boolean {
        return checkPermissions()
    }

    private fun requestPermissions() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)

        ActivityCompat.requestPermissions(
            requireActivity(),
            permissions.toTypedArray(),
            PERMISSION_REQUEST_ACTIVITY_RECOGNITION
        )
    }

    fun onPermissionsResult() {
        if (checkPermissionsGranted()) {
            Toast.makeText(context, "Permissions granted!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Some permissions denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareProgress() {
        val sharedPrefs = requireContext().getSharedPreferences("SteppsPrefs", Context.MODE_PRIVATE)
        val steps = sharedPrefs.getInt("current_steps", 0)
        val calories = sharedPrefs.getFloat("current_calories", 0f)
        val distance = sharedPrefs.getFloat("current_distance", 0f)

        val shareMessage = """
            🚶‍♂️ Stepps Progress Update 🚶‍♀️
            Today's Steps: $steps
            Calories Burned: ${String.format("%.1f", calories)} kcal
            Distance: ${String.format("%.2f", distance)} meters
            Keep moving! 💪
            
            #Stepps #FitnessTracker
        """.trimIndent()

        // Implicit Intent for sharing
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareMessage)
            putExtra(Intent.EXTRA_SUBJECT, "My Stepps Progress")
        }

        // Check if there's an app that can handle this intent
        if (shareIntent.resolveActivity(requireContext().packageManager) != null) {
            startActivity(Intent.createChooser(shareIntent, "Share your progress"))
        } else {
            Toast.makeText(context, "No app available to share", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSION_REQUEST_ACTIVITY_RECOGNITION -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    Toast.makeText(context, "Permissions granted!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Some permissions denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}