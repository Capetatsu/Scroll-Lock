package com.scrolllock.app.detection.linkedin

import android.view.accessibility.AccessibilityNodeInfo
import com.scrolllock.app.detection.*

class LinkedInDetector : ContentDetector {
    override val packageNames = setOf("com.linkedin.android")

    companion object {
        private const val VIDEO_PLAYER = "com.linkedin.android:id/video_player"
        private const val MEDIA_CONTAINER = "com.linkedin.android:id/media_container"
    }

    override fun classify(root: AccessibilityNodeInfo): List<DetectionCandidate> {
        val candidates = mutableListOf<DetectionCandidate>()

        if (isOnVideo(root)) {
            candidates.add(DetectionCandidate(
                packageName = "com.linkedin.android",
                surface = DetectionSurface.VIDEO_FEED,
                confidence = 0.55,
                reasonCodes = listOf("video_player")
            ))
        }

        return candidates
    }

    private fun isOnVideo(root: AccessibilityNodeInfo): Boolean {
        return NodeUtils.hasDescendantWithId(root, VIDEO_PLAYER) ||
                NodeUtils.hasDescendantWithId(root, MEDIA_CONTAINER)
    }
}
