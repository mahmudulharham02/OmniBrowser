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

        if (_activeTabId.value == null) {
            selectTab(tabId)
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
        
        // Keep in recently closed tabs (max 10)
        recentlyClosedTabs.add(0, tabToClose)
        if (recentlyClosedTabs.size > 10) {
            recentlyClosedTabs.removeAt(recentlyClosedTabs.lastIndex)
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

        // Handle active ID changes
        if (_activeTabId.value == id) {
            if (listAfterClose.isNotEmpty()) {
                selectTab(listAfterClose.last().id)
            } else {
                _activeTabId.value = null
            }
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

    fun closeAllTabs() {
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
    }

    fun getWebViewForTab(id: String): WebView? {
        return activeWebViews[id]
    }

    fun getThumbnail(id: String): Bitmap? {
        return thumbnails[id]
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
