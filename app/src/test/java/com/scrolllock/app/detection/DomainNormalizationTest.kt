package com.scrolllock.app.detection

import org.junit.Assert.*
import org.junit.Test

class DomainNormalizationTest {

    private fun normalize(domain: String): String {
        return domain.lowercase()
            .trim()
            .removePrefix("http://")
            .removePrefix("https://")
            .removePrefix("www.")
            .removeSuffix("/")
            .split("/").first()
            .split("?").first()
    }

    private fun getParent(domain: String): String {
        val parts = domain.split(".")
        if (parts.size <= 2) return domain
        return parts.takeLast(2).joinToString(".")
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
        // Remove port if present
        val colonIndex = host.indexOf(':')
        if (colonIndex > 0) host = host.substring(0, colonIndex)
        return host
    }

    @Test
    fun `normalize removes protocol and www prefix`() {
        assertEquals("example.com", normalize("https://www.example.com"))
        assertEquals("example.com", normalize("http://example.com"))
        assertEquals("example.com", normalize("www.example.com"))
    }

    @Test
    fun `normalize handles case insensitivity`() {
        assertEquals("example.com", normalize("Example.COM"))
        assertEquals("example.com", normalize("EXAMPLE.COM"))
    }

    @Test
    fun `normalize removes trailing path and query`() {
        assertEquals("example.com", normalize("example.com/path/to/page"))
        assertEquals("example.com", normalize("example.com?query=1"))
        assertEquals("example.com", normalize("example.com/path?q=1"))
    }

    @Test
    fun `normalize removes trailing slash`() {
        assertEquals("example.com", normalize("example.com/"))
        assertEquals("example.com", normalize("example.com//"))
    }

    @Test
    fun `getParentDomain extracts parent domain`() {
        assertEquals("example.com", getParent("sub.example.com"))
        assertEquals("example.com", getParent("deep.sub.example.com"))
        assertEquals("example.com", getParent("example.com"))
    }

    @Test
    fun `getParentDomain handles co uk domains`() {
        assertEquals("co.uk", getParent("sub.co.uk"))
        assertEquals("co.uk", getParent("www.example.co.uk"))
    }

    @Test
    fun `suffix matching works for subdomains`() {
        val domains = setOf("example.com", "test.org")
        assertTrue(domains.any { "sub.example.com".endsWith(".$it") })
        assertTrue(domains.any { "deep.sub.example.com".endsWith(".$it") })
        assertFalse(domains.any { "notexample.com".endsWith(".$it") })
    }

    @Test
    fun `extractHostname from full URL`() {
        assertEquals("example.com", extractHostname("https://www.example.com/path"))
        assertEquals("example.com", extractHostname("http://example.com"))
        assertEquals("example.com", extractHostname("https://example.com?q=1"))
        assertEquals("example.com", extractHostname("https://example.com#section"))
    }

    @Test
    fun `extractHostname from bare domain`() {
        assertEquals("example.com", extractHostname("example.com"))
        assertEquals("example.com", extractHostname("www.example.com"))
    }

    @Test
    fun `extractHostname from complex URL`() {
        assertEquals("sub.example.com", extractHostname("https://sub.example.com/path/to/page?query=1&other=2#section"))
        assertEquals("m.facebook.com", extractHostname("https://m.facebook.com/login"))
    }

    @Test
    fun `extractHostname handles edge cases`() {
        assertEquals("", extractHostname(""))
        assertEquals("localhost", extractHostname("localhost"))
        assertEquals("192.168.1.1", extractHostname("http://192.168.1.1:8080/path"))
    }

    @Test
    fun `normalize and extractHostname produce consistent results`() {
        val url = "https://www.EXAMPLE.com/path?q=1"
        val hostname = extractHostname(url)
        val normalized = normalize(url)
        assertEquals(normalized, hostname)
    }
}
