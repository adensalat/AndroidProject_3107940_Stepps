package com.stepps.models

data class Achievement(
    val id: Int,
    val title: String,
    val description: String,
    val unlocked: Boolean = false
)