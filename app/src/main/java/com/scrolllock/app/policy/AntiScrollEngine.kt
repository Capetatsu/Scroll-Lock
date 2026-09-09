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

    suspend fun recordSwipe(packageName: String, direction: Int) {
        val swipe = Swipe(
            packageName = packageName,
            timestamp = System.currentTimeMillis(),
            swipeDirection = direction
        )
        swipeDao.insert(swipe)

        recentSwipes.getOrPut(packageName) { mutableListOf() }.add(swipe)
        cleanupOldSwipes(packageName)
    }

    suspend fun shouldBlock(packageName: String): Boolean {
        val mode = antiScrollDao.getByPackage(packageName) ?: defaultMode
        val now = System.currentTimeMillis()
        val windowStart = now - mode.checkWindow * 1000L

        val recentSwipesList = swipeDao.getRecentSwipes(packageName, windowStart)

        if (recentSwipesList.size < mode.swipesFrequency) return false

        val sessionDuration = calculateSessionDuration(recentSwipesList)
        return sessionDuration >= mode.durationThreshold * 1000L
    }

    suspend fun getMode(packageName: String): CustomAntiScrollMode {
        return antiScrollDao.getByPackage(packageName) ?: defaultMode.copy(packageName = packageName)
    }

    suspend fun setMode(mode: CustomAntiScrollMode) {
        antiScrollDao.upsert(mode)
    }

    private fun calculateSessionDuration(swipes: List<Swipe>): Long {
        if (swipes.isEmpty()) return 0
        val sorted = swipes.sortedBy { it.timestamp }
        return sorted.last().timestamp - sorted.first().timestamp
    }

    private fun cleanupOldSwipes(packageName: String) {
        val swipes = recentSwipes[packageName] ?: return
        val cutoff = System.currentTimeMillis() - 60_000L
        swipes.removeAll { it.timestamp < cutoff }
    }

    suspend fun cleanupAll() {
        val cutoff = System.currentTimeMillis() - 300_000L
        swipeDao.deleteOlderThan(cutoff)
        recentSwipes.clear()
    }
}
