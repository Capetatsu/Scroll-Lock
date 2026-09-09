package com.scrolllock.app.detection.reddit

import android.view.accessibility.AccessibilityNodeInfo
import com.scrolllock.app.detection.*

class RedditDetector : ContentDetector {
    override val packageNames = setOf("com.reddit.frontpage")

    companion object {
        private const val VIDEO_PLAYER = "com.reddit.frontpage:id/media_player"
        private const val SHORTS_CONTAINER = "com.reddit.frontpage:id/shorts_container"
        private const val VIDEO_FEED = "com.reddit.frontpage:id/video_feed"
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
            signals.add("media_player")
            confidence += 0.25
        }
        if (NodeUtils.hasDescendantWithId(root, SHORTS_CONTAINER)) {
            signals.add("shorts_container")
            confidence += 0.25
        }
        if (NodeUtils.hasDescendantWithId(root, VIDEO_FEED)) {
            signals.add("video_feed")
            confidence += 0.20
        }

        if (signals.isEmpty()) return null

        return DetectionCandidate(
            packageName = "com.reddit.frontpage",
            surface = DetectionSurface.VIDEO_FEED,
            confidence = confidence.coerceAtMost(1.0),
            reasonCodes = signals
        )
    }
}
