package com.scrolllock.app.detection

import android.view.accessibility.AccessibilityNodeInfo

interface ContentDetector {
    val packageNames: Set<String>
    fun classify(root: AccessibilityNodeInfo): List<DetectionCandidate>
}

object DetectorEngine {
    private val detectors = mutableListOf<ContentDetector>()
    private var lastPackageName: String? = null
    private var lastDetection: List<DetectionCandidate> = emptyList()

    fun registerDetector(detector: ContentDetector) {
        detectors.add(detector)
    }

    fun detect(packageName: String?, root: AccessibilityNodeInfo?): List<DetectionCandidate> {
        if (packageName == null || root == null) return emptyList()

        if (packageName == lastPackageName && lastDetection.isNotEmpty()) {
            return lastDetection
        }

        val candidates = mutableListOf<DetectionCandidate>()
        for (detector in detectors) {
            if (packageName in detector.packageNames) {
                try {
                    candidates.addAll(detector.classify(root))
                } catch (e: Exception) {
                    // Detector failed, skip
                }
            }
        }

        lastPackageName = packageName
        lastDetection = candidates
        return candidates
    }

    fun clearCache() {
        lastPackageName = null
        lastDetection = emptyList()
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
