package com.scrolllock.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedule_rules")
data class ScheduleRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String? = null,
    val enabled: Boolean = true,
    val appliesToAllApps: Boolean = false,
    val packageNamesJSON: String = "[]",
    val daysBitmask: Int = 0x7F,
    val timeSlotsJSON: String = "[]",
    val featuresMask: Int = 0xFF,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun isDayEnabled(dayOfWeek: Int): Boolean {
        return (daysBitmask and (1 shl dayOfWeek)) != 0
    }
}

object FeatureMask {
    const val ANTI_SCROLL = 1
    const val ANTI_REELS = 2
    const val APP_BLOCKING = 4
    const val BROWSER_BLOCKING = 8

    fun hasFeature(mask: Int, feature: Int): Boolean = (mask and feature) != 0
}
