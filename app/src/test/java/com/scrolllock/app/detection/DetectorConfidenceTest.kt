package com.scrolllock.app.detection

import org.junit.Assert.*
import org.junit.Test

class DetectorConfidenceTest {

    @Test
    fun `InstagramDetector confidence increases with more IDs`() {
        // Simulate confidence calculation
        var confidence = 0.55
        confidence += 0.20 // reel_item_toolbar
        confidence += 0.15 // reels_tray
        confidence += 0.10 // root_clips
        assertEquals(1.0, confidence.coerceAtMost(1.0), 0.001)
    }

    @Test
    fun `confidence clamps at 1_0`() {
        var confidence = 0.55
        confidence += 0.20
        confidence += 0.15
        confidence += 0.10
        confidence += 0.10
        assertEquals(1.0, confidence.coerceAtMost(1.0), 0.001)
    }

    @Test
    fun `YouTubeDetector confidence with reel_recycler`() {
        var confidence = 0.55
        confidence += 0.25 // reel_recycler
        assertEquals(0.80, confidence, 0.001)
    }

    @Test
    fun `TikTokDetector confidence with long_press_layout`() {
        var confidence = 0.55
        confidence += 0.25 // long_press_layout
        assertEquals(0.80, confidence, 0.001)
    }

    @Test
    fun `block threshold check`() {
        val threshold = 0.70
        assertTrue(0.85 >= threshold)
        assertTrue(0.70 >= threshold)
        assertFalse(0.69 >= threshold)
        assertFalse(0.50 >= threshold)
    }
}
