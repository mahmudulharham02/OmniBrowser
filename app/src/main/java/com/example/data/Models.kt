package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// Tab state
data class TabModel(
    val id: String,                     // UUID
    val title: String,
    val url: String,                    // current URL
    val faviconUrl: String? = null,
    val isPrivate: Boolean = false,
    val isLoading: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val progress: Int = 0,              // 0-100
    val desktopMode: Boolean = false,
    val findInPageQuery: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val blockedCount: Int = 0
)

@Entity(tableName = "saved_tabs")
data class SavedTabEntity(
    @PrimaryKey val id: String,
    val url: String,
    val title: String,
    val faviconUrl: String?,
    val position: Int,
    val isPrivate: Boolean,
    val lastAccessedEpoch: Long
)

// History record
@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val title: String,
    val visitCount: Int = 1,
    val lastVisitEpoch: Long = System.currentTimeMillis(),
    val faviconUrl: String? = null
)

// Bookmark
@Entity(tableName = "bookmarks", primaryKeys = ["url"])
data class BookmarkEntity(
    val url: String,
    val title: String,
    val folder: String = "Default",
    val faviconUrl: String? = null,
    val addedAtEpoch: Long = System.currentTimeMillis()
)

// Download
@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val id: Long,          // matches DownloadManager ID
    val url: String,
    val filename: String,
    val mimeType: String,
    val totalBytes: Long,
    val downloadedBytes: Long = 0,
    val status: String,                // "pending" | "running" | "completed" | "failed" | "blocked"
    val refererUrl: String? = null,
    val startedAtEpoch: Long = System.currentTimeMillis(),
    val completedAtEpoch: Long? = null,
    val errorMessage: String? = null,
    val savePath: String? = null
)

// Installed extension record
@Entity(tableName = "extensions")
data class InstalledExtensionEntity(
    @PrimaryKey val id: String,        // extension id (from manifest or generated hash)
    val name: String,
    val version: String,
    val enabled: Boolean = true,
    val sourcePath: String,            // local file path of the unpacked extension
    val installedAtEpoch: Long = System.currentTimeMillis()
)

// Extension storage for storage.local
@Entity(tableName = "extension_storage", primaryKeys = ["extensionId", "key"])
data class ExtensionStorageEntity(
    val extensionId: String,
    val key: String,
    val value: String,                 // JSON-serialized string
    val updatedAtEpoch: Long = System.currentTimeMillis()
)

// Catalog index (remote & local cache)
@Entity(tableName = "catalog_index")
data class CatalogIndexEntity(
    @PrimaryKey val id: String,
    val name: String,
    val author: String,
    val description: String,
    val iconUrl: String,
    val screenshotUrlsJson: String,    // JSON array of strings
    val downloadUrl: String,
    val sizeBytes: Long,
    val permissionsJson: String,       // JSON array of strings
    val rating: Float,
    val ratingCount: Int,
    val category: String,
    val tagsJson: String,              // JSON array of strings
    val version: String,
    val minBrowserVersion: String,
    val changelog: String?,
    val lastFetchedEpoch: Long = System.currentTimeMillis()
) {
    // Helper accessors
    val screenshotUrls: List<String>
        get() = try {
            val arr = org.json.JSONArray(screenshotUrlsJson)
            List(arr.length()) { arr.getString(it) }
        } catch (e: Exception) { emptyList() }

    val permissions: List<String>
        get() = try {
            val arr = org.json.JSONArray(permissionsJson)
            List(arr.length()) { arr.getString(it) }
        } catch (e: Exception) { emptyList() }

    val tags: List<String>
        get() = try {
            val arr = org.json.JSONArray(tagsJson)
            List(arr.length()) { arr.getString(it) }
        } catch (e: Exception) { emptyList() }
}

// Track catalog installations
@Entity(tableName = "installed_from_catalog")
data class InstalledFromCatalogEntity(
    @PrimaryKey val extensionId: String,
    val installedAtEpoch: Long = System.currentTimeMillis(),
    val lastUpdatedEpoch: Long = System.currentTimeMillis()
)

// Reported extensions
@Entity(tableName = "reported_extensions")
data class ReportedExtensionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val extensionId: String,
    val reason: String,
    val reportedAtEpoch: Long = System.currentTimeMillis()
)

// Non-Entity Extension Manifest structures
data class ExtensionManifest(
    val manifestVersion: Int = 3,
    val id: String = "",
    val name: String,
    val version: String,
    val description: String? = null,
    val permissions: List<String> = emptyList(),
    val hostPermissions: List<String> = emptyList(),
    val contentScripts: List<ContentScriptSpec> = emptyList(),
    val background: BackgroundSpec? = null,
    val icons: Map<String, String> = emptyMap() // size -> asset path
)

data class ContentScriptSpec(
    val matches: List<String>,         // e.g. ["<all_urls>"]
    val js: List<String> = emptyList(),
    val css: List<String> = emptyList(),
    val runAt: String = "document_idle" // "document_start" | "document_end" | "document_idle"
)

data class BackgroundSpec(
    val scripts: List<String> = emptyList(),
    val persistent: Boolean = false
)
