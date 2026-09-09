package com.scrolllock.app.detection.snapchat

import android.view.accessibility.AccessibilityNodeInfo
import com.scrolllock.app.detection.*

class SnapchatDetector : ContentDetector {
    override val packageNames = setOf("com.snapchat.android")

    companion object {
        private const val SPOTLIGHT_TAB = "com.snapchat.android:id/spotlight_tab"
        private const val STORIES_PAGE = "com.snapchat.android:id/stories_page"
    }

    override fun classify(root: AccessibilityNodeInfo): List<DetectionCandidate> {
        val candidates = mutableListOf<DetectionCandidate>()

        if (isOnSpotlight(root)) {
            candidates.add(DetectionCandidate(
                packageName = "com.snapchat.android",
                surface = DetectionSurface.VIDEO_FEED,
                confidence = 0.65,
                reasonCodes = listOf("spotlight_tab")
            ))
        }

        if (hasStories(root)) {
            candidates.add(DetectionCandidate(
                packageName = "com.snapchat.android",
                surface = DetectionSurface.STORIES,
                confidence = 0.60,
                reasonCodes = listOf("stories_page")
            ))
        }

        return candidates
    }

    private fun isOnSpotlight(root: AccessibilityNodeInfo): Boolean {
        return NodeUtils.hasDescendantWithId(root, SPOTLIGHT_TAB)
    }

    private fun hasStories(root: AccessibilityNodeInfo): Boolean {
        return NodeUtils.hasDescendantWithId(root, STORIES_PAGE)
    }
}
