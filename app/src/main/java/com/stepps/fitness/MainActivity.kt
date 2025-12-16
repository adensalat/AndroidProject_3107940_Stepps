package com.stepps

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.stepps.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPrefs = getSharedPreferences("SteppsPrefs", MODE_PRIVATE)
        dbHelper = DatabaseHelper(this)

        setupNavigation()
        startStepCounterService()
        loadUserStepsFromDatabase()
        checkAchievements()
    }

    private fun setupNavigation() {
        val navView: BottomNavigationView = binding.navView
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        val navController = navHostFragment.navController

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_dashboard,
                R.id.navigation_history
            )
        )
        navView.setupWithNavController(navController)
    }

    private fun loadUserStepsFromDatabase() {
        val userId = getCurrentUserId()
        if (userId == -1L) return // Guest user

        // Load today's steps from database
        val todayStats = dbHelper.getTodayStats(userId)

        sharedPrefs.edit()
            .putInt("current_steps", todayStats.steps)
            .putFloat("current_calories", todayStats.calories.toFloat())
            .putFloat("current_distance", todayStats.distance.toFloat())
            .apply()
    }

    private fun getCurrentUserId(): Long {
        val userId = sharedPrefs.getLong("user_id", -1)
        val isGuest = sharedPrefs.getBoolean("is_guest", false)
        return if (isGuest) -1 else userId
    }

    private fun startStepCounterService() {
        val serviceIntent = Intent(this, StepCounterService::class.java).apply {
            putExtra("start_tracking", true)
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun checkAchievements() {
        val userId = getCurrentUserId()
        if (userId == -1L) return // Guest user

        val totalSteps = dbHelper.getTotalSteps(userId)

        if (totalSteps >= 1000) {
            dbHelper.unlockAchievement(userId, 1)
        }
        if (totalSteps >= 10000) {
            dbHelper.unlockAchievement(userId, 2)
        }

        val streak = sharedPrefs.getInt("current_streak", 0)
        if (streak >= 7) {
            dbHelper.unlockAchievement(userId, 3)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_activity_main) as? NavHostFragment
        val currentFragment = navHostFragment?.childFragmentManager?.primaryNavigationFragment

        if (currentFragment is DashboardFragment) {
            currentFragment.onPermissionsResult(requestCode, permissions, grantResults)
        }

        navHostFragment?.childFragmentManager?.fragments?.forEach { fragment ->
            if (fragment is DashboardFragment && fragment.isVisible) {
                fragment.onPermissionsResult(requestCode, permissions, grantResults)
            }
        }
    }
}