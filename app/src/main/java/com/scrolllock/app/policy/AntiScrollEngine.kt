package com.scrolllock.app.policy

import com.scrolllock.app.data.model.CustomAntiScrollMode
import com.scrolllock.app.data.model.Swipe
import com.scrolllock.app.data.room.dao.SwipeDao
import com.scrolllock.app.data.room.dao.AntiScrollDao
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

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

    private val persistJob = SupervisorJob()
    private val persistScope = CoroutineScope(persistJob + Dispatchers.IO)

    private val recentSwipes = ConcurrentHashMap<String, MutableList<Swipe>>()
    private val lastScrollY = ConcurrentHashMap<String, Int>()
    private val lastEventTime = ConcurrentHashMap<String, Long>()
    private val blockedUntil = ConcurrentHashMap<String, Long>()

    private val sessionStart = ConcurrentHashMap<String, Long>()
    private val sessionLastActivity = ConcurrentHashMap<String, Long>()
    private val sessionDirectionChanges = ConcurrentHashMap<String, Int>()
    private val sessionLastDirection = ConcurrentHashMap<String, Int>()

    companion object {
        private const val DEBOUNCE_MS = 80L
        private const val MIN_DELTA_THRESHOLD = 3
        private const val NOISE_WINDOW_MS = 40L
        private const val SESSION_TIMEOUT_MS = 3000L
        private const val BLOCK_DURATION_MS = 60_000L
    }

    fun analyzeScrollEvent(
        packageName: String,
        scrollY: Int,
        eventTime: Long,
        maxScrollY: Int = 0
    ): ScrollAnalysis {
        val now = System.currentTimeMillis()

        if (isBlocked(packageName, now)) {
            return ScrollAnalysis.Blocked
        }

        val prevTime = lastEventTime[packageName] ?: 0L
        val timeDelta = eventTime - prevTime

        if (timeDelta < DEBOUNCE_MS) {
            return ScrollAnalysis.Debounced(timeDelta)
        }

        val prevScrollY = lastScrollY[packageName]
        if (prevScrollY == null) {
            lastScrollY[packageName] = scrollY
            lastEventTime[packageName] = eventTime
            updateSession(packageName, 0, eventTime)
            return ScrollAnalysis.Initial
        }

        val delta = scrollY - prevScrollY

        lastScrollY[packageName] = scrollY
        lastEventTime[packageName] = eventTime

        if (kotlin.math.abs(delta) < MIN_DELTA_THRESHOLD) {
            return ScrollAnalysis.NoiseFiltered(delta, MIN_DELTA_THRESHOLD)
        }

        val direction = if (delta > 0) 1 else -1
        val sessionAnalysis = updateSession(packageName, direction, eventTime)

        val swipe = Swipe(
            packageName = packageName,
            timestamp = eventTime,
            swipeDirection = direction,
            scrollDeltaY = delta,
            fromIndex = 0,
            toIndex = 0
        )

        recentSwipes.getOrPut(packageName) { mutableListOf() }.add(swipe)
        scopePersistSwipe(swipe)
        cleanupOldSwipes(packageName, now)

        return ScrollAnalysis.Recorded(
            delta = delta,
            direction = direction,
            timeDelta = timeDelta,
            sessionDuration = sessionAnalysis.duration,
            directionChanges = sessionAnalysis.directionChanges,
            swipeCount = sessionAnalysis.swipeCount
        )
    }

    private fun scopePersistSwipe(swipe: Swipe) {
        // Fire-and-forget persistence to avoid blocking event processing
        persistScope.launch {
            try {
                swipeDao.insert(swipe)
            } catch (e: Exception) {
                // Persistence failed, in-memory state remains
            }
        }
    }

    private fun updateSession(packageName: String, direction: Int, eventTime: Long): SessionState {
        val start = sessionStart[packageName]
        val lastActivity = sessionLastActivity[packageName]
        val directionChanges = sessionDirectionChanges[packageName] ?: 0
        val lastDir = sessionLastDirection[packageName] ?: 0

        if (start == null || lastActivity == null || (eventTime - lastActivity) > SESSION_TIMEOUT_MS) {
            sessionStart[packageName] = eventTime
            sessionLastActivity[packageName] = eventTime
            sessionDirectionChanges[packageName] = 0
            sessionLastDirection[packageName] = direction
            return SessionState(0L, 0, 1)
        }

        val duration = eventTime - start
        var newDirectionChanges = directionChanges
        if (direction != 0 && lastDir != 0 && direction != lastDir) {
            newDirectionChanges++
        }

        sessionLastActivity[packageName] = eventTime
        sessionDirectionChanges[packageName] = newDirectionChanges
        sessionLastDirection[packageName] = direction

        val swipes = recentSwipes[packageName]
        val swipesInSession = swipes?.count { it.timestamp >= start } ?: 0

        return SessionState(duration, newDirectionChanges, swipesInSession)
    }

    suspend fun shouldBlock(packageName: String): BlockDecision {
        val now = System.currentTimeMillis()
        if (isBlocked(packageName, now)) return BlockDecision.Blocked

        val mode = try {
            antiScrollDao.getByPackage(packageName) ?: defaultMode
        } catch (e: Exception) {
            defaultMode
        }

        val sessionDuration = sessionStart[packageName]?.let { now - it } ?: 0L
        if (sessionDuration == 0L) return BlockDecision.Allowed("no_active_session")

        val sessionEnd = sessionLastActivity[packageName] ?: now
        val sessionWindowStart = sessionEnd - mode.checkWindow * 1000L

        val sessionSwipes = recentSwipes[packageName]?.filter { it.timestamp >= sessionWindowStart } ?: emptyList()
        if (sessionSwipes.isEmpty()) return BlockDecision.Allowed("no_swipes_in_session_window")

        val swipeCount = sessionSwipes.size
        val directionChanges = sessionDirectionChanges[packageName] ?: 0

        val frequencyMet = swipeCount >= mode.swipesFrequency
        val durationMet = sessionDuration >= mode.durationThreshold * 1000L
        val hasHighDirectionChanges = directionChanges > 3

        if (mode.durationThreshold <= 0) {
            if (frequencyMet) {
                blockedUntil[packageName] = now + BLOCK_DURATION_MS
                return BlockDecision.Blocked
            }
        } else {
            if (frequencyMet && durationMet) {
                blockedUntil[packageName] = now + BLOCK_DURATION_MS
                return BlockDecision.Blocked
            }
            if (frequencyMet && hasHighDirectionChanges && sessionDuration >= mode.durationThreshold * 500L) {
                blockedUntil[packageName] = now + BLOCK_DURATION_MS
                return BlockDecision.Blocked
            }
        }

        return BlockDecision.Allowed("thresholds_not_met")
    }

    private fun isBlocked(packageName: String, now: Long): Boolean {
        val expiry = blockedUntil[packageName] ?: return false
        if (now < expiry) return true
        blockedUntil.remove(packageName)
        return false
    }

    private fun cleanupOldSwipes(packageName: String, now: Long) {
        val swipes = recentSwipes[packageName] ?: return
        val cutoff = now - 120_000L
        swipes.removeAll { it.timestamp < cutoff }
    }

    fun cleanupPackage(packageName: String) {
        recentSwipes.remove(packageName)
        lastScrollY.remove(packageName)
        lastEventTime.remove(packageName)
        blockedUntil.remove(packageName)
        sessionStart.remove(packageName)
        sessionLastActivity.remove(packageName)
        sessionDirectionChanges.remove(packageName)
        sessionLastDirection.remove(packageName)
    }

    suspend fun cleanupAll() {
        val cutoff = System.currentTimeMillis() - 300_000L
        swipeDao.deleteOlderThan(cutoff)
        persistJob.cancel()
        recentSwipes.clear()
        lastScrollY.clear()
        lastEventTime.clear()
        blockedUntil.clear()
        sessionStart.clear()
        sessionLastActivity.clear()
        sessionDirectionChanges.clear()
        sessionLastDirection.clear()
    }

    fun getDebugInfo(packageName: String): AntiScrollDebugInfo {
        val swipes = recentSwipes[packageName] ?: emptyList()
        val now = System.currentTimeMillis()
        val recentCount = swipes.count { it.timestamp > now - 20_000L }
        val blocked = blockedUntil[packageName]?.let { now < it } ?: false
        val duration = sessionStart[packageName]?.let { now - it } ?: 0L
        return AntiScrollDebugInfo(
            packageName = packageName,
            recentSwipeCount = swipes.size,
            recentWindowCount = recentCount,
            isBlocked = blocked,
            blockedUntil = blockedUntil[packageName] ?: 0L,
            sessionDuration = duration,
            directionChanges = sessionDirectionChanges[packageName] ?: 0,
            lastScrollDelta = if (swipes.size >= 2) {
                val last = swipes.last().swipeDirection
                val prev = swipes[swipes.size - 2].swipeDirection
                if (last != prev) "changed" else "same"
            } else "n/a"
        )
    }

    private data class SessionState(
        val duration: Long,
        val directionChanges: Int,
        val swipeCount: Int
    )
}

sealed class ScrollAnalysis {
    data object Blocked : ScrollAnalysis()
    data object Initial : ScrollAnalysis()
    data class Debounced(val timeDelta: Long) : ScrollAnalysis()
    data class NoiseFiltered(val delta: Int, val threshold: Int) : ScrollAnalysis()
    data class Recorded(
        val delta: Int,
        val direction: Int,
        val timeDelta: Long,
        val sessionDuration: Long,
        val directionChanges: Int,
        val swipeCount: Int
    ) : ScrollAnalysis()
}

sealed class BlockDecision {
    data object Blocked : BlockDecision()
    data class Allowed(val reason: String) : BlockDecision()
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
    val blockedUntil: Long,
    val sessionDuration: Long = 0L,
    val directionChanges: Int = 0,
    val lastScrollDelta: String = "n/a"
)