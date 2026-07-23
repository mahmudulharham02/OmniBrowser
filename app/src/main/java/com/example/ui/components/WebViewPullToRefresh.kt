package com.example.ui.components

import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewPullToRefresh(
    webView: WebView?,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    onScrollChanged: ((scrollY: Int, oldScrollY: Int) -> Unit)? = null
) {
    var isAtTop by remember { mutableStateOf(true) }

    DisposableEffect(webView) {
        if (webView != null) {
            webView.isNestedScrollingEnabled = true
            isAtTop = (webView.scrollY == 0)
            webView.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
                isAtTop = (scrollY <= 0)
                onScrollChanged?.invoke(scrollY, oldScrollY)
            }
        }
        onDispose {
            webView?.setOnScrollChangeListener(null)
        }
    }

    val state = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = isLoading,
        onRefresh = {
            if (isAtTop && (webView?.scrollY ?: 0) <= 0) {
                onRefresh()
            }
        },
        state = state,
        modifier = modifier.fillMaxSize()
    ) {
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
        }
    }
}
