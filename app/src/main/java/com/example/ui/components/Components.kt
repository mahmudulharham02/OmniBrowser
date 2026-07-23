@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BookmarkEntity
import com.example.data.DownloadEntity
import com.example.data.InstalledExtensionEntity
import com.example.data.HistoryEntity
import com.example.data.TabModel

@Composable
fun AddressBar(
    url: String,
    isLoading: Boolean,
    progress: Int,
    blockedCount: Int,
    onUrlSubmit: (String) -> Unit,
    onReload: () -> Unit,
    onStop: () -> Unit,
    onShieldClick: () -> Unit,
    modifier: Modifier = Modifier,
    customDnsUrl: String = "",
    isPrivate: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }
    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = if (url == "omni://home") "" else url,
                selection = TextRange.Zero
            )
        )
    }
    LaunchedEffect(url) {
        val newText = if (url == "omni://home") "" else url
        textFieldValue = TextFieldValue(
            text = newText,
            selection = if (isFocused) TextRange(0, newText.length) else TextRange.Zero
        )
    }

    var suggestions by remember { mutableStateOf<List<com.example.search.SearchSuggestion>>(emptyList()) }

    LaunchedEffect(textFieldValue.text, isFocused) {
        if (!isFocused || textFieldValue.text.isBlank()) {
            suggestions = emptyList()
            return@LaunchedEffect
        }
        kotlinx.coroutines.delay(150)
        suggestions = com.example.search.SearchSuggestionRepository.fetchSuggestions(textFieldValue.text)
    }

    val isHttps = url.startsWith("https://")
    val isLocal = url.startsWith("omni://")

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .heightIn(min = 36.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // HTTPS / HTTP Indicator Icon
            Icon(
                imageVector = when {
                    isLocal -> Icons.Default.Home
                    isHttps -> Icons.Default.Lock
                    else -> Icons.Default.Warning
                },
                contentDescription = if (isHttps) "Secure connection" else "Not secure",
                tint = when {
                    isLocal -> MaterialTheme.colorScheme.primary
                    isHttps -> Color(0xFF4CAF50)
                    else -> Color(0xFFF44336)
                },
                modifier = Modifier
                    .size(20.dp)
                    .combinedClickable(
                        onLongClick = { /* Copy url */ },
                        onClick = {}
                    )
            )

            if (isPrivate) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.VisibilityOff,
                    contentDescription = "Private Tab",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (customDnsUrl.isNotBlank()) {
                val context = androidx.compose.ui.platform.LocalContext.current
                val dnsProviderName = when (customDnsUrl) {
                    "https://dns.google/dns-query" -> "Google Public DNS"
                    "https://cloudflare-dns.com/dns-query" -> "Cloudflare DNS"
                    "https://families.cloudflare-dns.com/dns-query" -> "Cloudflare Families"
                    "https://dns.adguard-dns.com/dns-query" -> "AdGuard DNS"
                    "https://dns.quad9.net/dns-query" -> "Quad9 DNS"
                    else -> "Custom DNS"
                }
                Surface(
                    onClick = {
                        android.widget.Toast.makeText(context, "Active DNS: $dnsProviderName", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🛡️ DNS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            // Text Input Field
            BasicTextField(
                value = textFieldValue,
                onValueChange = { textFieldValue = it },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        val input = textFieldValue.text
                        if (input.trim().isNotEmpty()) {
                            onUrlSubmit(input)
                        }
                    }
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused && !isFocused) {
                            if (textFieldValue.text.isNotEmpty()) {
                                textFieldValue = textFieldValue.copy(
                                    selection = TextRange(0, textFieldValue.text.length)
                                )
                            }
                        }
                        isFocused = focusState.isFocused
                    }
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyUp && 
                            (keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)) {
                            val input = textFieldValue.text
                            if (input.trim().isNotEmpty()) {
                                onUrlSubmit(input)
                                true
                            } else {
                                false
                            }
                        } else {
                            false
                        }
                    }
                    .testTag("url_input"),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (textFieldValue.text.isEmpty()) {
                            Text(
                                text = "Search or type URL",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        innerTextField()
                    }
                }
            )



            // Reload or Stop Button
            IconButton(
                onClick = { if (isLoading) onStop() else onReload() },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isLoading) Icons.Default.Close else Icons.Default.Refresh,
                    contentDescription = if (isLoading) "Stop Loading" else "Reload page",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Horizontal Loading Progress Bar
        if (isLoading && progress < 100) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
            )
        }

        if (isFocused && suggestions.isNotEmpty()) {
            SearchSuggestionsOverlay(
                suggestions = suggestions,
                onSuggestionSelected = { selected ->
                    textFieldValue = TextFieldValue(text = selected, selection = TextRange(selected.length))
                    suggestions = emptyList()
                    onUrlSubmit(selected)
                },
                onSuggestionInserted = { inserted ->
                    textFieldValue = TextFieldValue(text = inserted, selection = TextRange(inserted.length))
                }
            )
        }
    }
}

@Composable
fun NavigationToolbar(
    canGoBack: Boolean,
    canGoForward: Boolean,
    tabCount: Int,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onHome: () -> Unit,
    onTabsClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, enabled = canGoBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Navigate back",
                    tint = if (canGoBack) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }

            IconButton(onClick = onForward, enabled = canGoForward) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Navigate forward",
                    tint = if (canGoForward) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }

            IconButton(onClick = onHome) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Homepage"
                )
            }

            IconButton(onClick = onTabsClick) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Layers,
                        contentDescription = "View tabs"
                    )
                    if (tabCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 6.dp, y = (-6).dp)
                                .size(18.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tabCount.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }

            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Main menu"
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TabCard(
    tab: TabModel,
    thumbnail: Bitmap?,
    isActive: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp)
            .height(180.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                tab.isPrivate && isActive -> Color(0xFF333333)
                tab.isPrivate -> Color(0xFF222222)
                isActive -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = RoundedCornerShape(12.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (tab.isPrivate) Icons.Default.VisibilityOff else Icons.Default.Web,
                        contentDescription = null,
                        tint = when {
                            tab.isPrivate -> Color.LightGray
                            isActive -> MaterialTheme.colorScheme.onPrimaryContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = tab.title,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = when {
                            tab.isPrivate -> Color.White
                            isActive -> MaterialTheme.colorScheme.onPrimaryContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close tab",
                        tint = when {
                            tab.isPrivate -> Color.LightGray
                            isActive -> MaterialTheme.colorScheme.onPrimaryContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Preview Thumbnail
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                if (thumbnail != null) {
                    Image(
                        bitmap = thumbnail.asImageBitmap(),
                        contentDescription = "Tab preview",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Language,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            
            // Footer URL
            Text(
                text = if (tab.url == "omni://home") "omni://home" else tab.url,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
fun ShortcutTile(
    title: String,
    url: String,
    isPinned: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onTogglePin: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(modifier = modifier.padding(8.dp)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(72.dp)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showMenu = true }
                )
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title.take(1).uppercase(),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                if (isPinned) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(2.dp),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Pinned",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text(if (isPinned) "Unpin" else "Pin") },
                onClick = {
                    showMenu = false
                    onTogglePin()
                },
                leadingIcon = { Icon(Icons.Default.PushPin, null) }
            )
            DropdownMenuItem(
                text = { Text("Edit") },
                onClick = {
                    showMenu = false
                    onEdit()
                },
                leadingIcon = { Icon(Icons.Default.Edit, null) }
            )
            DropdownMenuItem(
                text = { Text("Delete") },
                onClick = {
                    showMenu = false
                    onDelete()
                },
                leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
            )
        }
    }
}

@Composable
fun DownloadRow(
    download: DownloadEntity,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = download.filename,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    color = when (download.status) {
                        "completed" -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                        "running", "pending" -> MaterialTheme.colorScheme.primaryContainer
                        "failed" -> Color(0xFFF44336).copy(alpha = 0.2f)
                        "blocked" -> Color(0xFFFF9800).copy(alpha = 0.2f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = download.status.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = when (download.status) {
                            "completed" -> Color(0xFF388E3C)
                            "running", "pending" -> MaterialTheme.colorScheme.primary
                            "failed" -> Color(0xFFD32F2F)
                            "blocked" -> Color(0xFFF57C00)
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress tracking
            if (download.status == "running" && download.totalBytes > 0) {
                val progress = download.downloadedBytes.toFloat() / download.totalBytes
                Column {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${download.downloadedBytes / 1024} KB of ${download.totalBytes / 1024} KB",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (download.totalBytes > 0) {
                Text(
                    text = "Size: ${download.totalBytes / 1024} KB",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (download.status == "completed") {
                    TextButton(onClick = onOpen) {
                        Icon(Icons.Default.Launch, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Open")
                    }
                }
                TextButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Remove")
                }
            }
        }
    }
}

@Composable
fun ExtensionRow(
    extension: InstalledExtensionEntity,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onUninstall: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Extension,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = extension.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Version ${extension.version}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggle
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Path: " + extension.sourcePath.substringAfterLast("/"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                TextButton(
                    onClick = onUninstall,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Uninstall")
                }
            }
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)) },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ChromeTopBar(
    url: String,
    isLoading: Boolean,
    progress: Int,
    tabCount: Int,
    onUrlSubmit: (String) -> Unit,
    onReload: () -> Unit,
    onStop: () -> Unit,
    onTabsClick: () -> Unit,
    onMenuClick: () -> Unit,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onBack: () -> Unit,
    onForward: () -> Unit,
    modifier: Modifier = Modifier,
    customDnsUrl: String = "",
    isPrivate: Boolean = false
) {
    val topBarColor = if (isPrivate) Color(0xFF1E1E1E) else MaterialTheme.colorScheme.surface
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = topBarColor,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Back button
            IconButton(
                onClick = onBack,
                enabled = canGoBack,
                modifier = Modifier.size(40.dp).testTag("top_back_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = if (canGoBack) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }

            // Center: CompactAddressBar with URL field
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            ) {
                CompactAddressBar(
                    url = url,
                    isLoading = isLoading,
                    progress = progress,
                    blockedCount = 0,
                    onUrlSubmit = onUrlSubmit,
                    onReload = onReload,
                    onStop = onStop,
                    onShieldClick = {},
                    customDnsUrl = customDnsUrl,
                    isPrivate = isPrivate
                )
            }

            // Right: Tab switcher count & Menu options
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Tab Counter ([1])
                IconButton(
                    onClick = onTabsClick,
                    modifier = Modifier.size(40.dp).testTag("top_tabs_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Layers,
                            contentDescription = "View tabs"
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

                // Menu Trigger (⋮)
                IconButton(
                    onClick = onMenuClick,
                    modifier = Modifier.size(40.dp).testTag("top_menu_button")
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                }
            }
        }
    }
}

@Composable
fun ChromeBottomBar(
    url: String,
    isLoading: Boolean,
    progress: Int,
    blockedCount: Int,
    tabCount: Int,
    onUrlSubmit: (String) -> Unit,
    onReload: () -> Unit,
    onStop: () -> Unit,
    onShieldClick: () -> Unit,
    onHomeClick: () -> Unit,
    onNewTabClick: () -> Unit,
    onTabsClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    customDnsUrl: String = "",
    isPrivate: Boolean = false
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Home Icon
            IconButton(
                onClick = onHomeClick,
                modifier = Modifier.size(40.dp).testTag("bottom_home_button")
            ) {
                Icon(Icons.Default.Home, contentDescription = "Home")
            }

            // Center: URL/Search Bar
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            ) {
                CompactAddressBar(
                    url = url,
                    isLoading = isLoading,
                    progress = progress,
                    blockedCount = blockedCount,
                    onUrlSubmit = onUrlSubmit,
                    onReload = onReload,
                    onStop = onStop,
                    onShieldClick = onShieldClick,
                    customDnsUrl = customDnsUrl,
                    isPrivate = isPrivate
                )
            }

            // Right: New Tab (+), Tab Counter ([1]), Menu (⋮)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // New Tab (+)
                IconButton(
                    onClick = onNewTabClick,
                    modifier = Modifier.size(40.dp).testTag("bottom_new_tab_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New Tab")
                }

                // Tab Counter ([1])
                IconButton(
                    onClick = onTabsClick,
                    modifier = Modifier.size(40.dp).testTag("bottom_tabs_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Layers,
                            contentDescription = "View tabs"
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

                // Menu (⋮)
                IconButton(
                    onClick = onMenuClick,
                    modifier = Modifier.size(40.dp).testTag("bottom_menu_button")
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                }
            }
        }
    }
}

@Composable
fun CompactAddressBar(
    url: String,
    isLoading: Boolean,
    progress: Int,
    blockedCount: Int,
    onUrlSubmit: (String) -> Unit,
    onReload: () -> Unit,
    onStop: () -> Unit,
    onShieldClick: () -> Unit,
    modifier: Modifier = Modifier,
    customDnsUrl: String = "",
    isPrivate: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }
    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = if (url == "omni://home") "" else url,
                selection = TextRange.Zero
            )
        )
    }
    LaunchedEffect(url) {
        val newText = if (url == "omni://home") "" else url
        textFieldValue = TextFieldValue(
            text = newText,
            selection = if (isFocused) TextRange(0, newText.length) else TextRange.Zero
        )
    }

    var suggestions by remember { mutableStateOf<List<com.example.search.SearchSuggestion>>(emptyList()) }

    LaunchedEffect(textFieldValue.text, isFocused) {
        if (!isFocused || textFieldValue.text.isBlank()) {
            suggestions = emptyList()
            return@LaunchedEffect
        }
        kotlinx.coroutines.delay(150)
        suggestions = com.example.search.SearchSuggestionRepository.fetchSuggestions(textFieldValue.text)
    }

    val isHttps = url.startsWith("https://")
    val isLocal = url.startsWith("omni://")

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp)
                .heightIn(min = 36.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // HTTPS / HTTP Indicator Icon
            Icon(
                imageVector = when {
                    isLocal -> Icons.Default.Home
                    isHttps -> Icons.Default.Lock
                    else -> Icons.Default.Warning
                },
                contentDescription = if (isHttps) "Secure connection" else "Not secure",
                tint = when {
                    isLocal -> MaterialTheme.colorScheme.primary
                    isHttps -> Color(0xFF4CAF50)
                    else -> Color(0xFFF44336)
                },
                modifier = Modifier.size(16.dp)
            )

            if (isPrivate) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.VisibilityOff,
                    contentDescription = "Private Tab",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Text Input Field
            BasicTextField(
                value = textFieldValue,
                onValueChange = { textFieldValue = it },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        val input = textFieldValue.text
                        if (input.trim().isNotEmpty()) {
                            onUrlSubmit(input)
                        }
                    }
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused && !isFocused) {
                            if (textFieldValue.text.isNotEmpty()) {
                                textFieldValue = textFieldValue.copy(
                                    selection = TextRange(0, textFieldValue.text.length)
                                )
                            }
                        }
                        isFocused = focusState.isFocused
                    }
                    .testTag("url_input_bottom"),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (textFieldValue.text.isEmpty()) {
                            Text(
                                text = "Search or type URL",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                maxLines = 1
                            )
                        }
                        innerTextField()
                    }
                }
            )



            // Reload or Stop Button
            IconButton(
                onClick = { if (isLoading) onStop() else onReload() },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = if (isLoading) Icons.Default.Close else Icons.Default.Refresh,
                    contentDescription = if (isLoading) "Stop Loading" else "Reload page",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Horizontal Loading Progress Bar
        if (isLoading && progress < 100) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            )
        }

        if (isFocused && suggestions.isNotEmpty()) {
            SearchSuggestionsOverlay(
                suggestions = suggestions,
                onSuggestionSelected = { selected ->
                    textFieldValue = TextFieldValue(text = selected, selection = TextRange(selected.length))
                    suggestions = emptyList()
                    onUrlSubmit(selected)
                },
                onSuggestionInserted = { inserted ->
                    textFieldValue = TextFieldValue(text = inserted, selection = TextRange(inserted.length))
                }
            )
        }
    }
}

@Composable
fun RedesignedMenuPopup(
    visible: Boolean,
    onDismiss: () -> Unit,
    onNewTab: () -> Unit,
    onNewIncognitoTab: () -> Unit,
    onHistory: () -> Unit,
    onDeleteBrowsingData: () -> Unit,
    onDownloads: () -> Unit,
    onBookmarks: () -> Unit,
    onRecentTabs: () -> Unit,
    onShare: () -> Unit,
    onFindInPage: () -> Unit,
    onTranslate: () -> Unit,
    onAddToHomeScreen: () -> Unit,
    isDesktopSite: Boolean,
    onToggleDesktopSite: () -> Unit,
    isDarkTheme: Boolean = false,
    onToggleDarkTheme: (() -> Unit)? = null,
    onSettings: () -> Unit,
    onHelpFeedback: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp)
                    .clickable(enabled = false) {},
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(28.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .size(width = 36.dp, height = 4.dp)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Browser Options",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        if (onToggleDarkTheme != null) {
                            FilterChip(
                                selected = isDarkTheme,
                                onClick = { onToggleDarkTheme() },
                                label = { Text(if (isDarkTheme) "Dark Mode" else "Light Mode") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                                        contentDescription = "Toggle Theme",
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                modifier = Modifier.testTag("menu_theme_chip")
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                            .verticalScroll(androidx.compose.foundation.rememberScrollState())
                    ) {
                        val items = mutableListOf<MenuItemData>().apply {
                            add(MenuItemData("New Tab", Icons.Default.Add, onNewTab))
                            add(MenuItemData("New Incognito Tab", Icons.Default.VisibilityOff, onNewIncognitoTab))
                            if (onToggleDarkTheme != null) {
                                add(
                                    MenuItemData(
                                        if (isDarkTheme) "Switch to Light Theme" else "Switch to Dark Theme",
                                        if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                        { onToggleDarkTheme() }
                                    )
                                )
                            }
                            add(MenuItemData("History", Icons.Default.History, onHistory))
                            add(MenuItemData("Delete Browsing Data", Icons.Default.Delete, onDeleteBrowsingData))
                            add(MenuItemData("Downloads", Icons.Default.FileDownload, onDownloads))
                            add(MenuItemData("Bookmarks", Icons.Default.Bookmark, onBookmarks))
                            add(MenuItemData("Recent Tabs", Icons.Default.Undo, onRecentTabs))
                            add(MenuItemData("Share", Icons.Default.Share, onShare))
                            add(MenuItemData("Find in Page", Icons.Default.Search, onFindInPage))
                            add(MenuItemData("Translate", Icons.Default.Language, onTranslate))
                            add(MenuItemData("Add to Home Screen", Icons.Default.Home, onAddToHomeScreen))
                            add(
                                MenuItemData(
                                    if (isDesktopSite) "Request Mobile Site" else "Request Desktop Site",
                                    Icons.Default.Phonelink,
                                    onToggleDesktopSite
                                )
                            )
                            add(MenuItemData("Settings", Icons.Default.Settings, onSettings))
                            add(MenuItemData("Help & Feedback", Icons.Default.Info, onHelpFeedback))
                        }

                        items.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onDismiss()
                                        item.onClick()
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.text,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = item.text,
                                    style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface)
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        }
                    }
                }
            }
        }
    }
}

data class MenuItemData(
    val text: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val onClick: () -> Unit
)
