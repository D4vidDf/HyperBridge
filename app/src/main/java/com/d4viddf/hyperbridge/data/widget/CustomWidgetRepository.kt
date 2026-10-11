package com.d4viddf.hyperbridge.data.widget

import android.content.Context
import android.net.Uri
import android.util.Log
import com.d4viddf.hyperbridge.data.db.AppDatabase
import com.d4viddf.hyperbridge.data.db.TranslatorDao
import com.d4viddf.hyperbridge.data.db.TranslatorEntity
import com.d4viddf.hyperbridge.models.NotificationType
import com.d4viddf.hyperbridge.models.translator.CustomTranslator
import com.d4viddf.hyperbridge.models.translator.PresentationConfig
import com.d4viddf.hyperbridge.models.translator.PresentationMode
import com.d4viddf.hyperbridge.models.translator.TargetScope
import com.d4viddf.hyperbridge.models.translator.TranslatorMetadata
import com.d4viddf.hyperbridge.models.widget.CustomWidgetDocument
import com.d4viddf.hyperbridge.models.widget.WidgetDimensionValidator
import com.d4viddf.hyperbridge.models.widget.WidgetImportGuard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Persists [CustomWidgetDocument]s under `filesDir/custom_widgets/<id>/widget.json` (+ `assets/`),
 * and packages/unpacks them as `.hwidget` archives for sharing. 1:1 clone of
 * [com.d4viddf.hyperbridge.data.theme.ThemeRepository]'s file-based persistence pattern.
 */
class CustomWidgetRepository(
    private val context: Context,
    private val translatorDao: TranslatorDao? = null
) {

    private val tag = "HyperBridgeWidgetStudio"
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val widgetsDir = File(context.filesDir, "custom_widgets")

    private fun getTranslatorDao(): TranslatorDao? {
        return translatorDao ?: try {
            AppDatabase.getDatabase(context).translatorDao()
        } catch (_: Throwable) {
            null
        }
    }

    init {
        if (!widgetsDir.exists()) widgetsDir.mkdirs()
    }

    fun getWidgetsDir(): File = widgetsDir

    fun assetFile(widgetId: String, fileName: String): File = File(File(widgetsDir, widgetId), "assets/$fileName")

    suspend fun getAvailableWidgets(): List<CustomWidgetDocument> = withContext(Dispatchers.IO) {
        val list = mutableListOf<CustomWidgetDocument>()
        widgetsDir.listFiles()?.forEach { widgetFolder ->
            try {
                val configFile = File(widgetFolder, "widget.json")
                if (configFile.exists()) {
                    list.add(decodeStored(configFile.readText()))
                }
            } catch (e: Exception) {
                Log.e(tag, "Corrupt widget found in: ${widgetFolder.name}", e)
            }
        }
        list
    }

    suspend fun getWidget(id: String): CustomWidgetDocument? = withContext(Dispatchers.IO) {
        try {
            val configFile = File(widgetsDir, "$id/widget.json")
            if (WidgetImportGuard.isSafeId(id) && configFile.exists()) decodeStored(configFile.readText()) else null
        } catch (e: Exception) {
            Log.e(tag, "Failed to load widget $id", e)
            null
        }
    }

    /** Widgets saved before the import guard existed get the same nesting check before decoding. */
    private fun decodeStored(text: String): CustomWidgetDocument {
        require(WidgetImportGuard.jsonNestingDepth(text) <= WidgetImportGuard.MAX_JSON_NESTING) { "widget.json nested too deeply" }
        return json.decodeFromString(CustomWidgetDocument.serializer(), text)
    }

    /** Legacy lookup for [CustomWidgetDocument.boundPackage]; designs are matched by translator now. */
    suspend fun getWidgetForPackage(pkg: String): CustomWidgetDocument? {
        return getAvailableWidgets().firstOrNull { it.boundPackage == pkg }
    }

    suspend fun saveWidget(doc: CustomWidgetDocument, translator: CustomTranslator? = null) {
        withContext(Dispatchers.IO) {
            val widgetFolder = File(widgetsDir, doc.id)
            if (!widgetFolder.exists()) widgetFolder.mkdirs()
            val configFile = File(widgetFolder, "widget.json")
            configFile.writeText(json.encodeToString(CustomWidgetDocument.serializer(), doc))
            if (translator != null) {
                val translatorFile = File(widgetFolder, "translator.json")
                translatorFile.writeText(CustomTranslator.toJson(translator))
            }
        }
    }

    suspend fun saveTranslatorForWidget(widgetId: String, translator: CustomTranslator) {
        withContext(Dispatchers.IO) {
            if (!WidgetImportGuard.isSafeId(widgetId)) return@withContext
            val widgetFolder = File(widgetsDir, widgetId)
            if (!widgetFolder.exists()) widgetFolder.mkdirs()
            val translatorFile = File(widgetFolder, "translator.json")
            translatorFile.writeText(CustomTranslator.toJson(translator))
        }
    }

    suspend fun getTranslatorForWidget(widgetId: String): CustomTranslator? = withContext(Dispatchers.IO) {
        if (!WidgetImportGuard.isSafeId(widgetId)) return@withContext null
        try {
            val translatorFile = File(widgetsDir, "$widgetId/translator.json")
            if (translatorFile.exists()) {
                val parsed = CustomTranslator.fromJson(translatorFile.readText()).getOrNull()
                if (parsed != null) return@withContext parsed
            }
            // Fallback: check database if available
            getTranslatorDao()?.getAllTranslators()?.firstOrNull {
                it.toCustomTranslator().getOrNull()?.presentation?.widgetId == widgetId
            }?.toCustomTranslator()?.getOrNull()
        } catch (e: Exception) {
            Log.e(tag, "Failed to load translator for widget $widgetId", e)
            null
        }
    }

    suspend fun deleteWidget(id: String) {
        if (!WidgetImportGuard.isSafeId(id)) return
        withContext(Dispatchers.IO) {
            File(widgetsDir, id).deleteRecursively()
        }
    }

    /**
     * Clones an existing widget design and its associated configuration to a new unique widget ID.
     * Copies all assets, writes a cloned widget.json with the new widget ID, and binds a new
     * translator with [newTranslatorId].
     */
    suspend fun duplicateWidget(
        sourceWidgetId: String,
        newName: String? = null,
        sourceTranslator: CustomTranslator? = null,
        newTranslatorId: String = UUID.randomUUID().toString()
    ): Pair<CustomWidgetDocument, CustomTranslator>? = withContext(Dispatchers.IO) {
        if (!WidgetImportGuard.isSafeId(sourceWidgetId)) return@withContext null
        val sourceFolder = File(widgetsDir, sourceWidgetId)
        if (!sourceFolder.exists()) return@withContext null

        val sourceDoc = getWidget(sourceWidgetId) ?: return@withContext null
        val newWidgetId = UUID.randomUUID().toString()
        val targetFolder = File(widgetsDir, newWidgetId)
        if (!targetFolder.exists()) targetFolder.mkdirs()

        // 1. Copy assets if present
        val sourceAssets = File(sourceFolder, "assets")
        if (sourceAssets.exists() && sourceAssets.isDirectory) {
            val targetAssets = File(targetFolder, "assets")
            targetAssets.mkdirs()
            sourceAssets.copyRecursively(targetAssets, overwrite = true)
        }

        // 2. Clone CustomWidgetDocument
        val effectiveName = newName ?: "${sourceDoc.meta.name} (Copy)"
        val duplicatedDoc = sourceDoc.copy(
            id = newWidgetId,
            meta = sourceDoc.meta.copy(name = effectiveName)
        )
        val configFile = File(targetFolder, "widget.json")
        configFile.writeText(json.encodeToString(CustomWidgetDocument.serializer(), duplicatedDoc))

        // 3. Resolve and clone bound CustomTranslator
        val boundSourceTranslator = sourceTranslator ?: getTranslatorForWidget(sourceWidgetId)
        val duplicatedTranslator = boundSourceTranslator?.copy(
            id = newTranslatorId,
            meta = boundSourceTranslator.meta.copy(
                name = effectiveName,
                iconName = duplicatedDoc.meta.icon.ifBlank { boundSourceTranslator.meta.iconName }
            ),
            priority = boundSourceTranslator.priority - 1,
            presentation = boundSourceTranslator.presentation.copy(
                mode = PresentationMode.WIDGET,
                widgetId = newWidgetId
            )
        ) ?: CustomTranslator(
            id = newTranslatorId,
            meta = TranslatorMetadata(
                name = effectiveName,
                iconName = duplicatedDoc.meta.icon.ifBlank { "dashboard_customize" }
            ),
            targetScope = TargetScope.NOTIFICATION_TYPE,
            targetPackages = emptyList(),
            targetNotificationTypes = listOf(NotificationType.STANDARD.name),
            presentation = PresentationConfig(
                mode = PresentationMode.WIDGET,
                widgetId = newWidgetId
            )
        )

        val translatorFile = File(targetFolder, "translator.json")
        translatorFile.writeText(CustomTranslator.toJson(duplicatedTranslator))

        // 4. Save duplicated translator in database if DAO is available
        try {
            getTranslatorDao()?.insertTranslator(TranslatorEntity.fromCustomTranslator(duplicatedTranslator))
        } catch (e: Exception) {
            Log.w(tag, "Failed to register duplicated translator in database", e)
        }

        Pair(duplicatedDoc, duplicatedTranslator)
    }

    suspend fun exportWidget(id: String, translator: CustomTranslator? = null): File? = withContext(Dispatchers.IO) {
        if (!WidgetImportGuard.isSafeId(id)) return@withContext null
        val sourceFolder = File(widgetsDir, id)
        if (!sourceFolder.exists()) return@withContext null

        // Ensure translator.json is included in the package if a translator is configured
        val targetTranslator = translator ?: getTranslatorForWidget(id)
        if (targetTranslator != null) {
            try {
                val translatorFile = File(sourceFolder, "translator.json")
                translatorFile.writeText(CustomTranslator.toJson(targetTranslator))
            } catch (e: Exception) {
                Log.w(tag, "Failed to write translator.json before export", e)
            }
        }

        // Must be a directory FileProvider actually serves: res/xml/file_paths.xml only declares
        // a `<cache-path name="exports" path="exports/" />` root (shared with ThemeRepository's own
        // export dir) - a different folder name here made the Studio's Export button throw
        // "Failed to find configured root" from FileProvider.getUriForFile at runtime.
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()

        val zipFile = File(exportDir, "$id.hwidget")
        try {
            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                sourceFolder.walkTopDown().forEach { file ->
                    if (file.isFile) {
                        val entryName = file.relativeTo(sourceFolder).path.replace('\\', '/')
                        zos.putNextEntry(ZipEntry(entryName))
                        file.inputStream().use { it.copyTo(zos) }
                        zos.closeEntry()
                    }
                }
            }
            zipFile
        } catch (e: Exception) {
            Log.e(tag, "Export failed", e)
            null
        }
    }

    /** Unzips a `.hwidget`, validates it, and installs it. Returns the installed widget's id. */
    suspend fun installWidgetFromUri(uri: Uri): String = withContext(Dispatchers.IO) {
        val tempId = UUID.randomUUID().toString()
        val tempDir = File(context.cacheDir, "widget_import_$tempId")
        if (!tempDir.exists()) tempDir.mkdirs()

        try {
            val tempRoot = tempDir.canonicalPath + File.separator
            var unpackedBytes = 0L
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        val file = File(tempDir, entry.name)
                        if (!file.canonicalPath.startsWith(tempRoot)) {
                            throw SecurityException("Invalid Zip Path: ${entry.name}")
                        }
                        if (entry.isDirectory) {
                            file.mkdirs()
                        } else {
                            file.parentFile?.mkdirs()
                            file.outputStream().use { output ->
                                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                                while (true) {
                                    val read = zip.read(buffer)
                                    if (read < 0) break
                                    unpackedBytes += read
                                    if (unpackedBytes > WidgetImportGuard.MAX_UNPACKED_BYTES) {
                                        throw IllegalArgumentException("Invalid widget: archive too large")
                                    }
                                    output.write(buffer, 0, read)
                                }
                            }
                        }
                        entry = zip.nextEntry
                    }
                }
            }

            val configFile = File(tempDir, "widget.json")
            if (!configFile.exists()) {
                throw IllegalArgumentException("Invalid widget: missing widget.json")
            }
            if (configFile.length() > WidgetImportGuard.MAX_CONFIG_BYTES) {
                throw IllegalArgumentException("Invalid widget: widget.json too large")
            }

            val configText = configFile.readText()
            // Checked before decoding: kotlinx decodes the node tree recursively, and a
            // StackOverflowError is an Error that no catch below would stop.
            if (WidgetImportGuard.jsonNestingDepth(configText) > WidgetImportGuard.MAX_JSON_NESTING) {
                throw IllegalArgumentException("Invalid widget: nested too deeply")
            }
            val decoded = try {
                json.decodeFromString(CustomWidgetDocument.serializer(), configText)
            } catch (_: Exception) {
                throw IllegalArgumentException("Invalid widget.json structure")
            }

            // The id becomes a folder name that is deleted and replaced below: an id like
            // "../../databases" must never reach File(widgetsDir, id).
            val finalId = decoded.id.takeIf { WidgetImportGuard.isSafeId(it) } ?: tempId
            val doc = if (finalId == decoded.id) decoded else decoded.copy(id = finalId)

            val validation = WidgetDimensionValidator.validate(doc)
            configFile.writeText(json.encodeToString(CustomWidgetDocument.serializer(), validation.clamped))

            // Check if translator.json was included; if not, create a default bound translator
            val translatorFile = File(tempDir, "translator.json")
            val parsedTranslator = if (translatorFile.exists()) {
                CustomTranslator.fromJson(translatorFile.readText()).getOrNull()?.let { parsed ->
                    parsed.copy(
                        presentation = parsed.presentation.copy(
                            mode = PresentationMode.WIDGET,
                            widgetId = finalId
                        )
                    )
                }
            } else null

            val translatorToSave = parsedTranslator ?: CustomTranslator(
                id = UUID.randomUUID().toString(),
                meta = TranslatorMetadata(
                    name = doc.meta.name.ifBlank { "Custom Design" },
                    iconName = doc.meta.icon.ifBlank { "dashboard_customize" }
                ),
                targetScope = TargetScope.NOTIFICATION_TYPE,
                targetPackages = emptyList(),
                targetNotificationTypes = listOf(NotificationType.STANDARD.name),
                presentation = PresentationConfig(
                    mode = PresentationMode.WIDGET,
                    widgetId = finalId
                )
            )

            translatorFile.writeText(CustomTranslator.toJson(translatorToSave))

            val targetDir = File(widgetsDir, finalId)
            if (targetDir.exists()) targetDir.deleteRecursively()

            if (!tempDir.renameTo(targetDir)) {
                tempDir.copyRecursively(targetDir, overwrite = true)
                tempDir.deleteRecursively()
            }

            // Register translator in database if available
            try {
                getTranslatorDao()?.insertTranslator(TranslatorEntity.fromCustomTranslator(translatorToSave))
            } catch (e: Exception) {
                Log.w(tag, "Failed to register translator in database", e)
            }

            finalId
        } catch (e: Exception) {
            Log.e(tag, "Widget install failed", e)
            tempDir.deleteRecursively()
            throw e
        }
    }
}
