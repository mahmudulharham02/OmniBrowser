package com.example.data.repository

import com.example.data.DownloadEntity
import com.example.data.db.DownloadDao
import kotlinx.coroutines.flow.Flow

class DownloadRepository(private val downloadDao: DownloadDao) {
    val allDownloads: Flow<List<DownloadEntity>> = downloadDao.getAllDownloads()

    suspend fun recordDownload(
        id: Long,
        url: String,
        filename: String,
        mimeType: String,
        totalBytes: Long,
        status: String = "pending",
        refererUrl: String? = null
    ) {
        downloadDao.insertDownload(
            DownloadEntity(
                id = id,
                url = url,
                filename = filename,
                mimeType = mimeType,
                totalBytes = totalBytes,
                downloadedBytes = 0,
                status = status,
                refererUrl = refererUrl,
                startedAtEpoch = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateProgress(id: Long, downloadedBytes: Long, status: String) {
        val existing = downloadDao.getDownloadById(id)
        if (existing != null) {
            downloadDao.insertDownload(
                existing.copy(
                    downloadedBytes = downloadedBytes,
                    status = status,
                    completedAtEpoch = if (status == "completed") System.currentTimeMillis() else existing.completedAtEpoch
                )
            )
        }
    }

    suspend fun updateStatus(id: Long, status: String, errorMessage: String? = null, savePath: String? = null) {
        val existing = downloadDao.getDownloadById(id)
        if (existing != null) {
            downloadDao.insertDownload(
                existing.copy(
                    status = status,
                    errorMessage = errorMessage ?: existing.errorMessage,
                    savePath = savePath ?: existing.savePath,
                    completedAtEpoch = if (status == "completed" || status == "failed" || status == "blocked") System.currentTimeMillis() else existing.completedAtEpoch
                )
            )
        }
    }

    suspend fun deleteById(id: Long) {
        downloadDao.deleteDownloadById(id)
    }

    suspend fun clearCompleted() {
        downloadDao.clearCompletedDownloads()
    }
}
