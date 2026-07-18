package com.example.browser.engine

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import android.widget.Toast
import com.example.data.repository.DownloadRepository
import com.example.data.prefs.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class PendingDownload(
    val url: String,
    val userAgent: String,
    val contentDisposition: String,
    val mimeType: String,
    val contentLength: Long,
    val filename: String,
    val onConfirm: (Boolean) -> Unit
)

class DownloadHandler(
    private val context: Context,
    private val adBlockEngine: AdBlockEngine,
    private val downloadRepository: DownloadRepository,
    private val settingsDataStore: SettingsDataStore
) {
    private val _pendingDownload = MutableStateFlow<PendingDownload?>(null)
    val pendingDownload: StateFlow<PendingDownload?> = _pendingDownload.asStateFlow()

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    fun onDownloadStart(
        url: String,
        userAgent: String,
        contentDisposition: String,
        mimeType: String,
        contentLength: Long,
        refererUrl: String? = null
    ) {
        coroutineScope.launch {
            // 1. Block if URL is on the ad/tracker list
            val isAdOrTracker = withContext(Dispatchers.IO) {
                adBlockEngine.shouldBlock(url)
            }
            if (isAdOrTracker) {
                Toast.makeText(context, "Download blocked: Ad/tracker detected", Toast.LENGTH_SHORT).show()
                return@launch
            }

            // 2. Safety filter for dangerous executable formats
            if (isDangerousFile(url, mimeType)) {
                Toast.makeText(context, "Blocked potentially dangerous executable file", Toast.LENGTH_LONG).show()
                return@launch
            }

            val filename = parseFilename(url, contentDisposition, mimeType)
            val askBeforeDownload = settingsDataStore.askBeforeDownload.first()

            if (!askBeforeDownload) {
                startDownload(url, userAgent, contentDisposition, mimeType, contentLength, filename, refererUrl)
            } else {
                _pendingDownload.value = PendingDownload(
                    url = url,
                    userAgent = userAgent,
                    contentDisposition = contentDisposition,
                    mimeType = mimeType,
                    contentLength = contentLength,
                    filename = filename,
                    onConfirm = { confirmed ->
                        _pendingDownload.value = null
                        if (confirmed) {
                            startDownload(url, userAgent, contentDisposition, mimeType, contentLength, filename, refererUrl)
                        }
                    }
                )
            }
        }
    }

    private fun startDownload(
        url: String,
        userAgent: String,
        contentDisposition: String,
        mimeType: String,
        contentLength: Long,
        filename: String,
        refererUrl: String?
    ) {
        try {
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setMimeType(mimeType)
                setTitle(filename)
                setDescription("Downloading from ${Uri.parse(url).host}")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
                addRequestHeader("User-Agent", userAgent)
                if (refererUrl != null) {
                    addRequestHeader("Referer", refererUrl)
                }
            }

            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = dm.enqueue(request)

            coroutineScope.launch(Dispatchers.IO) {
                downloadRepository.recordDownload(
                    id = downloadId,
                    url = url,
                    filename = filename,
                    mimeType = mimeType,
                    totalBytes = contentLength,
                    status = "running",
                    refererUrl = refererUrl
                )
            }
            Toast.makeText(context, "Starting download: $filename", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Download failed to initiate: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isDangerousFile(url: String, mimeType: String): Boolean {
        val dangerousExtensions = listOf(".exe", ".bat", ".cmd", ".sh", ".vbs", ".scr", ".lnk")
        val lowercaseUrl = url.lowercase()
        if (dangerousExtensions.any { lowercaseUrl.endsWith(it) }) return true
        
        val dangerousMimes = listOf("application/x-msdownload", "application/x-sh", "application/x-bat")
        if (dangerousMimes.contains(mimeType.lowercase())) return true
        
        return false
    }

    private fun parseFilename(url: String, contentDisposition: String?, mimeType: String?): String {
        var filename = ""
        if (!contentDisposition.isNullOrEmpty()) {
            val parts = contentDisposition.split(";")
            for (part in parts) {
                val trimmed = part.trim()
                if (trimmed.startsWith("filename=")) {
                    filename = trimmed.substringAfter("filename=").replace("\"", "").trim()
                    break
                } else if (trimmed.startsWith("filename*=")) {
                    filename = trimmed.substringAfter("filename*=").substringAfter("''").replace("\"", "").trim()
                    // URL decode if needed
                    filename = Uri.decode(filename)
                    break
                }
            }
        }

        if (filename.isEmpty()) {
            filename = url.substringAfterLast('/').substringBefore('?').substringBefore('#')
            if (filename.isEmpty()) {
                filename = "download_" + System.currentTimeMillis()
            }
        }

        // Add extension if missing
        if (!filename.contains(".") && !mimeType.isNullOrEmpty()) {
            val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
            if (ext != null) {
                filename = "$filename.$ext"
            }
        }

        return filename
    }
}
