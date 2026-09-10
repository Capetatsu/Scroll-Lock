package com.scrolllock.app.policy

import com.scrolllock.app.data.model.FeatureMask
import com.scrolllock.app.data.model.InstagramAntiReelsSettings
import com.scrolllock.app.data.model.ScheduleRule
import com.scrolllock.app.data.model.TimeSlot
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZonedDateTime

data class DecisionContext(
    val packageName: String,
    val appEnabled: Boolean = false,
    val antiReelsEnabled: Boolean = false,
    val antiScrollEnabled: Boolean = false,
    val browserBlockEnabled: Boolean = false,
    val scheduleActive: Boolean = true,
    val cooldownActive: Boolean = false,
    val blockingSessionActive: Boolean = false,
    val confidence: Double = 0.0,
    val detectedFeature: Int = 0,
    val surfaceName: String = "",
    val instagramSettings: InstagramAntiReelsSettings? = null
) {
    val isInstagram: Boolean get() = packageName == "com.instagram.android"
    val isAntiScrollSurface: Boolean get() = detectedFeature == FeatureMask.ANTI_SCROLL
    val isAntiReelsSurface: Boolean get() = detectedFeature == FeatureMask.ANTI_REELS
    val isBrowserSurface: Boolean get() = detectedFeature == FeatureMask.BROWSER_BLOCKING
}

data class PolicyDecision(
    val action: Action,
    val reason: String,
    val redirectTarget: String? = null
) {
    enum class Action {
        ALLOW,
        BLOCK,
        REDIRECT,
        HIDE
    }
}

object PolicyEngine {
    fun evaluate(context: DecisionContext): PolicyDecision {
        if (!context.appEnabled) {
            return PolicyDecision(PolicyDecision.Action.ALLOW, "app_not_enabled")
        }

        if (!context.antiReelsEnabled && !context.antiScrollEnabled && !context.browserBlockEnabled) {
            return PolicyDecision(PolicyDecision.Action.ALLOW, "no_features_enabled")
        }

        if (!context.scheduleActive) {
            return PolicyDecision(PolicyDecision.Action.ALLOW, "schedule_inactive")
        }

        if (context.confidence < 0.70 && context.detectedFeature != 0) {
            return PolicyDecision(PolicyDecision.Action.ALLOW, "low_confidence")
        }

        if (context.cooldownActive) {
            return PolicyDecision(PolicyDecision.Action.BLOCK, "cooldown_active")
        }

        if (context.blockingSessionActive) {
            return PolicyDecision(PolicyDecision.Action.BLOCK, "timeout_active")
        }

        if (context.isInstagram && context.antiReelsEnabled) {
            val igDecision = evaluateInstagram(context)
            if (igDecision != null) return igDecision
        }

        if (context.isAntiScrollSurface && context.antiScrollEnabled) {
            return PolicyDecision(PolicyDecision.Action.BLOCK, "anti_scroll_triggered")
        }

        if (context.isBrowserSurface && context.browserBlockEnabled) {
            return PolicyDecision(PolicyDecision.Action.BLOCK, "browser_block_triggered")
        }

        if (context.isAntiReelsSurface && context.antiReelsEnabled) {
            return PolicyDecision(PolicyDecision.Action.BLOCK, "anti_reels_triggered")
        }

        return PolicyDecision(PolicyDecision.Action.ALLOW, "no_rule_matched")
    }

    private fun evaluateInstagram(context: DecisionContext): PolicyDecision? {
        // ALWAYS use valid settings - create defaults if missing
        val settings = context.instagramSettings ?: InstagramAntiReelsSettings(
            hideReelsOnHome = true,
            blockExplore = true,
            blockMainFeed = false,
            blockStories = false,
            blockComments = false,
            allowReelsInDMs = true,
            redirectOnBlock = false
        )
        val surface = context.surfaceName

        return when (surface) {
            "REELS" -> {
                if (settings.hideReelsOnHome) {
                    PolicyDecision(PolicyDecision.Action.BLOCK, "instagram_reels_blocked")
                } else null
            }
            "STORIES" -> {
                if (settings.blockStories) {
                    PolicyDecision(PolicyDecision.Action.BLOCK, "instagram_stories_blocked")
                } else null
            }
            "MAIN_FEED" -> {
                if (settings.blockMainFeed) {
                    PolicyDecision(PolicyDecision.Action.REDIRECT, "instagram_feed_blocked", "direct")
                } else null
            }
            "COMMENTS" -> {
                if (settings.blockComments) {
                    PolicyDecision(PolicyDecision.Action.BLOCK, "instagram_comments_blocked")
                } else null
            }
            "EXPLORE" -> {
                if (settings.blockExplore) {
                    PolicyDecision(PolicyDecision.Action.BLOCK, "instagram_explore_blocked")
                } else null
            }
            "DM" -> {
                if (settings.allowReelsInDMs) {
                    PolicyDecision(PolicyDecision.Action.ALLOW, "instagram_dm_allowed")
                } else {
                    PolicyDecision(PolicyDecision.Action.BLOCK, "instagram_dm_blocked")
                }
            }
            else -> null
        }
    }

    fun buildContext(
        packageName: String,
        appEnabled: Boolean,
        antiReelsEnabled: Boolean,
        antiScrollEnabled: Boolean,
        browserBlockEnabled: Boolean,
        scheduleActive: Boolean,
        cooldownActive: Boolean,
        blockingSessionActive: Boolean,
        confidence: Double,
        detectedFeature: Int,
        surfaceName: String,
        instagramSettings: InstagramAntiReelsSettings? = null
    ): DecisionContext {
        return DecisionContext(
            packageName = packageName,
            appEnabled = appEnabled,
            antiReelsEnabled = antiReelsEnabled,
            antiScrollEnabled = antiScrollEnabled,
            browserBlockEnabled = browserBlockEnabled,
            scheduleActive = scheduleActive,
            cooldownActive = cooldownActive,
            blockingSessionActive = blockingSessionActive,
            confidence = confidence,
            detectedFeature = detectedFeature,
            surfaceName = surfaceName,
            instagramSettings = instagramSettings
        )
    }
}

object ScheduleEngine {
    fun isScheduleActive(rules: List<ScheduleRule>, packageName: String, featureMask: Int): Boolean {
        val now = ZonedDateTime.now()
        for (rule in rules) {
            if (!rule.enabled) continue
            if (!FeatureMask.hasFeature(rule.featuresMask, featureMask)) continue
            if (!rule.appliesToAllApps && packageName !in parsePackageNames(rule.packageNamesJSON)) continue
            if (!isDayEnabled(rule.daysBitmask, now.dayOfWeek)) continue
            if (!isTimeSlotActive(rule.timeSlotsJSON, now.toLocalTime())) continue
            return true
        }
        return false
    }

    fun isDayEnabled(daysBitmask: Int, dayOfWeek: DayOfWeek): Boolean {
        val dayValue = when (dayOfWeek) {
            DayOfWeek.MONDAY -> 1
            DayOfWeek.TUESDAY -> 2
            DayOfWeek.WEDNESDAY -> 4
            DayOfWeek.THURSDAY -> 8
            DayOfWeek.FRIDAY -> 16
            DayOfWeek.SATURDAY -> 32
            DayOfWeek.SUNDAY -> 64
        }
        return (daysBitmask and dayValue) != 0
    }

    fun isTimeSlotActive(timeSlotsJSON: String, now: LocalTime): Boolean {
        val slots = parseTimeSlots(timeSlotsJSON)
        if (slots.isEmpty()) return true
        return slots.any { it.contains(now.hour, now.minute) }
    }

    private fun parsePackageNames(json: String): Set<String> {
        if (json.isBlank() || json == "[]") return emptySet()
        return json.removeSurrounding("[", "]")
            .split(",")
            .map { it.trim().removeSurrounding("\"") }
            .filter { it.isNotEmpty() }
            .toSet()
    }

    private fun parseTimeSlots(json: String): List<TimeSlot> {
        if (json.isBlank() || json == "[]") return emptyList()
        val slots = mutableListOf<TimeSlot>()
        val clean = json.removeSurrounding("[", "]")
        val pairs = clean.split("}, {".toRegex())
        for (pair in pairs) {
            val nums = pair.filter { it.isDigit() || it == ',' }
                .split(",")
                .map { it.trim().toIntOrNull() ?: 0 }
            if (nums.size >= 4) {
                slots.add(TimeSlot(nums[0], nums[1], nums[2], nums[3]))
            }
        }
        return slots
    }
}

object CooldownEngine {
    fun isCooldownActive(
        sourceApp: String?,
        cooldownStart: Long,
        cooldownDurationMinutes: Int,
        currentPackage: String,
        extraApps: Set<String>
    ): Boolean {
        if (sourceApp.isNullOrEmpty()) return false
        if (cooldownStart <= 0) return false

        val isSource = currentPackage == sourceApp
        val isExtra = currentPackage in extraApps
        if (!isSource && !isExtra) return false

        val expiry = cooldownStart + cooldownDurationMinutes * 60_000L
        return System.currentTimeMillis() < expiry
    }

    fun isSourceOrExtraApp(
        sourceApp: String?,
        currentPackage: String,
        extraApps: Set<String>
    ): Boolean {
        if (sourceApp.isNullOrEmpty()) return false
        return currentPackage == sourceApp || currentPackage in extraApps
    }

    fun remainingMinutes(sourceApp: String?, cooldownStart: Long, cooldownDurationMinutes: Int): Long {
        if (sourceApp.isNullOrEmpty() || cooldownStart <= 0) return 0
        val expiry = cooldownStart + cooldownDurationMinutes * 60_000L
        val remaining = expiry - System.currentTimeMillis()
        return (remaining / 60_000L).coerceAtLeast(0)
    }
}