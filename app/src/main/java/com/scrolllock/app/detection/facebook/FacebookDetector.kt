package com.scrolllock.app.detection.facebook

import android.view.accessibility.AccessibilityNodeInfo
import com.scrolllock.app.detection.*

class FacebookDetector : ContentDetector {
    override val packageNames = setOf("com.facebook.katana", "com.facebook.lite")

    companion object {
        private const val REELS_TAB = "com.facebook.katana:id/video_tab"
        private const val STORIES_TRAY = "com.facebook.katana:id/stories_tray"
        private const val FEED_CONTAINER = "com.facebook.katana:id/news_feed_container"
    }

    override fun classify(root: AccessibilityNodeInfo): List<DetectionCandidate> {
        val candidates = mutableListOf<DetectionCandidate>()

        if (isOnReels(root)) {
            candidates.add(DetectionCandidate(
                packageName = "com.facebook.katana",
                surface = DetectionSurface.REELS,
                confidence = 0.70,
                reasonCodes = listOf("video_tab")
            ))
        }

        if (hasStories(root)) {
            candidates.add(DetectionCandidate(
                packageName = "com.facebook.katana",
                surface = DetectionSurface.STORIES,
                confidence = 0.65,
                reasonCodes = listOf("stories_tray")
            ))
        }

        return candidates
    }

    private fun isOnReels(root: AccessibilityNodeInfo): Boolean {
        return NodeUtils.hasDescendantWithId(root, REELS_TAB)
    }

    private fun hasStories(root: AccessibilityNodeInfo): Boolean {
        return NodeUtils.hasDescendantWithId(root, STORIES_TRAY)
    }
}
