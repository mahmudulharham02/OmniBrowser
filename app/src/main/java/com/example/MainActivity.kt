package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
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
import com.example.browser.incognito.IncognitoNotificationManager
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
            val cachePaths = listOf(
                "WebView/Default/HTTP Cache/Code Cache/js",
                "WebView/Default/HTTP Cache/Code Cache/wasm",
                "WebView/HTTP Cache/Code Cache/js",
                "WebView/HTTP Cache/Code Cache/wasm",
                "org.chromium.android_webview/HTTP Cache/Code Cache/js",
                "org.chromium.android_webview/HTTP Cache/Code Cache/wasm"
            )
            for (path in cachePaths) {
                val dir = java.io.File(cacheDir, path)
                dir.mkdirs()
                java.io.File(dir, ".keep").createNewFile()
            }
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
                LaunchedEffect(isDarkTheme) {
                    val activeViews = container.tabManager.getActiveWebViews()
                    activeViews.forEach { (_, webView) ->
                        container.browserEngine.updateWebViewDarkMode(webView)
                    }
                }

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
                var isInitialized by remember { mutableStateOf(false) }

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
                    isInitialized = true
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

                // Incognito Session Manager Notification
                val incognitoNotificationManager = remember { IncognitoNotificationManager(context) }
                val incognitoCount = remember(tabs) { tabs.count { it.isPrivate } }

                LaunchedEffect(incognitoCount) {
                    incognitoNotificationManager.updateNotification(incognitoCount)
                }

                DisposableEffect(Unit) {
                    val listener = androidx.core.util.Consumer<android.content.Intent> { intent ->
                        if (intent.action == IncognitoNotificationManager.ACTION_CLOSE_INCOGNITO) {
                            browserViewModel.tabManager.closeAllIncognitoTabs()
                            Toast.makeText(context, "Closed all Incognito tabs", Toast.LENGTH_SHORT).show()
                        }
                    }
                    addOnNewIntentListener(listener)
                    onDispose {
                        removeOnNewIntentListener(listener)
                    }
                }

                var isIncognitoLocked by remember { mutableStateOf(false) }
                val lockIncognitoSetting by settingsViewModel.lockIncognitoTabs.collectAsState()

                DisposableEffect(Unit) {
                    val lifecycleListener = androidx.lifecycle.LifecycleEventObserver { _, event ->
                        if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                            if (lockIncognitoSetting && tabs.any { it.isPrivate }) {
                                isIncognitoLocked = true
                            }
                        }
                    }
                    lifecycle.addObserver(lifecycleListener)
                    onDispose {
                        lifecycle.removeObserver(lifecycleListener)
                    }
                }

                LaunchedEffect(tabs, isInitialized) {
                    if (isInitialized && tabs.isEmpty()) {
                        (context as? android.app.Activity)?.finish()
                    }
                }

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
                                        tabCount = tabs.size,
                                        isDarkTheme = isDarkTheme,
                                        onToggleDarkTheme = {
                                            settingsViewModel.setDarkMode(if (isDarkTheme) "light" else "dark")
                                        }
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
                                            val delta = scrollY - oldScrollY
                                            if (scrollY <= 10) {
                                                if (!areBarsVisible) {
                                                    areBarsVisible = true
                                                }
                                            } else if (delta > 2) {
                                                if (areBarsVisible) {
                                                    areBarsVisible = false
                                                }
                                            } else if (delta < -2) {
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
                                        browserViewModel.tabManager.closeAllRegularTabs()
                                    },
                                    onCloseAllIncognito = {
                                        browserViewModel.tabManager.closeAllIncognitoTabs()
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

                        // Permanent Top System Strip (always stays visible above status bar area)
                        val permanentBarColor = if (currentTab?.isPrivate == true) androidx.compose.ui.graphics.Color(0xFF1E1E1E) else MaterialTheme.colorScheme.surface
                        Box(
                            modifier = Modifier
                                .align(androidx.compose.ui.Alignment.TopCenter)
                                .fillMaxWidth()
                                .windowInsetsTopHeight(WindowInsets.statusBars)
                                .background(permanentBarColor)
                                .zIndex(200f)
                        )

                        if (currentScreen == BrowserScreenType.BROWSER && currentTab != null && currentTab?.url != "omni://home") {
                            val density = androidx.compose.ui.platform.LocalDensity.current
                            val statusBarHeightDp = with(density) { WindowInsets.statusBars.getTop(density).toDp() }
                            val topBarHeightPx = with(density) { 56.dp.toPx() }
                            val translationY by androidx.compose.animation.core.animateFloatAsState(
                                targetValue = if (areBarsVisible || showMenu) 0f else -topBarHeightPx,
                                animationSpec = androidx.compose.animation.core.tween(
                                    durationMillis = 200,
                                    easing = androidx.compose.animation.core.FastOutSlowInEasing
                                ),
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
                                    .padding(top = statusBarHeightDp)
                                    .zIndex(100f)
                                    .graphicsLayer {
                                        this.translationY = translationY
                                    },
                                customDnsUrl = customDnsUrl,
                                isPrivate = currentTab?.isPrivate == true
                            )
                        }

                        // Incognito Lock Overlay Screen
                        if (isIncognitoLocked && currentTab?.isPrivate == true) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(androidx.compose.ui.graphics.Color(0xFF121212))
                                    .zIndex(300f),
                                contentAlignment = androidx.compose.ui.Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(32.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(80.dp)
                                            .background(androidx.compose.ui.graphics.Color(0xFF2B2B2B), androidx.compose.foundation.shape.CircleShape),
                                        contentAlignment = androidx.compose.ui.Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Locked",
                                            tint = androidx.compose.ui.graphics.Color.White,
                                            modifier = Modifier.size(40.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Text(
                                        text = "Incognito tabs are locked",
                                        style = MaterialTheme.typography.titleLarge.copy(color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Unlock to view your open Incognito tabs.",
                                        style = MaterialTheme.typography.bodyMedium.copy(color = androidx.compose.ui.graphics.Color.LightGray),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(28.dp))
                                    Button(
                                        onClick = { isIncognitoLocked = false },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Text("Unlock Incognito Tabs", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
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
                            isDarkTheme = isDarkTheme,
                            onToggleDarkTheme = {
                                settingsViewModel.setDarkMode(if (isDarkTheme) "light" else "dark")
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

        // Ephemeral tabs lifecycle: destroy all incognito tabs when app goes to background / closes
        container.tabManager.closeAllIncognitoTabs()

        // Persist only non-private tabs
        val regularTabs = container.tabManager.tabs.value.filter { !it.isPrivate }
        val list = regularTabs.mapIndexed { index, tab ->
            com.example.data.SavedTabEntity(
                id = tab.id,
                url = tab.url,
                title = tab.title,
                faviconUrl = tab.faviconUrl,
                position = index,
                isPrivate = false,
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
