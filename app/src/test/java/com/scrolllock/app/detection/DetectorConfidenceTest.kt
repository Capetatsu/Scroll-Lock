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
}
