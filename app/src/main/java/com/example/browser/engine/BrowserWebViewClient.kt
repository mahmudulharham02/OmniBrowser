package com.example.browser.engine

import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.browser.extension.ContentScriptInjector
import com.example.browser.extension.WebRequestBlockingEngine
import com.example.browser.tab.TabManager
import com.example.data.repository.HistoryRepository
import com.example.data.prefs.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayInputStream

class BrowserWebViewClient(
    private val tabId: String,
    private val tabManager: TabManager,
    private val adBlockEngine: AdBlockEngine,
    private val webRequestBlockingEngine: WebRequestBlockingEngine,
    private val contentScriptInjector: ContentScriptInjector,
    private val settingsDataStore: SettingsDataStore,
    private val historyRepository: HistoryRepository
) : WebViewClient() {

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val url = request.url.toString()
        
        // Handle custom URL schemes
        if (url.startsWith("omni://")) {
            tabManager.updateTab(tabId) { it.copy(url = url, title = "OmniBrowser") }
            return true
        }
        
        // Upgrade to HTTPS if enabled
        val httpsOnly = runBlocking { settingsDataStore.httpsOnly.first() }
        if (httpsOnly && url.startsWith("http://")) {
            val upgraded = HttpsOnlyEnforcer.upgradeUrl(url)
            view.loadUrl(upgraded)
            return true
        }

        return false
    }

    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
        val url = request.url.toString()
        
        // Standard Android file loads or assets should not be intercepted
        if (url.startsWith("file://") || url.startsWith("data:")) return null

        // 1. Check dynamic extension webRequest block rules
        if (webRequestBlockingEngine.shouldBlock(url)) {
            adBlockEngine.recordBlock(BlockCategory.AD, tabId, tabManager)
            return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
        }

        // 2. Check static adblock list
        val adBlockEnabled = runBlocking { settingsDataStore.adBlockEnabled.first() }
        if (adBlockEnabled && adBlockEngine.shouldBlock(url)) {
            val category = if (adBlockEngine.isTracker(url)) BlockCategory.TRACKER else BlockCategory.AD
            adBlockEngine.recordBlock(category, tabId, tabManager)
            return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
        }

        // 3. Handle http -> https upgrades for subresources
        val httpsOnly = runBlocking { settingsDataStore.httpsOnly.first() }
        if (httpsOnly && request.url.scheme == "http") {
            // If main frame load and cannot be upgraded, we should show warning.
            // For now, subresource upgrade is safe.
        }

        return null
    }

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        
        // Update loading state
        tabManager.updateTab(tabId) {
            it.copy(
                url = url,
                isLoading = true,
                progress = 10,
                canGoBack = view.canGoBack(),
                canGoForward = view.canGoForward()
            )
        }

        // 1. Apply cosmetic ad blocking CSS immediately
        val adBlockEnabled = runBlocking { settingsDataStore.adBlockEnabled.first() }
        if (adBlockEnabled) {
            val cosmeticCss = adBlockEngine.cosmeticCssFor(url)
            if (cosmeticCss != null) {
                val cosmeticJs = """
                    (function() {
                        var style = document.createElement('style');
                        style.type = 'text/css';
                        style.textContent = "$cosmeticCss";
                        (document.head || document.documentElement).appendChild(style);
                    })();
                """.trimIndent()
                view.evaluateJavascript(cosmeticJs, null)
            }
        }

        // 2. Inject content scripts running at document_start
        val extensionsEnabled = runBlocking { settingsDataStore.extensionsEnabled.first() }
        if (extensionsEnabled) {
            contentScriptInjector.injectForPage(view, url, "document_start")
        }
    }

    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)
        
        // Update loading state
        tabManager.updateTab(tabId) {
            it.copy(
                url = url,
                title = view.title ?: url,
                isLoading = false,
                progress = 100,
                canGoBack = view.canGoBack(),
                canGoForward = view.canGoForward()
            )
        }

        // Inject content scripts running at document_end & document_idle
        val extensionsEnabled = runBlocking { settingsDataStore.extensionsEnabled.first() }
        if (extensionsEnabled) {
            contentScriptInjector.injectForPage(view, url, "document_end")
            contentScriptInjector.injectForPage(view, url, "document_idle")
        }

        // Persist navigation in history if not private
        val currentTab = tabManager.tabs.value.find { it.id == tabId }
        if (currentTab != null && !currentTab.isPrivate) {
            coroutineScope.launch {
                historyRepository.addVisit(url, view.title ?: url)
            }
        }
    }
}
