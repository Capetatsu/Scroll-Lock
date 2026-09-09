package com.scrolllock.app.detection.youtube

import android.view.accessibility.AccessibilityNodeInfo
import com.scrolllock.app.detection.*

class YouTubeDetector : ContentDetector {
    override val packageNames = setOf("com.google.android.youtube")

    companion object {
        private const val REEL_RECYCLER = "com.google.android.youtube:id/reel_recycler"
        private const val REELS_SHELF = "com.google.android.youtube:id/reels_shelf"
        private const val SHORTS_SHELF = "com.google.android.youtube:id/shorts_shelf"
        private const val SHORTS_PLAYER = "com.google.android.youtube:id/shorts_player_root"

        private const val CONTENT_DESC_SHORTS = "Shorts"
        private const val CONTENT_DESC_REELS = "Reels"
    }

    override fun classify(root: AccessibilityNodeInfo): List<DetectionCandidate> {
        val candidates = mutableListOf<DetectionCandidate>()

        if (isOnShorts(root)) {
            candidates.add(DetectionCandidate(
                packageName = "com.google.android.youtube",
                surface = DetectionSurface.SHORTS,
                confidence = calculateShortsConfidence(root),
                reasonCodes = listOf("reel_recycler", "shorts_player")
            ))
        }

        return candidates
    }

    private fun isOnShorts(root: AccessibilityNodeInfo): Boolean {
        return NodeUtils.hasDescendantWithId(root, REEL_RECYCLER) ||
                NodeUtils.hasDescendantWithId(root, REELS_SHELF) ||
                NodeUtils.hasDescendantWithId(root, SHORTS_SHELF) ||
                NodeUtils.hasDescendantWithId(root, SHORTS_PLAYER) ||
                hasShortsContentDescription(root)
    }

    private fun calculateShortsConfidence(root: AccessibilityNodeInfo): Double {
        var confidence = 0.55
        if (NodeUtils.hasDescendantWithId(root, REEL_RECYCLER)) confidence += 0.25
        if (NodeUtils.hasDescendantWithId(root, SHORTS_PLAYER)) confidence += 0.15
        if (hasShortsContentDescription(root)) confidence += 0.10
        return confidence.coerceAtMost(1.0)
    }

    private fun hasShortsContentDescription(root: AccessibilityNodeInfo): Boolean {
        return NodeUtils.findByText(root, CONTENT_DESC_SHORTS) != null ||
                NodeUtils.findByText(root, CONTENT_DESC_REELS) != null
    }
}
