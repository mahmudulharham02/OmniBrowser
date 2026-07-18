package com.example.browser.engine

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.prefs.SettingsDataStore
import com.example.browser.tab.TabManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

enum class BlockCategory { AD, TRACKER, POPUP, MALWARE }

data class AdBlockStats(val adsBlocked: Int, val trackersBlocked: Int)

class AdBlockEngine(private val settingsDataStore: SettingsDataStore) {

    private val networkBlocklist = mutableSetOf<String>()
    private val networkPatterns = mutableListOf<Regex>()
    
    private val _statsFlow = MutableStateFlow(AdBlockStats(0, 0))
    val statsFlow: StateFlow<AdBlockStats> = _statsFlow.asStateFlow()
    
    // In-memory cosmetic selectors
    private val cosmeticBlocklist = listOf(
        "*" to "div[class*=\"ad-container\"]",
        "*" to "div[class*=\"ad-wrapper\"]",
        "*" to "div[id*=\"ad-banner\"]",
        "*" to "ins.adsbygoogle",
        "*" to "div.ad-box",
        "*" to "div.ad-wrapper",
        "*" to "div[class*=\"-ad-\"]",
        "*" to "div[id*=\"-ad-\"]"
    )

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    init {
        // Initialize with core fast-block fallback ad networks in case file isn't loaded yet
        val fallbackDomains = listOf(
            "doubleclick.net", "googlesyndication.com", "googleadservices.com",
            "google-analytics.com", "googletagmanager.com", "googletagservices.com",
            "facebook.com/tr", "connect.facebook.net", "scorecardresearch.com",
            "quantserve.com", "adsystem.amazon.com", "adnxs.com", "rubiconproject.com",
            "pubmatic.com", "casalemedia.com", "openx.net", "criteo.com", "taboola.com",
            "outbrain.com", "adsrvr.org", "mathtag.com", "adsafeprotected.com",
            "moatads.com", "doubleverify.com"
        )
        networkBlocklist.addAll(fallbackDomains)
    }

    suspend fun loadBundled(context: Context) {
        try {
            val assetManager = context.assets
            val inputStream = assetManager.open("adblock/default.txt")
            val reader = BufferedReader(InputStreamReader(inputStream))
            var line: String? = reader.readLine()
            networkBlocklist.clear() // Clear fallback now that we are loading the full list
            while (line != null) {
                val trimmed = line.trim()
                if (trimmed.isNotEmpty() && !trimmed.startsWith("!") && !trimmed.startsWith("#")) {
                    if (trimmed.startsWith("/") && trimmed.endsWith("/")) {
                        // Regex pattern
                        try {
                            val regexStr = trimmed.substring(1, trimmed.length - 1)
                            networkPatterns.add(Regex(regexStr, RegexOption.IGNORE_CASE))
                        } catch (e: Exception) {
                            // Invalid regex
                        }
                    } else {
                        // Plain host/domain match
                        networkBlocklist.add(trimmed.lowercase())
                    }
                }
                line = reader.readLine()
            }
            reader.close()
            Log.d("AdBlock", "Loaded ${networkBlocklist.size} blocklist entries")
        } catch (e: Exception) {
            Log.e("AdBlock", "Failed to load bundled adblock list", e)
            e.printStackTrace()
        }
    }

    fun shouldBlock(url: String): Boolean {
        val uri = try { Uri.parse(url) } catch (e: Exception) { return false }
        val host = uri.host?.lowercase() ?: return false

        // Check exact match and parent domain matches (e.g. sub.doubleclick.net matches doubleclick.net)
        if (networkBlocklist.contains(host)) return true
        
        var tempHost = host
        while (tempHost.contains(".")) {
            val nextDot = tempHost.indexOf(".")
            tempHost = tempHost.substring(nextDot + 1)
            if (networkBlocklist.contains(tempHost)) {
                return true
            }
        }

        // Check patterns
        if (networkPatterns.any { it.containsMatchIn(url) }) return true

        // Keyword heuristic matching in path/query after host matching fails to avoid false positives
        val pathAndQuery = "${uri.path ?: ""}?${uri.query ?: ""}".lowercase()
        if (pathAndQuery.contains("ads?") || 
            pathAndQuery.contains("track") || 
            pathAndQuery.contains("pixel") || 
            pathAndQuery.contains("beacon") || 
            pathAndQuery.contains("analytics")) {
            return true
        }

        return false
    }

    fun isTracker(url: String): Boolean {
        val lowercaseUrl = url.lowercase()
        return lowercaseUrl.contains("tracker") ||
               lowercaseUrl.contains("telemetry") ||
               lowercaseUrl.contains("pixel") ||
               lowercaseUrl.contains("analytics") ||
               lowercaseUrl.contains("metrics") ||
               lowercaseUrl.contains("beacon")
    }

    fun cosmeticCssFor(url: String): String? {
        val uri = try { Uri.parse(url) } catch (e: Exception) { return null }
        val host = uri.host?.lowercase() ?: return null
        
        val selectors = cosmeticBlocklist
            .filter { it.first == "*" || host.contains(it.first) }
            .map { it.second }
            
        return if (selectors.isEmpty()) null
        else selectors.joinToString(", ") { it }.let { "$it { display: none !important; }" }
    }

    fun recordBlock(category: BlockCategory, tabId: String? = null, tabManager: TabManager? = null) {
        coroutineScope.launch {
            when (category) {
                BlockCategory.AD -> {
                    settingsDataStore.incrementAdsBlocked()
                    _statsFlow.update { it.copy(adsBlocked = it.adsBlocked + 1) }
                }
                BlockCategory.TRACKER -> {
                    settingsDataStore.incrementTrackersBlocked()
                    _statsFlow.update { it.copy(trackersBlocked = it.trackersBlocked + 1) }
                }
                BlockCategory.POPUP -> {
                    settingsDataStore.incrementPopupsBlocked()
                }
                BlockCategory.MALWARE -> {
                    settingsDataStore.incrementAdsBlocked()
                    _statsFlow.update { it.copy(adsBlocked = it.adsBlocked + 1) }
                }
            }
            if (tabId != null && tabManager != null) {
                tabManager.updateTab(tabId) { tab ->
                    tab.copy(blockedCount = tab.blockedCount + 1)
                }
            }
        }
    }
}
