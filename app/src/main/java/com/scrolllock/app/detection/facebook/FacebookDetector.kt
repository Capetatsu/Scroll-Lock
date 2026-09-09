package com.scrolllock.app.detection.facebook

import android.view.accessibility.AccessibilityNodeInfo
import com.scrolllock.app.detection.*

class FacebookDetector : ContentDetector {
    override val packageNames = setOf("com.facebook.katana", "com.facebook.lite")

    companion object {
        private const val REELS_TAB = "com.facebook.katana:id/video_tab"
        private const val STORIES_TRAY = "com.facebook.katana:id/stories_tray"
        private const val FEED_CONTAINER = "com.facebook.katana:id/news_feed_container"
        private const val REEL_PLAYER = "com.facebook.katana:id/reel_player_view"

        private const val CONTENT_DESC_REELS = "Reels"
        private const val CONTENT_DESC_STORIES = "Stories"
    }

    override fun classify(root: AccessibilityNodeInfo): List<DetectionCandidate> {
        val candidates = mutableListOf<DetectionCandidate>()

        detectReels(root)?.let { candidates.add(it) }
        detectStories(root)?.let { candidates.add(it) }

        return candidates
    }

    private fun detectReels(root: AccessibilityNodeInfo): DetectionCandidate? {
        val signals = mutableListOf<String>()
        var confidence = 0.0

        if (NodeUtils.hasDescendantWithId(root, REELS_TAB)) {
            signals.add("video_tab")
            confidence += 0.30
        }
        if (NodeUtils.hasDescendantWithId(root, REEL_PLAYER)) {
            signals.add("reel_player_view")
            confidence += 0.25
        }
        if (NodeUtils.findByText(root, CONTENT_DESC_REELS) != null) {
            signals.add("reels_text")
            confidence += 0.15
        }

        if (signals.isEmpty()) return null

        return DetectionCandidate(
            packageName = "com.facebook.katana",
            surface = DetectionSurface.REELS,
            confidence = confidence.coerceAtMost(1.0),
            reasonCodes = signals
        )
    }

    private fun detectStories(root: AccessibilityNodeInfo): DetectionCandidate? {
        val signals = mutableListOf<String>()
        var confidence = 0.0

        if (NodeUtils.hasDescendantWithId(root, STORIES_TRAY)) {
            signals.add("stories_tray")
            confidence += 0.30
        }
        if (NodeUtils.findByText(root, CONTENT_DESC_STORIES) != null) {
            signals.add("stories_text")
            confidence += 0.15
        }

        if (signals.isEmpty()) return null

        return DetectionCandidate(
            packageName = "com.facebook.katana",
            surface = DetectionSurface.STORIES,
            confidence = confidence.coerceAtMost(1.0),
            reasonCodes = signals
        )
    }
}
