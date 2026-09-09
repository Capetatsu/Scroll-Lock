package com.scrolllock.app.detection.instagram

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.scrolllock.app.detection.*

class InstagramDetector : ContentDetector {
    override val packageNames = setOf("com.instagram.android")

    companion object {
        private const val REEL_ITEM_TOOLBAR = "com.instagram.android:id/reel_item_toolbar_container"
        private const val REELS_TRAY = "com.instagram.android:id/reels_tray_container"
        private const val ROOT_CLIPS = "com.instagram.android:id/root_clips_layout"
        private const val CLIPS_AUTHOR = "com.instagram.android:id/clips_author_username"
        private const val DIRECT_TAB = "com.instagram.android:id/direct_tab"
        private const val FEED_TAB = "com.instagram.android:id/feed_tab"
        private const val EXPLORE_ACTION_BAR = "com.instagram.android:id/explore_action_bar"
        private const val LIKE_COUNT = "com.instagram.android:id/like_count"
        private const val INBOX_LIST = "com.instagram.android:id/inbox_refreshable_thread_list_recyclerview"
        private const val DIRECT_THREAD_HEADER = "com.instagram.android:id/direct_thread_header"

        private const val CONTENT_DESC_REELS = "Reels"
        private const val CONTENT_DESC_REELS_HU = "Reelek"
        private const val CONTENT_DESC_EXPLORE = "Explore"
        private const val CONTENT_DESC_STORIES = "Stories"
    }

    override fun classify(root: AccessibilityNodeInfo): List<DetectionCandidate> {
        val candidates = mutableListOf<DetectionCandidate>()

        if (hasReelsIndicator(root)) {
            candidates.add(DetectionCandidate(
                packageName = "com.instagram.android",
                surface = DetectionSurface.REELS,
                confidence = calculateReelsConfidence(root),
                bounds = findReelsBounds(root),
                reasonCodes = listOf("reel_ids", "reels_tray")
            ))
        }

        if (hasStoriesIndicator(root)) {
            candidates.add(DetectionCandidate(
                packageName = "com.instagram.android",
                surface = DetectionSurface.STORIES,
                confidence = 0.75,
                reasonCodes = listOf("stories_descriptor")
            ))
        }

        if (isOnExplore(root)) {
            candidates.add(DetectionCandidate(
                packageName = "com.instagram.android",
                surface = DetectionSurface.EXPLORE,
                confidence = 0.80,
                reasonCodes = listOf("explore_action_bar")
            ))
        }

        if (isOnFeed(root)) {
            candidates.add(DetectionCandidate(
                packageName = "com.instagram.android",
                surface = DetectionSurface.MAIN_FEED,
                confidence = 0.70,
                reasonCodes = listOf("feed_tab_selected")
            ))
        }

        if (isInDM(root)) {
            candidates.add(DetectionCandidate(
                packageName = "com.instagram.android",
                surface = DetectionSurface.DM,
                confidence = 0.85,
                reasonCodes = listOf("direct_tab", "inbox_list")
            ))
        }

        return candidates
    }

    private fun hasReelsIndicator(root: AccessibilityNodeInfo): Boolean {
        return NodeUtils.hasDescendantWithId(root, REEL_ITEM_TOOLBAR) ||
                NodeUtils.hasDescendantWithId(root, REELS_TRAY) ||
                NodeUtils.hasDescendantWithId(root, ROOT_CLIPS) ||
                NodeUtils.hasDescendantWithId(root, CLIPS_AUTHOR) ||
                hasContentDescription(root, CONTENT_DESC_REELS)
    }

    private fun calculateReelsConfidence(root: AccessibilityNodeInfo): Double {
        var confidence = 0.55
        if (NodeUtils.hasDescendantWithId(root, REEL_ITEM_TOOLBAR)) confidence += 0.20
        if (NodeUtils.hasDescendantWithId(root, REELS_TRAY)) confidence += 0.15
        if (NodeUtils.hasDescendantWithId(root, ROOT_CLIPS)) confidence += 0.10
        if (hasContentDescription(root, CONTENT_DESC_REELS)) confidence += 0.10
        return confidence.coerceAtMost(1.0)
    }

    private fun findReelsBounds(root: AccessibilityNodeInfo): Rect? {
        val node = NodeUtils.findNodeById(root, REEL_ITEM_TOOLBAR)
            ?: NodeUtils.findNodeById(root, ROOT_CLIPS)
        return NodeUtils.getBounds(node)
    }

    private fun hasStoriesIndicator(root: AccessibilityNodeInfo): Boolean {
        return hasContentDescription(root, CONTENT_DESC_STORIES)
    }

    private fun isOnExplore(root: AccessibilityNodeInfo): Boolean {
        return NodeUtils.hasDescendantWithId(root, EXPLORE_ACTION_BAR)
    }

    private fun isOnFeed(root: AccessibilityNodeInfo): Boolean {
        val feedTab = NodeUtils.findNodeById(root, FEED_TAB)
        return feedTab != null && NodeUtils.isSelected(feedTab)
    }

    private fun isInDM(root: AccessibilityNodeInfo): Boolean {
        return NodeUtils.hasDescendantWithId(root, DIRECT_TAB) ||
                NodeUtils.hasDescendantWithId(root, INBOX_LIST) ||
                NodeUtils.hasDescendantWithId(root, DIRECT_THREAD_HEADER)
    }

    private fun hasContentDescription(root: AccessibilityNodeInfo, desc: String): Boolean {
        return NodeUtils.findByText(root, desc) != null
    }
}
