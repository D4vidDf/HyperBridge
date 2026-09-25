package com.d4viddf.hyperbridge.data.widget

import android.content.Context
import android.net.Uri
import android.util.Log
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
class CustomWidgetRepository(private val context: Context) {

    private val tag = "HyperBridgeWidgetStudio"
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val widgetsDir = File(context.filesDir, "custom_widgets")

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

    suspend fun saveWidget(doc: CustomWidgetDocument) {
        withContext(Dispatchers.IO) {
            val widgetFolder = File(widgetsDir, doc.id)
            if (!widgetFolder.exists()) widgetFolder.mkdirs()
            val configFile = File(widgetFolder, "widget.json")
            configFile.writeText(json.encodeToString(CustomWidgetDocument.serializer(), doc))
        }
    }

    suspend fun deleteWidget(id: String) {
        if (!WidgetImportGuard.isSafeId(id)) return
        withContext(Dispatchers.IO) {
            File(widgetsDir, id).deleteRecursively()
        }
    }

    suspend fun exportWidget(id: String): File? = withContext(Dispatchers.IO) {
        if (!WidgetImportGuard.isSafeId(id)) return@withContext null
        val sourceFolder = File(widgetsDir, id)
        if (!sourceFolder.exists()) return@withContext null

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
                        val entryName = file.relativeTo(sourceFolder).path
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
            if (validation.clamped != decoded) {
                configFile.writeText(json.encodeToString(CustomWidgetDocument.serializer(), validation.clamped))
            }

            val targetDir = File(widgetsDir, finalId)
            if (targetDir.exists()) targetDir.deleteRecursively()

            if (!tempDir.renameTo(targetDir)) {
                tempDir.copyRecursively(targetDir, overwrite = true)
                tempDir.deleteRecursively()
            }

            finalId
        } catch (e: Exception) {
            Log.e(tag, "Widget install failed", e)
            tempDir.deleteRecursively()
            throw e
        }
    }
}
