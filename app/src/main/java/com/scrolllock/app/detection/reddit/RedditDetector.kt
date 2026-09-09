package com.scrolllock.app.detection.reddit

import android.view.accessibility.AccessibilityNodeInfo
import com.scrolllock.app.detection.*

class RedditDetector : ContentDetector {
    override val packageNames = setOf("com.reddit.frontpage")

    companion object {
        private const val VIDEO_PLAYER = "com.reddit.frontpage:id/media_player"
        private const val SHORTS_CONTAINER = "com.reddit.frontpage:id/shorts_container"
    }

    override fun classify(root: AccessibilityNodeInfo): List<DetectionCandidate> {
        val candidates = mutableListOf<DetectionCandidate>()

        if (isOnVideo(root)) {
            candidates.add(DetectionCandidate(
                packageName = "com.reddit.frontpage",
                surface = DetectionSurface.VIDEO_FEED,
                confidence = 0.60,
                reasonCodes = listOf("video_player")
            ))
        }

        return candidates
    }

    private fun isOnVideo(root: AccessibilityNodeInfo): Boolean {
        return NodeUtils.hasDescendantWithId(root, VIDEO_PLAYER) ||
                NodeUtils.hasDescendantWithId(root, SHORTS_CONTAINER)
    }
}
