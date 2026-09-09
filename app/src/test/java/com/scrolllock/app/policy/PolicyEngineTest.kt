package com.scrolllock.app.policy

import com.scrolllock.app.data.model.ScheduleRule
import com.scrolllock.app.data.model.FeatureMask
import org.junit.Assert.*
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime

class PolicyEngineTest {

    @Test
    fun `isDayEnabled correctly evaluates bitmask`() {
        // Monday = 1, Tuesday = 2, ..., Sunday = 64
        val allDays = 0x7F
        assertTrue(ScheduleEngine.isDayEnabled(allDays, DayOfWeek.MONDAY))
        assertTrue(ScheduleEngine.isDayEnabled(allDays, DayOfWeek.SUNDAY))

        val weekdays = 31 // Mon-Fri
        assertTrue(ScheduleEngine.isDayEnabled(weekdays, DayOfWeek.MONDAY))
        assertTrue(ScheduleEngine.isDayEnabled(weekdays, DayOfWeek.FRIDAY))
        assertFalse(ScheduleEngine.isDayEnabled(weekdays, DayOfWeek.SATURDAY))
        assertFalse(ScheduleEngine.isDayEnabled(weekdays, DayOfWeek.SUNDAY))

        val weekend = 96 // Sat + Sun
        assertFalse(ScheduleEngine.isDayEnabled(weekend, DayOfWeek.MONDAY))
        assertTrue(ScheduleEngine.isDayEnabled(weekend, DayOfWeek.SATURDAY))
        assertTrue(ScheduleEngine.isDayEnabled(weekend, DayOfWeek.SUNDAY))
    }

    @Test
    fun `isTimeSlotActive checks time ranges`() {
        val morningSlot = "[{\"startHour\":6,\"startMinute\":0,\"endHour\":12,\"endMinute\":0}]"
        assertTrue(ScheduleEngine.isTimeSlotActive(morningSlot, LocalTime.of(8, 30)))
        assertFalse(ScheduleEngine.isTimeSlotActive(morningSlot, LocalTime.of(14, 0)))
    }

    @Test
    fun `FeatureMask hasFeature works correctly`() {
        assertTrue(FeatureMask.hasFeature(0xFF, FeatureMask.ANTI_SCROLL))
        assertTrue(FeatureMask.hasFeature(0xFF, FeatureMask.ANTI_REELS))
        assertFalse(FeatureMask.hasFeature(FeatureMask.ANTI_SCROLL, FeatureMask.ANTI_REELS))
        assertTrue(FeatureMask.hasFeature(FeatureMask.ANTI_SCROLL or FeatureMask.ANTI_REELS, FeatureMask.ANTI_REELS))
    }

    @Test
    fun `PolicyEngine blocks when all conditions met`() {
        val context = DecisionContext(
            packageName = "com.instagram.android",
            appEnabled = true,
            antiReelsEnabled = true,
            confidence = 0.85,
            detectedFeature = FeatureMask.ANTI_REELS
        )
        assertEquals(Decision.BLOCK, PolicyEngine.evaluate(context))
    }

    @Test
    fun `PolicyEngine allows when app not enabled`() {
        val context = DecisionContext(
            packageName = "com.instagram.android",
            appEnabled = false,
            antiReelsEnabled = true,
            confidence = 0.85,
            detectedFeature = FeatureMask.ANTI_REELS
        )
        assertEquals(Decision.ALLOW, PolicyEngine.evaluate(context))
    }

    @Test
    fun `PolicyEngine allows when confidence too low`() {
        val context = DecisionContext(
            packageName = "com.instagram.android",
            appEnabled = true,
            antiReelsEnabled = true,
            confidence = 0.50,
            detectedFeature = FeatureMask.ANTI_REELS
        )
        assertEquals(Decision.ALLOW, PolicyEngine.evaluate(context))
    }

    @Test
    fun `PolicyEngine blocks during cooldown`() {
        val context = DecisionContext(
            packageName = "com.instagram.android",
            appEnabled = true,
            cooldownActive = true
        )
        assertEquals(Decision.COOLDOWN_BLOCK, PolicyEngine.evaluate(context))
    }
}
