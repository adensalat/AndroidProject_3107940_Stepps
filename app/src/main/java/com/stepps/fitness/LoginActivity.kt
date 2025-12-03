package com.stepps

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.stepps.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPrefs = getSharedPreferences("SteppsPrefs", Context.MODE_PRIVATE)
        dbHelper = DatabaseHelper(this)

        setupClickListeners()
        checkFirstLaunch()
    }

    private fun setupClickListeners() {
        // Login Button - Explicit Intent to MainActivity
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (validateInput(email, password)) {
                // Reset step data for new user
                resetStepData()

                // Simulate login - In milestone 3, add actual authentication
                sharedPrefs.edit()
                    .putString("user_email", email)
                    .putBoolean("is_logged_in", true)
                    .apply()

                // Check if user exists, if not create default profile
                if (dbHelper.getUser(email) == null) {
                    dbHelper.addUser(email, 170f, 70f, 25)
                }

                // Explicit Intent to MainActivity
                val intent = Intent(this, MainActivity::class.java).apply {
                    putExtra("user_email", email)
                }
                startActivity(intent)
                finish()
            }
        }

        // Register Button
        binding.btnRegister.setOnClickListener {
            showRegistrationDialog()
        }

        // Guest Button - Continue without account
        binding.btnGuest.setOnClickListener {
            // Reset step data for guest user
            resetStepData()

            sharedPrefs.edit()
                .putBoolean("is_guest", true)
                .apply()

            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Register Now Text - Implicit Intent for registration help
        binding.tvRegisterNow.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = android.net.Uri.parse("https://www.example.com/register-help")
                // Add package check for specific browser if needed
            }

            // Check if there's an app that can handle this intent
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(this, "No browser available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun resetStepData() {
        // Clear all step-related data from SharedPreferences
        sharedPrefs.edit()
            .putInt("current_steps", 0)
            .putFloat("current_calories", 0f)
            .putFloat("current_distance", 0f)
            .putLong("tracking_start", System.currentTimeMillis())
            .putBoolean("is_tracking", false)
            .putInt("current_streak", 0)
            .putBoolean("achievement_1k", false)
            .putBoolean("achievement_10k", false)
            .apply()
    }

    private fun validateInput(email: String, password: String): Boolean {
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = getString(R.string.error_invalid_email)
            return false
        }

        if (password.isEmpty() || password.length < 6) {
            binding.etPassword.error = getString(R.string.error_short_password)
            return false
        }

        return true
    }

    private fun showRegistrationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Register Account")
        builder.setMessage("For Milestone 2, registration is simulated. Enter any email and password (min 6 chars). In Milestone 3, this will connect to a backend.")

        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
            Toast.makeText(this, "Registration successful! Now login.", Toast.LENGTH_SHORT).show()
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    private fun checkFirstLaunch() {
        val isFirstLaunch = sharedPrefs.getBoolean("first_launch", true)
        if (isFirstLaunch) {
            sharedPrefs.edit().putBoolean("first_launch", false).apply()
            showWelcomeDialog()
        }
    }

    private fun showWelcomeDialog() {
        AlertDialog.Builder(this)
            .setTitle("Welcome to Stepps!")
            .setMessage("Track your daily steps, set goals, and stay active. Login or continue as guest to get started.")
            .setPositiveButton("Let's Go") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}