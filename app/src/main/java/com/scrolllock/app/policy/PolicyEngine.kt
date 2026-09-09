package com.scrolllock.app.policy

import com.scrolllock.app.data.model.FeatureMask
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
    val instagramReelsBlocked: Boolean = false,
    val instagramStoriesBlocked: Boolean = false,
    val instagramFeedBlocked: Boolean = false,
    val instagramCommentsBlocked: Boolean = false,
    val instagramExploreBlocked: Boolean = false,
    val instagramAllowReelsInDM: Boolean = true
)

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

        if (context.cooldownActive) {
            return PolicyDecision(PolicyDecision.Action.BLOCK, "cooldown_active")
        }

        if (context.blockingSessionActive) {
            return PolicyDecision(PolicyDecision.Action.BLOCK, "timeout_active")
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

        if (context.instagramReelsBlocked && context.surfaceName == "REELS") {
            return PolicyDecision(PolicyDecision.Action.BLOCK, "instagram_reels_blocked")
        }

        if (context.instagramStoriesBlocked && context.surfaceName == "STORIES") {
            return PolicyDecision(PolicyDecision.Action.BLOCK, "instagram_stories_blocked")
        }

        if (context.instagramFeedBlocked && context.surfaceName == "MAIN_FEED") {
            return PolicyDecision(PolicyDecision.Action.REDIRECT, "instagram_feed_blocked", "direct")
        }

        if (context.instagramCommentsBlocked && context.surfaceName == "COMMENTS") {
            return PolicyDecision(PolicyDecision.Action.BLOCK, "instagram_comments_blocked")
        }

        if (context.instagramExploreBlocked && context.surfaceName == "EXPLORE") {
            return PolicyDecision(PolicyDecision.Action.BLOCK, "instagram_explore_blocked")
        }

        if (context.instagramAllowReelsInDM && context.surfaceName == "DM") {
            return PolicyDecision(PolicyDecision.Action.ALLOW, "instagram_dm_allowed")
        }

        if (FeatureMask.hasFeature(context.detectedFeature, FeatureMask.ANTI_REELS) && context.antiReelsEnabled) {
            return PolicyDecision(PolicyDecision.Action.BLOCK, "anti_reels_triggered")
        }

        if (FeatureMask.hasFeature(context.detectedFeature, FeatureMask.ANTI_SCROLL) && context.antiScrollEnabled) {
            return PolicyDecision(PolicyDecision.Action.BLOCK, "anti_scroll_triggered")
        }

        if (FeatureMask.hasFeature(context.detectedFeature, FeatureMask.BROWSER_BLOCKING) && context.browserBlockEnabled) {
            return PolicyDecision(PolicyDecision.Action.BLOCK, "browser_block_triggered")
        }

        return PolicyDecision(PolicyDecision.Action.ALLOW, "no_rule_matched")
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
        instagramSettings: com.scrolllock.app.data.model.InstagramAntiReelsSettings? = null
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
            instagramReelsBlocked = instagramSettings?.hideReelsOnHome ?: true,
            instagramStoriesBlocked = instagramSettings?.blockStories ?: false,
            instagramFeedBlocked = instagramSettings?.blockMainFeed ?: false,
            instagramCommentsBlocked = instagramSettings?.blockComments ?: false,
            instagramExploreBlocked = instagramSettings?.blockExplore ?: true,
            instagramAllowReelsInDM = instagramSettings?.allowReelsInDMs ?: true
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
        val isSource = currentPackage == sourceApp
        val isExtra = currentPackage in extraApps
        if (!isSource && !isExtra) return false
        if (cooldownStart <= 0) return false
        val expiry = cooldownStart + cooldownDurationMinutes * 60_000L
        return System.currentTimeMillis() < expiry
    }

    fun remainingMinutes(sourceApp: String?, cooldownStart: Long, cooldownDurationMinutes: Int): Long {
        if (sourceApp.isNullOrEmpty() || cooldownStart <= 0) return 0
        val expiry = cooldownStart + cooldownDurationMinutes * 60_000L
        val remaining = expiry - System.currentTimeMillis()
        return (remaining / 60_000L).coerceAtLeast(0)
    }
}
