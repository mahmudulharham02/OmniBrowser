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
                saveFormData = false
            } else {
                cacheMode = WebSettings.LOAD_DEFAULT
                databaseEnabled = true
                domStorageEnabled = true
                saveFormData = true
            }

            // Standard options
            useWideViewPort = true
            loadWithOverviewMode = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false

            // Native Force Dark / Smart Dark Mode
            updateWebViewDarkMode(webView)
        }

        // 3. Configure Cookie Isolation
        val cookieManager = CookieManager.getInstance()
        if (isPrivate) {
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(webView, false)
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
                refererUrl = webView.url,
                isPrivate = isPrivate
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

    fun updateWebViewDarkMode(webView: WebView) {
        val darkMode = runBlocking { settingsDataStore.darkModeMode() }
        val isSystemDark = context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val isDarkTheme = when (darkMode) {
            "dark" -> true
            "light" -> false
            else -> isSystemDark
        }

        if (isDarkTheme) {
            webView.setBackgroundColor(android.graphics.Color.parseColor("#121212"))
        } else {
            webView.setBackgroundColor(android.graphics.Color.WHITE)
        }

        webView.settings.apply {
            if (isDarkTheme) {
                if (androidx.webkit.WebViewFeature.isFeatureSupported(androidx.webkit.WebViewFeature.FORCE_DARK)) {
                    androidx.webkit.WebSettingsCompat.setForceDark(
                        this,
                        androidx.webkit.WebSettingsCompat.FORCE_DARK_ON
                    )
                } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    @Suppress("DEPRECATION")
                    forceDark = android.webkit.WebSettings.FORCE_DARK_ON
                }

                if (androidx.webkit.WebViewFeature.isFeatureSupported(androidx.webkit.WebViewFeature.FORCE_DARK_STRATEGY)) {
                    androidx.webkit.WebSettingsCompat.setForceDarkStrategy(
                        this,
                        2 // WebSettingsCompat.DARK_STRATEGY_PREFER_WEB_THEME_OVER_USER_AGENT_DARKENING
                    )
                }

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    isAlgorithmicDarkeningAllowed = true
                }
            } else {
                if (androidx.webkit.WebViewFeature.isFeatureSupported(androidx.webkit.WebViewFeature.FORCE_DARK)) {
                    androidx.webkit.WebSettingsCompat.setForceDark(
                        this,
                        androidx.webkit.WebSettingsCompat.FORCE_DARK_OFF
                    )
                } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    @Suppress("DEPRECATION")
                    forceDark = android.webkit.WebSettings.FORCE_DARK_OFF
                }

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    isAlgorithmicDarkeningAllowed = false
                }
            }
        }
    }
}
