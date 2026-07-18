package com.example.browser.extension

import android.content.Context
import com.example.data.ExtensionManifest
import com.example.data.InstalledExtensionEntity
import com.example.data.repository.ExtensionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

class ExtensionHost(
    private val context: Context,
    private val extensionRepository: ExtensionRepository,
    private val messageBus: MessageBus
) {
    private val loadedExtensions = mutableMapOf<String, Pair<InstalledExtensionEntity, ExtensionManifest>>()
    private val backgroundPages = mutableMapOf<String, BackgroundPage>()
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    init {
        // Collect messages from the MessageBus and forward to background pages
        coroutineScope.launch {
            messageBus.messages.collect { message ->
                if (message.toId != null) {
                    backgroundPages[message.toId]?.deliverMessage(message.fromId, message.payload)
                } else {
                    // Broadcast to all active background pages
                    backgroundPages.forEach { (id, page) ->
                        if (id != message.fromId) {
                            page.deliverMessage(message.fromId, message.payload)
                        }
                    }
                }
            }
        }
    }

    suspend fun start() = withContext(Dispatchers.IO) {
        // First, check if we need to copy and register the bundled Omni Dark Mode extension
        copyBundledExtensionIfNeeded()

        // Load all enabled extensions
        val dbExtensions = extensionRepository.getEnabledExtensions()
        for (entity in dbExtensions) {
            try {
                val manifestFile = File(entity.sourcePath, "manifest.json")
                if (manifestFile.exists()) {
                    val manifest = ExtensionManifestParser.parse(manifestFile)
                    withContext(Dispatchers.Main) {
                        loadedExtensions[entity.id] = Pair(entity, manifest)
                        if (manifest.background != null) {
                            val bgPage = BackgroundPage(context, entity, manifest, extensionRepository, messageBus)
                            backgroundPages[entity.id] = bgPage
                            bgPage.start()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getLoadedExtensions(): List<Pair<InstalledExtensionEntity, ExtensionManifest>> {
        return loadedExtensions.values.toList()
    }

    fun isExtensionEnabled(id: String): Boolean {
        return loadedExtensions.containsKey(id)
    }

    suspend fun enableExtension(id: String, enabled: Boolean) = withContext(Dispatchers.IO) {
        val existing = extensionRepository.getExtensionById(id) ?: return@withContext
        val updated = existing.copy(enabled = enabled)
        extensionRepository.insertExtension(updated)
        
        withContext(Dispatchers.Main) {
            if (enabled) {
                val manifestFile = File(updated.sourcePath, "manifest.json")
                if (manifestFile.exists()) {
                    val manifest = ExtensionManifestParser.parse(manifestFile)
                    loadedExtensions[updated.id] = Pair(updated, manifest)
                    if (manifest.background != null) {
                        backgroundPages[updated.id]?.stop()
                        val bgPage = BackgroundPage(context, updated, manifest, extensionRepository, messageBus)
                        backgroundPages[updated.id] = bgPage
                        bgPage.start()
                    }
                }
            } else {
                loadedExtensions.remove(id)
                backgroundPages.remove(id)?.stop()
            }
        }
    }

    suspend fun uninstallExtension(id: String) = withContext(Dispatchers.IO) {
        val existing = extensionRepository.getExtensionById(id) ?: return@withContext
        extensionRepository.deleteExtension(id)
        
        // Delete local files
        val dir = File(existing.sourcePath)
        if (dir.exists()) {
            dir.deleteRecursively()
        }

        withContext(Dispatchers.Main) {
            loadedExtensions.remove(id)
            backgroundPages.remove(id)?.stop()
        }
    }

    suspend fun installUnpackedZip(zipInputStream: InputStream, originalFileName: String): Result<InstalledExtensionEntity> = withContext(Dispatchers.IO) {
        runCatching {
            val extensionId = "ext_" + originalFileName.substringBeforeLast(".").lowercase().replace(Regex("[^a-z0-9_]"), "") + "_" + System.currentTimeMillis() % 10000
            val targetDir = File(context.filesDir, "extensions/$extensionId").apply { mkdirs() }
            
            // Extract ZIP
            ZipInputStream(zipInputStream).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val outFile = File(targetDir, entry.name)
                    if (!outFile.canonicalPath.startsWith(targetDir.canonicalPath)) {
                        error("Zip contains unsafe directory traversal path")
                    }
                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        FileOutputStream(outFile).use { fos ->
                            zis.copyTo(fos)
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            // Parse manifest
            val manifestFile = File(targetDir, "manifest.json")
            if (!manifestFile.exists()) {
                targetDir.deleteRecursively()
                error("No manifest.json found in extension ZIP")
            }

            val manifest = ExtensionManifestParser.parse(manifestFile)
            val entity = InstalledExtensionEntity(
                id = extensionId,
                name = manifest.name,
                version = manifest.version,
                enabled = true,
                sourcePath = targetDir.absolutePath,
                installedAtEpoch = System.currentTimeMillis()
            )

            extensionRepository.insertExtension(entity)
            
            withContext(Dispatchers.Main) {
                loadedExtensions[entity.id] = Pair(entity, manifest)
                if (manifest.background != null) {
                    backgroundPages[entity.id]?.stop()
                    val bgPage = BackgroundPage(context, entity, manifest, extensionRepository, messageBus)
                    backgroundPages[entity.id] = bgPage
                    bgPage.start()
                }
            }

            entity
        }
    }

    suspend fun installFromCatalogLocal(id: String, name: String, version: String, sourcePath: String) = withContext(Dispatchers.IO) {
        val manifestFile = File(sourcePath, "manifest.json")
        if (manifestFile.exists()) {
            val manifest = ExtensionManifestParser.parse(manifestFile)
            val entity = InstalledExtensionEntity(
                id = id,
                name = name,
                version = version,
                enabled = true,
                sourcePath = sourcePath,
                installedAtEpoch = System.currentTimeMillis()
            )
            extensionRepository.insertExtension(entity)
            withContext(Dispatchers.Main) {
                loadedExtensions[id] = Pair(entity, manifest)
                if (manifest.background != null) {
                    backgroundPages[id]?.stop()
                    val bgPage = BackgroundPage(context, entity, manifest, extensionRepository, messageBus)
                    backgroundPages[id] = bgPage
                    bgPage.start()
                }
            }
        }
    }

    fun readExtensionFile(extension: InstalledExtensionEntity, filePath: String): String? {
        return try {
            val file = File(extension.sourcePath, filePath)
            if (file.exists()) file.readText() else null
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun copyBundledExtensionIfNeeded() {
        val bundledId = "omni-dark-mode"
        val existing = extensionRepository.getExtensionById(bundledId)
        if (existing == null) {
            // Copy assets/extensions/omni-dark-mode recursively
            val targetDir = File(context.filesDir, "extensions/$bundledId").apply { mkdirs() }
            try {
                copyAssetFolder(context, "extensions/omni-dark-mode", targetDir.absolutePath)
                val manifestFile = File(targetDir, "manifest.json")
                if (manifestFile.exists()) {
                    val manifest = ExtensionManifestParser.parse(manifestFile)
                    val entity = InstalledExtensionEntity(
                        id = bundledId,
                        name = manifest.name,
                        version = manifest.version,
                        enabled = true,
                        sourcePath = targetDir.absolutePath,
                        installedAtEpoch = System.currentTimeMillis()
                    )
                    extensionRepository.insertExtension(entity)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun copyAssetFolder(context: Context, srcFolder: String, destPath: String) {
        val assetManager = context.assets
        var files: Array<String>? = null
        try {
            files = assetManager.list(srcFolder)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (files == null || files.isEmpty()) {
            // It's a file
            copyAssetFile(context, srcFolder, destPath)
        } else {
            // It's a folder
            val folder = File(destPath)
            folder.mkdirs()
            for (file in files) {
                copyAssetFolder(context, "$srcFolder/$file", "$destPath/$file")
            }
        }
    }

    private fun copyAssetFile(context: Context, srcFile: String, destPath: String) {
        try {
            val `in` = context.assets.open(srcFile)
            val out = FileOutputStream(destPath)
            val buffer = ByteArray(1024)
            var read: Int
            while (`in`.read(buffer).also { read = it } != -1) {
                out.write(buffer, 0, read)
            }
            `in`.close()
            out.flush()
            out.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
