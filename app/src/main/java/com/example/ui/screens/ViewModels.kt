package com.example.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.AppContainer
import com.example.data.*
import com.example.data.prefs.SettingsDataStore
import com.example.data.repository.*
import com.example.browser.tab.TabManager
import com.example.browser.extension.ExtensionHost
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BrowserViewModel(
    val tabManager: TabManager,
    private val settingsDataStore: SettingsDataStore,
    private val historyRepository: HistoryRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val downloadRepository: DownloadRepository,
    private val extensionRepository: ExtensionRepository
) : ViewModel() {

    val tabs = tabManager.tabs
    val activeTabId = tabManager.activeTabId

    val searchEngineUrl = settingsDataStore.searchEngineUrl.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "https://www.google.com/search?q=")
    val homePage = settingsDataStore.homePage.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "omni://home")
    val adBlockEnabled = settingsDataStore.adBlockEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val customDnsUrl = settingsDataStore.customDnsUrl.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    
    // Total blocks tracking
    val totalAdsBlocked = settingsDataStore.totalAdsBlocked.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val totalTrackersBlocked = settingsDataStore.totalTrackersBlocked.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val totalPopupsBlocked = settingsDataStore.totalPopupsBlocked.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val currentTab = combine(tabs, activeTabId) { tabsList, activeId ->
        tabsList.find { it.id == activeId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun createNewTab(url: String = "omni://home", isPrivate: Boolean = false) {
        tabManager.createTab(url, isPrivate)
    }

    fun selectTab(id: String) {
        tabManager.selectTab(id)
    }

    fun closeTab(id: String) {
        tabManager.closeTab(id)
    }

    fun closeAllTabs() {
        tabManager.closeAllTabs()
    }

    fun restoreLastClosedTab() {
        tabManager.restoreLastClosedTab()
    }

    fun loadUrlInActiveTab(url: String) {
        // Check if query or URL
        var destination = url.trim()
        if (!destination.contains(".") && !destination.startsWith("omni://") && !destination.contains("://")) {
            // Search query - URL encode the query
            try {
                val encodedQuery = java.net.URLEncoder.encode(destination, "UTF-8")
                destination = searchEngineUrl.value + encodedQuery
            } catch (e: Exception) {
                destination = searchEngineUrl.value + destination
            }
        } else if (!destination.contains("://")) {
            destination = "https://" + destination
        }

        val activeId = activeTabId.value
        if (activeId == null) {
            createNewTab(destination)
        } else {
            tabManager.updateTab(activeId) {
                it.copy(url = destination, title = "Loading...")
            }

            val wv = tabManager.getWebViewForTab(activeId)
            wv?.post {
                wv.loadUrl(destination)
            }
        }
    }

    fun reloadActiveTab() {
        val activeId = activeTabId.value ?: return
        val wv = tabManager.getWebViewForTab(activeId)
        wv?.post { wv.reload() }
    }

    fun stopLoadingActiveTab() {
        val activeId = activeTabId.value ?: return
        val wv = tabManager.getWebViewForTab(activeId)
        wv?.post { wv.stopLoading() }
    }

    fun goBackInActiveTab() {
        val activeId = activeTabId.value ?: return
        val wv = tabManager.getWebViewForTab(activeId)
        wv?.post {
            if (wv.canGoBack()) wv.goBack()
        }
    }

    fun goForwardInActiveTab() {
        val activeId = activeTabId.value ?: return
        val wv = tabManager.getWebViewForTab(activeId)
        wv?.post {
            if (wv.canGoForward()) wv.goForward()
        }
    }

    fun toggleDesktopModeForActiveTab() {
        val activeId = activeTabId.value ?: return
        val tab = tabs.value.find { it.id == activeId } ?: return
        val nextMode = !tab.desktopMode
        
        tabManager.updateTab(activeId) {
            it.copy(desktopMode = nextMode)
        }

        val wv = tabManager.getWebViewForTab(activeId) ?: return
        wv.post {
            wv.settings.userAgentString = if (nextMode) {
                com.example.browser.engine.UserAgentProvider.DESKTOP_USER_AGENT
            } else {
                com.example.browser.engine.UserAgentProvider.MOBILE_USER_AGENT
            }
            wv.reload()
        }
    }

    fun addBookmarkForActiveTab() {
        val tab = currentTab.value ?: return
        viewModelScope.launch {
            bookmarkRepository.addBookmark(tab.url, tab.title)
        }
    }
}

class BookmarksViewModel(private val bookmarkRepository: BookmarkRepository) : ViewModel() {
    val bookmarks = bookmarkRepository.allBookmarks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addBookmark(url: String, title: String) {
        viewModelScope.launch {
            bookmarkRepository.addBookmark(url, title)
        }
    }

    fun deleteBookmark(url: String) {
        viewModelScope.launch {
            bookmarkRepository.deleteBookmark(url)
        }
    }
}

class HistoryViewModel(private val historyRepository: HistoryRepository) : ViewModel() {
    val history = historyRepository.allHistory.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteHistory(id: Long) {
        viewModelScope.launch {
            historyRepository.deleteById(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            historyRepository.clearAll()
        }
    }
}

class DownloadsViewModel(private val downloadRepository: DownloadRepository) : ViewModel() {
    val downloads = downloadRepository.allDownloads.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteDownload(id: Long) {
        viewModelScope.launch {
            downloadRepository.deleteById(id)
        }
    }

    fun clearCompleted() {
        viewModelScope.launch {
            downloadRepository.clearCompleted()
        }
    }
}

class ExtensionsViewModel(
    private val extensionRepository: ExtensionRepository,
    val extensionHost: ExtensionHost
) : ViewModel() {
    val extensions = extensionRepository.allExtensions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleExtension(id: String, enabled: Boolean) {
        viewModelScope.launch {
            extensionHost.enableExtension(id, enabled)
        }
    }

    fun uninstallExtension(id: String) {
        viewModelScope.launch {
            extensionHost.uninstallExtension(id)
        }
    }

    fun installUnpacked(zipInputStream: java.io.InputStream, fileName: String, onResult: (Result<InstalledExtensionEntity>) -> Unit) {
        viewModelScope.launch {
            val result = extensionHost.installUnpackedZip(zipInputStream, fileName)
            onResult(result)
        }
    }
}

class CatalogViewModel(
    private val catalogRepository: CatalogRepository,
    private val extensionRepository: ExtensionRepository,
    private val extensionHost: ExtensionHost
) : ViewModel() {
    val catalogItems = catalogRepository.allCatalogItems.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun refreshCatalog(context: Context, catalogUrl: String) {
        viewModelScope.launch {
            catalogRepository.fetchIndex(context, catalogUrl, force = true)
        }
    }

    fun installFromCatalog(context: Context, item: CatalogIndexEntity) {
        viewModelScope.launch {
            // Unpack directly from mock catalog link or copy asset folder for Omni Dark Mode
            if (item.id == "omni-dark-mode") {
                extensionHost.enableExtension(item.id, true)
                catalogRepository.recordCatalogInstall(item.id)
            } else {
                // For demonstration, register as catalog-installed
                val destDir = File(context.filesDir, "extensions/" + item.id).apply { mkdirs() }
                val manifestFile = File(destDir, "manifest.json")
                if (!manifestFile.exists()) {
                    manifestFile.writeText("""
                        {
                          "manifest_version": 3,
                          "id": "${item.id}",
                          "name": "${item.name}",
                          "version": "${item.version}",
                          "description": "${item.description}"
                        }
                    """.trimIndent())
                }
                extensionHost.installFromCatalogLocal(item.id, item.name, item.version, destDir.absolutePath)
                catalogRepository.recordCatalogInstall(item.id)
            }
        }
    }

    fun reportExtension(id: String, reason: String) {
        viewModelScope.launch {
            catalogRepository.recordReport(id, reason)
        }
    }
}

class SettingsViewModel(val settingsDataStore: SettingsDataStore) : ViewModel() {
    val searchEngineUrl = settingsDataStore.searchEngineUrl.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "https://www.google.com/search?q=")
    val homePage = settingsDataStore.homePage.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "omni://home")
    val httpsOnly = settingsDataStore.httpsOnly.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val blockTrackers = settingsDataStore.blockTrackers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val blockThirdPartyCookies = settingsDataStore.blockThirdPartyCookies.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val blockPopups = settingsDataStore.blockPopups.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val askBeforeDownload = settingsDataStore.askBeforeDownload.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val javascriptEnabled = settingsDataStore.javascriptEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val desktopModeDefault = settingsDataStore.desktopModeDefault.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val fontSize = settingsDataStore.fontSize.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 100)
    val darkMode = settingsDataStore.darkMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")
    val savePasswords = settingsDataStore.savePasswords.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val closeTabsOnExit = settingsDataStore.closeTabsOnExit.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val extensionsEnabled = settingsDataStore.extensionsEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val adBlockEnabled = settingsDataStore.adBlockEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val darkModeEnabled = settingsDataStore.darkModeEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val customDnsUrl = settingsDataStore.customDnsUrl.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val restoreTabsOnStartup = settingsDataStore.restoreTabsOnStartup.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val hideNavBar = settingsDataStore.hideNavBar.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
 
    fun setSearchEngineUrl(url: String) = viewModelScope.launch { settingsDataStore.setSearchEngineUrl(url) }
    fun setHomePage(url: String) = viewModelScope.launch { settingsDataStore.setHomePage(url) }
    fun setHttpsOnly(enabled: Boolean) = viewModelScope.launch { settingsDataStore.setHttpsOnly(enabled) }
    fun setBlockTrackers(enabled: Boolean) = viewModelScope.launch { settingsDataStore.setBlockTrackers(enabled) }
    fun setBlockThirdPartyCookies(enabled: Boolean) = viewModelScope.launch { settingsDataStore.setBlockThirdPartyCookies(enabled) }
    fun setBlockPopups(enabled: Boolean) = viewModelScope.launch { settingsDataStore.setBlockPopups(enabled) }
    fun setAskBeforeDownload(enabled: Boolean) = viewModelScope.launch { settingsDataStore.setAskBeforeDownload(enabled) }
    fun setJavascriptEnabled(enabled: Boolean) = viewModelScope.launch { settingsDataStore.setJavascriptEnabled(enabled) }
    fun setDesktopModeDefault(enabled: Boolean) = viewModelScope.launch { settingsDataStore.setDesktopModeDefault(enabled) }
    fun setFontSize(size: Int) = viewModelScope.launch { settingsDataStore.setFontSize(size) }
    fun setDarkMode(mode: String) = viewModelScope.launch { settingsDataStore.setDarkMode(mode) }
    fun setSavePasswords(enabled: Boolean) = viewModelScope.launch { settingsDataStore.setSavePasswords(enabled) }
    fun setCloseTabsOnExit(enabled: Boolean) = viewModelScope.launch { settingsDataStore.setCloseTabsOnExit(enabled) }
    fun setExtensionsEnabled(enabled: Boolean) = viewModelScope.launch { settingsDataStore.setExtensionsEnabled(enabled) }
    fun setAdBlockEnabled(enabled: Boolean) = viewModelScope.launch { settingsDataStore.setAdBlockEnabled(enabled) }
    fun setDarkModeEnabled(enabled: Boolean) = viewModelScope.launch { settingsDataStore.setDarkModeEnabled(enabled) }
    fun setCustomDnsUrl(url: String) = viewModelScope.launch { settingsDataStore.setCustomDnsUrl(url) }
    fun setRestoreTabsOnStartup(enabled: Boolean) = viewModelScope.launch { settingsDataStore.setRestoreTabsOnStartup(enabled) }
    fun setHideNavBar(enabled: Boolean) = viewModelScope.launch { settingsDataStore.setHideNavBar(enabled) }
}

// Custom factories for ViewModel creation using AppContainer
class ViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(BrowserViewModel::class.java) -> {
                BrowserViewModel(
                    container.tabManager,
                    container.settingsDataStore,
                    container.historyRepository,
                    container.bookmarkRepository,
                    container.downloadRepository,
                    container.extensionRepository
                ) as T
            }
            modelClass.isAssignableFrom(BookmarksViewModel::class.java) -> {
                BookmarksViewModel(container.bookmarkRepository) as T
            }
            modelClass.isAssignableFrom(HistoryViewModel::class.java) -> {
                HistoryViewModel(container.historyRepository) as T
            }
            modelClass.isAssignableFrom(DownloadsViewModel::class.java) -> {
                DownloadsViewModel(container.downloadRepository) as T
            }
            modelClass.isAssignableFrom(ExtensionsViewModel::class.java) -> {
                ExtensionsViewModel(container.extensionRepository, container.extensionHost) as T
            }
            modelClass.isAssignableFrom(CatalogViewModel::class.java) -> {
                CatalogViewModel(container.catalogRepository, container.extensionRepository, container.extensionHost) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(container.settingsDataStore) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
