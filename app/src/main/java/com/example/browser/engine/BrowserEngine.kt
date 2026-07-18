package com.example.browser.engine

import android.content.Context
import android.view.MotionEvent
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import com.example.browser.extension.BridgeInterface
import com.example.browser.extension.ContentScriptInjector
import com.example.browser.extension.WebRequestBlockingEngine
import com.example.browser.tab.TabManager
import com.example.data.repository.ExtensionRepository
import com.example.data.repository.HistoryRepository
import com.example.data.prefs.SettingsDataStore
import com.example.browser.extension.MessageBus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class BrowserEngine(
    private val context: Context,
    private val tabManager: TabManager,
    private val adBlockEngine: AdBlockEngine,
    private val webRequestBlockingEngine: WebRequestBlockingEngine,
    private val contentScriptInjector: ContentScriptInjector,
    private val settingsDataStore: SettingsDataStore,
    private val historyRepository: HistoryRepository,
    private val extensionRepository: ExtensionRepository,
    private val messageBus: MessageBus,
    private val popupBlocker: PopupBlocker,
    val downloadHandler: DownloadHandler
) {

    fun setupWebView(webView: WebView, tabId: String, isPrivate: Boolean) {
        val tab = tabManager.tabs.value.find { it.id == tabId } ?: return

        // 1. Get current settings
        val jsEnabled = runBlocking { settingsDataStore.javascriptEnabled.first() }
        val desktopModeDefault = runBlocking { settingsDataStore.desktopModeDefault.first() }
        val fontSizePercent = runBlocking { settingsDataStore.fontSize.first() }
        val blockThirdParty = runBlocking { settingsDataStore.blockThirdPartyCookies.first() }

        // 2. Configure WebSettings
        webView.settings.apply {
            javaScriptEnabled = jsEnabled
            textZoom = fontSizePercent
            
            // Set User Agent based on desktop mode
            userAgentString = if (tab.desktopMode || desktopModeDefault) {
                UserAgentProvider.DESKTOP_USER_AGENT
            } else {
                UserAgentProvider.MOBILE_USER_AGENT
            }

            // Private tab configs
            if (isPrivate) {
                cacheMode = WebSettings.LOAD_NO_CACHE
                databaseEnabled = false
                domStorageEnabled = false
            } else {
                cacheMode = WebSettings.LOAD_DEFAULT
                databaseEnabled = true
                domStorageEnabled = true
            }

            // Standard options
            useWideViewPort = true
            loadWithOverviewMode = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false

            // Native Force Dark
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val darkMode = runBlocking { settingsDataStore.darkModeMode() }
                val forceDarkMode = when (darkMode) {
                    "on" -> WebSettings.FORCE_DARK_ON
                    "off" -> WebSettings.FORCE_DARK_OFF
                    else -> WebSettings.FORCE_DARK_AUTO
                }
                forceDark = forceDarkMode
            }
        }

        // 3. Configure Cookie Isolation
        val cookieManager = CookieManager.getInstance()
        if (isPrivate) {
            cookieManager.setAcceptCookie(false)
        } else {
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(webView, !blockThirdParty)
        }

        // 4. Attach Touch Listener for user gesture validation (popup blocker)
        webView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                popupBlocker.onUserGesture()
            }
            false
        }

        // 5. Attach Clients
        webView.webViewClient = BrowserWebViewClient(
            tabId = tabId,
            tabManager = tabManager,
            adBlockEngine = adBlockEngine,
            webRequestBlockingEngine = webRequestBlockingEngine,
            contentScriptInjector = contentScriptInjector,
            settingsDataStore = settingsDataStore,
            historyRepository = historyRepository
        )

        webView.webChromeClient = BrowserChromeClient(
            tabId = tabId,
            tabManager = tabManager,
            popupBlocker = popupBlocker,
            settingsDataStore = settingsDataStore
        )

        // 6. Attach Download Listener
        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
            downloadHandler.onDownloadStart(
                url = url,
                userAgent = userAgent,
                contentDisposition = contentDisposition,
                mimeType = mimeType,
                contentLength = contentLength,
                refererUrl = webView.url
            )
        }

        // 7. Inject Extension Bridge Interface
        val extensionsEnabled = runBlocking { settingsDataStore.extensionsEnabled.first() }
        if (extensionsEnabled) {
            webView.addJavascriptInterface(
                BridgeInterface(
                    extensionId = "webpage",
                    extensionRepository = extensionRepository,
                    messageBus = messageBus,
                    getWebView = { webView }
                ),
                "OmniExtensionBridge"
            )
        }
    }
}
