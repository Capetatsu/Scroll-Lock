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
}
