package com.example.data.repository

import com.example.data.InstalledExtensionEntity
import com.example.data.ExtensionStorageEntity
import com.example.data.db.InstalledExtensionDao
import com.example.data.db.ExtensionStorageDao
import kotlinx.coroutines.flow.Flow

class ExtensionRepository(
    private val extensionDao: InstalledExtensionDao,
    private val storageDao: ExtensionStorageDao
) {
    val allExtensions: Flow<List<InstalledExtensionEntity>> = extensionDao.getAllExtensions()

    suspend fun getEnabledExtensions(): List<InstalledExtensionEntity> {
        return extensionDao.getEnabledExtensions()
    }

    suspend fun getExtensionById(id: String): InstalledExtensionEntity? {
        return extensionDao.getExtensionById(id)
    }

    suspend fun insertExtension(extension: InstalledExtensionEntity) {
        extensionDao.insertExtension(extension)
    }

    suspend fun deleteExtension(id: String) {
        extensionDao.deleteExtensionById(id)
    }

    suspend fun getStorageValue(extensionId: String, key: String): String? {
        return storageDao.getStorageValue(extensionId, key)?.value
    }

    suspend fun insertStorageValue(extensionId: String, key: String, value: String) {
        storageDao.insertStorageValue(
            ExtensionStorageEntity(
                extensionId = extensionId,
                key = key,
                value = value,
                updatedAtEpoch = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteStorageValue(extensionId: String, key: String) {
        storageDao.deleteStorageValue(extensionId, key)
    }

    suspend fun getStorageValues(extensionId: String): Map<String, String> {
        return storageDao.getStorageValues(extensionId).associate { it.key to it.value }
    }
}
