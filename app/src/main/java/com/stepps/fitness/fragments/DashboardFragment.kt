package com.stepps.fragments

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
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

    // BroadcastReceiver to listen for step updates
    private val stepUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "STEP_UPDATE") {
                loadDashboardData()
            }
        }
    }

    companion object {
        private const val PERMISSION_REQUEST_ACTIVITY_RECOGNITION = 100
        private const val PERMISSION_REQUEST_LOCATION = 101
        const val PERMISSION_REQUEST_CODE = 100
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
        loadTrackingState()

        if (!checkPermissionsGranted()) {
            requestPermissions()
        } else {
            loadDashboardData()
            // Start service if tracking was previously active
            if (isTracking) {
                startStepService()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Register broadcast receiver
        val filter = IntentFilter("STEP_UPDATE")
        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(stepUpdateReceiver, filter)

        loadDashboardData()
    }

    override fun onPause() {
        super.onPause()
        // Unregister broadcast receiver
        LocalBroadcastManager.getInstance(requireContext())
            .unregisterReceiver(stepUpdateReceiver)
    }

    private fun setupUI() {
        binding.progressRing.max = 100
        binding.progressRing.progress = 0

        binding.btnStartStop.setOnClickListener {
            if (checkPermissionsGranted()) {
                toggleTracking()
            } else {
                requestPermissions()
            }
        }

        binding.btnShare.setOnClickListener {
            shareProgress()
        }

        binding.btnSettings.setOnClickListener {
            Toast.makeText(context, "Settings will be available in Milestone 3", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadTrackingState() {
        val sharedPrefs = requireContext().getSharedPreferences("SteppsPrefs", Context.MODE_PRIVATE)
        isTracking = sharedPrefs.getBoolean("is_tracking", false)
        updateTrackingUI()
    }

    private fun saveTrackingState() {
        val sharedPrefs = requireContext().getSharedPreferences("SteppsPrefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putBoolean("is_tracking", isTracking).apply()
    }

    private fun toggleTracking() {
        isTracking = !isTracking
        saveTrackingState()

        if (isTracking) {
            startStepService()
            // Save tracking start time
            val sharedPrefs = requireContext().getSharedPreferences("SteppsPrefs", Context.MODE_PRIVATE)
            sharedPrefs.edit()
                .putLong("tracking_start", System.currentTimeMillis())
                .apply()
        } else {
            stopStepService()
        }

        updateTrackingUI()
    }

    private fun updateTrackingUI() {
        if (isTracking) {
            binding.btnStartStop.text = getString(R.string.stop_tracking)
            binding.tvTrackingStatus.text = getString(R.string.tracking_active)
            binding.tvTrackingStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.success))
            binding.statusIndicator.setBackgroundResource(R.drawable.circle_green)
            binding.statusIndicator.visibility = View.VISIBLE

        } else {
            binding.btnStartStop.text = getString(R.string.start_tracking)
            binding.tvTrackingStatus.text = getString(R.string.tracking_paused)
            binding.tvTrackingStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            binding.statusIndicator.setBackgroundResource(R.drawable.circle_red)
            binding.statusIndicator.visibility = View.VISIBLE
        }

        // Update current date
        val dateFormat = java.text.SimpleDateFormat("MMMM dd, yyyy", java.util.Locale.getDefault())
        binding.tvCurrentDate.text = dateFormat.format(java.util.Date())
    }

    private fun startStepService() {
        val serviceIntent = Intent(requireContext(), StepCounterService::class.java)
        serviceIntent.putExtra("start_tracking", true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireContext().startForegroundService(serviceIntent)
        } else {
            requireContext().startService(serviceIntent)
        }
    }

    private fun stopStepService() {
        val serviceIntent = Intent(requireContext(), StepCounterService::class.java)
        requireContext().stopService(serviceIntent)
    }

    private fun loadDashboardData() {
        lifecycleScope.launch {
            try {
                val sharedPrefs = requireContext().getSharedPreferences("SteppsPrefs", Context.MODE_PRIVATE)

                // Get current stats from SharedPreferences (real-time from service)
                val currentSteps = sharedPrefs.getInt("current_steps", 0)
                val currentCalories = sharedPrefs.getFloat("current_calories", 0f)
                val currentDistance = sharedPrefs.getFloat("current_distance", 0f)
                val trackingStart = sharedPrefs.getLong("tracking_start", System.currentTimeMillis())

                val dailyGoal = sharedPrefs.getInt("daily_goal", 10000)
                val streak = sharedPrefs.getInt("current_streak", 0)

                // Calculate active time
                val activeTime = if (isTracking) {
                    System.currentTimeMillis() - trackingStart
                } else {
                    0L
                }

                // Update UI on main thread
                withContext(Dispatchers.Main) {
                    binding.tvStepCount.text = currentSteps.toString()
                    binding.tvGoalProgress.text = getString(R.string.daily_goal, currentSteps, dailyGoal)
                    binding.tvCalories.text = String.format("%.1f kcal", currentCalories)
                    binding.tvDistance.text = String.format("%.2f m", currentDistance)
                    binding.tvTime.text = formatTime(activeTime)

                    // Update progress ring
                    val progress = if (dailyGoal > 0) {
                        (currentSteps * 100 / dailyGoal).coerceAtMost(100)
                    } else {
                        0
                    }
                    binding.progressRing.progress = progress

                    // Update streak
                    binding.tvStreak.text = getString(R.string.streak, streak)

                    // Update motivational message
                    updateMotivationalMessage(currentSteps, dailyGoal)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateMotivationalMessage(steps: Int, goal: Int) {
        val message = when {
            steps == 0 -> "Let's get moving!"
            steps < goal / 4 -> "Great start! Keep going!"
            steps < goal / 2 -> "You're making progress!"
            steps < goal * 3 / 4 -> "Halfway there! You can do it!"
            steps < goal -> "Almost there! Keep pushing!"
            else -> "Goal achieved! Amazing work! 🎉"
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

    private fun checkPermissionsGranted(): Boolean {
        val permissionsNeeded = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }

        return true
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
            PERMISSION_REQUEST_CODE
        )
    }

    fun onPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(context, "Permissions granted! You can now start tracking.", Toast.LENGTH_SHORT).show()
                loadDashboardData()
            } else {
                Toast.makeText(context, "Permissions are required for step tracking", Toast.LENGTH_LONG).show()
            }
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

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareMessage)
            putExtra(Intent.EXTRA_SUBJECT, "My Stepps Progress")
        }

        if (shareIntent.resolveActivity(requireContext().packageManager) != null) {
            startActivity(Intent.createChooser(shareIntent, "Share your progress"))
        } else {
            Toast.makeText(context, "No app available to share", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}