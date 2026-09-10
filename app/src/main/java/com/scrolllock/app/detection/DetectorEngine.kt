package com.scrolllock.app.detection

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

interface ContentDetector {
    val packageNames: Set<String>
    fun classify(root: AccessibilityNodeInfo): List<DetectionCandidate>
}

object DetectorEngine {
    private val detectors = mutableListOf<ContentDetector>()

    private var lastPackageName: String? = null
    private var lastRootHashCode: Int = 0
    private var lastWindowId: Int = -1
    private var lastDetection: List<DetectionCandidate> = emptyList()
    private var lastDetectionTime: Long = 0L
    private var lastEventType: Int = 0

    internal const val CACHE_TTL_MS = 200L
    private const val WINDOW_CHANGE_INVALIDATE = true
    internal const val SCROLL_CACHE_TTL_MS = 50L
    internal val CONTENT_CHANGE_INVALIDATE_TYPES = (
        AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
        AccessibilityEvent.TYPE_VIEW_FOCUSED or
        AccessibilityEvent.TYPE_VIEW_CLICKED
    )

    fun registerDetector(detector: ContentDetector) {
        detectors.add(detector)
    }

    fun detect(
        packageName: String?,
        root: AccessibilityNodeInfo?,
        eventType: Int = 0
    ): List<DetectionCandidate> {
        if (packageName == null || root == null) return emptyList()

        val now = System.currentTimeMillis()
        val rootHash = computeRootHash(root)
        val windowId = root.windowId

        if (!shouldInvalidate(packageName, rootHash, windowId, eventType, now)) {
            return lastDetection
        }

        val candidates = mutableListOf<DetectionCandidate>()
        for (detector in detectors) {
            if (packageName in detector.packageNames) {
                try {
                    candidates.addAll(detector.classify(root))
                } catch (e: Exception) {
                    // Detector failed, skip silently
                }
            }
        }

        lastPackageName = packageName
        lastRootHashCode = rootHash
        lastWindowId = windowId
        lastDetection = candidates
        lastDetectionTime = now
        lastEventType = eventType

        return candidates
    }

    private fun shouldInvalidate(
        packageName: String,
        rootHash: Int,
        windowId: Int,
        eventType: Int,
        now: Long
    ): Boolean {
        if (packageName != lastPackageName) return true
        if (windowId != lastWindowId) return true

        if (eventType and CONTENT_CHANGE_INVALIDATE_TYPES != 0) {
            return true
        }

        val effectiveTtl = if (eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            SCROLL_CACHE_TTL_MS
        } else {
            CACHE_TTL_MS
        }

        if ((now - lastDetectionTime) >= effectiveTtl) return true

        if (rootHash != lastRootHashCode) return true

        return false
    }

    fun forceInvalidate() {
        lastPackageName = null
        lastRootHashCode = 0
        lastWindowId = -1
        lastDetection = emptyList()
        lastDetectionTime = 0L
        lastEventType = 0
    }

    fun clearCache() {
        forceInvalidate()
    }

    private fun computeRootHash(root: AccessibilityNodeInfo): Int {
        var hash = 17
        hash = 31 * hash + (root.packageName?.hashCode() ?: 0)
        hash = 31 * hash + root.childCount
        hash = 31 * hash + root.windowId
        hash = 31 * hash + (root.viewIdResourceName?.hashCode() ?: 0)

        val childCount = minOf(root.childCount, 5)
        for (i in 0 until childCount) {
            val child = root.getChild(i)
            if (child != null) {
                hash = 31 * hash + (child.viewIdResourceName?.hashCode() ?: 0)
                hash = 31 * hash + child.childCount
                hash = 31 * hash + (child.text?.hashCode() ?: 0)
                hash = 31 * hash + (child.contentDescription?.hashCode() ?: 0)
                hash = 31 * hash + if (child.isSelected) 1 else 0
                hash = 31 * hash + if (child.isVisibleToUser) 1 else 0
            }
        }
        return hash
    }

    fun getDetectedSurface(packageName: String?, root: AccessibilityNodeInfo?): DetectionSurface {
        val candidates = detect(packageName, root)
        return candidates.maxByOrNull { it.confidence }?.surface ?: DetectionSurface.UNKNOWN
    }

    fun getConfidence(packageName: String?, root: AccessibilityNodeInfo?): Double {
        val candidates = detect(packageName, root)
        return candidates.maxByOrNull { it.confidence }?.confidence ?: 0.0
    }
}
