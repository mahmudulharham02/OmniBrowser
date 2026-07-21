package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.lifecycle.ViewModelProvider
import com.example.ui.screens.*
import com.example.ui.components.ChromeTopBar
import com.example.ui.components.RedesignedMenuPopup
import com.example.ui.components.ConfirmDialog
import com.example.ui.theme.MyApplicationTheme
import com.example.data.CatalogIndexEntity
import com.example.data.SavedTabEntity
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

enum class BrowserScreenType {
    BROWSER,
    TABS,
    BOOKMARKS,
    HISTORY,
    DOWNLOADS,
    EXTENSIONS,
    CATALOG,
    CATALOG_DETAIL,
    SETTINGS
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)

        // Pre-create WebView Code Cache directories to prevent Chromium opendir errors
        try {
            val webViewCacheDir = java.io.File(cacheDir, "WebView/Default/HTTP Cache/Code Cache")
            java.io.File(webViewCacheDir, "js").mkdirs()
            java.io.File(webViewCacheDir, "wasm").mkdirs()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val container = (application as BrowserApplication).container

        setContent {
            val factory = remember { ViewModelFactory(container) }
            val browserViewModel = remember { ViewModelProvider(this@MainActivity, factory)[BrowserViewModel::class.java] }
            val settingsViewModel = remember { ViewModelProvider(this@MainActivity, factory)[SettingsViewModel::class.java] }
            val currentTab by browserViewModel.currentTab.collectAsState()
            val searchEngineUrl by browserViewModel.searchEngineUrl.collectAsState()

            val darkModePref by settingsViewModel.darkMode.collectAsState()
            val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val isDarkTheme = when (darkModePref) {
                "dark" -> true
                "light" -> false
                else -> isSystemDark
            }

            MyApplicationTheme(
                darkTheme = isDarkTheme,
                isIncognito = currentTab?.isPrivate == true
            ) {
                val context = LocalContext.current
                val coroutineScope = rememberCoroutineScope()

                // Instantiate ViewModels
                val bookmarksViewModel = remember { ViewModelProvider(this@MainActivity, factory)[BookmarksViewModel::class.java] }
                val historyViewModel = remember { ViewModelProvider(this@MainActivity, factory)[HistoryViewModel::class.java] }
                val downloadsViewModel = remember { ViewModelProvider(this@MainActivity, factory)[DownloadsViewModel::class.java] }
                val extensionsViewModel = remember { ViewModelProvider(this@MainActivity, factory)[ExtensionsViewModel::class.java] }
                val catalogViewModel = remember { ViewModelProvider(this@MainActivity, factory)[CatalogViewModel::class.java] }

                val hideNavBar = settingsViewModel.hideNavBar.collectAsState().value
                DisposableEffect(hideNavBar) {
                    val activity = context as? android.app.Activity
                    if (activity != null) {
                        val window = activity.window
                        val controller = androidx.core.view.WindowInsetsControllerCompat(window, window.decorView)
                        if (hideNavBar) {
                            controller.hide(androidx.core.view.WindowInsetsCompat.Type.navigationBars())
                            controller.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                        } else {
                            controller.show(androidx.core.view.WindowInsetsCompat.Type.navigationBars())
                        }
                    }
                    onDispose { }
                }

                // Ensure at least one tab is created on startup
                LaunchedEffect(Unit) {
                    val restoreEnabled = container.settingsDataStore.restoreTabsOnStartup.first()
                    var restored = false
                    if (restoreEnabled) {
                        try {
                            val savedTabs = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                container.database.savedTabDao().getAllSavedTabs().first()
                            }
                            if (savedTabs.isNotEmpty()) {
                                browserViewModel.tabManager.closeAllTabs(addPlaceholder = false)
                                var activeIdToSelect: String? = null
                                val lastUsedTab = savedTabs.maxByOrNull { it.lastAccessedEpoch } ?: savedTabs.first()
                                savedTabs.forEach { saved ->
                                    browserViewModel.tabManager.createTab(saved.url, saved.isPrivate).let { tab ->
                                        browserViewModel.tabManager.updateTab(tab.id) {
                                            it.copy(title = saved.title, faviconUrl = saved.faviconUrl)
                                        }
                                        if (saved.id == lastUsedTab.id) {
                                            activeIdToSelect = tab.id
                                        }
                                    }
                                }
                                val selectId = activeIdToSelect ?: browserViewModel.tabs.value.firstOrNull()?.id
                                if (selectId != null) {
                                    browserViewModel.selectTab(selectId)
                                }
                                restored = true
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } else {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            container.database.savedTabDao().clearAllSavedTabs()
                        }
                    }

                    if (!restored && browserViewModel.tabs.value.isEmpty()) {
                        browserViewModel.createNewTab("omni://home")
                    }
                }

                // App Navigation states
                var currentScreen by remember { mutableStateOf(BrowserScreenType.BROWSER) }
                var selectedCatalogItem by remember { mutableStateOf<CatalogIndexEntity?>(null) }
                var showMenu by remember { mutableStateOf(false) }
                var areBarsVisible by remember { mutableStateOf(true) }

                // State observers
                val tabs by browserViewModel.tabs.collectAsState()
                val activeTabId by browserViewModel.activeTabId.collectAsState()
                val customDnsUrl by browserViewModel.customDnsUrl.collectAsState()

                LaunchedEffect(activeTabId, currentTab?.url, currentScreen) {
                    areBarsVisible = true
                }

                val totalAdsBlocked by browserViewModel.totalAdsBlocked.collectAsState()
                val totalTrackersBlocked by browserViewModel.totalTrackersBlocked.collectAsState()
                val totalPopupsBlocked by browserViewModel.totalPopupsBlocked.collectAsState()

                val bookmarks by bookmarksViewModel.bookmarks.collectAsState()
                val history by historyViewModel.history.collectAsState()
                val downloads by downloadsViewModel.downloads.collectAsState()
                val extensions by extensionsViewModel.extensions.collectAsState()
                val catalogItems by catalogViewModel.catalogItems.collectAsState()

                // Observe pending downloads
                val pendingDownload by container.downloadHandler.pendingDownload.collectAsState()

                // Global dialog overlays
                if (pendingDownload != null) {
                    ConfirmDialog(
                        title = "Confirm File Download",
                        message = "Do you want to download '${pendingDownload!!.filename}' (${pendingDownload!!.contentLength / 1024} KB)?",
                        onConfirm = { pendingDownload!!.onConfirm(true) },
                        onDismiss = { pendingDownload!!.onConfirm(false) }
                    )
                }

                // Handle global hardware back button to navigate screens back to BROWSER viewport
                BackHandler(enabled = currentScreen != BrowserScreenType.BROWSER) {
                    currentScreen = when (currentScreen) {
                        BrowserScreenType.CATALOG_DETAIL -> BrowserScreenType.CATALOG
                        else -> BrowserScreenType.BROWSER
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentScreen) {
                            BrowserScreenType.BROWSER -> {
                                if (currentTab == null || currentTab?.url == "omni://home") {
                                    HomeScreen(
                                        totalAds = totalAdsBlocked,
                                        totalTrackers = totalTrackersBlocked,
                                        totalPopups = totalPopupsBlocked,
                                        searchEngineUrl = searchEngineUrl,
                                        onSearch = { browserViewModel.loadUrlInActiveTab(it) },
                                        onNavigateTo = { browserViewModel.loadUrlInActiveTab(it) },
                                        onTabsClick = {
                                            browserViewModel.tabManager.captureActiveTabThumbnail()
                                            currentScreen = BrowserScreenType.TABS
                                        },
                                        onMenuClick = { showMenu = true },
                                        tabCount = tabs.size
                                    )
                                } else {
                                    BrowserScreen(
                                        tabId = activeTabId!!,
                                        url = currentTab!!.url,
                                        isLoading = currentTab!!.isLoading,
                                        progress = currentTab!!.progress,
                                        isPrivate = currentTab!!.isPrivate,
                                        browserEngine = container.browserEngine,
                                        tabManager = container.tabManager,
                                        areBarsVisible = areBarsVisible,
                                        onScrollChanged = { scrollY, oldScrollY ->
                                            if (scrollY == 0) {
                                                if (!areBarsVisible) {
                                                    areBarsVisible = true
                                                }
                                            } else if (scrollY > oldScrollY + 5) {
                                                if (areBarsVisible) {
                                                    areBarsVisible = false
                                                }
                                            } else if (scrollY < oldScrollY - 5) {
                                                if (!areBarsVisible) {
                                                    areBarsVisible = true
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                            BrowserScreenType.TABS -> {
                                TabSwitcherScreen(
                                    tabs = tabs,
                                    activeTabId = activeTabId,
                                    tabManager = container.tabManager,
                                    onTabSelect = { id ->
                                        browserViewModel.selectTab(id)
                                        currentScreen = BrowserScreenType.BROWSER
                                    },
                                    onTabClose = { id ->
                                        browserViewModel.closeTab(id)
                                    },
                                    onCloseAll = {
                                        browserViewModel.closeAllTabs()
                                    },
                                    onAddTab = { privateMode ->
                                        browserViewModel.createNewTab("omni://home", privateMode)
                                        currentScreen = BrowserScreenType.BROWSER
                                    },
                                    onBack = { currentScreen = BrowserScreenType.BROWSER }
                                )
                            }
                            BrowserScreenType.BOOKMARKS -> {
                                BookmarksScreen(
                                    bookmarks = bookmarks,
                                    onSelectBookmark = { url ->
                                        browserViewModel.loadUrlInActiveTab(url)
                                        currentScreen = BrowserScreenType.BROWSER
                                    },
                                    onDeleteBookmark = { bookmarksViewModel.deleteBookmark(it) },
                                    onBack = { currentScreen = BrowserScreenType.BROWSER }
                                )
                            }
                            BrowserScreenType.HISTORY -> {
                                HistoryScreen(
                                    history = history,
                                    onSelectHistory = { url ->
                                        browserViewModel.loadUrlInActiveTab(url)
                                        currentScreen = BrowserScreenType.BROWSER
                                    },
                                    onDeleteHistory = { historyViewModel.deleteHistory(it) },
                                    onClearAll = { historyViewModel.clearAllHistory() },
                                    onBack = { currentScreen = BrowserScreenType.BROWSER }
                                )
                            }
                            BrowserScreenType.DOWNLOADS -> {
                                DownloadsScreen(
                                    downloads = downloads,
                                    onOpenDownload = { item ->
                                        Toast.makeText(context, "Opening file: ${item.filename}", Toast.LENGTH_SHORT).show()
                                    },
                                    onDeleteDownload = { downloadsViewModel.deleteDownload(it) },
                                    onBack = { currentScreen = BrowserScreenType.BROWSER }
                                )
                            }
                            BrowserScreenType.EXTENSIONS -> {
                                ExtensionsScreen(
                                    extensions = extensions,
                                    onToggle = { id, enabled -> extensionsViewModel.toggleExtension(id, enabled) },
                                    onUninstall = { extensionsViewModel.uninstallExtension(it) },
                                    onLoadUnpacked = { stream, name ->
                                        extensionsViewModel.installUnpacked(stream, name) { result ->
                                            if (result.isSuccess) {
                                                Toast.makeText(context, "Extension loaded successfully!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Failed to load unpacked extension: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    },
                                    onOpenCatalog = { currentScreen = BrowserScreenType.CATALOG },
                                    onBack = { currentScreen = BrowserScreenType.BROWSER }
                                )
                            }
                            BrowserScreenType.CATALOG -> {
                                ExtensionCatalogScreen(
                                    catalogItems = catalogItems,
                                    onSelectExtension = { item ->
                                        selectedCatalogItem = item
                                        currentScreen = BrowserScreenType.CATALOG_DETAIL
                                    },
                                    onBack = { currentScreen = BrowserScreenType.EXTENSIONS }
                                )
                            }
                            BrowserScreenType.CATALOG_DETAIL -> {
                                selectedCatalogItem?.let { item ->
                                    ExtensionDetailScreen(
                                        item = item,
                                        onInstall = {
                                            catalogViewModel.installFromCatalog(context, item)
                                            Toast.makeText(context, "Installed ${item.name}", Toast.LENGTH_SHORT).show()
                                        },
                                        onReport = { reason ->
                                            catalogViewModel.reportExtension(item.id, reason)
                                            Toast.makeText(context, "Extension reported. Thank you for keeping OmniBrowser secure.", Toast.LENGTH_SHORT).show()
                                        },
                                        onBack = { currentScreen = BrowserScreenType.CATALOG }
                                    )
                                }
                            }
                            BrowserScreenType.SETTINGS -> {
                                SettingsScreen(
                                    viewModel = settingsViewModel,
                                    onBack = { currentScreen = BrowserScreenType.BROWSER }
                                )
                            }
                        }

                        if (currentScreen == BrowserScreenType.BROWSER && currentTab != null && currentTab?.url != "omni://home") {
                            val density = androidx.compose.ui.platform.LocalDensity.current
                            val topBarHeightPx = with(density) { 85.dp.toPx() }
                            val translationY by androidx.compose.animation.core.animateFloatAsState(
                                targetValue = if (areBarsVisible || showMenu) 0f else -topBarHeightPx,
                                animationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow),
                                label = "TopBarTranslation"
                            )

                            ChromeTopBar(
                                url = currentTab?.url ?: "omni://home",
                                isLoading = currentTab?.isLoading == true,
                                progress = currentTab?.progress ?: 0,
                                tabCount = tabs.size,
                                onUrlSubmit = { browserViewModel.loadUrlInActiveTab(it) },
                                onReload = { browserViewModel.reloadActiveTab() },
                                onStop = { browserViewModel.stopLoadingActiveTab() },
                                onTabsClick = {
                                    browserViewModel.tabManager.captureActiveTabThumbnail()
                                    currentScreen = BrowserScreenType.TABS
                                },
                                onMenuClick = { showMenu = true },
                                canGoBack = currentTab?.canGoBack == true,
                                canGoForward = currentTab?.canGoForward == true,
                                onBack = { browserViewModel.goBackInActiveTab() },
                                onForward = { browserViewModel.goForwardInActiveTab() },
                                modifier = Modifier
                                    .align(androidx.compose.ui.Alignment.TopCenter)
                                    .graphicsLayer {
                                        this.translationY = translationY
                                    },
                                customDnsUrl = customDnsUrl,
                                isPrivate = currentTab?.isPrivate == true
                            )
                        }

                        // Custom Redesigned Menu Popup Overlay
                        RedesignedMenuPopup(
                            visible = showMenu,
                            onDismiss = { showMenu = false },
                            onNewTab = {
                                browserViewModel.createNewTab("omni://home")
                                currentScreen = BrowserScreenType.BROWSER
                            },
                            onNewIncognitoTab = {
                                browserViewModel.createNewTab("omni://home", isPrivate = true)
                                currentScreen = BrowserScreenType.BROWSER
                                Toast.makeText(context, "Opened Incognito Tab", Toast.LENGTH_SHORT).show()
                            },
                            onHistory = {
                                currentScreen = BrowserScreenType.HISTORY
                            },
                            onDeleteBrowsingData = {
                                coroutineScope.launch {
                                    historyViewModel.clearAllHistory()
                                    Toast.makeText(context, "Browsing history deleted!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onDownloads = {
                                currentScreen = BrowserScreenType.DOWNLOADS
                            },
                            onBookmarks = {
                                currentScreen = BrowserScreenType.BOOKMARKS
                            },
                            onRecentTabs = {
                                browserViewModel.restoreLastClosedTab()
                            },
                            onShare = {
                                val currentUrl = currentTab?.url
                                if (currentUrl != null && currentUrl != "omni://home") {
                                    val sendIntent = android.content.Intent().apply {
                                        action = android.content.Intent.ACTION_SEND
                                        putExtra(android.content.Intent.EXTRA_TEXT, currentUrl)
                                        type = "text/plain"
                                    }
                                    val shareIntent = android.content.Intent.createChooser(sendIntent, null)
                                    context.startActivity(shareIntent)
                                } else {
                                    Toast.makeText(context, "Nothing to share", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onFindInPage = {
                                Toast.makeText(context, "Find in Page is under development", Toast.LENGTH_SHORT).show()
                            },
                            onTranslate = {
                                Toast.makeText(context, "Translate is under development", Toast.LENGTH_SHORT).show()
                            },
                            onAddToHomeScreen = {
                                Toast.makeText(context, "Add to Home Screen is under development", Toast.LENGTH_SHORT).show()
                            },
                            isDesktopSite = currentTab?.desktopMode == true,
                            onToggleDesktopSite = {
                                browserViewModel.toggleDesktopModeForActiveTab()
                            },
                            onSettings = {
                                currentScreen = BrowserScreenType.SETTINGS
                            },
                            onHelpFeedback = {
                                Toast.makeText(context, "Help & Feedback: Visit our github page or settings.", Toast.LENGTH_LONG).show()
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        val container = (application as BrowserApplication).container
        val tabs = container.tabManager.tabs.value
        val list = tabs.mapIndexed { index, tab ->
            com.example.data.SavedTabEntity(
                id = tab.id,
                url = tab.url,
                title = tab.title,
                faviconUrl = tab.faviconUrl,
                position = index,
                isPrivate = tab.isPrivate,
                lastAccessedEpoch = tab.createdAt
            )
        }
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                container.database.savedTabDao().saveAllTabs(list)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
