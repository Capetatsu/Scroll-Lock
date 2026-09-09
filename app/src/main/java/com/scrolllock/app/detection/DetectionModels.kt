package com.scrolllock.app.detection

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

enum class DetectionSurface {
    REELS,
    SHORTS,
    TIKTOK_VIDEO,
    STORIES,
    MAIN_FEED,
    EXPLORE,
    COMMENTS,
    DM,
    BROWSER_URL,
    VIDEO_FEED,
    UNKNOWN
}

enum class DetectionResult {
    MATCH,
    NO_MATCH,
    UNKNOWN
}

data class DetectionCandidate(
    val packageName: String,
    val surface: DetectionSurface,
    val confidence: Double,
    val bounds: Rect? = null,
    val reasonCodes: List<String> = emptyList(),
    val nodeReference: AccessibilityNodeInfo? = null,
    val matchResult: DetectionResult = if (confidence >= 0.70) DetectionResult.MATCH else DetectionResult.UNKNOWN,
    val hierarchyDepth: Int = 0,
    val isVisible: Boolean = true,
    val signals: List<DetectionSignal> = emptyList()
)

data class DetectionSignal(
    val type: SignalType,
    val identifier: String,
    val confidence: Double,
    val isVisible: Boolean = true,
    val isSelected: Boolean = false,
    val hierarchyDepth: Int = 0,
    val description: String = ""
)

enum class SignalType {
    RESOURCE_ID,
    CONTENT_DESCRIPTION,
    TEXT_MATCH,
    HIERARCHY,
    SELECTED_STATE,
    VISIBILITY,
    BOUNDS
}

data class DetectionDebugInfo(
    val packageName: String,
    val surface: DetectionSurface,
    val confidence: Double,
    val matchResult: DetectionResult,
    val signals: List<DetectionSignal>,
    val reasonCodes: List<String>,
    val timestamp: Long = System.currentTimeMillis()
)
