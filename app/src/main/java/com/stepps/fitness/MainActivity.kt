package com.stepps

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
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
        checkAchievements()
    }

    private fun setupNavigation() {
        val navView: BottomNavigationView = binding.navView

        // Get NavHostFragment properly
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        val navController = navHostFragment.navController

        // Passing each menu ID as a set of Ids because each menu should be considered as top level destinations
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_dashboard,
                R.id.navigation_history
            )
        )

        //setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
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
        // Check and unlock achievements based on user progress
        val totalSteps = dbHelper.getTotalSteps()

        if (totalSteps >= 1000) {
            dbHelper.unlockAchievement(1) // First 1K Steps
        }

        if (totalSteps >= 10000) {
            dbHelper.unlockAchievement(2) // 10K Master
        }

        // Check streak (simplified for milestone 2)
        val streak = sharedPrefs.getInt("current_streak", 0)
        if (streak >= 7) {
            dbHelper.unlockAchievement(3) // Week Streak
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // Get the NavHostFragment
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_activity_main) as? NavHostFragment

        // Get the current fragment from NavHostFragment
        val currentFragment = navHostFragment?.childFragmentManager?.primaryNavigationFragment

        // Forward permission result to current fragment if it's DashboardFragment
        if (currentFragment is DashboardFragment) {
            currentFragment.onPermissionsResult(requestCode, permissions, grantResults)
        }

        // Alternatively, forward to all fragments in the NavHostFragment
        navHostFragment?.childFragmentManager?.fragments?.forEach { fragment ->
            if (fragment is DashboardFragment && fragment.isVisible) {
                fragment.onPermissionsResult(requestCode, permissions, grantResults)
            }
        }
    }
}