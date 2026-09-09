package com.scrolllock.app.detection.tiktok

import android.view.accessibility.AccessibilityNodeInfo
import com.scrolllock.app.detection.*

class TikTokDetector : ContentDetector {
    override val packageNames = setOf(
        "com.zhiliaoapp.musically",
        "com.ss.android.ugc.trill"
    )

    companion object {
        private const val LONG_PRESS_LAYOUT = "com.zhiliaoapp.musically:id/long_press_layout"
        private const val FEED_CONTAINER = "com.zhiliaoapp.musically:id/d55"
        private const val VIDEO_PLAYER = "com.zhiliaoapp.musically:id/a3b"
        private const val LIKE_BUTTON = "com.zhiliaoapp.musically:id/like_button"
        private const val COMMENT_BUTTON = "com.zhiliaoapp.musically:id/comment_button"
        private const val SHARE_BUTTON = "com.zhiliaoapp.musically:id/share_button"
        private const val PROFILE_PICTURE = "com.zhiliaoapp.musically:id/profile_picture"
        private const val MUSIC_SCORE = "com.zhiliaoapp.musically:id/music_score"

        private const val CONTENT_DESC_FYP = "For You"
        private const val CONTENT_DESC_HOME = "Home"
    }

    override fun classify(root: AccessibilityNodeInfo): List<DetectionCandidate> {
        val candidates = mutableListOf<DetectionCandidate>()

        detectVideoFeed(root)?.let { candidates.add(it) }

        return candidates
    }

    private fun detectVideoFeed(root: AccessibilityNodeInfo): DetectionCandidate? {
        val signals = mutableListOf<String>()
        var confidence = 0.0

        if (NodeUtils.hasDescendantWithId(root, LONG_PRESS_LAYOUT)) {
            signals.add("long_press_layout")
            confidence += 0.25
        }
        if (NodeUtils.hasDescendantWithId(root, VIDEO_PLAYER)) {
            signals.add("video_player")
            confidence += 0.20
        }
        if (NodeUtils.hasDescendantWithId(root, LIKE_BUTTON)) {
            signals.add("like_button")
            confidence += 0.10
        }
        if (NodeUtils.hasDescendantWithId(root, COMMENT_BUTTON)) {
            signals.add("comment_button")
            confidence += 0.10
        }
        if (NodeUtils.hasDescendantWithId(root, SHARE_BUTTON)) {
            signals.add("share_button")
            confidence += 0.10
        }
        if (NodeUtils.hasDescendantWithId(root, MUSIC_SCORE)) {
            signals.add("music_score")
            confidence += 0.10
        }
        if (NodeUtils.findByText(root, CONTENT_DESC_FYP) != null) {
            signals.add("fyp_text")
            confidence += 0.15
        }

        if (signals.isEmpty()) return null

        return DetectionCandidate(
            packageName = "com.zhiliaoapp.musically",
            surface = DetectionSurface.TIKTOK_VIDEO,
            confidence = confidence.coerceAtMost(1.0),
            reasonCodes = signals,
            nodeReference = NodeUtils.findNodeById(root, LONG_PRESS_LAYOUT)
                ?: NodeUtils.findNodeById(root, VIDEO_PLAYER)
        )
    }
}
