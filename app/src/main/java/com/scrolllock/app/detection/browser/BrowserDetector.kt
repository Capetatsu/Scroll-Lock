package com.scrolllock.app.detection.browser

import android.view.accessibility.AccessibilityNodeInfo
import com.scrolllock.app.browser.BrowserRegistry
import com.scrolllock.app.detection.*

class BrowserDetector : ContentDetector {
    override val packageNames: Set<String>
        get() = BrowserRegistry.getAllBrowsers().map { it.packageName }.toSet()

    override fun classify(root: AccessibilityNodeInfo): List<DetectionCandidate> {
        val candidates = mutableListOf<DetectionCandidate>()
        val packageName = root.packageName?.toString() ?: return candidates
        val config = BrowserRegistry.getConfigForPackage(packageName) ?: return candidates

        val url = extractUrl(root, config.urlBarId)
        if (url.isNotEmpty()) {
            candidates.add(DetectionCandidate(
                packageName = config.packageName,
                surface = DetectionSurface.BROWSER_URL,
                confidence = 0.90,
                reasonCodes = listOf("url_bar", url),
                matchResult = DetectionResult.MATCH
            ))
        }

        return candidates
    }

    private fun extractUrl(root: AccessibilityNodeInfo, urlBarId: String): String {
        val urlBar = NodeUtils.findNodeById(root, urlBarId) ?: return ""
        return NodeUtils.getNodeText(urlBar).trim()
    }
}
