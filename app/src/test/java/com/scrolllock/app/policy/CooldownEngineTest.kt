package com.scrolllock.app.policy

import org.junit.Assert.*
import org.junit.Test

class CooldownEngineTest {

    @Test
    fun `CooldownEngine checks source app correctly`() {
        assertTrue(CooldownEngine.isCooldownActive(
            "com.instagram.android",
            System.currentTimeMillis() - 60_000,
            30,
            "com.instagram.android",
            emptySet()
        ))
    }

    @Test
    fun `CooldownEngine checks extra apps correctly`() {
        assertTrue(CooldownEngine.isCooldownActive(
            "com.instagram.android",
            System.currentTimeMillis() - 60_000,
            30,
            "com.facebook.katana",
            setOf("com.facebook.katana")
        ))
    }

    @Test
    fun `CooldownEngine rejects unrelated app`() {
        assertFalse(CooldownEngine.isCooldownActive(
            "com.instagram.android",
            System.currentTimeMillis() - 60_000,
            30,
            "com.twitter.android",
            setOf("com.facebook.katana")
        ))
    }

    @Test
    fun `CooldownEngine rejects expired cooldown`() {
        assertFalse(CooldownEngine.isCooldownActive(
            "com.instagram.android",
            System.currentTimeMillis() - 3600_000,
            30,
            "com.instagram.android",
            emptySet()
        ))
    }

    @Test
    fun `CooldownEngine rejects null source app`() {
        assertFalse(CooldownEngine.isCooldownActive(
            null,
            System.currentTimeMillis(),
            30,
            "com.instagram.android",
            emptySet()
        ))
    }

    @Test
    fun `CooldownEngine rejects empty source app`() {
        assertFalse(CooldownEngine.isCooldownActive(
            "",
            System.currentTimeMillis(),
            30,
            "com.instagram.android",
            emptySet()
        ))
    }

    @Test
    fun `CooldownEngine rejects zero start time`() {
        assertFalse(CooldownEngine.isCooldownActive(
            "com.instagram.android",
            0L,
            30,
            "com.instagram.android",
            emptySet()
        ))
    }

    @Test
    fun `CooldownEngine rejects negative start time`() {
        assertFalse(CooldownEngine.isCooldownActive(
            "com.instagram.android",
            -1L,
            30,
            "com.instagram.android",
            emptySet()
        ))
    }

    @Test
    fun `CooldownEngine calculates remaining minutes`() {
        val remaining = CooldownEngine.remainingMinutes(
            "com.instagram.android",
            System.currentTimeMillis() - 60_000,
            30
        )
        assertTrue(remaining in 28..30)
    }

    @Test
    fun `CooldownEngine remaining minutes zero for expired`() {
        val remaining = CooldownEngine.remainingMinutes(
            "com.instagram.android",
            System.currentTimeMillis() - 3600_000,
            30
        )
        assertEquals(0L, remaining)
    }

    @Test
    fun `CooldownEngine remaining minutes zero for null source`() {
        val remaining = CooldownEngine.remainingMinutes(
            null,
            System.currentTimeMillis(),
            30
        )
        assertEquals(0L, remaining)
    }

    @Test
    fun `isSourceOrExtraApp identifies source correctly`() {
        assertTrue(CooldownEngine.isSourceOrExtraApp(
            "com.instagram.android",
            "com.instagram.android",
            emptySet()
        ))
    }

    @Test
    fun `isSourceOrExtraApp identifies extra app correctly`() {
        assertTrue(CooldownEngine.isSourceOrExtraApp(
            "com.instagram.android",
            "com.facebook.katana",
            setOf("com.facebook.katana", "com.twitter.android")
        ))
    }

    @Test
    fun `isSourceOrExtraApp rejects unrelated app`() {
        assertFalse(CooldownEngine.isSourceOrExtraApp(
            "com.instagram.android",
            "com.whatsapp",
            setOf("com.facebook.katana")
        ))
    }

    @Test
    fun `isSourceOrExtraApp rejects null source`() {
        assertFalse(CooldownEngine.isSourceOrExtraApp(
            null,
            "com.instagram.android",
            emptySet()
        ))
    }

    @Test
    fun `isSourceOrExtraApp rejects empty extra apps`() {
        assertFalse(CooldownEngine.isSourceOrExtraApp(
            "com.instagram.android",
            "com.facebook.katana",
            emptySet()
        ))
    }

    @Test
    fun `CooldownEngine blocks only source and extra apps, not unrelated`() {
        val sourceApp = "com.instagram.android"
        val extraApps = setOf("com.facebook.katana")
        val cooldownStart = System.currentTimeMillis() - 60_000
        val duration = 30

        assertTrue(CooldownEngine.isCooldownActive(sourceApp, cooldownStart, duration, sourceApp, extraApps))
        assertTrue(CooldownEngine.isCooldownActive(sourceApp, cooldownStart, duration, "com.facebook.katana", extraApps))
        assertFalse(CooldownEngine.isCooldownActive(sourceApp, cooldownStart, duration, "com.twitter.android", extraApps))
        assertFalse(CooldownEngine.isCooldownActive(sourceApp, cooldownStart, duration, "com.whatsapp", extraApps))
        assertFalse(CooldownEngine.isCooldownActive(sourceApp, cooldownStart, duration, "com.google.android.youtube", extraApps))
    }

    @Test
    fun `CooldownEngine with zero duration blocks immediately`() {
        val cooldownStart = System.currentTimeMillis()
        assertFalse(CooldownEngine.isCooldownActive(
            "com.instagram.android",
            cooldownStart,
            0,
            "com.instagram.android",
            emptySet()
        ))
    }
}
