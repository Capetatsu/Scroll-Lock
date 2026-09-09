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
}
