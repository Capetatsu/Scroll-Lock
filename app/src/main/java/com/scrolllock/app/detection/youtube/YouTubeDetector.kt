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
        private const val SHORTS_PIVOT = "com.google.android.youtube:id/shorts_pivot_bar"
        private const val VIDEO_PLAYER = "com.google.android.youtube:id/video_player"
        private const val HOME_FEED = "com.google.android.youtube:id/home"
    }

    override fun classify(root: AccessibilityNodeInfo): List<DetectionCandidate> {
        val candidates = mutableListOf<DetectionCandidate>()

        detectShorts(root)?.let { candidates.add(it) }

        return candidates
    }

    private fun detectShorts(root: AccessibilityNodeInfo): DetectionCandidate? {
        val signals = mutableListOf<String>()
        var confidence = 0.0

        if (NodeUtils.hasDescendantWithId(root, REEL_RECYCLER)) {
            signals.add("reel_recycler")
            confidence += 0.30
        }
        if (NodeUtils.hasDescendantWithId(root, SHORTS_PLAYER)) {
            signals.add("shorts_player_root")
            confidence += 0.25
        }
        if (NodeUtils.hasDescendantWithId(root, SHORTS_SHELF)) {
            signals.add("shorts_shelf")
            confidence += 0.20
        }
        if (NodeUtils.hasDescendantWithId(root, REELS_SHELF)) {
            signals.add("reels_shelf")
            confidence += 0.15
        }
        if (NodeUtils.hasDescendantWithId(root, SHORTS_PIVOT)) {
            signals.add("shorts_pivot_bar")
            confidence += 0.10
        }

        if (signals.isEmpty()) return null

        return DetectionCandidate(
            packageName = "com.google.android.youtube",
            surface = DetectionSurface.SHORTS,
            confidence = confidence.coerceAtMost(1.0),
            reasonCodes = signals,
            nodeReference = NodeUtils.findNodeById(root, REEL_RECYCLER)
                ?: NodeUtils.findNodeById(root, SHORTS_PLAYER)
        )
    }
}
