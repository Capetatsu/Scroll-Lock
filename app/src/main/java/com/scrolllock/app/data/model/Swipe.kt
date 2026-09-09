package com.scrolllock.app.data.model

import androidx.room.Entity

@Entity(
    tableName = "swipes",
    primaryKeys = ["packageName", "timestamp"]
)
data class Swipe(
    val packageName: String,
    val timestamp: Long,
    val swipeDirection: Int,
    val scrollDeltaY: Int = 0,
    val fromIndex: Int = -1,
    val toIndex: Int = -1
)
