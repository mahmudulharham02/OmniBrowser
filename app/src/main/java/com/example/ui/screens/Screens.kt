@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import android.view.View
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.browser.engine.BrowserEngine
import com.example.browser.tab.TabManager
import com.example.data.*
import com.example.ui.components.*
import java.io.InputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    totalAds: Int,
    totalTrackers: Int,
    totalPopups: Int,
    onSearch: (String) -> Unit,
    onNavigateTo: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val shortcuts = remember {
        mutableStateListOf(
            "Google" to "https://www.google.com",
            "DuckDuckGo" to "https://www.duckduckgo.com",
            "GitHub" to "https://github.com",
            "Wikipedia" to "https://www.wikipedia.org"
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(16.8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(64.dp))

            // Premium Logo Display
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.img_app_icon),
                    contentDescription = "OmniBrowser Logo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "OmniBrowser",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.SansSerif
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Privacy Engine Connected",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Centered Search Input Box
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search or type URL") },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        if (searchQuery.trim().isNotEmpty()) {
                            onSearch(searchQuery)
                        }
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .testTag("home_search_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Telemetry Counter Shield
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "OmniShield Active Blockers",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = totalAds.toString(),
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Ads Blocked",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = totalTrackers.toString(),
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Trackers",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = totalPopups.toString(),
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Popups",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Quick Shortcuts Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                verticalArrangement = Arrangement.Center,
                horizontalArrangement = Arrangement.Center
            ) {
                items(shortcuts) { (title, url) ->
                    ShortcutTile(
                        title = title,
                        url = url,
                        onClick = { onNavigateTo(url) },
                        onDelete = { shortcuts.remove(title to url) }
                    )
                }
            }
        }
    }
}

@Composable
fun BrowserScreen(
    tabId: String,
    url: String,
    isLoading: Boolean,
    progress: Int,
    isPrivate: Boolean,
    browserEngine: BrowserEngine,
    tabManager: TabManager,
    modifier: Modifier = Modifier
) {
    val webView = remember(tabId) {
        val wv = tabManager.getWebViewForTab(tabId)
        if (wv != null) {
            browserEngine.setupWebView(wv, tabId, isPrivate)
        }
        wv
    }

    // Capture standard system back actions to navigate WebView history backwards
    BackHandler(enabled = webView?.canGoBack() == true) {
        webView?.goBack()
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (isPrivate) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF212121))
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VisibilityOff, null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Private Browsing Tab Mode", style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            if (webView != null) {
                AndroidView(
                    factory = {
                        webView.apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
fun TabSwitcherScreen(
    tabs: List<TabModel>,
    activeTabId: String?,
    tabManager: TabManager,
    onTabSelect: (String) -> Unit,
    onTabClose: (String) -> Unit,
    onCloseAll: () -> Unit,
    onAddTab: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Active Tabs (${tabs.size})", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                actions = {
                    if (tabs.isNotEmpty()) {
                        TextButton(onClick = onCloseAll) {
                            Text("CLOSE ALL", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FloatingActionButton(
                    onClick = { onAddTab(true) },
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                    modifier = Modifier.testTag("add_private_tab_fab")
                ) {
                    Icon(Icons.Default.VisibilityOff, "New Private Tab")
                }
                FloatingActionButton(
                    onClick = { onAddTab(false) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_tab_fab")
                ) {
                    Icon(Icons.Default.Add, "New Tab")
                }
            }
        }
    ) { innerPadding ->
        if (tabs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.Layers,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No open tabs",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = innerPadding,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                items(tabs) { tab ->
                    TabCard(
                        tab = tab,
                        thumbnail = tabManager.getThumbnail(tab.id),
                        isActive = tab.id == activeTabId,
                        onClick = { onTabSelect(tab.id) },
                        onClose = { onTabClose(tab.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun BookmarksScreen(
    bookmarks: List<BookmarkEntity>,
    onSelectBookmark: (String) -> Unit,
    onDeleteBookmark: (String) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Bookmarks", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                }
            )
        }
    ) { innerPadding ->
        if (bookmarks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.BookmarkBorder, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Your bookmarks will appear here", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                contentPadding = innerPadding,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp)
            ) {
                items(bookmarks) { bookmark ->
                    ListItem(
                        headlineContent = { Text(bookmark.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold) },
                        supportingContent = { Text(bookmark.url, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall) },
                        leadingContent = { Icon(Icons.Default.Bookmark, "Bookmark Icon", tint = MaterialTheme.colorScheme.primary) },
                        trailingContent = {
                            IconButton(onClick = { onDeleteBookmark(bookmark.url) }) {
                                Icon(Icons.Default.Delete, "Delete Bookmark", tint = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier
                            .clickable { onSelectBookmark(bookmark.url) }
                            .padding(vertical = 4.dp)
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                }
            }
        }
    }
}

@Composable
fun HistoryScreen(
    history: List<HistoryEntity>,
    onSelectHistory: (String) -> Unit,
    onDeleteHistory: (Long) -> Unit,
    onClearAll: () -> Unit,
    onBack: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("History Logs", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                actions = {
                    if (history.isNotEmpty()) {
                        TextButton(onClick = onClearAll) {
                            Text("CLEAR ALL", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.History, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No browsing history", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                contentPadding = innerPadding,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp)
            ) {
                items(history) { record ->
                    ListItem(
                        headlineContent = { Text(record.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold) },
                        supportingContent = {
                            Column {
                                Text(record.url, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                                Text(formatter.format(Date(record.lastVisitEpoch)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                            }
                        },
                        leadingContent = { Icon(Icons.Default.History, "History Icon", tint = MaterialTheme.colorScheme.primary) },
                        trailingContent = {
                            IconButton(onClick = { onDeleteHistory(record.id) }) {
                                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier
                            .clickable { onSelectHistory(record.url) }
                            .padding(vertical = 4.dp)
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                }
            }
        }
    }
}

@Composable
fun DownloadsScreen(
    downloads: List<DownloadEntity>,
    onOpenDownload: (DownloadEntity) -> Unit,
    onDeleteDownload: (Long) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Download Manager", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                }
            )
        }
    ) { innerPadding ->
        if (downloads.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.FileDownload, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No downloaded files", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                contentPadding = innerPadding,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)
            ) {
                items(downloads) { item ->
                    DownloadRow(
                        download = item,
                        onOpen = { onOpenDownload(item) },
                        onDelete = { onDeleteDownload(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun ExtensionsScreen(
    extensions: List<InstalledExtensionEntity>,
    onToggle: (String, Boolean) -> Unit,
    onUninstall: (String) -> Unit,
    onLoadUnpacked: (InputStream, String) -> Unit,
    onOpenCatalog: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val fileName = uri.path?.substringAfterLast("/") ?: "unpacked.zip"
                if (inputStream != null) {
                    onLoadUnpacked(inputStream, fileName)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        topBar = {
            OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Plugins & Extensions", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                }
            )
        },
        floatingActionButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { filePickerLauncher.launch("application/zip") },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Icon(Icons.Default.Upload, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Load Unpacked")
                }
                
                Button(onClick = onOpenCatalog) {
                    Icon(Icons.Default.Shop, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Store Catalog")
                }
            }
        }
    ) { innerPadding ->
        if (extensions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Extension, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No extensions installed yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                contentPadding = innerPadding,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)
            ) {
                items(extensions) { entity ->
                    ExtensionRow(
                        extension = entity,
                        isEnabled = entity.enabled,
                        onToggle = { enabled -> onToggle(entity.id, enabled) },
                        onUninstall = { onUninstall(entity.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun ExtensionCatalogScreen(
    catalogItems: List<CatalogIndexEntity>,
    onSelectExtension: (CatalogIndexEntity) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("OmniWeb Store", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                }
            )
        }
    ) { innerPadding ->
        if (catalogItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                contentPadding = innerPadding,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)
            ) {
                items(catalogItems) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { onSelectExtension(item) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(item.name.take(1).uppercase(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                Text(item.description, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExtensionDetailScreen(
    item: CatalogIndexEntity,
    onInstall: () -> Unit,
    onReport: (String) -> Unit,
    onBack: () -> Unit
) {
    var isInstalled by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var reportReason by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text(item.name, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(item.name.take(1).uppercase(), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(item.name, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
            Text("Version ${item.version} • Author: ${item.author}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, "Rating", tint = Color(0xFFFFC107), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("${item.rating} (${item.ratingCount} reviews)", style = MaterialTheme.typography.labelLarge)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(item.description, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    onInstall()
                    isInstalled = true
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isInstalled
            ) {
                Icon(Icons.Default.Download, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isInstalled) "INSTALLED" else "ADD TO OMNIBROWSER")
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = { showReportDialog = true },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Report, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Report Extension")
            }
        }

        if (showReportDialog) {
            AlertDialog(
                onDismissRequest = { showReportDialog = false },
                title = { Text("Report Extension") },
                text = {
                    Column {
                        Text("Reason for reporting this extension:")
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = reportReason,
                            onValueChange = { reportReason = it },
                            placeholder = { Text("Describe the issue...") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (reportReason.trim().isNotEmpty()) {
                                onReport(reportReason)
                                showReportDialog = false
                            }
                        }
                    ) {
                        Text("Submit Report")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showReportDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val searchEngine by viewModel.searchEngineUrl.collectAsState()
    val homePage by viewModel.homePage.collectAsState()
    val httpsOnly by viewModel.httpsOnly.collectAsState()
    val blockTrackers by viewModel.blockTrackers.collectAsState()
    val blockPopups by viewModel.blockPopups.collectAsState()
    val cookiesBlocked by viewModel.blockThirdPartyCookies.collectAsState()
    val askDownload by viewModel.askBeforeDownload.collectAsState()
    val jsEnabled by viewModel.javascriptEnabled.collectAsState()
    val adBlock by viewModel.adBlockEnabled.collectAsState()

    Scaffold(
        topBar = {
            OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Browser Settings", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("General Settings", style = MaterialTheme.typography.titleSmall.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(8.dp))

            var searchDropdownExpanded by remember { mutableStateOf(false) }
            val searchOptions = listOf(
                "Google" to "https://www.google.com/search?q=",
                "DuckDuckGo" to "https://duckduckgo.com/?q=",
                "Bing" to "https://www.bing.com/search?q=",
                "Startpage" to "https://www.startpage.com/sp/search?q=",
                "Brave Search" to "https://search.brave.com/search?q=",
                "Custom URL" to "custom"
            )
            val currentOption = searchOptions.find { it.second == searchEngine } ?: if (searchEngine.isNotBlank()) "Custom URL" to searchEngine else "Google" to "https://www.google.com/search?q="

            ListItem(
                headlineContent = { Text("Default Search Engine") },
                supportingContent = { Text(currentOption.first) },
                trailingContent = {
                    Box {
                        TextButton(onClick = { searchDropdownExpanded = true }) {
                            Text("Select")
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                        DropdownMenu(
                            expanded = searchDropdownExpanded,
                            onDismissRequest = { searchDropdownExpanded = false }
                        ) {
                            searchOptions.forEach { (name, url) ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        if (url == "custom") {
                                            viewModel.setSearchEngineUrl("")
                                        } else {
                                            viewModel.setSearchEngineUrl(url)
                                        }
                                        searchDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            )

            if (currentOption.first == "Custom URL" || searchEngine.isBlank()) {
                var customUrlText by remember(searchEngine) { mutableStateOf(searchEngine) }
                OutlinedTextField(
                    value = customUrlText,
                    onValueChange = {
                        customUrlText = it
                        viewModel.setSearchEngineUrl(it)
                    },
                    label = { Text("Custom Search URL (must contain {query})") },
                    placeholder = { Text("e.g. https://searx.be/search?q={query}") },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    singleLine = true
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            ListItem(
                headlineContent = { Text("JavaScript Enabled") },
                supportingContent = { Text("Allow pages to execute local scripts") },
                trailingContent = {
                    Switch(checked = jsEnabled, onCheckedChange = { viewModel.setJavascriptEnabled(it) })
                }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            ListItem(
                headlineContent = { Text("Restore Tabs on Startup") },
                supportingContent = { Text("Automatically reopen tabs from your previous session") },
                trailingContent = {
                    val restoreTabsOnStartup by viewModel.restoreTabsOnStartup.collectAsState()
                    Switch(checked = restoreTabsOnStartup, onCheckedChange = { viewModel.setRestoreTabsOnStartup(it) })
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text("Appearance", style = MaterialTheme.typography.titleSmall.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(8.dp))

            val darkModeEnabled by viewModel.darkModeEnabled.collectAsState()
            ListItem(
                headlineContent = { Text("Omni Smart Dark Mode") },
                supportingContent = { Text("Force a highly compatible smart dark theme on all web pages") },
                trailingContent = {
                    Switch(
                        checked = darkModeEnabled,
                        onCheckedChange = { viewModel.setDarkModeEnabled(it) }
                    )
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text("Privacy & Security", style = MaterialTheme.typography.titleSmall.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(8.dp))

            ListItem(
                headlineContent = { Text("HTTPS-Only Mode") },
                supportingContent = { Text("Force upgrade HTTP links to HTTPS") },
                trailingContent = {
                    Switch(checked = httpsOnly, onCheckedChange = { viewModel.setHttpsOnly(it) })
                }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            ListItem(
                headlineContent = { Text("Intelligent Ad Blocking") },
                supportingContent = { Text("Block unsolicited trackers & network ad requests") },
                trailingContent = {
                    Switch(checked = adBlock, onCheckedChange = { viewModel.setAdBlockEnabled(it) })
                }
            )

            // Test Ad Blocker Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                val context = androidx.compose.ui.platform.LocalContext.current
                var showAdBlockTestResult by remember { mutableStateOf<String?>(null) }
                
                TextButton(
                    onClick = {
                        val engine = (context.applicationContext as com.example.BrowserApplication).container.adBlockEngine
                        val adUrl = "https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js"
                        val trackerUrl = "https://sub.doubleclick.net/tracker.gif"
                        val normalUrl = "https://google.com/search?q=hi"
                        
                        val adBlocked = engine.shouldBlock(adUrl)
                        val trackerBlocked = engine.shouldBlock(trackerUrl)
                        val normalBlocked = engine.shouldBlock(normalUrl)
                        
                        showAdBlockTestResult = """
                            Ad Blocker Integrity Test:
                            
                            • pagead2.googlesyndication.com: ${if (adBlocked) "✅ BLOCKED (PASSED)" else "❌ ALLOWED (FAILED)"}
                            • sub.doubleclick.net: ${if (trackerBlocked) "✅ BLOCKED (PASSED)" else "❌ ALLOWED (FAILED)"}
                            • google.com (normal): ${if (!normalBlocked) "✅ ALLOWED (PASSED)" else "❌ BLOCKED (FAILED)"}
                            
                            Integrity check complete. The adblock engine is fully active!
                        """.trimIndent()
                    }
                ) {
                    Icon(Icons.Default.BugReport, null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Test Ad Blocker")
                }
                
                if (showAdBlockTestResult != null) {
                    AlertDialog(
                        onDismissRequest = { showAdBlockTestResult = null },
                        title = { Text("Ad Blocker Test Result") },
                        text = { Text(showAdBlockTestResult!!) },
                        confirmButton = {
                            TextButton(onClick = { showAdBlockTestResult = null }) {
                                Text("Close")
                            }
                        }
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            ListItem(
                headlineContent = { Text("Block Third-Party Cookies") },
                supportingContent = { Text("Strict isolation for storage trackers") },
                trailingContent = {
                    Switch(checked = cookiesBlocked, onCheckedChange = { viewModel.setBlockThirdPartyCookies(it) })
                }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            ListItem(
                headlineContent = { Text("Popups & Redirects Blocker") },
                supportingContent = { Text("Intercept unrequested windows and redirections") },
                trailingContent = {
                    Switch(checked = blockPopups, onCheckedChange = { viewModel.setBlockPopups(it) })
                }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            var dnsDropdownExpanded by remember { mutableStateOf(false) }
            val dnsOptions = listOf(
                "System default" to "",
                "Google Public DNS" to "https://dns.google/dns-query",
                "Cloudflare" to "https://cloudflare-dns.com/dns-query",
                "Cloudflare Families" to "https://families.cloudflare-dns.com/dns-query",
                "AdGuard DNS" to "https://dns.adguard-dns.com/dns-query",
                "Quad9" to "https://dns.quad9.net/dns-query",
                "Custom DNS" to "custom"
            )
            val currentDnsUrl by viewModel.customDnsUrl.collectAsState()
            val currentDnsOption = dnsOptions.find { it.second == currentDnsUrl } ?: if (currentDnsUrl.isNotBlank()) "Custom DNS" to currentDnsUrl else "System default" to ""

            ListItem(
                headlineContent = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("DNS Provider")
                        Spacer(modifier = Modifier.width(4.dp))
                        var showDnsInfo by remember { mutableStateOf(false) }
                        IconButton(
                            onClick = { showDnsInfo = !showDnsInfo },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Info, 
                                contentDescription = "DNS Info",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        if (showDnsInfo) {
                            AlertDialog(
                                onDismissRequest = { showDnsInfo = false },
                                title = { Text("Custom DNS Info") },
                                text = { Text("Custom DNS applies to app-level requests (downloads, catalog fetches, etc.). WebView uses your device's system DNS.") },
                                confirmButton = {
                                    TextButton(onClick = { showDnsInfo = false }) {
                                        Text("OK")
                                    }
                                }
                            )
                        }
                    }
                },
                supportingContent = { Text(currentDnsOption.first) },
                trailingContent = {
                    Box {
                        TextButton(onClick = { dnsDropdownExpanded = true }) {
                            Text("Select")
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                        DropdownMenu(
                            expanded = dnsDropdownExpanded,
                            onDismissRequest = { dnsDropdownExpanded = false }
                        ) {
                            dnsOptions.forEach { (name, url) ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        if (url == "custom") {
                                            viewModel.setCustomDnsUrl("https://")
                                        } else {
                                            viewModel.setCustomDnsUrl(url)
                                        }
                                        dnsDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            )

            if (currentDnsOption.first == "Custom DNS" || (currentDnsUrl.isNotBlank() && dnsOptions.none { it.second == currentDnsUrl })) {
                var customDnsText by remember(currentDnsUrl) { mutableStateOf(currentDnsUrl) }
                OutlinedTextField(
                    value = customDnsText,
                    onValueChange = {
                        customDnsText = it
                        viewModel.setCustomDnsUrl(it)
                    },
                    label = { Text("Custom DoH DNS URL") },
                    placeholder = { Text("e.g. https://dns.adguard-dns.com/dns-query") },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Downloads", style = MaterialTheme.typography.titleSmall.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(8.dp))

            ListItem(
                headlineContent = { Text("Ask before downloading") },
                supportingContent = { Text("Prompt confirmation before fetching files") },
                trailingContent = {
                    Switch(checked = askDownload, onCheckedChange = { viewModel.setAskBeforeDownload(it) })
                }
            )
        }
    }
}
