package com.scrolllock.app.detection

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

    private const val CACHE_TTL_MS = 500L
    private const val WINDOW_CHANGE_INVALIDATE = true

    fun registerDetector(detector: ContentDetector) {
        detectors.add(detector)
    }

    fun detect(packageName: String?, root: AccessibilityNodeInfo?, eventType: Int = 0): List<DetectionCandidate> {
        if (packageName == null || root == null) return emptyList()

        val now = System.currentTimeMillis()
        val rootHash = computeRootHash(root)
        val windowId = root.windowId

        if (packageName == lastPackageName &&
            rootHash == lastRootHashCode &&
            windowId == lastWindowId &&
            (now - lastDetectionTime) < CACHE_TTL_MS
        ) {
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

        return candidates
    }

    fun forceInvalidate() {
        lastPackageName = null
        lastRootHashCode = 0
        lastWindowId = -1
        lastDetection = emptyList()
        lastDetectionTime = 0L
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
        hash = 31 * hash + root.hashCode()
        val child = root.getChild(0)
        if (child != null) {
            hash = 31 * hash + (child.viewIdResourceName?.hashCode() ?: 0)
            hash = 31 * hash + child.childCount
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
