package com.scrolllock.app.detection

import java.util.concurrent.ConcurrentLinkedDeque

object DebugEventBus {
    private val recentEvents = ConcurrentLinkedDeque<DetectionDebugInfo>()
    private const val MAX_EVENTS = 50

    fun postEvent(info: DetectionDebugInfo) {
        recentEvents.addFirst(info)
        while (recentEvents.size > MAX_EVENTS) {
            recentEvents.removeLast()
        }
    }

    fun getRecentEvents(): List<DetectionDebugInfo> = recentEvents.toList()

    fun getLatestEvent(): DetectionDebugInfo? = recentEvents.peekFirst()

    fun clear() {
        recentEvents.clear()
    }
}
