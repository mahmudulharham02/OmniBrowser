package com.omnibrowser.app.components

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.omnibrowser.app.search.SearchEngine
import kotlinx.coroutines.delay

/**
 * ⚠️ CRITICAL COMPONENT — DO NOT BREAK THE SEARCH ⚠️
 *
 * This Composable is the only way users can submit a search in OmniBrowser.
 * If you change this, you MUST:
 *  1. Run SearchEngineTest (unit test) — should all pass
 *  2. Run SearchBarTest (instrumentation test) — should all pass
 *  3. Manually test: type a query, press search key, verify browser opens
 *  4. Manually test: type a query, tap the Go button, verify browser opens
 *
 * The user has reported this being broken 5+ times. Do not regress it.
 * If you must change this file, add a comment explaining why and add a test
 * that covers the new behavior.
 */
@Composable
fun SearchBar(
    searchEngineUrl: String,
    onSubmit: (String) -> Unit,
    modifier: Modifier = Modifier,
    initialValue: String = "",
    autoFocus: Boolean = false,
    placeholder: String = "Search or type URL",
    inputTestTag: String = "search_input",
    goButtonTestTag: String = "search_go_button"
) {
    var text by remember(initialValue) { mutableStateOf(initialValue) }
    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(autoFocus) {
        if (autoFocus) {
            // Small delay to ensure layout is ready
            delay(100)
            runCatching { focusRequester.requestFocus() }
        }
    }
    
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { 
                text = it
                Log.d("OmniSearch", "Text changed: '$it'")
            },
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .onKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyUp && 
                        (keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)) {
                        if (text.trim().isNotEmpty()) {
                            performSearch(text, searchEngineUrl, onSubmit)
                            true
                        } else {
                            false
                        }
                    } else {
                        false
                    }
                }
                .testTag(inputTestTag),
            placeholder = { Text(placeholder) },
            leadingIcon = {
                IconButton(
                    onClick = { performSearch(text, searchEngineUrl, onSubmit) },
                    modifier = Modifier.testTag("search_icon_button")
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
            },
            trailingIcon = {
                if (text.isNotEmpty()) {
                    IconButton(
                        onClick = { text = "" },
                        modifier = Modifier.testTag("search_clear_button")
                    ) {
                        Icon(Icons.Default.Close, "Clear")
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Search,
                keyboardType = KeyboardType.Uri
            ),
            keyboardActions = KeyboardActions(
                onSearch = { performSearch(text, searchEngineUrl, onSubmit) }
            ),
            shape = RoundedCornerShape(28.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )
        
        Spacer(modifier = Modifier.width(8.dp))

        // Visible Go button as a backup submit path
        IconButton(
            onClick = { performSearch(text, searchEngineUrl, onSubmit) },
            enabled = text.isNotBlank(),
            modifier = Modifier
                .size(48.dp)
                .testTag(goButtonTestTag)
        ) {
            Icon(
                Icons.Default.ArrowForward,
                contentDescription = "Go",
                tint = if (text.isNotBlank()) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun performSearch(
    text: String,
    searchEngineUrl: String,
    onSubmit: (String) -> Unit
) {
    val url = SearchEngine.resolveUrl(text, searchEngineUrl) ?: return
    if (SearchEngine.isValidUrl(url)) {
        onSubmit(url)
    }
}
