package com.example.browser.extension

import android.net.Uri

class WebRequestBlockingEngine {
    private val blockedDomains = mutableSetOf<String>()

    fun registerBlockDomain(domain: String) {
        blockedDomains.add(domain.lowercase().trim())
    }

    fun unregisterBlockDomain(domain: String) {
        blockedDomains.remove(domain.lowercase().trim())
    }

    fun clearRules() {
        blockedDomains.clear()
    }

    fun shouldBlock(url: String): Boolean {
        val host = try { Uri.parse(url).host?.lowercase() } catch (e: Exception) { null } ?: return false
        
        if (blockedDomains.contains(host)) return true
        
        var tempHost = host
        while (tempHost.contains(".")) {
            val nextDot = tempHost.indexOf(".")
            tempHost = tempHost.substring(nextDot + 1)
            if (blockedDomains.contains(tempHost)) {
                return true
            }
        }
        return false
    }
}
