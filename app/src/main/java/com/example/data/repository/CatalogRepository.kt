package com.example.data.repository

import android.content.Context
import com.example.data.CatalogIndexEntity
import com.example.data.InstalledFromCatalogEntity
import com.example.data.ReportedExtensionEntity
import com.example.data.db.CatalogIndexDao
import com.example.data.db.InstalledFromCatalogDao
import com.example.data.db.ReportedExtensionDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.InputStream
import java.nio.charset.Charset

class CatalogRepository(
    private val catalogIndexDao: CatalogIndexDao,
    private val installedFromCatalogDao: InstalledFromCatalogDao,
    private val reportedExtensionDao: ReportedExtensionDao,
    private val okHttpClient: OkHttpClient
) {
    val allCatalogItems: Flow<List<CatalogIndexEntity>> = catalogIndexDao.getAllCatalogItems()

    fun getCatalogItemsByCategory(category: String): Flow<List<CatalogIndexEntity>> {
        return catalogIndexDao.getCatalogItemsByCategory(category)
    }

    suspend fun searchCatalog(query: String): List<CatalogIndexEntity> = withContext(Dispatchers.IO) {
        if (query.trim().isEmpty()) return@withContext emptyList()
        catalogIndexDao.searchCatalog(query)
    }

    suspend fun isInstalledFromCatalog(extensionId: String): Boolean = withContext(Dispatchers.IO) {
        installedFromCatalogDao.isInstalled(extensionId)
    }

    suspend fun recordCatalogInstall(extensionId: String) {
        installedFromCatalogDao.insert(
            InstalledFromCatalogEntity(
                extensionId = extensionId,
                installedAtEpoch = System.currentTimeMillis(),
                lastUpdatedEpoch = System.currentTimeMillis()
            )
        )
    }

    suspend fun recordCatalogUninstall(extensionId: String) {
        installedFromCatalogDao.delete(extensionId)
    }

    suspend fun recordReport(extensionId: String, reason: String) {
        reportedExtensionDao.insert(
            ReportedExtensionEntity(
                extensionId = extensionId,
                reason = reason,
                reportedAtEpoch = System.currentTimeMillis()
            )
        )
    }

    suspend fun fetchIndex(context: Context, catalogUrl: String, force: Boolean = false): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            var jsonString: String? = null

            // Try network first if not offline or fallback needed
            if (!force && catalogUrl.startsWith("http")) {
                try {
                    val request = Request.Builder().url(catalogUrl).build()
                    okHttpClient.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            jsonString = response.body?.string()
                        }
                    }
                } catch (e: Exception) {
                    // Fail silently and fallback to assets
                }
            }

            // Fallback to assets index.json
            if (jsonString == null) {
                jsonString = loadJSONFromAsset(context, "catalog/index.json")
            }

            if (jsonString != null) {
                val parsedEntities = parseCatalogJson(jsonString!!)
                if (parsedEntities.isNotEmpty()) {
                    catalogIndexDao.clearCatalog()
                    catalogIndexDao.insertCatalogItems(parsedEntities)
                }
            } else {
                error("No catalog sources found")
            }
        }
    }

    private fun loadJSONFromAsset(context: Context, fileName: String): String? {
        return try {
            val inputStream: InputStream = context.assets.open(fileName)
            val size: Int = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            String(buffer, Charset.forName("UTF-8"))
        } catch (ex: Exception) {
            null
        }
    }

    private fun parseCatalogJson(jsonStr: String): List<CatalogIndexEntity> {
        val list = mutableListOf<CatalogIndexEntity>()
        try {
            val root = JSONObject(jsonStr)
            val extensions = root.getJSONArray("extensions")
            for (i in 0 until extensions.length()) {
                val extObj = extensions.getJSONObject(i)
                
                val id = extObj.getString("id")
                val name = extObj.getString("name")
                val version = extObj.getString("version")
                val author = extObj.optString("author", "Unknown")
                val description = extObj.optString("description", "")
                val iconUrl = extObj.optString("iconUrl", "")
                
                val screenshotsArr = extObj.optJSONArray("screenshotUrls") ?: org.json.JSONArray()
                val screenshotUrlsJson = screenshotsArr.toString()
                
                val downloadUrl = extObj.getString("downloadUrl")
                val sizeBytes = extObj.optLong("sizeBytes", 0L)
                
                val permissionsArr = extObj.optJSONArray("permissions") ?: org.json.JSONArray()
                val permissionsJson = permissionsArr.toString()
                
                val rating = extObj.optDouble("rating", 0.0).toFloat()
                val ratingCount = extObj.optInt("ratingCount", 0)
                val category = extObj.optString("category", "general")
                
                val tagsArr = extObj.optJSONArray("tags") ?: org.json.JSONArray()
                val tagsJson = tagsArr.toString()
                
                val minBrowserVersion = extObj.optString("minBrowserVersion", "1.0.0")
                val changelog = extObj.optString("changelog", "")

                list.add(
                    CatalogIndexEntity(
                        id = id,
                        name = name,
                        author = author,
                        description = description,
                        iconUrl = iconUrl,
                        screenshotUrlsJson = screenshotUrlsJson,
                        downloadUrl = downloadUrl,
                        sizeBytes = sizeBytes,
                        permissionsJson = permissionsJson,
                        rating = rating,
                        ratingCount = ratingCount,
                        category = category,
                        tagsJson = tagsJson,
                        version = version,
                        minBrowserVersion = minBrowserVersion,
                        changelog = changelog,
                        lastFetchedEpoch = System.currentTimeMillis()
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
