package com.stepps.fitness.models

data class DailyStats(
    val date: String,
    val steps: Int,
    val calories: Double,
    val distance: Double,
    val time: Long
)