package com.scrolllock.app.policy

import com.scrolllock.app.data.model.FeatureMask
import com.scrolllock.app.data.model.ScheduleRule
import com.scrolllock.app.data.model.TimeSlot
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

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
    val detectedFeature: Int = 0
)

enum class Decision {
    ALLOW,
    BLOCK,
    COOLDOWN_BLOCK
}

object PolicyEngine {
    fun evaluate(context: DecisionContext): Decision {
        if (!context.appEnabled) return Decision.ALLOW
        if (context.cooldownActive) return Decision.COOLDOWN_BLOCK
        if (context.blockingSessionActive) return Decision.BLOCK
        if (!context.scheduleActive) return Decision.ALLOW

        if (FeatureMask.hasFeature(context.detectedFeature, FeatureMask.ANTI_REELS) && context.antiReelsEnabled) {
            if (context.confidence >= 0.70) return Decision.BLOCK
        }

        if (FeatureMask.hasFeature(context.detectedFeature, FeatureMask.ANTI_SCROLL) && context.antiScrollEnabled) {
            return Decision.BLOCK
        }

        if (FeatureMask.hasFeature(context.detectedFeature, FeatureMask.BROWSER_BLOCKING) && context.browserBlockEnabled) {
            return Decision.BLOCK
        }

        return Decision.ALLOW
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
    fun isCooldownActive(sourceApp: String?, cooldownStart: Long, cooldownDurationMinutes: Int, currentPackage: String, extraApps: Set<String>): Boolean {
        if (sourceApp == null) return false
        if (currentPackage != sourceApp && currentPackage !in extraApps) return false
        val expiry = cooldownStart + cooldownDurationMinutes * 60_000L
        return System.currentTimeMillis() < expiry
    }
}
