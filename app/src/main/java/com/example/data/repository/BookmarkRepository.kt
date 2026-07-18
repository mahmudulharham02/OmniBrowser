package com.example.data.repository

import com.example.data.BookmarkEntity
import com.example.data.db.BookmarkDao
import kotlinx.coroutines.flow.Flow

class BookmarkRepository(private val bookmarkDao: BookmarkDao) {
    val allBookmarks: Flow<List<BookmarkEntity>> = bookmarkDao.getAllBookmarks()

    suspend fun addBookmark(url: String, title: String, folder: String = "Default", faviconUrl: String? = null) {
        if (url.trim().isEmpty() || url.startsWith("omni://")) return
        bookmarkDao.insertBookmark(
            BookmarkEntity(
                url = url,
                title = title.ifEmpty { url },
                folder = folder,
                faviconUrl = faviconUrl,
                addedAtEpoch = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteBookmark(url: String) {
        bookmarkDao.deleteBookmark(url)
    }

    fun isBookmarked(url: String): Flow<Boolean> {
        return bookmarkDao.isBookmarked(url)
    }
}
