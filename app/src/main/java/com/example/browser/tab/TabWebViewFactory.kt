package com.example.browser.tab

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView

class TabWebViewFactory(private val context: Context) {
    private val pool = mutableListOf<WebView>()

    fun obtainWebView(): WebView {
        if (pool.isNotEmpty()) {
            val wv = pool.removeAt(0)
            wv.resumeTimers()
            wv.onResume()
            return wv
        }
        return createNewWebView()
    }

    fun recycle(webView: WebView) {
        try {
            webView.stopLoading()
            webView.onPause()
            webView.pauseTimers()
            webView.loadUrl("about:blank")
            webView.clearHistory()
            pool.add(webView)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun createNewWebView(): WebView {
        return WebView(context).apply {
            // Apply typical performance optimization settings
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.setSupportMultipleWindows(true)
            settings.javaScriptCanOpenWindowsAutomatically = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            
            // Safe Browsing
            settings.safeBrowsingEnabled = true

            // Cookies settings
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(this, false) // Default block third-party cookies
        }
    }
}
