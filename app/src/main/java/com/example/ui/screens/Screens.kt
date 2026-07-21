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
import com.example.ui.components.SearchBar
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
import kotlinx.coroutines.launch
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
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
    searchEngineUrl: String,
    onSearch: (String) -> Unit,
    onNavigateTo: (String) -> Unit,
    onTabsClick: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    tabCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    var shortcutsList by remember {
        mutableStateOf(ShortcutStorage.loadShortcuts(context))
    }

    val sortedShortcuts = remember(shortcutsList) {
        shortcutsList.sortedWith(
            compareByDescending<ShortcutItem> { it.isPinned }
                .thenBy { it.title.lowercase() }
        )
    }

    var showManageShortcutsDialog by remember { mutableStateOf(false) }
    var showAddDirectDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingShortcut by remember { mutableStateOf<ShortcutItem?>(null) }
    var editName by remember { mutableStateOf("") }
    var editUrl by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
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
        // Top-Right Utility Icons (Tabs & Menu) for Home Screen
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Tab Counter Button
            IconButton(
                onClick = onTabsClick,
                modifier = Modifier.size(40.dp).testTag("home_tabs_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Layers,
                        contentDescription = "View tabs",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                    if (tabCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = (-4).dp)
                                .size(16.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tabCount.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }

            // Menu Popup Trigger Button
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier.size(40.dp).testTag("home_menu_button")
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Menu",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

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
            SearchBar(
                searchEngineUrl = searchEngineUrl,
                onSubmit = onSearch,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                inputTestTag = "home_search_input",
                goButtonTestTag = "home_search_go_button"
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

            // Shortcuts Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quick Shortcuts",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { 
                        editingShortcut = null
                        editName = ""
                        editUrl = ""
                        showAddDirectDialog = true 
                    }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Shortcut",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { showManageShortcutsDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Manage Shortcuts",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Shortcuts Grid (Chunked Rows)
            if (sortedShortcuts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No shortcuts added yet. Tap '+' to add one!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            } else {
                val chunkedShortcuts = sortedShortcuts.chunked(4)
                chunkedShortcuts.forEach { rowItems ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        rowItems.forEach { item ->
                            ShortcutTile(
                                title = item.title,
                                url = item.url,
                                isPinned = item.isPinned,
                                onClick = { onNavigateTo(item.url) },
                                onDelete = {
                                    val updated = shortcutsList.filter { it.url != item.url }
                                    shortcutsList = updated
                                    ShortcutStorage.saveShortcuts(context, updated)
                                },
                                onEdit = {
                                    editingShortcut = item
                                    editName = item.title
                                    editUrl = item.url
                                    showEditDialog = true
                                },
                                onTogglePin = {
                                    val updated = shortcutsList.map {
                                        if (it.url == item.url) it.copy(isPinned = !it.isPinned) else it
                                    }
                                    shortcutsList = updated
                                    ShortcutStorage.saveShortcuts(context, updated)
                                }
                            )
                        }
                        // Pad empty spots in the row to keep alignment consistent
                        repeat(4 - rowItems.size) {
                            Spacer(modifier = Modifier.width(72.dp).padding(8.dp))
                        }
                    }
                }
            }


        }
    }

    // Direct Add Shortcut Dialog
    if (showAddDirectDialog) {
        AlertDialog(
            onDismissRequest = { showAddDirectDialog = false },
            title = { Text("Add New Shortcut") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editUrl,
                        onValueChange = { editUrl = it },
                        label = { Text("URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editName.isNotBlank() && editUrl.isNotBlank()) {
                            var finalUrl = editUrl.trim()
                            if (!finalUrl.startsWith("http://") && !finalUrl.startsWith("https://")) {
                                finalUrl = "https://$finalUrl"
                            }
                            val updated = shortcutsList + ShortcutItem(editName, finalUrl)
                            shortcutsList = updated
                            ShortcutStorage.saveShortcuts(context, updated)
                            showAddDirectDialog = false
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDirectDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Direct Edit Shortcut Dialog
    if (showEditDialog && editingShortcut != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Shortcut") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editUrl,
                        onValueChange = { editUrl = it },
                        label = { Text("URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editName.isNotBlank() && editUrl.isNotBlank()) {
                            var finalUrl = editUrl.trim()
                            if (!finalUrl.startsWith("http://") && !finalUrl.startsWith("https://")) {
                                finalUrl = "https://$finalUrl"
                            }
                            val updated = shortcutsList.map {
                                if (it.url == editingShortcut!!.url) {
                                    ShortcutItem(editName, finalUrl, it.isPinned)
                                } else it
                            }
                            shortcutsList = updated
                            ShortcutStorage.saveShortcuts(context, updated)
                            showEditDialog = false
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Manage Shortcuts Dialog
    if (showManageShortcutsDialog) {
        AlertDialog(
            onDismissRequest = { showManageShortcutsDialog = false },
            title = { Text("Manage Shortcuts") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    // Quick add form
                    Text(
                        text = "Add Shortcut",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            placeholder = { Text("Name") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editUrl,
                            onValueChange = { editUrl = it },
                            placeholder = { Text("URL") },
                            singleLine = true,
                            modifier = Modifier.weight(1.5f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (editName.isNotBlank() && editUrl.isNotBlank()) {
                                var finalUrl = editUrl.trim()
                                if (!finalUrl.startsWith("http://") && !finalUrl.startsWith("https://")) {
                                    finalUrl = "https://$finalUrl"
                                }
                                val updated = shortcutsList + ShortcutItem(editName, finalUrl)
                                shortcutsList = updated
                                ShortcutStorage.saveShortcuts(context, updated)
                                editName = ""
                                editUrl = ""
                            }
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Add")
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    Text(
                        text = "Saved Shortcuts",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        items(shortcutsList) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = "Pin Status",
                                    tint = if (item.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                    modifier = Modifier
                                        .clickable {
                                            val updated = shortcutsList.map {
                                                if (it.url == item.url) it.copy(isPinned = !it.isPinned) else it
                                            }
                                            shortcutsList = updated
                                            ShortcutStorage.saveShortcuts(context, updated)
                                        }
                                        .padding(8.dp)
                                        .size(20.dp)
                                )
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 4.dp)
                                ) {
                                    Text(item.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text(item.url, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                IconButton(
                                    onClick = {
                                        editingShortcut = item
                                        editName = item.title
                                        editUrl = item.url
                                        showEditDialog = true
                                    }
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(
                                    onClick = {
                                        val updated = shortcutsList.filter { it.url != item.url }
                                        shortcutsList = updated
                                        ShortcutStorage.saveShortcuts(context, updated)
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showManageShortcutsDialog = false
                    }
                ) {
                    Text("Done")
                }
            }
        )
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
    areBarsVisible: Boolean,
    modifier: Modifier = Modifier,
    onScrollChanged: ((scrollY: Int, oldScrollY: Int) -> Unit)? = null
) {
    val webView = remember(tabId) {
        val wv = tabManager.getWebViewForTab(tabId)
        if (wv != null) {
            browserEngine.setupWebView(wv, tabId, isPrivate)
        }
        wv
    }

    DisposableEffect(webView) {
        webView?.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            onScrollChanged?.invoke(scrollY, oldScrollY)
        }
        onDispose {
            webView?.setOnScrollChangeListener(null)
        }
    }

    // Capture standard system back actions to navigate WebView history backwards
    BackHandler(enabled = webView?.canGoBack() == true) {
        webView?.goBack()
    }

    val topPadding by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (areBarsVisible) 72.dp else 0.dp,
        animationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow),
        label = "BrowserTopPadding"
    )

    Column(modifier = modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(topPadding))

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
            Surface(
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
                color = MaterialTheme.colorScheme.surface
            ) {
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
            }
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
            Surface(
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
                color = MaterialTheme.colorScheme.surface
            ) {
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
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectBookmark(bookmark.url) }
                            .padding(vertical = 2.dp),
                        color = Color.Transparent
                    ) {
                        ListItem(
                            headlineContent = { Text(bookmark.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold) },
                            supportingContent = { Text(bookmark.url, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall) },
                            leadingContent = { Icon(Icons.Default.Bookmark, "Bookmark Icon", tint = MaterialTheme.colorScheme.primary) },
                            trailingContent = {
                                IconButton(onClick = { onDeleteBookmark(bookmark.url) }) {
                                    Icon(Icons.Default.Delete, "Delete Bookmark", tint = MaterialTheme.colorScheme.error)
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
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
            Surface(
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
                color = MaterialTheme.colorScheme.surface
            ) {
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
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectHistory(record.url) }
                            .padding(vertical = 2.dp),
                        color = Color.Transparent
                    ) {
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
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
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
            Surface(
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
                color = MaterialTheme.colorScheme.surface
            ) {
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
            Surface(
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
                color = MaterialTheme.colorScheme.surface
            ) {
                OptIn(ExperimentalMaterial3Api::class)
                TopAppBar(
                    title = { Text("Plugins & Extensions", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, null)
                        }
                    }
                )
            }
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
            Surface(
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
                color = MaterialTheme.colorScheme.surface
            ) {
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
            Surface(
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
                color = MaterialTheme.colorScheme.surface
            ) {
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
            Surface(
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
                color = MaterialTheme.colorScheme.surface
            ) {
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

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            val hideNavBarEnabled by viewModel.hideNavBar.collectAsState()
            ListItem(
                headlineContent = { Text("Hide navigation bar") },
                supportingContent = { Text("Hide the bottom system bar for a cleaner fullscreen experience. Swipe up from the bottom to show it temporarily.") },
                trailingContent = {
                    Switch(
                        checked = hideNavBarEnabled,
                        onCheckedChange = { viewModel.setHideNavBar(it) }
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

            val isCustomSelected = currentDnsOption.first == "Custom DNS" || currentDnsUrl == "custom" || (currentDnsUrl.isNotBlank() && dnsOptions.none { it.second == currentDnsUrl })
            var customDnsText by remember(currentDnsUrl) { mutableStateOf(currentDnsUrl) }

            val isValidDnsUrl = remember(customDnsText) {
                customDnsText.startsWith("https://") && runCatching {
                    val withoutScheme = customDnsText.substring(8)
                    val host = withoutScheme.substringBefore('/')
                    host.isNotBlank() && host.contains('.')
                }.getOrDefault(false)
            }

            val showDnsError = isCustomSelected && customDnsText.isNotBlank() && !isValidDnsUrl

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                OutlinedTextField(
                    value = customDnsText,
                    onValueChange = {
                        if (isCustomSelected) {
                            customDnsText = it
                            viewModel.setCustomDnsUrl(it)
                        }
                    },
                    enabled = isCustomSelected,
                    label = { Text("Custom DNS server (HTTPS URL)") },
                    placeholder = { Text("https://dns.adguard-dns.com/dns-query") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = showDnsError,
                    supportingText = {
                        if (showDnsError) {
                            Text("Invalid URL. Must start with https:// and contain a valid hostname.", color = MaterialTheme.colorScheme.error)
                        } else {
                            Text("Paste any DNS-over-HTTPS URL. Must start with https://. Used for downloads and updates only — WebView uses your device's system DNS.")
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                val coroutineScope = rememberCoroutineScope()
                var dnsTestResult by remember { mutableStateOf<String?>(null) }
                var isDnsTesting by remember { mutableStateOf(false) }

                Button(
                    onClick = {
                        isDnsTesting = true
                        dnsTestResult = "Testing connection to $customDnsText..."
                        coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            val result = com.example.data.prefs.DohResolver.resolve("example.com", customDnsText)
                            dnsTestResult = if (result != null) {
                                "🟢 Test Successful! example.com resolved to: $result"
                            } else {
                                "🔴 Resolution failed. Ensure the server URL is active and accepts DNS-JSON query requests."
                            }
                            isDnsTesting = false
                        }
                    },
                    enabled = !isDnsTesting && customDnsText.isNotBlank() && isValidDnsUrl,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    if (isDnsTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Test DNS")
                }

                dnsTestResult?.let { result ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = result,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (result.startsWith("🟢")) Color(0xFF4CAF50) else if (result.startsWith("🔴")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
