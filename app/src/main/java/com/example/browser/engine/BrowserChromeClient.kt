package com.example.browser.engine

import android.graphics.Bitmap
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import com.example.browser.tab.TabManager
import com.example.data.prefs.SettingsDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class BrowserChromeClient(
    private val tabId: String,
    private val tabManager: TabManager,
    private val popupBlocker: PopupBlocker,
    private val settingsDataStore: SettingsDataStore
) : WebChromeClient() {

    override fun onProgressChanged(view: WebView, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        tabManager.updateTab(tabId) {
            it.copy(
                progress = newProgress,
                isLoading = newProgress < 100
            )
        }
    }

    override fun onReceivedTitle(view: WebView, title: String) {
        super.onReceivedTitle(view, title)
        tabManager.updateTab(tabId) {
            it.copy(title = title)
        }
    }

    override fun onReceivedIcon(view: WebView, icon: Bitmap) {
        super.onReceivedIcon(view, icon)
        // Store or update favicon URL state if needed, or simply let the tab hold a local ref if desired.
    }

    override fun onCreateWindow(
        view: WebView,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: android.os.Message
    ): Boolean {
        val blockPopups = runBlocking { settingsDataStore.blockPopups.first() }
        if (blockPopups && !popupBlocker.shouldAllowPopup(isUserGesture)) {
            popupBlocker.onPopupBlocked()
            return false
        }

        // Open in a new tab!
        val parentTab = tabManager.tabs.value.find { it.id == tabId }
        val isPrivate = parentTab?.isPrivate == true
        val newTab = tabManager.createTab(url = "omni://home", isPrivate = isPrivate)
        tabManager.selectTab(newTab.id)
        
        val newWebView = tabManager.getWebViewForTab(newTab.id) ?: return false
        val transport = resultMsg.obj as WebView.WebViewTransport
        transport.webView = newWebView
        resultMsg.sendToTarget()
        return true
    }
}
