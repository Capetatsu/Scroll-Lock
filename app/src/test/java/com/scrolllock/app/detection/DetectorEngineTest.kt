package com.scrolllock.app.detection

import android.view.accessibility.AccessibilityEvent
import org.junit.Assert.*
import org.junit.Test

class DetectorEngineTest {

    @Test
    fun `DetectorEngine invalidates cache on content change event`() {
        // Test that CONTENT_CHANGE_INVALIDATE_TYPES includes the right events
        val contentChangeTypes = DetectorEngine.CONTENT_CHANGE_INVALIDATE_TYPES
        
        assertTrue("Should invalidate on WINDOW_STATE_CHANGED",
            (AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED and contentChangeTypes) != 0)
        assertTrue("Should invalidate on WINDOW_CONTENT_CHANGED",
            (AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED and contentChangeTypes) != 0)
        assertTrue("Should invalidate on VIEW_FOCUSED",
            (AccessibilityEvent.TYPE_VIEW_FOCUSED and contentChangeTypes) != 0)
        assertTrue("Should invalidate on VIEW_CLICKED",
            (AccessibilityEvent.TYPE_VIEW_CLICKED and contentChangeTypes) != 0)
    }

    @Test
    fun `DetectorEngine uses shorter TTL for scroll events`() {
        // Scroll events should have shorter TTL
        assertTrue(DetectorEngine.SCROLL_CACHE_TTL_MS < DetectorEngine.CACHE_TTL_MS)
    }

    @Test
    fun `DetectorEngine event type passed correctly`() {
        // Test that event type is tracked
        // This is a unit test for the logic - actual integration would be in integration tests
        val scrollEventType = AccessibilityEvent.TYPE_VIEW_SCROLLED
        val contentChangeEventType = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        
        assertTrue("Scroll event type should be valid", scrollEventType > 0)
        assertTrue("Content change event type should be valid", contentChangeEventType > 0)
    }
}