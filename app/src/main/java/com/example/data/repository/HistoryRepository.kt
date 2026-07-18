package com.example.data.repository

import com.example.data.HistoryEntity
import com.example.data.db.HistoryDao
import kotlinx.coroutines.flow.Flow

class HistoryRepository(private val historyDao: HistoryDao) {
    val allHistory: Flow<List<HistoryEntity>> = historyDao.getAllHistory()

    suspend fun addVisit(url: String, title: String, faviconUrl: String? = null) {
        if (url.trim().isEmpty() || url.startsWith("omni://")) return
        val existing = historyDao.getHistoryByUrl(url)
        if (existing != null) {
            historyDao.insertHistory(
                existing.copy(
                    visitCount = existing.visitCount + 1,
                    lastVisitEpoch = System.currentTimeMillis(),
                    title = title.ifEmpty { existing.title },
                    faviconUrl = faviconUrl ?: existing.faviconUrl
                )
            )
        } else {
            historyDao.insertHistory(
                HistoryEntity(
                    url = url,
                    title = title.ifEmpty { url },
                    visitCount = 1,
                    lastVisitEpoch = System.currentTimeMillis(),
                    faviconUrl = faviconUrl
                )
            )
        }
    }

    suspend fun deleteById(id: Long) {
        historyDao.deleteHistoryById(id)
    }

    suspend fun clearAll() {
        historyDao.clearAllHistory()
    }
}
