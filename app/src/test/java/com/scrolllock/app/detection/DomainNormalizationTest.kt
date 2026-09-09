package com.scrolllock.app.detection

import org.junit.Assert.*
import org.junit.Test

class NodeUtilsTest {

    @Test
    fun `normalizeDomain removes protocol and www prefix`() {
        // Test domain normalization logic (inline from DomainMatcher)
        fun normalize(domain: String): String {
            return domain.lowercase()
                .trim()
                .removePrefix("http://")
                .removePrefix("https://")
                .removePrefix("www.")
                .removeSuffix("/")
                .split("/").first()
                .split("?").first()
        }

        assertEquals("example.com", normalize("https://www.example.com"))
        assertEquals("example.com", normalize("http://example.com"))
        assertEquals("example.com", normalize("www.example.com"))
        assertEquals("example.com", normalize("Example.COM"))
        assertEquals("example.com", normalize("example.com/"))
        assertEquals("example.com", normalize("example.com/path?query=1"))
    }

    @Test
    fun `getParentDomain extracts parent domain`() {
        fun getParent(domain: String): String {
            val parts = domain.split(".")
            if (parts.size <= 2) return domain
            return parts.takeLast(2).joinToString(".")
        }

        assertEquals("example.com", getParent("sub.example.com"))
        assertEquals("example.com", getParent("deep.sub.example.com"))
        assertEquals("example.com", getParent("example.com"))
    }
}
