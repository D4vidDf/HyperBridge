package com.d4viddf.hyperbridge.data.translator

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.d4viddf.hyperbridge.data.db.AppDatabase
import com.d4viddf.hyperbridge.data.db.TranslatorDao
import com.d4viddf.hyperbridge.data.db.TranslatorEntity
import com.d4viddf.hyperbridge.models.NotificationType
import com.d4viddf.hyperbridge.models.translator.CustomTranslator
import com.d4viddf.hyperbridge.models.translator.DataExtractionConfig
import com.d4viddf.hyperbridge.models.translator.PresentationConfig
import com.d4viddf.hyperbridge.models.translator.PresentationMode
import com.d4viddf.hyperbridge.models.translator.TargetScope
import com.d4viddf.hyperbridge.models.translator.TranslatorConditions
import com.d4viddf.hyperbridge.models.translator.TranslatorMetadata
import com.d4viddf.hyperbridge.models.widget.CustomWidgetDocument
import com.d4viddf.hyperbridge.models.widget.WidgetDimensionValidator
import com.d4viddf.hyperbridge.models.widget.WidgetImportGuard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Repository responsible for archiving, extracting, importing, exporting, and sharing .htrans packages.
 *
 * .htrans package specification:
 * - ZIP archive container:
 *   - translator.json (required): Serialized CustomTranslator JSON specification.
 *   - icons/ (optional): Custom icon image assets referenced by action slots or presentation configs.
 */
class TranslatorRepository(
    private val context: Context,
    private val translatorDao: TranslatorDao = AppDatabase.getDatabase(context).translatorDao()
) {

    private val tag = "TranslatorRepository"
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val translatorsStorageDir = File(context.filesDir, "translators")

    init {
        if (!translatorsStorageDir.exists()) {
            translatorsStorageDir.mkdirs()
        }
    }

    /**
     * Packages a [CustomTranslator] into a .htrans or .hwidget ZIP archive and writes it directly to the provided [OutputStream].
     */
    suspend fun exportTranslatorToZip(
        translator: CustomTranslator,
        outputStream: OutputStream,
        includeTranslator: Boolean = true
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            ZipOutputStream(outputStream).use { zos ->
                val isWidgetMode = translator.presentation.mode == PresentationMode.WIDGET

                // 1. Write translator.json:
                // - Always write if includeTranslator is true
                // - If includeTranslator is false, only write for non-widget presentations (e.g. templates)
                //   where translator.json holds the presentation config, but strip triggers and rules.
                if (includeTranslator || !isWidgetMode) {
                    val translatorToExport = if (includeTranslator) {
                        translator
                    } else {
                        translator.copy(
                            targetScope = TargetScope.NOTIFICATION_TYPE,
                            targetPackages = emptyList(),
                            excludedPackages = emptyList(),
                            targetNotificationTypes = emptyList(),
                            conditions = TranslatorConditions(),
                            customVariables = emptyList(),
                            customActions = emptyList(),
                            dataExtraction = DataExtractionConfig()
                        )
                    }
                    val jsonContent = CustomTranslator.toJson(translatorToExport)
                    val jsonEntry = ZipEntry("translator.json")
                    zos.putNextEntry(jsonEntry)
                    zos.write(jsonContent.toByteArray(Charsets.UTF_8))
                    zos.closeEntry()

                    // 2. Write icons if they exist in the translator's storage directory
                    val iconDir = File(translatorsStorageDir, "${translator.id}/icons")
                    if (iconDir.exists() && iconDir.isDirectory) {
                        iconDir.walkTopDown().filter { it.isFile }.forEach { file ->
                            val relativePath = "icons/${file.relativeTo(iconDir).path.replace('\\', '/')}"
                            val iconEntry = ZipEntry(relativePath)
                            zos.putNextEntry(iconEntry)
                            file.inputStream().use { input ->
                                input.copyTo(zos)
                            }
                            zos.closeEntry()
                        }
                    }
                }

                // 3. If presentation mode is WIDGET, package the custom widget files (widget.json + assets)
                if (isWidgetMode) {
                    val widgetId = translator.presentation.widgetId
                    if (!widgetId.isNullOrBlank()) {
                        val widgetDir = File(context.filesDir, "custom_widgets/$widgetId")
                        if (widgetDir.exists() && widgetDir.isDirectory) {
                            widgetDir.walkTopDown().filter { it.isFile }.forEach { file ->
                                val relPath = file.relativeTo(widgetDir).path.replace('\\', '/')
                                if (relPath != "translator.json") {
                                    val entry = ZipEntry(relPath)
                                    zos.putNextEntry(entry)
                                    file.inputStream().use { input -> input.copyTo(zos) }
                                    zos.closeEntry()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Exports a [CustomTranslator] into a cached .htrans or .hwidget file ready for sharing.
     */
    suspend fun exportTranslatorToFile(
        translator: CustomTranslator,
        includeTranslator: Boolean = true
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()

            val sanitizedName = sanitizeFilename(translator.meta.name.ifBlank { translator.id })
            val extension = if (!includeTranslator) "hwidget" else "htrans"
            val targetFile = File(exportDir, "$sanitizedName.$extension")

            targetFile.outputStream().use { fos ->
                val exportResult = exportTranslatorToZip(translator, fos, includeTranslator)
                if (exportResult.isFailure) {
                    throw exportResult.exceptionOrNull() ?: Exception("Export failed")
                }
            }
            targetFile
        }
    }

    /**
     * Unarchives a .htrans package (or raw .json file) from the provided [InputStream],
     * extracts associated icons into storage, and saves the translator to the database.
     */
    suspend fun importTranslatorFromStream(inputStream: InputStream): Result<CustomTranslator> = withContext(Dispatchers.IO) {
        runCatching {
            // Buffer stream bytes into memory so we can inspect whether it is a ZIP or plain JSON
            val bytes = inputStream.readBytes()
            if (bytes.isEmpty()) {
                throw IllegalArgumentException("Empty translator input")
            }

            // Check if input is a valid ZIP archive (magic bytes PK: 0x50, 0x4B, 0x03, 0x04)
            val isZip = bytes.size >= 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()

            if (isZip) {
                importFromZipBytes(bytes)
            } else {
                // Fallback: parse as plain JSON
                val jsonString = String(bytes, Charsets.UTF_8)
                val translator = CustomTranslator.fromJson(jsonString).getOrThrow()
                val entity = TranslatorEntity.fromCustomTranslator(translator)
                translatorDao.insertTranslator(entity)
                translator
            }
        }
    }

    private suspend fun importFromZipBytes(bytes: ByteArray): CustomTranslator {
        val tempId = UUID.randomUUID().toString()
        val tempDir = File(context.cacheDir, "trans_import_$tempId")
        if (!tempDir.exists()) tempDir.mkdirs()

        try {
            var translatorJsonString: String? = null

            ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val file = File(tempDir, entry.name)

                    // Security: Zip Slip check
                    if (!file.canonicalPath.startsWith(tempDir.canonicalPath)) {
                        throw SecurityException("Invalid zip entry path: ${entry.name}")
                    }

                    if (entry.isDirectory) {
                        file.mkdirs()
                    } else {
                        file.parentFile?.mkdirs()
                        file.outputStream().use { output ->
                            zip.copyTo(output)
                        }

                        if (entry.name == "translator.json" || entry.name.endsWith("/translator.json")) {
                            translatorJsonString = file.readText(Charsets.UTF_8)
                        }
                    }
                    entry = zip.nextEntry
                }
            }

            val widgetJsonFile = File(tempDir, "widget.json").takeIf { it.exists() }
                ?: tempDir.walkTopDown().firstOrNull { it.isFile && it.name == "widget.json" }

            if (widgetJsonFile != null) {
                // This package contains a custom widget design (.hwidget or bundled .htrans)
                if (widgetJsonFile.length() > WidgetImportGuard.MAX_CONFIG_BYTES) {
                    throw IllegalArgumentException("Invalid widget: widget.json too large")
                }
                val widgetText = widgetJsonFile.readText(Charsets.UTF_8)
                if (WidgetImportGuard.jsonNestingDepth(widgetText) > WidgetImportGuard.MAX_JSON_NESTING) {
                    throw IllegalArgumentException("Invalid widget: nested too deeply")
                }
                val decodedWidget = try {
                    json.decodeFromString(CustomWidgetDocument.serializer(), widgetText)
                } catch (e: Exception) {
                    throw IllegalArgumentException("Invalid widget.json structure", e)
                }

                val finalWidgetId = decodedWidget.id.takeIf { WidgetImportGuard.isSafeId(it) } ?: UUID.randomUUID().toString()
                val docToSave = if (finalWidgetId == decodedWidget.id) decodedWidget else decodedWidget.copy(id = finalWidgetId)
                val validatedDoc = WidgetDimensionValidator.validate(docToSave).clamped

                val widgetsStorageDir = File(context.filesDir, "custom_widgets")
                if (!widgetsStorageDir.exists()) widgetsStorageDir.mkdirs()
                val targetWidgetDir = File(widgetsStorageDir, finalWidgetId)
                if (targetWidgetDir.exists()) targetWidgetDir.deleteRecursively()
                targetWidgetDir.mkdirs()

                val savedConfigFile = File(targetWidgetDir, "widget.json")
                savedConfigFile.writeText(json.encodeToString(CustomWidgetDocument.serializer(), validatedDoc))

                val assetsSourceDir = File(widgetJsonFile.parentFile, "assets")
                if (assetsSourceDir.exists() && assetsSourceDir.isDirectory) {
                    val targetAssetsDir = File(targetWidgetDir, "assets")
                    targetAssetsDir.mkdirs()
                    assetsSourceDir.copyRecursively(targetAssetsDir, overwrite = true)
                }

                // Resolve translator: either from translator.json if configured, or generate a default one
                val translator = if (!translatorJsonString.isNullOrBlank()) {
                    val parsed = CustomTranslator.fromJson(translatorJsonString).getOrThrow()
                    val existing = translatorDao.getTranslatorById(parsed.id)
                    val safeId = if (existing != null && existing.toCustomTranslator().getOrNull()?.presentation?.widgetId != finalWidgetId) {
                        UUID.randomUUID().toString()
                    } else {
                        parsed.id
                    }
                    parsed.copy(
                        id = safeId,
                        presentation = parsed.presentation.copy(
                            mode = PresentationMode.WIDGET,
                            widgetId = finalWidgetId
                        )
                    )
                } else {
                    CustomTranslator(
                        id = UUID.randomUUID().toString(),
                        meta = TranslatorMetadata(
                            name = validatedDoc.meta.name.ifBlank { "Custom Design" },
                            iconName = validatedDoc.meta.icon.ifBlank { "dashboard_customize" }
                        ),
                        targetScope = TargetScope.NOTIFICATION_TYPE,
                        targetPackages = emptyList(),
                        targetNotificationTypes = listOf(NotificationType.STANDARD.name),
                        presentation = PresentationConfig(
                            mode = PresentationMode.WIDGET,
                            widgetId = finalWidgetId
                        )
                    )
                }

                File(targetWidgetDir, "translator.json").writeText(CustomTranslator.toJson(translator))

                val extractedIconsDir = File(tempDir, "icons")
                if (extractedIconsDir.exists() && extractedIconsDir.isDirectory) {
                    val targetIconsDir = File(translatorsStorageDir, "${translator.id}/icons")
                    if (targetIconsDir.exists()) targetIconsDir.deleteRecursively()
                    targetIconsDir.mkdirs()
                    extractedIconsDir.copyRecursively(targetIconsDir, overwrite = true)
                }

                val entity = TranslatorEntity.fromCustomTranslator(translator)
                translatorDao.insertTranslator(entity)

                try {
                    Log.i(tag, "Custom widget design imported successfully: ${translator.meta.name} (widgetId=$finalWidgetId)")
                } catch (_: Throwable) {}
                return translator
            }

            if (translatorJsonString.isNullOrBlank()) {
                // Check if any .json file exists in root of archive
                val fallbackJson = tempDir.walkTopDown().firstOrNull { it.isFile && it.extension.equals("json", ignoreCase = true) }
                if (fallbackJson != null) {
                    translatorJsonString = fallbackJson.readText(Charsets.UTF_8)
                }
            }

            val rawJson = translatorJsonString ?: throw IllegalArgumentException("Missing translator.json in package")
            val translator = CustomTranslator.fromJson(rawJson).getOrThrow()

            // Extract icons if present into translator's permanent storage directory
            val extractedIconsDir = File(tempDir, "icons")
            if (extractedIconsDir.exists() && extractedIconsDir.isDirectory) {
                val targetIconsDir = File(translatorsStorageDir, "${translator.id}/icons")
                if (targetIconsDir.exists()) targetIconsDir.deleteRecursively()
                targetIconsDir.mkdirs()
                extractedIconsDir.copyRecursively(targetIconsDir, overwrite = true)
            }

            // Save to database
            val entity = TranslatorEntity.fromCustomTranslator(translator)
            translatorDao.insertTranslator(entity)

            try {
                Log.i(tag, "CustomTranslator imported successfully: ${translator.meta.name} (${translator.id})")
            } catch (_: Throwable) {}
            return translator
        } finally {
            tempDir.deleteRecursively()
        }
    }

    /**
     * Imports a translator from a SAF or content [Uri].
     */
    suspend fun importTranslatorFromUri(uri: Uri): Result<CustomTranslator> = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                importTranslatorFromStream(stream).getOrThrow()
            } ?: throw IllegalArgumentException("Failed to open input stream for URI: $uri")
        }
    }

    /**
     * Shares a [CustomTranslator] via Android ACTION_SEND Intent using FileProvider.
     */
    suspend fun shareTranslator(
        context: Context,
        translator: CustomTranslator,
        includeTranslator: Boolean = true
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val file = exportTranslatorToFile(translator, includeTranslator).getOrThrow()
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, translator.meta.name)
                putExtra(Intent.EXTRA_TEXT, "${translator.meta.name}\n${translator.meta.description}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, "Share Custom Translator").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }
    }

    private fun sanitizeFilename(name: String): String {
        return name
            .removeSuffix(".htrans")
            .removeSuffix(".hwidget")
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .trim('_')
            .ifBlank { "hyperbridge_translator" }
    }
}
