package com.stepps

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
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
        checkAlreadyLoggedIn()
    }

    private fun checkAlreadyLoggedIn() {
        val userId = sharedPrefs.getLong("user_id", -1)
        val isGuest = sharedPrefs.getBoolean("is_guest", false)

        if (userId != -1L || isGuest) {
            navigateToMain()
        }
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (validateInput(email, password)) {
                performLogin(email, password)
            }
        }

        binding.btnRegister.setOnClickListener {
            showRegistrationDialog()
        }

        binding.btnGuest.setOnClickListener {
            loginAsGuest()
        }

        binding.tvRegisterNow.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = android.net.Uri.parse("https://www.example.com/register-help")
            }
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(this, "No browser available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun performLogin(email: String, password: String) {
        val userId = dbHelper.loginUser(email, password)

        if (userId != -1L) {
            val user = dbHelper.getUserById(userId)

            sharedPrefs.edit()
                .putLong("user_id", userId)
                .putString("user_email", email)
                .putBoolean("is_logged_in", true)
                .putBoolean("is_guest", false)
                .putInt("current_steps", 0)
                .putFloat("current_calories", 0f)
                .putFloat("current_distance", 0f)
                .apply()

            Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show()
            navigateToMain()
        } else {
            Toast.makeText(this, "Invalid email or password", Toast.LENGTH_SHORT).show()
            binding.etPassword.error = "Incorrect password"
        }
    }

    private fun loginAsGuest() {
        sharedPrefs.edit()
            .putLong("user_id", -1L)
            .putBoolean("is_guest", true)
            .putBoolean("is_logged_in", false)
            .remove("user_email")
            .putInt("current_steps", 0)
            .putFloat("current_calories", 0f)
            .putFloat("current_distance", 0f)
            .apply()

        Toast.makeText(this, "Continuing as guest", Toast.LENGTH_SHORT).show()
        navigateToMain()
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
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
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 40)
        }

        val etEmail = EditText(this).apply {
            hint = "Email"
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }
        val etPassword = EditText(this).apply {
            hint = "Password (min 6 chars)"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val etConfirm = EditText(this).apply {
            hint = "Confirm Password"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val etHeight = EditText(this).apply {
            hint = "Height (cm)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        val etWeight = EditText(this).apply {
            hint = "Weight (kg)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        val etAge = EditText(this).apply {
            hint = "Age"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }

        layout.addView(etEmail)
        layout.addView(etPassword)
        layout.addView(etConfirm)
        layout.addView(etHeight)
        layout.addView(etWeight)
        layout.addView(etAge)

        AlertDialog.Builder(this)
            .setTitle("Create Account")
            .setView(layout)
            .setPositiveButton("Register") { dialog, _ ->
                val email = etEmail.text.toString().trim()
                val password = etPassword.text.toString().trim()
                val confirmPassword = etConfirm.text.toString().trim()
                val height = etHeight.text.toString().toFloatOrNull() ?: 170f
                val weight = etWeight.text.toString().toFloatOrNull() ?: 70f
                val age = etAge.text.toString().toIntOrNull() ?: 25

                when {
                    email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                        Toast.makeText(this, "Invalid email", Toast.LENGTH_SHORT).show()
                    }
                    password.length < 6 -> {
                        Toast.makeText(this, "Password must be 6+ chars", Toast.LENGTH_SHORT).show()
                    }
                    password != confirmPassword -> {
                        Toast.makeText(this, "Passwords don't match", Toast.LENGTH_SHORT).show()
                    }
                    dbHelper.getUserByEmail(email) != null -> {
                        Toast.makeText(this, "Email already registered", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        val userId = dbHelper.registerUser(email, password, height, weight, age)
                        if (userId != -1L) {
                            Toast.makeText(this, "Registration successful! Please login.", Toast.LENGTH_LONG).show()
                            binding.etEmail.setText(email)
                            binding.etPassword.setText("")
                        } else {
                            Toast.makeText(this, "Registration failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
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