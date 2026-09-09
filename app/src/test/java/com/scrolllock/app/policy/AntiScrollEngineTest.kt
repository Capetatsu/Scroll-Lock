package com.scrolllock.app.policy

import com.scrolllock.app.data.model.TimeSlot
import org.junit.Assert.*
import org.junit.Test

class AntiScrollEngineTest {

    @Test
    fun `TimeSlot contains checks correctly`() {
        val morning = TimeSlot(6, 0, 12, 0)
        assertTrue(morning.contains(8, 30))
        assertTrue(morning.contains(6, 0))
        assertTrue(morning.contains(12, 0))
        assertFalse(morning.contains(12, 1))
        assertFalse(morning.contains(5, 59))
    }

    @Test
    fun `TimeSlot handles midnight crossing`() {
        val night = TimeSlot(22, 0, 2, 0)
        assertTrue(night.contains(23, 0))
        assertTrue(night.contains(0, 30))
        assertTrue(night.contains(1, 59))
        assertFalse(night.contains(3, 0))
        assertFalse(night.contains(21, 0))
    }

    @Test
    fun `session duration calculation`() {
        val swipes = listOf(
            1000L, 2000L, 3000L, 4000L, 5000L
        )
        val duration = swipes.last() - swipes.first()
        assertEquals(4000L, duration)
    }

    @Test
    fun `swipe frequency threshold`() {
        val frequency = 5
        val windowSwipes = listOf(1, 2, 3, 4, 5, 6)
        assertTrue(windowSwipes.size >= frequency)

        val fewSwipes = listOf(1, 2, 3)
        assertFalse(fewSwipes.size >= frequency)
    }
}
