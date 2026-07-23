package com.example.browser.tab

import android.graphics.Bitmap
import android.webkit.WebView
import com.example.data.TabModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class TabManager(private val webViewFactory: TabWebViewFactory) {

    private val _tabs = MutableStateFlow<List<TabModel>>(emptyList())
    val tabs: StateFlow<List<TabModel>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow<String?>(null)
    val activeTabId: StateFlow<String?> = _activeTabId.asStateFlow()

    private val activeWebViews = mutableMapOf<String, WebView>()
    private val thumbnails = mutableMapOf<String, Bitmap?>()
    val recentlyClosedTabs = mutableListOf<TabModel>()

    private var browserEngine: com.example.browser.engine.BrowserEngine? = null

    fun setBrowserEngine(engine: com.example.browser.engine.BrowserEngine) {
        browserEngine = engine
        // Set up any existing tabs
        _tabs.value.forEach { tab ->
            val wv = activeWebViews[tab.id]
            if (wv != null) {
                engine.setupWebView(wv, tab.id, tab.isPrivate)
            }
        }
    }

    fun createTab(url: String = "omni://home", isPrivate: Boolean = false): TabModel {
        val tabId = UUID.randomUUID().toString()
        val title = if (url == "omni://home") "New Tab" else url
        
        val newTab = TabModel(
            id = tabId,
            title = title,
            url = url,
            isPrivate = isPrivate,
            createdAt = System.currentTimeMillis()
        )

        _tabs.value = _tabs.value + newTab

        // Immediately obtain WebView and setup
        val currentWv = webViewFactory.obtainWebView()
        activeWebViews[tabId] = currentWv
        
        browserEngine?.let { engine ->
            engine.setupWebView(currentWv, tabId, isPrivate)
        }

        if (url != "omni://home") {
            currentWv.loadUrl(url)
        }

        if (_activeTabId.value == null) {
            selectTab(tabId)
        } else {
            currentWv.onPause()
            currentWv.pauseTimers()
        }
        return newTab
    }

    fun selectTab(id: String) {
        val previousActiveId = _activeTabId.value
        if (previousActiveId == id) return

        // 1. Pause previous active WebView
        if (previousActiveId != null) {
            val prevWv = activeWebViews[previousActiveId]
            if (prevWv != null) {
                // Capture thumbnail before moving away
                thumbnails[previousActiveId] = TabThumbnailer.capture(prevWv)
                prevWv.onPause()
                prevWv.pauseTimers()
            }
        }

        // 2. Switch ID
        _activeTabId.value = id

        // 3. Obtain or resume new active WebView
        val newTab = _tabs.value.find { it.id == id } ?: return
        var currentWv = activeWebViews[id]
        if (currentWv == null) {
            currentWv = webViewFactory.obtainWebView()
            activeWebViews[id] = currentWv
            browserEngine?.let { engine ->
                engine.setupWebView(currentWv, id, newTab.isPrivate)
            }
            // Load URL if it's not a local home page
            if (newTab.url != "omni://home") {
                currentWv.loadUrl(newTab.url)
            }
        } else {
            currentWv.resumeTimers()
            currentWv.onResume()
        }
    }

    fun closeTab(id: String) {
        val tabToClose = _tabs.value.find { it.id == id } ?: return
        
        // Keep in recently closed tabs (max 10) if not private
        if (!tabToClose.isPrivate) {
            recentlyClosedTabs.add(0, tabToClose)
            if (recentlyClosedTabs.size > 10) {
                recentlyClosedTabs.removeAt(recentlyClosedTabs.lastIndex)
            }
        } else {
            // Private tab - clear cookies upon closing
            android.webkit.CookieManager.getInstance().removeAllCookies {
                // Cookies cleared
            }
        }

        // Recycle WebView
        val wv = activeWebViews.remove(id)
        if (wv != null) {
            webViewFactory.recycle(wv)
        }
        
        // Remove thumbnail
        thumbnails.remove(id)?.recycle()

        val listAfterClose = _tabs.value.filter { it.id != id }
        _tabs.value = listAfterClose
        if (listAfterClose.isEmpty()) {
            _activeTabId.value = null
        } else if (_activeTabId.value == id) {
            selectTab(listAfterClose.last().id)
        }
    }

    fun restoreLastClosedTab(): TabModel? {
        if (recentlyClosedTabs.isEmpty()) return null
        val restored = recentlyClosedTabs.removeAt(0)
        val newTab = createTab(restored.url, restored.isPrivate)
        updateTab(newTab.id) {
            it.copy(title = restored.title, faviconUrl = restored.faviconUrl)
        }
        selectTab(newTab.id)
        return newTab
    }

    fun closeAllIncognitoTabs() {
        val incognitoTabs = _tabs.value.filter { it.isPrivate }
        if (incognitoTabs.isEmpty()) return

        incognitoTabs.forEach { tab ->
            val wv = activeWebViews.remove(tab.id)
            if (wv != null) {
                webViewFactory.recycle(wv)
            }
            thumbnails.remove(tab.id)?.recycle()
        }

        val remaining = _tabs.value.filter { !it.isPrivate }
        _tabs.value = remaining

        val activeId = _activeTabId.value
        if (activeId == null || incognitoTabs.any { it.id == activeId }) {
            if (remaining.isNotEmpty()) {
                selectTab(remaining.last().id)
            } else {
                _activeTabId.value = null
            }
        }

        try {
            android.webkit.CookieManager.getInstance().flush()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun closeAllRegularTabs() {
        val regularTabs = _tabs.value.filter { !it.isPrivate }
        if (regularTabs.isEmpty()) return

        regularTabs.forEach { tab ->
            val wv = activeWebViews.remove(tab.id)
            if (wv != null) {
                webViewFactory.recycle(wv)
            }
            thumbnails.remove(tab.id)?.recycle()
        }

        val remaining = _tabs.value.filter { it.isPrivate }
        _tabs.value = remaining

        val activeId = _activeTabId.value
        if (activeId == null || regularTabs.any { it.id == activeId }) {
            if (remaining.isNotEmpty()) {
                selectTab(remaining.last().id)
            } else {
                _activeTabId.value = null
            }
        }
    }

    fun closeAllTabs(addPlaceholder: Boolean = false) {
        // Recycle all active WebViews
        activeWebViews.forEach { (_, wv) ->
            webViewFactory.recycle(wv)
        }
        activeWebViews.clear()
        
        // Recycle thumbnails
        thumbnails.forEach { (_, bmp) -> bmp?.recycle() }
        thumbnails.clear()

        _tabs.value = emptyList()
        _activeTabId.value = null

        if (addPlaceholder) {
            createTab("omni://home")
        }
    }

    fun getWebViewForTab(id: String): WebView? {
        return activeWebViews[id]
    }

    fun getThumbnail(id: String): Bitmap? {
        return thumbnails[id]
    }

    fun getActiveWebViews(): Map<String, android.webkit.WebView> {
        return activeWebViews
    }

    fun updateTab(id: String, transform: (TabModel) -> TabModel) {
        _tabs.value = _tabs.value.map {
            if (it.id == id) {
                val updated = transform(it)
                // If url changes, keep activeWebViews in sync if URL matches
                updated
            } else {
                it
            }
        }
    }

    fun captureActiveTabThumbnail() {
        val activeId = _activeTabId.value ?: return
        val wv = activeWebViews[activeId] ?: return
        thumbnails[activeId] = TabThumbnailer.capture(wv)
    }
}
