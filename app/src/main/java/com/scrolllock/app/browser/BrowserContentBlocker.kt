package com.scrolllock.app.browser

import android.view.accessibility.AccessibilityNodeInfo
import com.scrolllock.app.detection.NodeUtils

class BrowserContentBlocker(private val domainMatcher: DomainMatcher) {

    data class BlockResult(
        val blocked: Boolean,
        val url: String? = null,
        val domain: String? = null
    )

    fun checkUrl(packageName: String, root: AccessibilityNodeInfo): BlockResult {
        val url = extractUrl(packageName, root) ?: return BlockResult(blocked = false)
        val normalized = domainMatcher.normalizeDomain(url)
        val blocked = domainMatcher.isBlocked(url)
        return BlockResult(
            blocked = blocked,
            url = url,
            domain = normalized
        )
    }

    private fun extractUrl(packageName: String, root: AccessibilityNodeInfo): String? {
        val urlBarId = getUrlBarId(packageName) ?: return null
        val urlBar = NodeUtils.findNodeById(root, urlBarId) ?: return null
        val text = NodeUtils.getNodeText(urlBar).trim()
        return text.ifEmpty { null }
    }

    private fun getUrlBarId(packageName: String): String? {
        return when (packageName) {
            "com.android.chrome" -> "com.android.chrome:id/url_bar"
            "org.mozilla.firefox", "org.mozilla.firefox_beta" ->
                "org.mozilla.firefox:id/mozac_browser_toolbar_url_view"
            "com.sec.android.app.sbrowser" -> "com.sec.android.app.sbrowser:id/location_bar_edit_text"
            "com.microsoft.emmx" -> "com.microsoft.emmx:id/url_bar"
            "com.brave.browser" -> "com.brave.browser:id/url_bar"
            "com.opera.browser", "com.opera.mini.native" -> "com.opera.browser:id/url_bar"
            "com.duckduckgo.mobile.android" -> "com.duckduckgo.mobile.android:id/omnibarTextInput"
            else -> null
        }
    }
}
