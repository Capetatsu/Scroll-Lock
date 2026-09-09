package com.scrolllock.app.policy

import com.scrolllock.app.data.model.FeatureMask
import com.scrolllock.app.data.model.ScheduleRule
import com.scrolllock.app.data.model.TimeSlot
import org.junit.Assert.*
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime

class PolicyEngineTest {

    @Test
    fun `isDayEnabled correctly evaluates bitmask`() {
        val allDays = 0x7F
        assertTrue(ScheduleEngine.isDayEnabled(allDays, DayOfWeek.MONDAY))
        assertTrue(ScheduleEngine.isDayEnabled(allDays, DayOfWeek.SUNDAY))

        val weekdays = 31
        assertTrue(ScheduleEngine.isDayEnabled(weekdays, DayOfWeek.MONDAY))
        assertTrue(ScheduleEngine.isDayEnabled(weekdays, DayOfWeek.FRIDAY))
        assertFalse(ScheduleEngine.isDayEnabled(weekdays, DayOfWeek.SATURDAY))
        assertFalse(ScheduleEngine.isDayEnabled(weekdays, DayOfWeek.SUNDAY))

        val weekend = 96
        assertFalse(ScheduleEngine.isDayEnabled(weekend, DayOfWeek.MONDAY))
        assertTrue(ScheduleEngine.isDayEnabled(weekend, DayOfWeek.SATURDAY))
        assertTrue(ScheduleEngine.isDayEnabled(weekend, DayOfWeek.SUNDAY))
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
        val context = PolicyEngine.buildContext(
            packageName = "com.instagram.android",
            appEnabled = true,
            antiReelsEnabled = true,
            antiScrollEnabled = false,
            browserBlockEnabled = false,
            scheduleActive = true,
            cooldownActive = false,
            blockingSessionActive = false,
            confidence = 0.85,
            detectedFeature = FeatureMask.ANTI_REELS,
            surfaceName = "REELS"
        )
        val decision = PolicyEngine.evaluate(context)
        assertEquals(PolicyDecision.Action.BLOCK, decision.action)
    }

    @Test
    fun `PolicyEngine allows when app not enabled`() {
        val context = PolicyEngine.buildContext(
            packageName = "com.instagram.android",
            appEnabled = false,
            antiReelsEnabled = true,
            antiScrollEnabled = false,
            browserBlockEnabled = false,
            scheduleActive = true,
            cooldownActive = false,
            blockingSessionActive = false,
            confidence = 0.85,
            detectedFeature = FeatureMask.ANTI_REELS,
            surfaceName = "REELS"
        )
        val decision = PolicyEngine.evaluate(context)
        assertEquals(PolicyDecision.Action.ALLOW, decision.action)
    }

    @Test
    fun `PolicyEngine allows when confidence too low`() {
        val context = PolicyEngine.buildContext(
            packageName = "com.instagram.android",
            appEnabled = true,
            antiReelsEnabled = true,
            antiScrollEnabled = false,
            browserBlockEnabled = false,
            scheduleActive = true,
            cooldownActive = false,
            blockingSessionActive = false,
            confidence = 0.50,
            detectedFeature = FeatureMask.ANTI_REELS,
            surfaceName = "REELS"
        )
        val decision = PolicyEngine.evaluate(context)
        assertEquals(PolicyDecision.Action.ALLOW, decision.action)
    }

    @Test
    fun `PolicyEngine blocks during cooldown`() {
        val context = DecisionContext(
            packageName = "com.instagram.android",
            appEnabled = true,
            cooldownActive = true
        )
        val decision = PolicyEngine.evaluate(context)
        assertEquals(PolicyDecision.Action.BLOCK, decision.action)
    }

    @Test
    fun `PolicyEngine redirects Instagram feed when configured`() {
        val context = PolicyEngine.buildContext(
            packageName = "com.instagram.android",
            appEnabled = true,
            antiReelsEnabled = true,
            antiScrollEnabled = false,
            browserBlockEnabled = false,
            scheduleActive = true,
            cooldownActive = false,
            blockingSessionActive = false,
            confidence = 0.80,
            detectedFeature = FeatureMask.ANTI_REELS,
            surfaceName = "MAIN_FEED",
            instagramSettings = com.scrolllock.app.data.model.InstagramAntiReelsSettings(
                blockMainFeed = true
            )
        )
        val decision = PolicyEngine.evaluate(context)
        assertEquals(PolicyDecision.Action.REDIRECT, decision.action)
    }

    @Test
    fun `PolicyEngine allows Instagram DM when configured`() {
        val context = PolicyEngine.buildContext(
            packageName = "com.instagram.android",
            appEnabled = true,
            antiReelsEnabled = true,
            antiScrollEnabled = false,
            browserBlockEnabled = false,
            scheduleActive = true,
            cooldownActive = false,
            blockingSessionActive = false,
            confidence = 0.85,
            detectedFeature = FeatureMask.ANTI_REELS,
            surfaceName = "DM",
            instagramSettings = com.scrolllock.app.data.model.InstagramAntiReelsSettings(
                allowReelsInDMs = true
            )
        )
        val decision = PolicyEngine.evaluate(context)
        assertEquals(PolicyDecision.Action.ALLOW, decision.action)
    }
}
