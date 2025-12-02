package com.stepps

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import kotlin.math.sqrt
import kotlin.math.pow

class StepCounterService : Service(), SensorEventListener {

    companion object {
        private const val TAG = "StepCounterService"
        private const val CHANNEL_ID = "step_counter_channel"
        private const val NOTIFICATION_ID = 101
        private const val STEP_THRESHOLD = 8.0f
        private const val STEP_COOLDOWN_MS = 300L
    }

    // Sensor components
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null

    // Location components
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    // Data storage
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var dbHelper: DatabaseHelper

    // Step counting variables
    private var stepCount = 0
    private var lastStepTime = 0L
    private val lastAccelerometer = FloatArray(3) { 0f }
    private val lastGyroscope = FloatArray(3) { 0f }

    // Distance tracking
    private var totalDistance = 0.0
    private var lastLocation: Location? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "StepCounterService created")

        // Initialize components
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        sharedPrefs = getSharedPreferences("SteppsPrefs", Context.MODE_PRIVATE)
        dbHelper = DatabaseHelper(this)

        // Get sensors - FIXED: Use nullable types and safe calls
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        // Setup location updates
        setupLocationTracking()

        // Create notification channel
        createNotificationChannel()

        // Start sensors
        startSensors()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "StepCounterService started")

        // Start as foreground service
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        return START_STICKY
    }

    private fun startSensors() {
        // Register sensor listeners safely
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        // Load previous step count
        stepCount = sharedPrefs.getInt("current_steps", 0)
        Log.d(TAG, "Loaded step count: $stepCount")
    }

    private fun setupLocationTracking() {
        val locationRequest = LocationRequest.create().apply {
            interval = 10000 // 10 seconds
            fastestInterval = 5000 // 5 seconds
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    processLocationUpdate(location)
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                null
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Location permission not granted", e)
        }
    }

    private fun processLocationUpdate(location: Location) {
        if (location.accuracy < 20) { // Acceptable accuracy
            lastLocation?.let { previousLocation ->
                // Calculate distance between locations
                val distance = previousLocation.distanceTo(location)
                if (distance > 1) { // Filter small movements
                    totalDistance += distance
                    updateNotification()
                    saveDistanceToPrefs()
                }
            }
            lastLocation = location
        }
    }

    // SensorEventListener implementation
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> processAccelerometerData(event.values)
                Sensor.TYPE_GYROSCOPE -> processGyroscopeData(event.values)
            }
        }
    }

    private fun processAccelerometerData(values: FloatArray) {
        // Calculate acceleration magnitude using squared values
        val deltaX = values[0] - lastAccelerometer[0]
        val deltaY = values[1] - lastAccelerometer[1]
        val deltaZ = values[2] - lastAccelerometer[2]

        // Calculate magnitude without pow function
        val magnitude = sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ)

        // Detect step based on threshold and cooldown
        val currentTime = System.currentTimeMillis()
        if (magnitude > STEP_THRESHOLD && currentTime - lastStepTime > STEP_COOLDOWN_MS) {
            stepCount++
            lastStepTime = currentTime

            Log.d(TAG, "Step detected! Total: $stepCount")

            // Save to preferences
            sharedPrefs.edit().putInt("current_steps", stepCount).apply()

            // Calculate and save calories
            val calories = calculateCalories(stepCount)
            sharedPrefs.edit().putFloat("current_calories", calories).apply()

            // Update notification
            updateNotification()

            // Save to database periodically (every 100 steps)
            if (stepCount % 100 == 0) {
                saveToDatabase()
            }

            // Check for achievements
            checkStepAchievements()
        }

        // Update last values
        values.copyInto(lastAccelerometer)
    }

    private fun processGyroscopeData(values: FloatArray) {
        // Use gyroscope to filter out non-walking movements
        val rotationMagnitude = sqrt(values[0] * values[0] + values[1] * values[1] + values[2] * values[2])

        // Store for future filtering (simplified for milestone 2)
        values.copyInto(lastGyroscope)
    }

    private fun calculateCalories(steps: Int): Float {
        // Simplified calorie calculation: 0.04 calories per step per kg
        val weight = sharedPrefs.getFloat("user_weight", 70f) // Default 70kg
        return steps * weight * 0.00004f
    }

    private fun checkStepAchievements() {
        // Check for step-based achievements
        if (stepCount >= 1000 && !sharedPrefs.getBoolean("achievement_1k", false)) {
            dbHelper.unlockAchievement(1)
            sharedPrefs.edit().putBoolean("achievement_1k", true).apply()
            showAchievementNotification("First 1K Steps!")
        }

        if (stepCount >= 10000 && !sharedPrefs.getBoolean("achievement_10k", false)) {
            dbHelper.unlockAchievement(2)
            sharedPrefs.edit().putBoolean("achievement_10k", true).apply()
            showAchievementNotification("10K Master!")
        }
    }

    private fun saveToDatabase() {
        val calories = sharedPrefs.getFloat("current_calories", 0f)
        val timeActive = System.currentTimeMillis() - sharedPrefs.getLong("tracking_start", System.currentTimeMillis())

        // Get today's date
        val date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())

        dbHelper.saveDailySteps(
            date = date,
            steps = stepCount,
            calories = calories.toDouble(),
            distance = totalDistance,
            time = timeActive
        )
    }

    private fun saveDistanceToPrefs() {
        sharedPrefs.edit().putFloat("current_distance", totalDistance.toFloat()).apply()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Step Counter",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Tracks your steps and activity"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Stepps - Step Tracker")
            .setContentText("Steps: $stepCount | Distance: ${String.format("%.2f", totalDistance)}m")
            .setSmallIcon(R.drawable.ic_steps)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification() {
        val notification = createNotification()
        val notificationManager = getSystemService(NotificationManager::class.java) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun showAchievementNotification(message: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Achievement Unlocked!")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_steps)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "StepCounterService destroyed")

        // Save final data
        saveToDatabase()

        // Unregister sensors
        sensorManager.unregisterListener(this)

        // Stop location updates
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing location updates", e)
        }
    }
}