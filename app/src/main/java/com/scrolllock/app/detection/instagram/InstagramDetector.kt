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
        private const val STORIES_TRAY = "com.instagram.android:id/stories_tray_container"
        private const val STORY_RING = "com.instagram.android:id/story_ring"
        private const val COMMENT_CONTAINER = "com.instagram.android:id/comment_thread_container"
        private const val COMMENT_INPUT = "com.instagram.android:id/layout_comment_thread_edittext"
        private const val NAVIGATION_TAB_BAR = "com.instagram.android:id/tab_bar"
        private const val REEL_VIEW_PAGER = "com.instagram.android:id/reels ViewPager"

        private const val CONTENT_DESC_REELS = "Reels"
        private const val CONTENT_DESC_EXPLORE = "Explore"
        private const val CONTENT_DESC_STORIES = "Stories"
        private const val CONTENT_DESC_DIRECT = "Direct"
        private const val CONTENT_DESC_COMMENTS = "Comments"
        private const val CONTENT_DESC_HOME = "Home"
        private const val CONTENT_DESC_SEARCH = "Search"
        private const val CONTENT_DESC_CREATE = "Create"
        private const val CONTENT_DESC_LIKES = "Likes"
    }

    override fun classify(root: AccessibilityNodeInfo): List<DetectionCandidate> {
        val candidates = mutableListOf<DetectionCandidate>()

        detectReels(root)?.let { candidates.add(it) }
        detectStories(root)?.let { candidates.add(it) }
        detectExplore(root)?.let { candidates.add(it) }
        detectFeed(root)?.let { candidates.add(it) }
        detectDM(root)?.let { candidates.add(it) }
        detectComments(root)?.let { candidates.add(it) }

        return candidates
    }

    private fun detectReels(root: AccessibilityNodeInfo): DetectionCandidate? {
        val signals = mutableListOf<String>()
        var confidence = 0.0

        if (NodeUtils.hasDescendantWithId(root, REEL_ITEM_TOOLBAR)) {
            signals.add("reel_item_toolbar_container")
            confidence += 0.30
        }
        if (NodeUtils.hasDescendantWithId(root, REELS_TRAY)) {
            signals.add("reels_tray_container")
            confidence += 0.20
        }
        if (NodeUtils.hasDescendantWithId(root, ROOT_CLIPS)) {
            signals.add("root_clips_layout")
            confidence += 0.15
        }
        if (NodeUtils.hasDescendantWithId(root, CLIPS_AUTHOR)) {
            signals.add("clips_author_username")
            confidence += 0.10
        }
        if (NodeUtils.hasDescendantWithId(root, REEL_VIEW_PAGER)) {
            signals.add("reels_view_pager")
            confidence += 0.10
        }
        if (hasContentDescription(root, CONTENT_DESC_REELS)) {
            signals.add("content_desc_reels")
            confidence += 0.10
        }
        if (NodeUtils.hasDescendantWithId(root, LIKE_COUNT)) {
            signals.add("like_count_visible")
            confidence += 0.05
        }

        if (signals.isEmpty()) return null

        val bounds = findReelsBounds(root)
        return DetectionCandidate(
            packageName = "com.instagram.android",
            surface = DetectionSurface.REELS,
            confidence = confidence.coerceAtMost(1.0),
            bounds = bounds,
            reasonCodes = signals,
            nodeReference = NodeUtils.findNodeById(root, REEL_ITEM_TOOLBAR)
                ?: NodeUtils.findNodeById(root, ROOT_CLIPS)
        )
    }

    private fun detectStories(root: AccessibilityNodeInfo): DetectionCandidate? {
        val signals = mutableListOf<String>()
        var confidence = 0.0

        if (NodeUtils.hasDescendantWithId(root, STORIES_TRAY)) {
            signals.add("stories_tray_container")
            confidence += 0.35
        }
        if (NodeUtils.hasDescendantWithId(root, STORY_RING)) {
            signals.add("story_ring")
            confidence += 0.25
        }
        if (hasContentDescription(root, CONTENT_DESC_STORIES)) {
            signals.add("content_desc_stories")
            confidence += 0.20
        }

        if (signals.isEmpty()) return null

        return DetectionCandidate(
            packageName = "com.instagram.android",
            surface = DetectionSurface.STORIES,
            confidence = confidence.coerceAtMost(1.0),
            reasonCodes = signals
        )
    }

    private fun detectExplore(root: AccessibilityNodeInfo): DetectionCandidate? {
        val signals = mutableListOf<String>()
        var confidence = 0.0

        if (NodeUtils.hasDescendantWithId(root, EXPLORE_ACTION_BAR)) {
            signals.add("explore_action_bar")
            confidence += 0.40
        }
        if (hasContentDescription(root, CONTENT_DESC_SEARCH)) {
            signals.add("content_desc_search")
            confidence += 0.20
        }
        if (hasContentDescription(root, CONTENT_DESC_EXPLORE)) {
            signals.add("content_desc_explore")
            confidence += 0.20
        }

        if (signals.isEmpty()) return null

        return DetectionCandidate(
            packageName = "com.instagram.android",
            surface = DetectionSurface.EXPLORE,
            confidence = confidence.coerceAtMost(1.0),
            reasonCodes = signals
        )
    }

    private fun detectFeed(root: AccessibilityNodeInfo): DetectionCandidate? {
        val signals = mutableListOf<String>()
        var confidence = 0.0

        val feedTab = NodeUtils.findNodeById(root, FEED_TAB)
        if (feedTab != null && NodeUtils.isSelected(feedTab)) {
            signals.add("feed_tab_selected")
            confidence += 0.35
        }
        if (NodeUtils.hasDescendantWithId(root, FEED_TAB)) {
            signals.add("feed_tab_present")
            confidence += 0.15
        }
        if (hasContentDescription(root, CONTENT_DESC_HOME)) {
            signals.add("content_desc_home")
            confidence += 0.15
        }

        if (signals.isEmpty()) return null

        return DetectionCandidate(
            packageName = "com.instagram.android",
            surface = DetectionSurface.MAIN_FEED,
            confidence = confidence.coerceAtMost(1.0),
            reasonCodes = signals,
            nodeReference = feedTab
        )
    }

    private fun detectDM(root: AccessibilityNodeInfo): DetectionCandidate? {
        val signals = mutableListOf<String>()
        var confidence = 0.0

        if (NodeUtils.hasDescendantWithId(root, DIRECT_TAB)) {
            signals.add("direct_tab")
            confidence += 0.30
        }
        if (NodeUtils.hasDescendantWithId(root, INBOX_LIST)) {
            signals.add("inbox_thread_list")
            confidence += 0.25
        }
        if (NodeUtils.hasDescendantWithId(root, DIRECT_THREAD_HEADER)) {
            signals.add("direct_thread_header")
            confidence += 0.25
        }
        if (hasContentDescription(root, CONTENT_DESC_DIRECT)) {
            signals.add("content_desc_direct")
            confidence += 0.15
        }

        if (signals.isEmpty()) return null

        return DetectionCandidate(
            packageName = "com.instagram.android",
            surface = DetectionSurface.DM,
            confidence = confidence.coerceAtMost(1.0),
            reasonCodes = signals
        )
    }

    private fun detectComments(root: AccessibilityNodeInfo): DetectionCandidate? {
        val signals = mutableListOf<String>()
        var confidence = 0.0

        if (NodeUtils.hasDescendantWithId(root, COMMENT_CONTAINER)) {
            signals.add("comment_thread_container")
            confidence += 0.35
        }
        if (NodeUtils.hasDescendantWithId(root, COMMENT_INPUT)) {
            signals.add("comment_input_visible")
            confidence += 0.25
        }
        if (hasContentDescription(root, CONTENT_DESC_COMMENTS)) {
            signals.add("content_desc_comments")
            confidence += 0.20
        }

        if (signals.isEmpty()) return null

        return DetectionCandidate(
            packageName = "com.instagram.android",
            surface = DetectionSurface.COMMENTS,
            confidence = confidence.coerceAtMost(1.0),
            reasonCodes = signals
        )
    }

    private fun findReelsBounds(root: AccessibilityNodeInfo): Rect? {
        val node = NodeUtils.findNodeById(root, REEL_ITEM_TOOLBAR)
            ?: NodeUtils.findNodeById(root, ROOT_CLIPS)
        return NodeUtils.getBounds(node)
    }

    private fun hasContentDescription(root: AccessibilityNodeInfo, desc: String): Boolean {
        return NodeUtils.findByText(root, desc) != null
    }
}
