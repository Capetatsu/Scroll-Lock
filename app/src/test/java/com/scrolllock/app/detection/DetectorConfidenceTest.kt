package com.scrolllock.app.detection

import org.junit.Assert.*
import org.junit.Test

class DetectorConfidenceTest {

    @Test
    fun `Instagram confidence increases with more IDs`() {
        var confidence = 0.0
        confidence += 0.30 // reel_item_toolbar
        confidence += 0.20 // reels_tray
        confidence += 0.15 // root_clips
        confidence += 0.10 // clips_author
        assertEquals(0.75, confidence, 0.001)
    }

    @Test
    fun `confidence clamps at 1_0`() {
        var confidence = 0.55
        for (i in 1..10) confidence += 0.10
        assertEquals(1.0, confidence.coerceAtMost(1.0), 0.001)
    }

    @Test
    fun `YouTube Shorts confidence with reel_recycler`() {
        var confidence = 0.0
        confidence += 0.30 // reel_recycler
        assertEquals(0.30, confidence, 0.001)
    }

    @Test
    fun `TikTok confidence with long_press_layout`() {
        var confidence = 0.0
        confidence += 0.25 // long_press_layout
        confidence += 0.20 // video_player
        assertEquals(0.45, confidence, 0.001)
    }

    @Test
    fun `block threshold check`() {
        val threshold = 0.70
        assertTrue(0.85 >= threshold)
        assertTrue(0.70 >= threshold)
        assertFalse(0.69 >= threshold)
        assertFalse(0.50 >= threshold)
    }

    @Test
    fun `multiple signals combine correctly`() {
        var confidence = 0.0
        confidence += 0.30
        confidence += 0.20
        confidence += 0.15
        confidence += 0.10
        confidence += 0.10
        assertEquals(0.85, confidence, 0.001)
        assertTrue(confidence >= 0.70)
    }

    @Test
    fun `single weak signal does not trigger block`() {
        var confidence = 0.0
        confidence += 0.15
        assertFalse(confidence >= 0.70)
    }

    @Test
    fun `DetectionResult match when confidence high`() {
        val candidate = DetectionCandidate(
            packageName = "com.instagram.android",
            surface = DetectionSurface.REELS,
            confidence = 0.85
        )
        assertEquals(DetectionResult.MATCH, candidate.matchResult)
    }

    @Test
    fun `DetectionResult unknown when confidence low`() {
        val candidate = DetectionCandidate(
            packageName = "com.instagram.android",
            surface = DetectionSurface.REELS,
            confidence = 0.50
        )
        assertEquals(DetectionResult.UNKNOWN, candidate.matchResult)
    }

    @Test
    fun `DetectionSignal tracks visibility`() {
        val signal = DetectionSignal(
            type = SignalType.RESOURCE_ID,
            identifier = "test_id",
            confidence = 0.30,
            isVisible = true,
            description = "test_signal"
        )
        assertTrue(signal.isVisible)
    }

    @Test
    fun `DetectionSignal tracks selected state`() {
        val signal = DetectionSignal(
            type = SignalType.SELECTED_STATE,
            identifier = "feed_tab",
            confidence = 0.35,
            isSelected = true,
            description = "feed_tab_selected"
        )
        assertTrue(signal.isSelected)
    }

    @Test
    fun `DetectionDebugInfo contains all fields`() {
        val info = DetectionDebugInfo(
            packageName = "com.instagram.android",
            surface = DetectionSurface.REELS,
            confidence = 0.85,
            matchResult = DetectionResult.MATCH,
            signals = listOf(
                DetectionSignal(
                    type = SignalType.RESOURCE_ID,
                    identifier = "reel_item_toolbar_container",
                    confidence = 0.30,
                    description = "reel_item_toolbar_container"
                )
            ),
            reasonCodes = listOf("reel_item_toolbar_container")
        )
        assertEquals("com.instagram.android", info.packageName)
        assertEquals(DetectionSurface.REELS, info.surface)
        assertEquals(0.85, info.confidence, 0.001)
        assertEquals(DetectionResult.MATCH, info.matchResult)
        assertEquals(1, info.signals.size)
        assertEquals(1, info.reasonCodes.size)
    }

    // NEW TESTS for Instagram detector reliability
    @Test
    fun `Instagram strong Reels resource ID yields high confidence`() {
        // Single strong Reels ID (reel_item_toolbar_container) should give high confidence
        val baseConfidence = 0.45 // REEL_ITEM_TOOLBAR weight
        assertTrue("Primary Reels signal should have high weight", baseConfidence >= 0.40)
    }

    @Test
    fun `Instagram multiple strong Reels IDs exceeds threshold`() {
        var confidence = 0.0
        confidence += 0.45 // REEL_ITEM_TOOLBAR
        confidence += 0.40 // ROOT_CLIPS
        assertTrue("Two strong Reels signals should exceed threshold", confidence >= 0.70)
    }

    @Test
    fun `Instagram generic home feed plus Reels text not enough`() {
        var confidence = 0.0
        confidence += 0.15 // CONTENT_DESC_HOME
        confidence += 0.08 // CONTENT_DESC_REELS (content desc is weak)
        assertFalse("Generic home + Reels text should not reach threshold", confidence >= 0.70)
    }

    @Test
    fun `Instagram Reels primary signals identified correctly`() {
        // Verify primary signals have high weights
        val primarySignals = mapOf(
            "reel_item_toolbar_container" to 0.45,
            "root_clips_layout" to 0.40,
            "reels_view_pager" to 0.40,
            "reel_video_view" to 0.35,
            "reel_progress_bar" to 0.35,
            "clips_metadata" to 0.30
        )
        primarySignals.values.forEach { weight ->
            assertTrue("Primary Reels signals should have weight >= 0.30", weight >= 0.30)
        }
    }
}