package com.stepps

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "stepps.db"
        private const val DATABASE_VERSION = 1

        // Table names
        const val TABLE_USER = "user_profile"
        const val TABLE_STEPS = "daily_steps"
        const val TABLE_ACHIEVEMENTS = "achievements"

        // Common columns
        const val COLUMN_ID = "id"
        const val COLUMN_DATE = "date"

        // User table columns
        const val COLUMN_EMAIL = "email"
        const val COLUMN_HEIGHT = "height"
        const val COLUMN_WEIGHT = "weight"
        const val COLUMN_AGE = "age"
        const val COLUMN_DAILY_GOAL = "daily_goal"

        // Steps table columns
        const val COLUMN_STEPS = "steps"
        const val COLUMN_CALORIES = "calories"
        const val COLUMN_DISTANCE = "distance"
        const val COLUMN_TIME = "active_time"

        // Achievements table columns
        const val COLUMN_TITLE = "title"
        const val COLUMN_DESCRIPTION = "description"
        const val COLUMN_UNLOCKED = "unlocked"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Create User Profile Table
        val CREATE_USER_TABLE = """
            CREATE TABLE $TABLE_USER (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_EMAIL TEXT UNIQUE,
                $COLUMN_HEIGHT REAL,
                $COLUMN_WEIGHT REAL,
                $COLUMN_AGE INTEGER,
                $COLUMN_DAILY_GOAL INTEGER DEFAULT 10000
            )
        """.trimIndent()
        db.execSQL(CREATE_USER_TABLE)

        // Create Daily Steps Table
        val CREATE_STEPS_TABLE = """
            CREATE TABLE $TABLE_STEPS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_DATE TEXT UNIQUE,
                $COLUMN_STEPS INTEGER DEFAULT 0,
                $COLUMN_CALORIES REAL DEFAULT 0,
                $COLUMN_DISTANCE REAL DEFAULT 0,
                $COLUMN_TIME INTEGER DEFAULT 0
            )
        """.trimIndent()
        db.execSQL(CREATE_STEPS_TABLE)

        // Create Achievements Table (Simplified for Milestone 2)
        val CREATE_ACHIEVEMENTS_TABLE = """
            CREATE TABLE $TABLE_ACHIEVEMENTS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TITLE TEXT,
                $COLUMN_DESCRIPTION TEXT,
                $COLUMN_UNLOCKED INTEGER DEFAULT 0
            )
        """.trimIndent()
        db.execSQL(CREATE_ACHIEVEMENTS_TABLE)

        insertDefaultAchievements(db)
    }

    private fun insertDefaultAchievements(db: SQLiteDatabase) {
        val achievements = listOf(
            arrayOf("First 1K Steps", "Take your first 1,000 steps"),
            arrayOf("10K Master", "Reach 10,000 steps in a day"),
            arrayOf("Week Streak", "7 consecutive active days")
        )

        achievements.forEach { achievement ->
            val values = ContentValues().apply {
                put(COLUMN_TITLE, achievement[0])
                put(COLUMN_DESCRIPTION, achievement[1])
            }
            db.insert(TABLE_ACHIEVEMENTS, null, values)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USER")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_STEPS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ACHIEVEMENTS")
        onCreate(db)
    }

    // User Operations
    fun addUser(email: String, height: Float, weight: Float, age: Int): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_EMAIL, email)
            put(COLUMN_HEIGHT, height)
            put(COLUMN_WEIGHT, weight)
            put(COLUMN_AGE, age)
        }

        val result = db.insert(TABLE_USER, null, values)
        return result != -1L
    }

    fun getUser(email: String): User? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USER,
            arrayOf(COLUMN_HEIGHT, COLUMN_WEIGHT, COLUMN_AGE, COLUMN_DAILY_GOAL),
            "$COLUMN_EMAIL = ?",
            arrayOf(email),
            null, null, null
        )

        return if (cursor.moveToFirst()) {
            User(
                email = email,
                height = cursor.getFloat(0),
                weight = cursor.getFloat(1),
                age = cursor.getInt(2),
                dailyGoal = cursor.getInt(3)
            )
        } else {
            null
        }.also { cursor.close() }
    }

    // Steps Operations
    fun saveDailySteps(date: String, steps: Int, calories: Double, distance: Double, time: Long): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_DATE, date)
            put(COLUMN_STEPS, steps)
            put(COLUMN_CALORIES, calories)
            put(COLUMN_DISTANCE, distance)
            put(COLUMN_TIME, time)
        }

        // Check if entry exists
        val cursor = db.query(
            TABLE_STEPS,
            arrayOf(COLUMN_ID),
            "$COLUMN_DATE = ?",
            arrayOf(date),
            null, null, null
        )

        return if (cursor.moveToFirst()) {
            // Update existing
            val rows = db.update(TABLE_STEPS, values, "$COLUMN_DATE = ?", arrayOf(date))
            cursor.close()
            rows > 0
        } else {
            // Insert new
            val result = db.insert(TABLE_STEPS, null, values)
            cursor.close()
            result != -1L
        }
    }

    fun getTodayStats(date: String): DailyStats {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_STEPS,
            arrayOf(COLUMN_STEPS, COLUMN_CALORIES, COLUMN_DISTANCE, COLUMN_TIME),
            "$COLUMN_DATE = ?",
            arrayOf(date),
            null, null, null
        )

        return if (cursor.moveToFirst()) {
            DailyStats(
                steps = cursor.getInt(0),
                calories = cursor.getDouble(1),
                distance = cursor.getDouble(2),
                time = cursor.getLong(3)
            )
        } else {
            DailyStats(0, 0.0, 0.0, 0)
        }.also { cursor.close() }
    }

    fun getWeeklyStats(): List<DailyStats> {
        val db = readableDatabase
        val stats = mutableListOf<DailyStats>()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val currentDate = Date()

        for (i in 6 downTo 0) {
            val date = Date(currentDate.time - (i * 24 * 60 * 60 * 1000))
            val dateStr = sdf.format(date)

            val cursor = db.query(
                TABLE_STEPS,
                arrayOf(COLUMN_STEPS, COLUMN_CALORIES, COLUMN_DISTANCE, COLUMN_TIME),
                "$COLUMN_DATE = ?",
                arrayOf(dateStr),
                null, null, null
            )

            if (cursor.moveToFirst()) {
                stats.add(
                    DailyStats(
                        steps = cursor.getInt(0),
                        calories = cursor.getDouble(1),
                        distance = cursor.getDouble(2),
                        time = cursor.getLong(3),
                        date = dateStr
                    )
                )
            } else {
                stats.add(DailyStats(date = dateStr))
            }
            cursor.close()
        }

        return stats
    }

    fun getTotalSteps(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT SUM($COLUMN_STEPS) FROM $TABLE_STEPS", null)

        return if (cursor.moveToFirst()) {
            cursor.getInt(0)
        } else {
            0
        }.also { cursor.close() }
    }

    // Achievements Operations
    fun getAchievements(): List<Achievement> {
        val db = readableDatabase
        val achievements = mutableListOf<Achievement>()

        val cursor = db.query(
            TABLE_ACHIEVEMENTS,
            arrayOf(COLUMN_ID, COLUMN_TITLE, COLUMN_DESCRIPTION, COLUMN_UNLOCKED),
            null, null, null, null, COLUMN_ID
        )

        while (cursor.moveToNext()) {
            achievements.add(
                Achievement(
                    id = cursor.getInt(0),
                    title = cursor.getString(1),
                    description = cursor.getString(2),
                    unlocked = cursor.getInt(3) == 1
                )
            )
        }
        cursor.close()

        return achievements
    }

    fun unlockAchievement(id: Int): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_UNLOCKED, 1)
        }

        val rows = db.update(TABLE_ACHIEVEMENTS, values, "$COLUMN_ID = ?", arrayOf(id.toString()))
        return rows > 0
    }
}

// Data Classes
data class User(
    val email: String,
    val height: Float = 170f,
    val weight: Float = 70f,
    val age: Int = 25,
    val dailyGoal: Int = 10000
)

data class DailyStats(
    val steps: Int = 0,
    val calories: Double = 0.0,
    val distance: Double = 0.0,
    val time: Long = 0,
    val date: String = ""
)

data class Achievement(
    val id: Int,
    val title: String,
    val description: String,
    val unlocked: Boolean = false
)