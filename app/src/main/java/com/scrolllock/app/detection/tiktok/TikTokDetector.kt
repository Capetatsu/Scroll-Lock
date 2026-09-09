package com.scrolllock.app.detection.tiktok

import android.view.accessibility.AccessibilityNodeInfo
import com.scrolllock.app.detection.*

class TikTokDetector : ContentDetector {
    override val packageNames = setOf(
        "com.zhiliaoapp.musically",
        "com.ss.android.ugc.trill"
    )

    companion object {
        private const val LONG_PRESS_LAYOUT = "com.zhiliaoapp.musically:id/long_press_layout"
        private const val FEED_CONTAINER = "com.zhiliaoapp.musically:id/d55"
        private const val VIDEO_PLAYER = "com.zhiliaoapp.musically:id/a3b"

        private const val CONTENT_DESC_TIKTOK = "TikTok"
        private const val CONTENT_DESC_FYP = "For You"
    }

    override fun classify(root: AccessibilityNodeInfo): List<DetectionCandidate> {
        val candidates = mutableListOf<DetectionCandidate>()

        if (isOnVideoFeed(root)) {
            candidates.add(DetectionCandidate(
                packageName = "com.zhiliaoapp.musically",
                surface = DetectionSurface.TIKTOK_VIDEO,
                confidence = calculateVideoConfidence(root),
                reasonCodes = listOf("long_press_layout", "video_player")
            ))
        }

        return candidates
    }

    private fun isOnVideoFeed(root: AccessibilityNodeInfo): Boolean {
        return NodeUtils.hasDescendantWithId(root, LONG_PRESS_LAYOUT) ||
                NodeUtils.hasDescendantWithId(root, FEED_CONTAINER) ||
                NodeUtils.hasDescendantWithId(root, VIDEO_PLAYER) ||
                hasTikTokContent(root)
    }

    private fun calculateVideoConfidence(root: AccessibilityNodeInfo): Double {
        var confidence = 0.55
        if (NodeUtils.hasDescendantWithId(root, LONG_PRESS_LAYOUT)) confidence += 0.25
        if (NodeUtils.hasDescendantWithId(root, VIDEO_PLAYER)) confidence += 0.15
        if (hasTikTokContent(root)) confidence += 0.10
        return confidence.coerceAtMost(1.0)
    }

    private fun hasTikTokContent(root: AccessibilityNodeInfo): Boolean {
        return NodeUtils.findByText(root, CONTENT_DESC_FYP) != null
    }
}
