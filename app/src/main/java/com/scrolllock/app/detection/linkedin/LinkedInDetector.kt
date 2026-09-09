package com.scrolllock.app.detection.linkedin

import android.view.accessibility.AccessibilityNodeInfo
import com.scrolllock.app.detection.*

class LinkedInDetector : ContentDetector {
    override val packageNames = setOf("com.linkedin.android")

    companion object {
        private const val VIDEO_PLAYER = "com.linkedin.android:id/video_player"
        private const val MEDIA_CONTAINER = "com.linkedin.android:id/media_container"
        private const val VIDEO_FEED = "com.linkedin.android:id/video_feed"
    }

    override fun classify(root: AccessibilityNodeInfo): List<DetectionCandidate> {
        val candidates = mutableListOf<DetectionCandidate>()

        detectVideo(root)?.let { candidates.add(it) }

        return candidates
    }

    private fun detectVideo(root: AccessibilityNodeInfo): DetectionCandidate? {
        val signals = mutableListOf<String>()
        var confidence = 0.0

        if (NodeUtils.hasDescendantWithId(root, VIDEO_PLAYER)) {
            signals.add("video_player")
            confidence += 0.25
        }
        if (NodeUtils.hasDescendantWithId(root, MEDIA_CONTAINER)) {
            signals.add("media_container")
            confidence += 0.20
        }
        if (NodeUtils.hasDescendantWithId(root, VIDEO_FEED)) {
            signals.add("video_feed")
            confidence += 0.15
        }

        if (signals.isEmpty()) return null

        return DetectionCandidate(
            packageName = "com.linkedin.android",
            surface = DetectionSurface.VIDEO_FEED,
            confidence = confidence.coerceAtMost(1.0),
            reasonCodes = signals
        )
    }
}
