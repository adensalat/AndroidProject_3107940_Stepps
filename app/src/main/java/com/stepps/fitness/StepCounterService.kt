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
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import kotlin.math.sqrt

class StepCounterService : Service(), SensorEventListener {

    companion object {
        private const val TAG = "StepCounterService"
        private const val CHANNEL_ID = "step_counter_channel"
        private const val NOTIFICATION_ID = 101
        private const val STEP_THRESHOLD = 8.0f
        private const val STEP_COOLDOWN_MS = 300L
    }

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var dbHelper: DatabaseHelper

    private var stepCount = 0
    private var lastStepTime = 0L
    private val lastAccelerometer = FloatArray(3) { 0f }
    private val lastGyroscope = FloatArray(3) { 0f }
    private var totalDistance = 0.0
    private var lastLocation: Location? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "StepCounterService created")

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        sharedPrefs = getSharedPreferences("SteppsPrefs", Context.MODE_PRIVATE)
        dbHelper = DatabaseHelper(this)

        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        setupLocationTracking()
        createNotificationChannel()
        startSensors()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "StepCounterService started")
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    private fun startSensors() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        stepCount = sharedPrefs.getInt("current_steps", 0)
        totalDistance = sharedPrefs.getFloat("current_distance", 0f).toDouble()
        Log.d(TAG, "Loaded step count: $stepCount")
    }

    private fun setupLocationTracking() {
        val locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
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
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
        } catch (e: SecurityException) {
            Log.e(TAG, "Location permission not granted", e)
        }
    }

    private fun processLocationUpdate(location: Location) {
        if (location.accuracy < 20) {
            lastLocation?.let { previousLocation ->
                val distance = previousLocation.distanceTo(location)
                if (distance > 1) {
                    totalDistance += distance
                    updateNotification()
                    saveDistanceToPrefs()
                }
            }
            lastLocation = location
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> processAccelerometerData(event.values)
                Sensor.TYPE_GYROSCOPE -> processGyroscopeData(event.values)
            }
        }
    }

    private fun processAccelerometerData(values: FloatArray) {
        val deltaX = values[0] - lastAccelerometer[0]
        val deltaY = values[1] - lastAccelerometer[1]
        val deltaZ = values[2] - lastAccelerometer[2]
        val magnitude = sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ)

        val currentTime = System.currentTimeMillis()
        if (magnitude > STEP_THRESHOLD && currentTime - lastStepTime > STEP_COOLDOWN_MS) {
            stepCount++
            lastStepTime = currentTime
            Log.d(TAG, "Step detected! Total: $stepCount")

            sharedPrefs.edit().putInt("current_steps", stepCount).apply()

            val calories = calculateCalories(stepCount)
            sharedPrefs.edit().putFloat("current_calories", calories).apply()

            updateNotification()
            broadcastStepUpdate()

            if (stepCount % 100 == 0) {
                saveToDatabase()
            }

            checkStepAchievements()
        }

        values.copyInto(lastAccelerometer)
    }

    private fun processGyroscopeData(values: FloatArray) {
        values.copyInto(lastGyroscope)
    }

    private fun getCurrentUserId(): Long {
        val userId = sharedPrefs.getLong("user_id", -1)
        val isGuest = sharedPrefs.getBoolean("is_guest", false)
        return if (isGuest) -1 else userId
    }

    private fun calculateCalories(steps: Int): Float {
        val weight = sharedPrefs.getFloat("user_weight", 70f)
        return steps * weight * 0.00004f
    }

    private fun checkStepAchievements() {
        val userId = getCurrentUserId()
        if (userId == -1L) return

        if (stepCount >= 1000 && !sharedPrefs.getBoolean("achievement_1k", false)) {
            dbHelper.unlockAchievement(userId, 1)
            sharedPrefs.edit().putBoolean("achievement_1k", true).apply()
            showAchievementNotification("First 1K Steps!")
        }

        if (stepCount >= 10000 && !sharedPrefs.getBoolean("achievement_10k", false)) {
            dbHelper.unlockAchievement(userId, 2)
            sharedPrefs.edit().putBoolean("achievement_10k", true).apply()
            showAchievementNotification("10K Master!")
        }
    }

    private fun saveToDatabase() {
        val userId = getCurrentUserId()
        if (userId == -1L) return

        val calories = sharedPrefs.getFloat("current_calories", 0f)
        val timeActive = System.currentTimeMillis() - sharedPrefs.getLong("tracking_start", System.currentTimeMillis())

        val date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())

        dbHelper.saveDailySteps(userId, date, stepCount, calories.toDouble(), totalDistance, timeActive)
        Log.d(TAG, "Saved to database: userId=$userId, steps=$stepCount")
    }

    private fun saveDistanceToPrefs() {
        sharedPrefs.edit().putFloat("current_distance", totalDistance.toFloat()).apply()
    }

    private fun broadcastStepUpdate() {
        val intent = Intent("STEP_UPDATE")
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
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

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "StepCounterService destroyed")
        saveToDatabase()
        sensorManager.unregisterListener(this)
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing location updates", e)
        }
    }
}