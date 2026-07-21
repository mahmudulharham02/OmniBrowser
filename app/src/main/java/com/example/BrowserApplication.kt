package com.example

import android.app.Application
import com.example.browser.engine.AdBlockEngine
import com.example.browser.engine.BrowserEngine
import com.example.browser.engine.DownloadHandler
import com.example.browser.engine.PopupBlocker
import com.example.browser.extension.ContentScriptInjector
import com.example.browser.extension.ExtensionHost
import com.example.browser.extension.MessageBus
import com.example.browser.extension.WebRequestBlockingEngine
import com.example.browser.tab.TabManager
import com.example.browser.tab.TabWebViewFactory
import com.example.data.db.AppDatabase
import com.example.data.prefs.SettingsDataStore
import com.example.data.repository.BookmarkRepository
import com.example.data.repository.CatalogRepository
import com.example.data.repository.DownloadRepository
import com.example.data.repository.ExtensionRepository
import com.example.data.repository.HistoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

interface AppContainer {
    val database: AppDatabase
    val settingsDataStore: SettingsDataStore
    val okHttpClient: OkHttpClient
    val historyRepository: HistoryRepository
    val bookmarkRepository: BookmarkRepository
    val downloadRepository: DownloadRepository
    val extensionRepository: ExtensionRepository
    val catalogRepository: CatalogRepository
    
    val adBlockEngine: AdBlockEngine
    val webRequestBlockingEngine: WebRequestBlockingEngine
    val contentScriptInjector: ContentScriptInjector
    val popupBlocker: PopupBlocker
    val downloadHandler: DownloadHandler
    
    val tabWebViewFactory: TabWebViewFactory
    val tabManager: TabManager
    val extensionHost: ExtensionHost
    val browserEngine: BrowserEngine
}

class BrowserApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()

        container = object : AppContainer {
            override val database: AppDatabase by lazy { AppDatabase.getDatabase(this@BrowserApplication) }
            override val settingsDataStore: SettingsDataStore by lazy { SettingsDataStore(this@BrowserApplication) }
            override val okHttpClient: OkHttpClient by lazy { 
                OkHttpClient.Builder()
                    .dns(com.example.data.prefs.AppDns(settingsDataStore))
                    .build()
            }
            
            override val historyRepository: HistoryRepository by lazy { HistoryRepository(database.historyDao()) }
            override val bookmarkRepository: BookmarkRepository by lazy { BookmarkRepository(database.bookmarkDao()) }
            override val downloadRepository: DownloadRepository by lazy { DownloadRepository(database.downloadDao()) }
            override val extensionRepository: ExtensionRepository by lazy {
                ExtensionRepository(database.installedExtensionDao(), database.extensionStorageDao())
            }
            override val catalogRepository: CatalogRepository by lazy {
                CatalogRepository(database.catalogIndexDao(), database.installedFromCatalogDao(), database.reportedExtensionDao(), okHttpClient)
            }
            
            override val adBlockEngine: AdBlockEngine by lazy { AdBlockEngine(settingsDataStore) }
            override val webRequestBlockingEngine: WebRequestBlockingEngine by lazy { WebRequestBlockingEngine() }
            
            override val tabWebViewFactory: TabWebViewFactory by lazy { TabWebViewFactory(this@BrowserApplication) }
            override val tabManager: TabManager by lazy { TabManager(tabWebViewFactory) }
            override val extensionHost: ExtensionHost by lazy { ExtensionHost(this@BrowserApplication, extensionRepository, MessageBus()) }
            override val contentScriptInjector: ContentScriptInjector by lazy { ContentScriptInjector(extensionHost) }
            override val popupBlocker: PopupBlocker by lazy { PopupBlocker(adBlockEngine) }
            override val downloadHandler: DownloadHandler by lazy { DownloadHandler(this@BrowserApplication, adBlockEngine, downloadRepository, settingsDataStore) }
            
            override val browserEngine: BrowserEngine by lazy {
                BrowserEngine(
                    context = this@BrowserApplication,
                    tabManager = tabManager,
                    adBlockEngine = adBlockEngine,
                    webRequestBlockingEngine = webRequestBlockingEngine,
                    contentScriptInjector = contentScriptInjector,
                    settingsDataStore = settingsDataStore,
                    historyRepository = historyRepository,
                    extensionRepository = extensionRepository,
                    messageBus = MessageBus(),
                    popupBlocker = popupBlocker,
                    downloadHandler = downloadHandler
                ).also { engine ->
                    tabManager.setBrowserEngine(engine)
                }
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val jsCacheDir = java.io.File(cacheDir, "WebView/Default/HTTP Cache/Code Cache/js")
                val wasmCacheDir = java.io.File(cacheDir, "WebView/Default/HTTP Cache/Code Cache/wasm")
                if (!jsCacheDir.exists()) jsCacheDir.mkdirs()
                if (!wasmCacheDir.exists()) wasmCacheDir.mkdirs()
            } catch (e: Exception) {
                // Ignore any cache directory pre-creation errors
            }

            container.adBlockEngine.loadBundled(this@BrowserApplication)
            try {
                container.extensionRepository.deleteExtension("omni-dark-mode")
            } catch (e: Exception) {
                // Ignore
            }
            container.extensionHost.start()
            // Pre-seed catalog cache
            container.catalogRepository.fetchIndex(this@BrowserApplication, "https://omnibrowser.app/extensions/index.json")
        }
    }
}
