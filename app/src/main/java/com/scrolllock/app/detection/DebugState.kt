package com.scrolllock.app.detection

import android.graphics.Rect

data class DebugState(
    val packageName: String = "N/A",
    val eventType: String = "N/A",
    val surface: DetectionSurface = DetectionSurface.UNKNOWN,
    val confidence: Double = 0.0,
    val matchResult: DetectionResult = DetectionResult.UNKNOWN,
    val matchedIds: List<String> = emptyList(),
    val matchedContentDescriptions: List<String> = emptyList(),
    val bounds: Rect? = null,
    val policyDecision: String = "N/A",
    val policyReason: String = "N/A",
    val cooldownActive: Boolean = false,
    val cooldownSourceApp: String? = null,
    val cooldownExpiry: Long = 0L,
    val antiScrollSessionDuration: Long = 0L,
    val antiScrollSwipeCount: Int = 0,
    val antiScrollDirectionChanges: Int = 0,
    val antiScrollBlocked: Boolean = false,
    val antiScrollBlockedUntil: Long = 0L,
    val timestamp: Long = System.currentTimeMillis()
)

object DebugStateHolder {
    @Volatile
    private var currentState = DebugState()

    fun update(newState: DebugState) {
        currentState = newState
    }

    fun get(): DebugState = currentState
}