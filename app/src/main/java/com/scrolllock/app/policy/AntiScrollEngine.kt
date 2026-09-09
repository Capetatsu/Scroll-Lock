package com.scrolllock.app.policy

import com.scrolllock.app.data.model.CustomAntiScrollMode
import com.scrolllock.app.data.model.Swipe
import com.scrolllock.app.data.room.dao.SwipeDao
import com.scrolllock.app.data.room.dao.AntiScrollDao
import java.util.concurrent.ConcurrentHashMap

class AntiScrollEngine(
    private val swipeDao: SwipeDao,
    private val antiScrollDao: AntiScrollDao
) {
    private val defaultMode = CustomAntiScrollMode(
        packageName = "",
        swipesFrequency = 1,
        checkWindow = 20,
        durationThreshold = 180
    )

    private val recentSwipes = ConcurrentHashMap<String, MutableList<Swipe>>()
    private val lastScrollTime = ConcurrentHashMap<String, Long>()
    private val lastScrollDelta = ConcurrentHashMap<String, Int>()
    private val blockedUntil = ConcurrentHashMap<String, Long>()

    companion object {
        private const val DEBOUNCE_MS = 150L
        private const val MIN_DELTA_THRESHOLD = 5
        private const val NOISE_WINDOW_MS = 50L
    }

    suspend fun recordSwipe(
        packageName: String,
        scrollDeltaY: Int,
        eventTime: Long,
        fromScrollY: Int = 0,
        toScrollY: Int = 0
    ): SwipeResult {
        val now = System.currentTimeMillis()

        if (isBlocked(packageName, now)) {
            return SwipeResult.BLOCKED
        }

        val prevTime = lastScrollTime[packageName] ?: 0L
        if (now - prevTime < DEBOUNCE_MS) {
            return SwipeResult.DEBOUNCED
        }

        val actualDelta = if (scrollDeltaY != 0) scrollDeltaY else (toScrollY - fromScrollY)
        if (kotlin.math.abs(actualDelta) < MIN_DELTA_THRESHOLD) {
            return SwipeResult.NOISE_FILTERED
        }

        val direction = when {
            actualDelta > 0 -> 1
            actualDelta < 0 -> -1
            else -> 0
        }

        val prevDelta = lastScrollDelta[packageName] ?: 0
        if (direction != 0 && direction == prevDelta) {
            val prevSwipeList = recentSwipes[packageName]
            if (prevSwipeList != null && prevSwipeList.isNotEmpty()) {
                val lastSwipe = prevSwipeList.last()
                if (now - lastSwipe.timestamp < NOISE_WINDOW_MS) {
                    return SwipeResult.DUPLICATE
                }
            }
        }

        val swipe = Swipe(
            packageName = packageName,
            timestamp = now,
            swipeDirection = direction
        )
        swipeDao.insert(swipe)

        recentSwipes.getOrPut(packageName) { mutableListOf() }.add(swipe)
        cleanupOldSwipes(packageName, now)

        lastScrollTime[packageName] = now
        lastScrollDelta[packageName] = direction

        return SwipeResult.RECORDED
    }

    suspend fun shouldBlock(packageName: String): Boolean {
        val now = System.currentTimeMillis()
        if (isBlocked(packageName, now)) return true

        val mode = antiScrollDao.getByPackage(packageName) ?: defaultMode
        val windowStart = now - mode.checkWindow * 1000L

        val swipes = getRecentSwipesInWindow(packageName, windowStart)

        if (swipes.isEmpty()) return false

        val sessionDuration = calculateSessionDuration(swipes)
        val swipeCount = swipes.size

        val frequencyMet = swipeCount >= mode.swipesFrequency
        val durationMet = sessionDuration >= mode.durationThreshold * 1000L

        if (frequencyMet && durationMet) {
            blockedUntil[packageName] = now + 60_000L
            return true
        }

        if (frequencyMet && mode.durationThreshold <= 0) {
            blockedUntil[packageName] = now + 60_000L
            return true
        }

        return false
    }

    private fun isBlocked(packageName: String, now: Long): Boolean {
        val expiry = blockedUntil[packageName] ?: return false
        if (now < expiry) return true
        blockedUntil.remove(packageName)
        return false
    }

    private fun getRecentSwipesInWindow(packageName: String, windowStart: Long): List<Swipe> {
        val memSwipes = recentSwipes[packageName] ?: return emptyList()
        return memSwipes.filter { it.timestamp >= windowStart }
    }

    private fun calculateSessionDuration(swipes: List<Swipe>): Long {
        if (swipes.size < 2) return 0
        val sorted = swipes.sortedBy { it.timestamp }
        return sorted.last().timestamp - sorted.first().timestamp
    }

    private fun cleanupOldSwipes(packageName: String, now: Long) {
        val swipes = recentSwipes[packageName] ?: return
        val cutoff = now - 120_000L
        swipes.removeAll { it.timestamp < cutoff }
    }

    suspend fun cleanupAll() {
        val cutoff = System.currentTimeMillis() - 300_000L
        swipeDao.deleteOlderThan(cutoff)
        recentSwipes.clear()
        lastScrollTime.clear()
        lastScrollDelta.clear()
        blockedUntil.clear()
    }

    fun getDebugInfo(packageName: String): AntiScrollDebugInfo {
        val swipes = recentSwipes[packageName] ?: emptyList()
        val now = System.currentTimeMillis()
        val recentCount = swipes.count { it.timestamp > now - 20_000L }
        val blocked = blockedUntil[packageName]?.let { now < it } ?: false
        return AntiScrollDebugInfo(
            packageName = packageName,
            recentSwipeCount = swipes.size,
            recentWindowCount = recentCount,
            isBlocked = blocked,
            blockedUntil = blockedUntil[packageName] ?: 0L
        )
    }
}

enum class SwipeResult {
    RECORDED,
    DEBOUNCED,
    DUPLICATE,
    NOISE_FILTERED,
    BLOCKED
}

data class AntiScrollDebugInfo(
    val packageName: String,
    val recentSwipeCount: Int,
    val recentWindowCount: Int,
    val isBlocked: Boolean,
    val blockedUntil: Long
)
