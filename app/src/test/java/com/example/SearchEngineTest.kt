package com.example

import com.example.search.SearchEngine
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SearchEngineTest {
    
    @Test fun `empty query returns null`() {
        assertNull(SearchEngine.resolveUrl("", "https://google.com/search?q="))
        assertNull(SearchEngine.resolveUrl("   ", "https://google.com/search?q="))
    }
    
    @Test fun `plain word becomes a search`() {
        val url = SearchEngine.resolveUrl("hello", "https://google.com/search?q=")
        assertEquals("https://google.com/search?q=hello", url)
    }
    
    @Test fun `multi-word becomes a search with encoding`() {
        val url = SearchEngine.resolveUrl("hello world", "https://google.com/search?q=")
        assertEquals("https://google.com/search?q=hello+world", url)
    }
    
    @Test fun `domain gets https prefix`() {
        val url = SearchEngine.resolveUrl("github.com", "https://google.com/search?q=")
        assertEquals("https://github.com", url)
    }
    
    @Test fun `full url is preserved`() {
        val url = SearchEngine.resolveUrl("https://example.com", "https://google.com/search?q=")
        assertEquals("https://example.com", url)
    }
    
    @Test fun `localhost gets https prefix`() {
        val url = SearchEngine.resolveUrl("localhost:3000", "https://google.com/search?q=")
        assertEquals("https://localhost:3000", url)
    }
    
    @Test fun `valid url passes validation`() {
        assertTrue(SearchEngine.isValidUrl("https://github.com"))
        assertTrue(SearchEngine.isValidUrl("http://example.com"))
        assertTrue(SearchEngine.isValidUrl("omni://home"))
    }
    
    @Test fun `invalid url fails validation`() {
        assertFalse(SearchEngine.isValidUrl("not a url"))
        assertFalse(SearchEngine.isValidUrl(""))
        assertFalse(SearchEngine.isValidUrl("javascript:alert(1)"))  // security
    }
}
