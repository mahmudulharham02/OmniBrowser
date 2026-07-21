package com.example.search

import android.util.Log

/**
 * ⚠️ CRITICAL CORE ENGINE — DO NOT BREAK THE SEARCH RESOLUTION ⚠️
 *
 * This object owns the URL/Query parsing and resolution logic.
 * Always run SearchEngineTest after making any changes.
 */
object SearchEngine {
    
    private const val TAG = "OmniSearch"
    
    /**
     * Converts a user query into a navigable URL.
     * - If it looks like a URL (has a dot, no spaces, has a TLD), prepend https://
     * - Otherwise, perform a search using the configured search engine
     */
    fun resolveUrl(query: String, searchEngineUrl: String): String? {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            Log.d(TAG, "Empty query, ignoring")
            return null
        }
        
        // Already a full URL or special internal URL?
        if (trimmed.startsWith("http://", ignoreCase = true) || 
            trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("omni://", ignoreCase = true)) {
            return trimmed
        }
        
        // Looks like a domain, localhost, or port (no spaces)?
        val isLocalhost = trimmed.startsWith("localhost", ignoreCase = true)
        val isDomainOrPort = (trimmed.contains(".") || (trimmed.contains(":") && trimmed.substringAfter(":").toIntOrNull() != null)) && !trimmed.contains(" ")
        
        if ((isLocalhost || isDomainOrPort) && !trimmed.contains(" ")) {
            val withScheme = if (trimmed.contains("://")) trimmed else "https://$trimmed"
            Log.d(TAG, "Treated as URL: $withScheme")
            return withScheme
        }
        
        // Otherwise, search
        val encoded = try {
            java.net.URLEncoder.encode(trimmed, "UTF-8")
        } catch (e: Exception) {
            trimmed
        }
        val url = if (searchEngineUrl.contains("{query}")) {
            searchEngineUrl.replace("{query}", encoded)
        } else {
            searchEngineUrl + encoded
        }
        Log.d(TAG, "Searching: $url")
        return url
    }
    
    /**
     * Validates a URL before navigation. Catches obvious errors.
     */
    fun isValidUrl(url: String): Boolean {
        if (url.trim().startsWith("omni://")) return true
        return runCatching {
            val parsed = java.net.URL(url)
            parsed.host.isNotBlank() && (parsed.protocol == "http" || parsed.protocol == "https")
        }.getOrDefault(false)
    }
}
