package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.*

@Database(
    entities = [
        HistoryEntity::class,
        BookmarkEntity::class,
        DownloadEntity::class,
        InstalledExtensionEntity::class,
        ExtensionStorageEntity::class,
        CatalogIndexEntity::class,
        InstalledFromCatalogEntity::class,
        ReportedExtensionEntity::class,
        SavedTabEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun downloadDao(): DownloadDao
    abstract fun installedExtensionDao(): InstalledExtensionDao
    abstract fun extensionStorageDao(): ExtensionStorageDao
    abstract fun catalogIndexDao(): CatalogIndexDao
    abstract fun installedFromCatalogDao(): InstalledFromCatalogDao
    abstract fun reportedExtensionDao(): ReportedExtensionDao
    abstract fun savedTabDao(): SavedTabDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "omnibrowser_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
