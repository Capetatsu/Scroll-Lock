package com.scrolllock.app.browser

import android.view.accessibility.AccessibilityNodeInfo
import com.scrolllock.app.detection.NodeUtils

class BrowserContentBlocker(private val domainMatcher: DomainMatcher) {

    data class BlockResult(
        val blocked: Boolean,
        val url: String? = null,
        val domain: String? = null,
        val hostname: String? = null,
        val matchedReason: String? = null
    )

    fun checkUrl(packageName: String, root: AccessibilityNodeInfo): BlockResult {
        val url = extractUrl(packageName, root) ?: return BlockResult(blocked = false)
        val hostname = extractHostname(url)
        val normalizedDomain = domainMatcher.normalizeDomain(hostname)
        val blocked = domainMatcher.isBlocked(url)

        val reason = if (blocked) {
            when {
                normalizedDomain in domainMatcher.getBuiltinDomains() -> "builtin_match"
                normalizedDomain in domainMatcher.getCustomDomains() -> "custom_match"
                domainMatcher.getParentDomain(normalizedDomain) in domainMatcher.getBuiltinDomains() -> "parent_match"
                domainMatcher.getParentDomain(normalizedDomain) in domainMatcher.getCustomDomains() -> "custom_parent_match"
                else -> "subdomain_match"
            }
        } else null

        return BlockResult(
            blocked = blocked,
            url = url,
            domain = normalizedDomain,
            hostname = hostname,
            matchedReason = reason
        )
    }

    private fun extractUrl(packageName: String, root: AccessibilityNodeInfo): String? {
        val config = BrowserRegistry.getConfigForPackage(packageName) ?: return null
        val urlBar = NodeUtils.findNodeById(root, config.urlBarId) ?: return null
        val text = NodeUtils.getNodeText(urlBar).trim()
        return text.ifEmpty { null }
    }

    private fun extractHostname(url: String): String {
        var host = url.lowercase().trim()
        host = host.removePrefix("http://").removePrefix("https://")
        host = host.removePrefix("www.")
        val slashIndex = host.indexOf('/')
        if (slashIndex > 0) host = host.substring(0, slashIndex)
        val questionIndex = host.indexOf('?')
        if (questionIndex > 0) host = host.substring(0, questionIndex)
        val hashIndex = host.indexOf('#')
        if (hashIndex > 0) host = host.substring(0, hashIndex)
        return host
    }
}

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
