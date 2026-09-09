package com.scrolllock.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_info")
data class AppInfo(
    @PrimaryKey val packageName: String,
    val blockingMode: Int = 0,
    val timeoutSeconds: Int = 0,
    val lastBlockedTS: Long = 0,
    val antiReels: Int = 0,
    val antiReelsSettingsJSON: String? = null,
    val antiScroll: Boolean = false,
    val enabled: Boolean = false
)
