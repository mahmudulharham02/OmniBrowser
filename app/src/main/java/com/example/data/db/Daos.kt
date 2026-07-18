package com.example.data.db

import androidx.room.*
import com.example.data.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY lastVisitEpoch DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: HistoryEntity)

    @Query("SELECT * FROM history WHERE url = :url LIMIT 1")
    suspend fun getHistoryByUrl(url: String): HistoryEntity?

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteHistoryById(id: Long)

    @Query("DELETE FROM history")
    suspend fun clearAllHistory()
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY addedAtEpoch DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE url = :url")
    suspend fun deleteBookmark(url: String)

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE url = :url)")
    fun isBookmarked(url: String): Flow<Boolean>
}

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY startedAtEpoch DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadEntity)

    @Query("SELECT * FROM downloads WHERE id = :id LIMIT 1")
    suspend fun getDownloadById(id: Long): DownloadEntity?

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteDownloadById(id: Long)

    @Query("DELETE FROM downloads WHERE status = 'completed' OR status = 'failed' OR status = 'blocked'")
    suspend fun clearCompletedDownloads()
}

@Dao
interface InstalledExtensionDao {
    @Query("SELECT * FROM extensions ORDER BY installedAtEpoch DESC")
    fun getAllExtensions(): Flow<List<InstalledExtensionEntity>>

    @Query("SELECT * FROM extensions WHERE enabled = 1")
    suspend fun getEnabledExtensions(): List<InstalledExtensionEntity>

    @Query("SELECT * FROM extensions WHERE id = :id LIMIT 1")
    suspend fun getExtensionById(id: String): InstalledExtensionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExtension(extension: InstalledExtensionEntity)

    @Query("DELETE FROM extensions WHERE id = :id")
    suspend fun deleteExtensionById(id: String)
}

@Dao
interface ExtensionStorageDao {
    @Query("SELECT * FROM extension_storage WHERE extensionId = :extensionId AND `key` = :key LIMIT 1")
    suspend fun getStorageValue(extensionId: String, key: String): ExtensionStorageEntity?

    @Query("SELECT * FROM extension_storage WHERE extensionId = :extensionId")
    suspend fun getStorageValues(extensionId: String): List<ExtensionStorageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStorageValue(entity: ExtensionStorageEntity)

    @Query("DELETE FROM extension_storage WHERE extensionId = :extensionId AND `key` = :key")
    suspend fun deleteStorageValue(extensionId: String, key: String)
}

@Dao
interface CatalogIndexDao {
    @Query("SELECT * FROM catalog_index ORDER BY rating DESC")
    fun getAllCatalogItems(): Flow<List<CatalogIndexEntity>>

    @Query("SELECT * FROM catalog_index WHERE category = :category ORDER BY rating DESC")
    fun getCatalogItemsByCategory(category: String): Flow<List<CatalogIndexEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCatalogItems(items: List<CatalogIndexEntity>)

    @Query("DELETE FROM catalog_index")
    suspend fun clearCatalog()

    @Query("SELECT * FROM catalog_index WHERE name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    suspend fun searchCatalog(query: String): List<CatalogIndexEntity>
}

@Dao
interface InstalledFromCatalogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: InstalledFromCatalogEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM installed_from_catalog WHERE extensionId = :extensionId)")
    suspend fun isInstalled(extensionId: String): Boolean

    @Query("DELETE FROM installed_from_catalog WHERE extensionId = :extensionId")
    suspend fun delete(extensionId: String)
}

@Dao
interface ReportedExtensionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ReportedExtensionEntity)
}

@Dao
interface SavedTabDao {
    @Query("SELECT * FROM saved_tabs ORDER BY position ASC")
    fun getAllSavedTabs(): Flow<List<SavedTabEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedTab(tab: SavedTabEntity)

    @Query("DELETE FROM saved_tabs WHERE id = :id")
    suspend fun deleteSavedTabById(id: String)

    @Query("DELETE FROM saved_tabs")
    suspend fun clearAllSavedTabs()

    @Transaction
    suspend fun saveAllTabs(tabs: List<SavedTabEntity>) {
        clearAllSavedTabs()
        tabs.forEach { insertSavedTab(it) }
    }
}
