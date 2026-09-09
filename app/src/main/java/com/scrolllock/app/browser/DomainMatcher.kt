package com.scrolllock.app.browser

import java.util.HashSet

class DomainMatcher {
    private val builtinDomains = HashSet<String>()
    private val customDomains = HashSet<String>()

    init {
        loadDefaultAdultDomains()
    }

    private fun loadDefaultAdultDomains() {
        val defaults = listOf(
            "pornhub.com", "xvideos.com", "xnxx.com", "xhamster.com",
            "redtube.com", "youporn.com", "tube8.com", "spankbang.com",
            "beeg.com", "brazzers.com", "bangbros.com", "naughtyamerica.com",
            "realitykings.com", "mofos.com", "digitalplayground.com",
            "teamskeet.com", "blacked.com", "deeper.com", "propertysex.com",
            "adultfriendfinder.com", "ashleymadison.com", "fling.com",
            "livejasmin.com", "chaturbate.com", "stripchat.com",
            "bongacams.com", "myfreecams.com", "cam4.com",
            "onlyfans.com", "fansly.com", "manyvids.com",
            "imagefap.com", "deviantart.com", "reddit.com/r/nsfw",
            "4chan.org", "8kun.top", "8chan.co",
            "eporner.com", "hclips.com", "txxx.com",
            "pornone.com", "hentaihaven.xxx", "nhentai.net",
            "hanime.tv", "fakku.net", "hentaidude.com"
        )
        builtinDomains.addAll(defaults.map { normalizeDomain(it) })
    }

    fun normalizeDomain(domain: String): String {
        return domain.lowercase()
            .trim()
            .removePrefix("http://")
            .removePrefix("https://")
            .removePrefix("www.")
            .removeSuffix("/")
            .split("/").first()
            .split("?").first()
    }

    fun getParentDomain(domain: String): String {
        val parts = domain.split(".")
        if (parts.size <= 2) return domain
        return parts.takeLast(2).joinToString(".")
    }

    fun isBlocked(url: String): Boolean {
        val normalized = normalizeDomain(url)
        if (normalized in builtinDomains) return true
        if (normalized in customDomains) return true

        val parent = getParentDomain(normalized)
        if (parent in builtinDomains) return true
        if (parent in customDomains) return true

        for (domain in builtinDomains) {
            if (normalized.endsWith(".$domain")) return true
        }
        for (domain in customDomains) {
            if (normalized.endsWith(".$domain")) return true
        }

        return false
    }

    fun addCustomDomain(domain: String) {
        customDomains.add(normalizeDomain(domain))
    }

    fun removeCustomDomain(domain: String) {
        customDomains.remove(normalizeDomain(domain))
    }

    fun getCustomDomains(): Set<String> = customDomains.toSet()

    fun getBuiltinDomains(): Set<String> = builtinDomains.toSet()

    fun setCustomDomains(domains: Set<String>) {
        customDomains.clear()
        customDomains.addAll(domains.map { normalizeDomain(it) })
    }

    fun isAdultDomain(url: String): Boolean {
        return isBlocked(url)
    }
}
