package com.scrolllock.app.detection.instagram

import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.scrolllock.app.detection.*

class InstagramDetector : ContentDetector {
    override val packageNames = setOf("com.instagram.android")

    companion object {
        private const val TAG = "InstagramDetector"

        // Instagram-internal resource IDs — NOT a stable API, will drift over time.
        // Keep this list as a fast-path exact-match short-circuit; substring match is primary.
        private val KNOWN_REELS_IDS = setOf(
            "com.instagram.android:id/reel_item_toolbar_container",
            "com.instagram.android:id/root_clips_layout",
            "com.instagram.android:id/reels_view_pager",
            "com.instagram.android:id/reel_video_view",
            "com.instagram.android:id/reel_progress_bar",
            "com.instagram.android:id/clips_metadata",
            "com.instagram.android:id/reels_recycler_view",
            "com.instagram.android:id/clips_recycler_view",
            "com.instagram.android:id/reels_tray_container",
            "com.instagram.android:id/reel_share_button",
            "com.instagram.android:id/reel_caption",
            "com.instagram.android:id/clips_author_username",
            "com.instagram.android:id/like_count",
            "com.instagram.android:id/clips_recycler",
            "com.instagram.android:id/reel_item_toolbar_container",
            "com.instagram.android:id/reel_item_container",
            "com.instagram.android:id/clips_root_view",
            "com.instagram.android:id/reel_root_view",
            "com.instagram.android:id/clips_player_view",
            "com.instagram.android:id/reel_player_view",
            "com.instagram.android:id/clips_tray_container",
            "com.instagram.android:id/reels_container",
            "com.instagram.android:id/clips_container",
            "com.instagram.android:id/reel_container"
        )

        // Substring keywords that indicate Reels/Clips content (primary match)
        private val REELS_KEYWORDS = listOf("reel", "clip", "clips")

        // Secondary detectors keyword mappings
        private val STORIES_KEYWORDS = listOf("story", "stories")
        private val EXPLORE_KEYWORDS = listOf("explore", "search")
        private val FEED_KEYWORDS = listOf("feed", "home")
        private val DM_KEYWORDS = listOf("direct", "thread", "inbox")
        private val COMMENT_KEYWORDS = listOf("comment")

        // Navigation tab IDs (exact match)
        private const val DIRECT_TAB = "com.instagram.android:id/direct_tab"
        private const val FEED_TAB = "com.instagram.android:id/feed_tab"
        private const val EXPLORE_ACTION_BAR = "com.instagram.android:id/explore_action_bar"
        private const val NAVIGATION_TAB_BAR = "com.instagram.android:id/tab_bar"

        // Engagement IDs
        private const val COMMENT_CONTAINER = "com.instagram.android:id/comment_thread_container"
        private const val COMMENT_INPUT = "com.instagram.android:id/layout_comment_thread_edittext"
        private const val INBOX_LIST = "com.instagram.android:id/inbox_refreshable_thread_list_recyclerview"
        private const val DIRECT_THREAD_HEADER = "com.instagram.android:id/direct_thread_header"

        // Stories
        private const val STORIES_TRAY = "com.instagram.android:id/stories_tray_container"
        private const val STORY_RING = "com.instagram.android:id/story_ring"

        // Content descriptions (localized)
        private const val CONTENT_DESC_REELS = "Reels"
        private const val CONTENT_DESC_EXPLORE = "Explore"
        private const val CONTENT_DESC_STORIES = "Stories"
        private const val CONTENT_DESC_DIRECT = "Direct"
        private const val CONTENT_DESC_COMMENTS = "Comments"
        private const val CONTENT_DESC_HOME = "Home"
        private const val CONTENT_DESC_SEARCH = "Search"
        private const val CONTENT_DESC_CREATE = "Create"
        private const val CONTENT_DESC_LIKES = "Likes"
        private const val CONTENT_DESC_SHARE = "Share"
        private const val CONTENT_DESC_REEL = "Reel"
    }

    override fun classify(root: AccessibilityNodeInfo): List<DetectionCandidate> {
        val candidates = mutableListOf<DetectionCandidate>()

        // Dump accessibility tree for debugging when in debug mode
        if (isDebugLoggingEnabled()) {
            dumpAccessibilityTree(root)
        }

        detectReels(root)?.let { candidates.add(it) }
        detectStories(root)?.let { candidates.add(it) }
        detectExplore(root)?.let { candidates.add(it) }
        detectFeed(root)?.let { candidates.add(it) }
        detectDM(root)?.let { candidates.add(it) }
        detectComments(root)?.let { candidates.add(it) }

        // Log if no detectors matched (for debugging ID drift)
        if (candidates.isEmpty() && isDebugLoggingEnabled()) {
            val allIds = collectAllResourceIds(root)
            Log.w(TAG, "InstagramDetector: no surface matched. Known IDs in tree: ${allIds.take(50).joinToString(", ")}")
        }

        return candidates
    }

    private fun detectReels(root: AccessibilityNodeInfo): DetectionCandidate? {
        val signals = mutableListOf<DetectionSignal>()
        var confidence = 0.0

        // PRIMARY: Substring match on "reel"/"clip" keywords (primary detector)
        // This runs FIRST as the primary detector, not a fallback
        val substringSignals = collectSubstringMatches(root, REELS_KEYWORDS, SignalType.RESOURCE_ID)
        var foundSubstringMatch = false
        for (signal in substringSignals) {
            // Avoid duplicate signals for same node
            if (!signals.any { it.identifier == signal.identifier }) {
                signals.add(signal)
                confidence += signal.confidence
                foundSubstringMatch = true
            }
        }

        // SECONDARY: Exact match fast-path (known IDs) - runs as confirmation/boost
        var foundExactReelsId = false
        for (knownId in KNOWN_REELS_IDS) {
            checkIdSignal(root, knownId, knownId, getReelsIdWeight(knownId))?.let {
                signals.add(it)
                confidence += it.confidence
                foundExactReelsId = true
            }
        }

        // If both substring and exact match found, boost confidence
        if (foundSubstringMatch && foundExactReelsId) {
            confidence += 0.15
        }

        // TERTIARY: Structural fallback - full-screen video with vertical pager
        if (confidence < 0.70) {
            val structuralSignal = detectReelsStructurally(root)
            if (structuralSignal != null) {
                signals.add(structuralSignal)
                confidence += structuralSignal.confidence
            }
        }

        // Author and engagement (supporting signals)
        checkIdSignal(root, "com.instagram.android:id/clips_author_username", "clips_author_username", 0.10)?.let { signals.add(it); confidence += it.confidence }
        checkIdSignal(root, "com.instagram.android:id/like_count", "like_count_visible", 0.08)?.let { signals.add(it); confidence += it.confidence }

        // Content description (low weight - text can be unreliable)
        checkContentDescSignal(root, "Reels", "content_desc_reels", 0.08)?.let { signals.add(it); confidence += it.confidence }
        checkContentDescSignal(root, "Reel", "content_desc_reel", 0.08)?.let { signals.add(it); confidence += it.confidence }

        if (signals.isEmpty()) return null

        val bounds = findReelsBounds(root)
        return DetectionCandidate(
            packageName = "com.instagram.android",
            surface = DetectionSurface.REELS,
            confidence = confidence.coerceAtMost(1.0),
            bounds = bounds,
            reasonCodes = signals.map { it.description },
            nodeReference = findPrimaryReelsNode(root),
            matchResult = if (confidence >= 0.70) DetectionResult.MATCH else DetectionResult.UNKNOWN,
            signals = signals
        )
    }

    private fun detectStories(root: AccessibilityNodeInfo): DetectionCandidate? {
        val signals = mutableListOf<DetectionSignal>()
        var confidence = 0.0

        // Substring match for "story"/"stories" (primary)
        val substringSignals = collectSubstringMatches(root, STORIES_KEYWORDS, SignalType.RESOURCE_ID)
        for (signal in substringSignals) {
            if (!signals.any { it.identifier == signal.identifier }) {
                signals.add(signal)
                confidence += signal.confidence
            }
        }

        // Exact match for known IDs (confirmation/boost)
        checkIdSignal(root, "com.instagram.android:id/stories_tray_container", "stories_tray_container", 0.35)?.let { signals.add(it); confidence += it.confidence }
        checkIdSignal(root, "com.instagram.android:id/story_ring", "story_ring", 0.25)?.let { signals.add(it); confidence += it.confidence }

        checkContentDescSignal(root, "Stories", "content_desc_stories", 0.20)?.let { signals.add(it); confidence += it.confidence }

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

        // Substring match for "explore"/"search" (primary)
        val substringSignals = collectSubstringMatches(root, EXPLORE_KEYWORDS, SignalType.RESOURCE_ID)
        for (signal in substringSignals) {
            if (!signals.any { it.identifier == signal.identifier }) {
                signals.add(signal)
                confidence += signal.confidence
            }
        }

        // Exact match for known IDs
        checkIdSignal(root, "com.instagram.android:id/explore_action_bar", "explore_action_bar", 0.40)?.let { signals.add(it); confidence += it.confidence }

        checkContentDescSignal(root, "Search", "content_desc_search", 0.20)?.let { signals.add(it); confidence += it.confidence }
        checkContentDescSignal(root, "Explore", "content_desc_explore", 0.20)?.let { signals.add(it); confidence += it.confidence }

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

        val feedTab = NodeUtils.findNodeById(root, "com.instagram.android:id/feed_tab")
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

        // Substring match for "feed"/"home" (primary)
        val substringSignals = collectSubstringMatches(root, FEED_KEYWORDS, SignalType.RESOURCE_ID)
        for (signal in substringSignals) {
            if (!signals.any { it.identifier == signal.identifier }) {
                signals.add(signal)
                confidence += signal.confidence
            }
        }

        checkContentDescSignal(root, "Home", "content_desc_home", 0.15)?.let { signals.add(it); confidence += it.confidence }

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

        // Exact match for known IDs
        checkIdSignal(root, "com.instagram.android:id/direct_tab", "direct_tab", 0.30)?.let { signals.add(it); confidence += it.confidence }
        checkIdSignal(root, "com.instagram.android:id/inbox_refreshable_thread_list_recyclerview", "inbox_thread_list", 0.25)?.let { signals.add(it); confidence += it.confidence }
        checkIdSignal(root, "com.instagram.android:id/direct_thread_header", "direct_thread_header", 0.25)?.let { signals.add(it); confidence += it.confidence }

        // Substring match for "direct"/"thread"/"inbox" (primary)
        val substringSignals = collectSubstringMatches(root, DM_KEYWORDS, SignalType.RESOURCE_ID)
        for (signal in substringSignals) {
            if (!signals.any { it.identifier == signal.identifier }) {
                signals.add(signal)
                confidence += signal.confidence
            }
        }

        checkContentDescSignal(root, "Direct", "content_desc_direct", 0.15)?.let { signals.add(it); confidence += it.confidence }

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

        // Exact match for known IDs
        checkIdSignal(root, "com.instagram.android:id/comment_thread_container", "comment_thread_container", 0.35)?.let { signals.add(it); confidence += it.confidence }
        checkIdSignal(root, "com.instagram.android:id/layout_comment_thread_edittext", "comment_input_visible", 0.25)?.let { signals.add(it); confidence += it.confidence }

        // Substring match for "comment" (primary)
        val substringSignals = collectSubstringMatches(root, COMMENT_KEYWORDS, SignalType.RESOURCE_ID)
        for (signal in substringSignals) {
            if (!signals.any { it.identifier == signal.identifier }) {
                signals.add(signal)
                confidence += signal.confidence
            }
        }

        checkContentDescSignal(root, "Comments", "content_desc_comments", 0.20)?.let { signals.add(it); confidence += it.confidence }

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

    // ===== Helper Methods =====

    private fun getReelsIdWeight(id: String): Double {
        return when {
            id.contains("reel_item_toolbar") || id.contains("reel_item_container") -> 0.45
            id.contains("root_clips") || id.contains("clips_root") || id.contains("reel_root") || id.contains("clips_root_view") -> 0.40
            id.contains("reels_view_pager") || id.contains("reel_view_pager") -> 0.40
            id.contains("reel_video_view") || id.contains("reel_player") || id.contains("clips_player") || id.contains("clips_player_view") -> 0.35
            id.contains("reel_progress_bar") -> 0.35
            id.contains("clips_metadata") -> 0.30
            id.contains("reels_recycler_view") || id.contains("clips_recycler_view") || id.contains("clips_recycler") || id.contains("reels_recycler") -> 0.25
            id.contains("reels_tray_container") || id.contains("clips_tray_container") -> 0.20
            id.contains("reel_share_button") -> 0.15
            id.contains("reel_caption") -> 0.15
            id.contains("clips_author_username") -> 0.10
            id.contains("like_count") -> 0.08
            else -> 0.05
        }
    }

    private fun collectSubstringMatches(
        root: AccessibilityNodeInfo,
        keywords: List<String>,
        type: SignalType
    ): List<DetectionSignal> {
        val signals = mutableListOf<DetectionSignal>()
        val visited = mutableSetOf<String>()

        fun traverse(node: AccessibilityNodeInfo, depth: Int) {
            if (depth > 20) return
            val id = node.viewIdResourceName
            if (id != null && !visited.contains(id)) {
                visited.add(id)
                val lowerId = id.lowercase()
                for (keyword in keywords) {
                    if (lowerId.contains(keyword)) {
                        val isVisible = NodeUtils.isVisible(node)
                        if (!isVisible) break
                        val adjustedConfidence = 0.25 * (1.0 - depth * 0.02).coerceAtLeast(0.4)
                        signals.add(DetectionSignal(
                            type = type,
                            identifier = id,
                            confidence = adjustedConfidence,
                            isVisible = true,
                            hierarchyDepth = depth,
                            description = "substring:$id"
                        ))
                        break
                    }
                }
            }
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                traverse(child, depth + 1)
            }
        }

        traverse(root, 0)
        return signals
    }

    private fun detectReelsStructurally(root: AccessibilityNodeInfo): DetectionSignal? {
        // Look for a full-screen (or near full-screen) node that:
        // 1. Is a video/surface view or fills most of the screen
        // 2. Has a vertically scrollable ancestor (ViewPager2/RecyclerView)
        // 3. Has an action rail (3-5 small clickable icons stacked vertically on right edge)

        val windowBounds = android.graphics.Rect()
        root.getBoundsInScreen(windowBounds)
        val windowHeight = windowBounds.height()
        val windowWidth = windowBounds.width()

        var candidateNode: AccessibilityNodeInfo? = null
        var maxArea = 0

        fun findLargeVideoNode(node: AccessibilityNodeInfo) {
            val bounds = android.graphics.Rect()
            node.getBoundsInScreen(bounds)
            val area = bounds.width() * bounds.height()
            val coverage = area.toFloat() / (windowHeight * windowWidth)

            // Node covers significant portion of screen
            if (coverage > 0.5 && area > maxArea) {
                val className = (node.className?.toString() ?: "").lowercase()
                val isVideoLike = className.contains("video") || className.contains("surface") ||
                        className.contains("player") || className.contains("texture") ||
                        className.contains("media")

                if (isVideoLike || coverage > 0.7) {
                    // Check for vertically scrollable ancestor
                    var parent = node.parent
                    var hasVerticalPager = false
                    var depth = 0
                    while (parent != null && depth < 10) {
                        val pClass = (parent.className?.toString() ?: "").lowercase()
                        val isVerticalScroll = parent.isScrollable &&
                                (pClass.contains("viewpager") || pClass.contains("recyclerview") ||
                                 pClass.contains("pager"))
                        if (isVerticalScroll) {
                            hasVerticalPager = true
                            break
                        }
                        parent = parent.parent
                        depth++
                    }

                    // Check for action rail on right side (like/comment/share/save buttons)
                    var hasActionRail = false
                    fun checkActionRail(n: AccessibilityNodeInfo) {
                        if (n.childCount >= 3 && n.childCount <= 6) {
                            var clickableCount = 0
                            for (i in 0 until n.childCount) {
                                val child = n.getChild(i) ?: continue
                                if (child.isClickable || child.isFocusable) clickableCount++
                            }
                            if (clickableCount >= 3) {
                                val bounds = android.graphics.Rect()
                                n.getBoundsInScreen(bounds)
                                // Action rail typically on right edge
                                if (bounds.left > windowWidth * 0.7) {
                                    hasActionRail = true
                                }
                            }
                        }
                    }

                    // Check siblings and ancestors for action rail
                    var p = node.parent
                    while (p != null) {
                        checkActionRail(p)
                        p = p.parent
                    }

                    if (hasVerticalPager || hasActionRail || isVideoLike) {
                        candidateNode = node
                        maxArea = area
                    }
                }
            }

            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                findLargeVideoNode(child)
            }
        }

        findLargeVideoNode(root)

        if (candidateNode != null) {
            return DetectionSignal(
                type = SignalType.HIERARCHY,
                identifier = "structural:reels_video_pager",
                confidence = 0.40,
                isVisible = true,
                hierarchyDepth = 0,
                description = "structural:fullscreen_video_with_vertical_pager"
            )
        }
        return null
    }

    private fun findPrimaryReelsNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        for (id in KNOWN_REELS_IDS) {
            val node = NodeUtils.findNodeById(root, id)
            if (node != null && NodeUtils.isVisible(node)) return node
        }
        // Fallback: find any node with reel/clip in ID
        var found: AccessibilityNodeInfo? = null
        fun traverse(node: AccessibilityNodeInfo) {
            if (found != null) return
            val id = node.viewIdResourceName
            if (id != null) {
                val lower = id.lowercase()
                if (lower.contains("reel") || lower.contains("clip")) {
                    if (NodeUtils.isVisible(node)) {
                        found = node
                        return
                    }
                }
            }
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                traverse(child)
            }
        }
        traverse(root)
        return found
    }

    private fun dumpAccessibilityTree(root: AccessibilityNodeInfo) {
        fun traverse(node: AccessibilityNodeInfo, depth: Int) {
            if (depth > 20) return
            val id = node.viewIdResourceName
            if (id != null) {
                val prefix = "  ".repeat(depth)
                val className = node.className?.toString() ?: "unknown"
                val desc = node.contentDescription?.toString() ?: ""
                val text = node.text?.toString() ?: ""
                Log.d(TAG, "$prefix$id | class=$className | desc='$desc' | text='$text'")
            }
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                traverse(child, depth + 1)
            }
        }
        traverse(root, 0)
    }

    private fun collectAllResourceIds(root: AccessibilityNodeInfo): List<String> {
        val ids = mutableListOf<String>()
        fun traverse(node: AccessibilityNodeInfo) {
            val id = node.viewIdResourceName
            if (id != null) ids.add(id)
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                traverse(child)
            }
        }
        traverse(root)
        return ids.distinct()
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
        return NodeUtils.getBounds(findPrimaryReelsNode(root))
    }

    private fun isDebugLoggingEnabled(): Boolean {
        // Check if debug mode is enabled via a static flag or preferences
        // For now, return true to enable logging during development
        return true
    }
}