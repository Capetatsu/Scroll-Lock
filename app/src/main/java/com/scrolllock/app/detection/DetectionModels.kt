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
    val nodeReference: AccessibilityNodeInfo? = null
)
