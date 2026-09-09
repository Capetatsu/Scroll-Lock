package com.scrolllock.app.detection.snapchat

import android.view.accessibility.AccessibilityNodeInfo
import com.scrolllock.app.detection.*

class SnapchatDetector : ContentDetector {
    override val packageNames = setOf("com.snapchat.android")

    companion object {
        private const val SPOTLIGHT_TAB = "com.snapchat.android:id/spotlight_tab"
        private const val STORIES_PAGE = "com.snapchat.android:id/stories_page"
        private const val SPOTLIGHT_PLAYER = "com.snapchat.android:id/spotlight_player"

        private const val CONTENT_DESC_SPOTLIGHT = "Spotlight"
        private const val CONTENT_DESC_STORIES = "Stories"
    }

    override fun classify(root: AccessibilityNodeInfo): List<DetectionCandidate> {
        val candidates = mutableListOf<DetectionCandidate>()

        detectSpotlight(root)?.let { candidates.add(it) }
        detectStories(root)?.let { candidates.add(it) }

        return candidates
    }

    private fun detectSpotlight(root: AccessibilityNodeInfo): DetectionCandidate? {
        val signals = mutableListOf<String>()
        var confidence = 0.0

        if (NodeUtils.hasDescendantWithId(root, SPOTLIGHT_TAB)) {
            signals.add("spotlight_tab")
            confidence += 0.30
        }
        if (NodeUtils.hasDescendantWithId(root, SPOTLIGHT_PLAYER)) {
            signals.add("spotlight_player")
            confidence += 0.25
        }
        if (NodeUtils.findByText(root, CONTENT_DESC_SPOTLIGHT) != null) {
            signals.add("spotlight_text")
            confidence += 0.15
        }

        if (signals.isEmpty()) return null

        return DetectionCandidate(
            packageName = "com.snapchat.android",
            surface = DetectionSurface.VIDEO_FEED,
            confidence = confidence.coerceAtMost(1.0),
            reasonCodes = signals
        )
    }

    private fun detectStories(root: AccessibilityNodeInfo): DetectionCandidate? {
        val signals = mutableListOf<String>()
        var confidence = 0.0

        if (NodeUtils.hasDescendantWithId(root, STORIES_PAGE)) {
            signals.add("stories_page")
            confidence += 0.30
        }
        if (NodeUtils.findByText(root, CONTENT_DESC_STORIES) != null) {
            signals.add("stories_text")
            confidence += 0.15
        }

        if (signals.isEmpty()) return null

        return DetectionCandidate(
            packageName = "com.snapchat.android",
            surface = DetectionSurface.STORIES,
            confidence = confidence.coerceAtMost(1.0),
            reasonCodes = signals
        )
    }
}
