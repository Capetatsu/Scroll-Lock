package com.scrolllock.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_antiscroll_mode")
data class CustomAntiScrollMode(
    @PrimaryKey val packageName: String,
    val swipesFrequency: Int = 1,
    val checkWindow: Int = 20,
    val durationThreshold: Int = 180
)
