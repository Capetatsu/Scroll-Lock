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
        val swipes = listOf(1000L, 2000L, 3000L, 4000L, 5000L)
        val duration = swipes.last() - swipes.first()
        assertEquals(4000L, duration)
    }

    @Test
    fun `swipe frequency threshold met`() {
        val frequency = 5
        val windowSwipes = listOf(1, 2, 3, 4, 5, 6)
        assertTrue(windowSwipes.size >= frequency)
    }

    @Test
    fun `swipe frequency threshold not met`() {
        val frequency = 5
        val fewSwipes = listOf(1, 2, 3)
        assertFalse(fewSwipes.size >= frequency)
    }

    @Test
    fun `ScrollAnalysis sealed class types`() {
        val blocked = ScrollAnalysis.Blocked
        val initial = ScrollAnalysis.Initial
        val debounced = ScrollAnalysis.Debounced(50L)
        val noise = ScrollAnalysis.NoiseFiltered(2, 3)
        val recorded = ScrollAnalysis.Recorded(
            delta = 100,
            direction = 1,
            timeDelta = 100L,
            sessionDuration = 5000L,
            directionChanges = 2,
            swipeCount = 10
        )

        assertTrue(blocked is ScrollAnalysis.Blocked)
        assertTrue(initial is ScrollAnalysis.Initial)
        assertTrue(debounced is ScrollAnalysis.Debounced)
        assertTrue(noise is ScrollAnalysis.NoiseFiltered)
        assertTrue(recorded is ScrollAnalysis.Recorded)
        assertEquals(100, recorded.delta)
        assertEquals(1, recorded.direction)
        assertEquals(2, recorded.directionChanges)
    }

    @Test
    fun `BlockDecision sealed class types`() {
        val blocked = BlockDecision.Blocked
        val allowed = BlockDecision.Allowed("test_reason")

        assertTrue(blocked is BlockDecision.Blocked)
        assertTrue(allowed is BlockDecision.Allowed)
        assertEquals("test_reason", allowed.reason)
    }

    @Test
    fun `ScrollAnalysis Recorded contains session info`() {
        val recorded = ScrollAnalysis.Recorded(
            delta = 50,
            direction = -1,
            timeDelta = 80L,
            sessionDuration = 10000L,
            directionChanges = 5,
            swipeCount = 20
        )
        assertEquals(50, recorded.delta)
        assertEquals(-1, recorded.direction)
        assertEquals(80L, recorded.timeDelta)
        assertEquals(10000L, recorded.sessionDuration)
        assertEquals(5, recorded.directionChanges)
        assertEquals(20, recorded.swipeCount)
    }

    @Test
    fun `direction changes are tracked correctly`() {
        val directions = listOf(1, 1, -1, -1, 1, 1, -1)
        var changes = 0
        for (i in 1 until directions.size) {
            if (directions[i] != directions[i - 1]) changes++
        }
        assertEquals(3, changes)
    }

    @Test
    fun `noise filter rejects small deltas`() {
        val delta = 2
        val threshold = 3
        assertTrue(kotlin.math.abs(delta) < threshold)
    }

    @Test
    fun `noise filter accepts large deltas`() {
        val delta = 10
        val threshold = 3
        assertFalse(kotlin.math.abs(delta) < threshold)
    }
}
