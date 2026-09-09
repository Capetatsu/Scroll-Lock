package com.scrolllock.app.detection.browser

import android.view.accessibility.AccessibilityNodeInfo
import com.scrolllock.app.detection.*

data class BrowserConfig(
    val packageName: String,
    val urlBarId: String
)

object BrowserRegistry {
    private val browsers = listOf(
        BrowserConfig("com.android.chrome", "com.android.chrome:id/url_bar"),
        BrowserConfig("com.android.chrome", "com.android.chrome:id/omnibox_title_bar"),
        BrowserConfig("org.mozilla.firefox", "org.mozilla.firefox:id/mozac_browser_toolbar_url_view"),
        BrowserConfig("org.mozilla.firefox_beta", "org.mozilla.firefox_beta:id/mozac_browser_toolbar_url_view"),
        BrowserConfig("com.sec.android.app.sbrowser", "com.sec.android.app.sbrowser:id/location_bar_edit_text"),
        BrowserConfig("com.microsoft.emmx", "com.microsoft.emmx:id/url_bar"),
        BrowserConfig("com.brave.browser", "com.brave.browser:id/url_bar"),
        BrowserConfig("com.opera.browser", "com.opera.browser:id/url_bar"),
        BrowserConfig("com.opera.mini.native", "com.opera.mini.native:id/url_bar"),
        BrowserConfig("com.duckduckgo.mobile.android", "com.duckduckgo.mobile.android:id/omnibarTextInput"),
    )

    fun getConfigForPackage(packageName: String): BrowserConfig? {
        return browsers.find { it.packageName == packageName }
    }

    fun isBrowser(packageName: String): Boolean {
        return browsers.any { it.packageName == packageName }
    }

    fun getAllBrowsers(): List<BrowserConfig> = browsers
}

class BrowserDetector : ContentDetector {
    override val packageNames: Set<String>
        get() = BrowserRegistry.getAllBrowsers().map { it.packageName }.toSet()

    override fun classify(root: AccessibilityNodeInfo): List<DetectionCandidate> {
        val candidates = mutableListOf<DetectionCandidate>()
        val config = BrowserRegistry.getConfigForPackage(root.packageName?.toString() ?: "")
            ?: return candidates

        val url = extractUrl(root, config.urlBarId)
        if (url.isNotEmpty()) {
            candidates.add(DetectionCandidate(
                packageName = config.packageName,
                surface = DetectionSurface.BROWSER_URL,
                confidence = 0.90,
                reasonCodes = listOf("url_bar", url)
            ))
        }

        return candidates
    }

    private fun extractUrl(root: AccessibilityNodeInfo, urlBarId: String): String {
        val urlBar = NodeUtils.findNodeById(root, urlBarId) ?: return ""
        return NodeUtils.getNodeText(urlBar).trim()
    }
}
