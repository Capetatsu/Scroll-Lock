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
        private const val REEL_VIEW_PAGER = "com.instagram.android:id/reels_view_pager"

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
        val signals = mutableListOf<DetectionSignal>()
        var confidence = 0.0

        checkIdSignal(root, REEL_ITEM_TOOLBAR, "reel_item_toolbar_container", 0.30)?.let {
            signals.add(it); confidence += it.confidence
        }
        checkIdSignal(root, REELS_TRAY, "reels_tray_container", 0.20)?.let {
            signals.add(it); confidence += it.confidence
        }
        checkIdSignal(root, ROOT_CLIPS, "root_clips_layout", 0.15)?.let {
            signals.add(it); confidence += it.confidence
        }
        checkIdSignal(root, CLIPS_AUTHOR, "clips_author_username", 0.10)?.let {
            signals.add(it); confidence += it.confidence
        }
        checkIdSignal(root, REEL_VIEW_PAGER, "reels_view_pager", 0.10)?.let {
            signals.add(it); confidence += it.confidence
        }
        checkContentDescSignal(root, CONTENT_DESC_REELS, "content_desc_reels", 0.10)?.let {
            signals.add(it); confidence += it.confidence
        }
        checkIdSignal(root, LIKE_COUNT, "like_count_visible", 0.05)?.let {
            signals.add(it); confidence += it.confidence
        }

        if (signals.isEmpty()) return null

        val bounds = findReelsBounds(root)
        return DetectionCandidate(
            packageName = "com.instagram.android",
            surface = DetectionSurface.REELS,
            confidence = confidence.coerceAtMost(1.0),
            bounds = bounds,
            reasonCodes = signals.map { it.description },
            nodeReference = NodeUtils.findNodeById(root, REEL_ITEM_TOOLBAR)
                ?: NodeUtils.findNodeById(root, ROOT_CLIPS),
            matchResult = if (confidence >= 0.70) DetectionResult.MATCH else DetectionResult.UNKNOWN,
            signals = signals
        )
    }

    private fun detectStories(root: AccessibilityNodeInfo): DetectionCandidate? {
        val signals = mutableListOf<DetectionSignal>()
        var confidence = 0.0

        checkIdSignal(root, STORIES_TRAY, "stories_tray_container", 0.35)?.let {
            signals.add(it); confidence += it.confidence
        }
        checkIdSignal(root, STORY_RING, "story_ring", 0.25)?.let {
            signals.add(it); confidence += it.confidence
        }
        checkContentDescSignal(root, CONTENT_DESC_STORIES, "content_desc_stories", 0.20)?.let {
            signals.add(it); confidence += it.confidence
        }

        if (signals.isEmpty()) return null

        return DetectionCandidate(
            packageName = "com.instagram.android",
            surface = DetectionSurface.STORIES,
            confidence = confidence.coerceAtMost(1.0),
            reasonCodes = signals.map { it.description },
            matchResult = if (confidence >= 0.70) DetectionResult.MATCH else DetectionResult.UNKNOWN,
            signals = signals
        )
    }

    private fun detectExplore(root: AccessibilityNodeInfo): DetectionCandidate? {
        val signals = mutableListOf<DetectionSignal>()
        var confidence = 0.0

        checkIdSignal(root, EXPLORE_ACTION_BAR, "explore_action_bar", 0.40)?.let {
            signals.add(it); confidence += it.confidence
        }
        checkContentDescSignal(root, CONTENT_DESC_SEARCH, "content_desc_search", 0.20)?.let {
            signals.add(it); confidence += it.confidence
        }
        checkContentDescSignal(root, CONTENT_DESC_EXPLORE, "content_desc_explore", 0.20)?.let {
            signals.add(it); confidence += it.confidence
        }

        if (signals.isEmpty()) return null

        return DetectionCandidate(
            packageName = "com.instagram.android",
            surface = DetectionSurface.EXPLORE,
            confidence = confidence.coerceAtMost(1.0),
            reasonCodes = signals.map { it.description },
            matchResult = if (confidence >= 0.70) DetectionResult.MATCH else DetectionResult.UNKNOWN,
            signals = signals
        )
    }

    private fun detectFeed(root: AccessibilityNodeInfo): DetectionCandidate? {
        val signals = mutableListOf<DetectionSignal>()
        var confidence = 0.0

        val feedTab = NodeUtils.findNodeById(root, FEED_TAB)
        if (feedTab != null) {
            val isSelected = NodeUtils.isSelected(feedTab)
            val isVisible = NodeUtils.isVisible(feedTab)
            if (isSelected) {
                signals.add(DetectionSignal(
                    type = SignalType.SELECTED_STATE,
                    identifier = "feed_tab_selected",
                    confidence = 0.35,
                    isVisible = isVisible,
                    isSelected = true,
                    description = "feed_tab_selected"
                ))
                confidence += 0.35
            }
            if (isVisible) {
                signals.add(DetectionSignal(
                    type = SignalType.RESOURCE_ID,
                    identifier = "feed_tab_present",
                    confidence = 0.15,
                    isVisible = true,
                    description = "feed_tab_present"
                ))
                confidence += 0.15
            }
        }
        checkContentDescSignal(root, CONTENT_DESC_HOME, "content_desc_home", 0.15)?.let {
            signals.add(it); confidence += it.confidence
        }

        if (signals.isEmpty()) return null

        return DetectionCandidate(
            packageName = "com.instagram.android",
            surface = DetectionSurface.MAIN_FEED,
            confidence = confidence.coerceAtMost(1.0),
            reasonCodes = signals.map { it.description },
            nodeReference = feedTab,
            matchResult = if (confidence >= 0.70) DetectionResult.MATCH else DetectionResult.UNKNOWN,
            signals = signals
        )
    }

    private fun detectDM(root: AccessibilityNodeInfo): DetectionCandidate? {
        val signals = mutableListOf<DetectionSignal>()
        var confidence = 0.0

        checkIdSignal(root, DIRECT_TAB, "direct_tab", 0.30)?.let {
            signals.add(it); confidence += it.confidence
        }
        checkIdSignal(root, INBOX_LIST, "inbox_thread_list", 0.25)?.let {
            signals.add(it); confidence += it.confidence
        }
        checkIdSignal(root, DIRECT_THREAD_HEADER, "direct_thread_header", 0.25)?.let {
            signals.add(it); confidence += it.confidence
        }
        checkContentDescSignal(root, CONTENT_DESC_DIRECT, "content_desc_direct", 0.15)?.let {
            signals.add(it); confidence += it.confidence
        }

        if (signals.isEmpty()) return null

        return DetectionCandidate(
            packageName = "com.instagram.android",
            surface = DetectionSurface.DM,
            confidence = confidence.coerceAtMost(1.0),
            reasonCodes = signals.map { it.description },
            matchResult = if (confidence >= 0.70) DetectionResult.MATCH else DetectionResult.UNKNOWN,
            signals = signals
        )
    }

    private fun detectComments(root: AccessibilityNodeInfo): DetectionCandidate? {
        val signals = mutableListOf<DetectionSignal>()
        var confidence = 0.0

        checkIdSignal(root, COMMENT_CONTAINER, "comment_thread_container", 0.35)?.let {
            signals.add(it); confidence += it.confidence
        }
        checkIdSignal(root, COMMENT_INPUT, "comment_input_visible", 0.25)?.let {
            signals.add(it); confidence += it.confidence
        }
        checkContentDescSignal(root, CONTENT_DESC_COMMENTS, "content_desc_comments", 0.20)?.let {
            signals.add(it); confidence += it.confidence
        }

        if (signals.isEmpty()) return null

        return DetectionCandidate(
            packageName = "com.instagram.android",
            surface = DetectionSurface.COMMENTS,
            confidence = confidence.coerceAtMost(1.0),
            reasonCodes = signals.map { it.description },
            matchResult = if (confidence >= 0.70) DetectionResult.MATCH else DetectionResult.UNKNOWN,
            signals = signals
        )
    }

    private fun checkIdSignal(
        root: AccessibilityNodeInfo,
        viewId: String,
        label: String,
        baseConfidence: Double
    ): DetectionSignal? {
        val node = NodeUtils.findNodeById(root, viewId) ?: return null
        val isVisible = NodeUtils.isVisible(node)
        val depth = getNodeDepth(root, node)

        if (!isVisible) return null

        val adjustedConfidence = baseConfidence * (1.0 - depth * 0.02).coerceAtLeast(0.5)

        return DetectionSignal(
            type = SignalType.RESOURCE_ID,
            identifier = viewId,
            confidence = adjustedConfidence,
            isVisible = true,
            hierarchyDepth = depth,
            description = label
        )
    }

    private fun checkContentDescSignal(
        root: AccessibilityNodeInfo,
        desc: String,
        label: String,
        baseConfidence: Double
    ): DetectionSignal? {
        val node = findByContentDescription(root, desc) ?: return null
        val isVisible = NodeUtils.isVisible(node)

        if (!isVisible) return null

        return DetectionSignal(
            type = SignalType.CONTENT_DESCRIPTION,
            identifier = desc,
            confidence = baseConfidence,
            isVisible = true,
            description = label
        )
    }

    private fun findByContentDescription(root: AccessibilityNodeInfo, desc: String): AccessibilityNodeInfo? {
        return findByContentDescriptionInternal(root, desc, 0)
    }

    private fun findByContentDescriptionInternal(
        node: AccessibilityNodeInfo,
        desc: String,
        depth: Int
    ): AccessibilityNodeInfo? {
        if (depth > 15) return null
        val nodeDesc = node.contentDescription?.toString() ?: ""
        if (nodeDesc.equals(desc, ignoreCase = true)) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findByContentDescriptionInternal(child, desc, depth + 1)
            if (result != null) return result
        }
        return null
    }

    private fun getNodeDepth(root: AccessibilityNodeInfo, target: AccessibilityNodeInfo): Int {
        return getNodeDepthInternal(root, target, 0)
    }

    private fun getNodeDepthInternal(
        node: AccessibilityNodeInfo,
        target: AccessibilityNodeInfo,
        depth: Int
    ): Int {
        if (node == target) return depth
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = getNodeDepthInternal(child, target, depth + 1)
            if (result >= 0) return result
        }
        return -1
    }

    private fun findReelsBounds(root: AccessibilityNodeInfo): Rect? {
        val node = NodeUtils.findNodeById(root, REEL_ITEM_TOOLBAR)
            ?: NodeUtils.findNodeById(root, ROOT_CLIPS)
        return NodeUtils.getBounds(node)
    }
}
