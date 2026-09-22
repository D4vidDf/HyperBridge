package com.d4viddf.hyperbridge.data.translator

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.d4viddf.hyperbridge.data.db.AppDatabase
import com.d4viddf.hyperbridge.data.db.TranslatorDao
import com.d4viddf.hyperbridge.data.db.TranslatorEntity
import com.d4viddf.hyperbridge.models.translator.CustomTranslator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    private val translatorsStorageDir = File(context.filesDir, "translators")

    init {
        if (!translatorsStorageDir.exists()) {
            translatorsStorageDir.mkdirs()
        }
    }

    /**
     * Packages a [CustomTranslator] into a .htrans ZIP archive and writes it directly to the provided [OutputStream].
     */
    suspend fun exportTranslatorToZip(
        translator: CustomTranslator,
        outputStream: OutputStream
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            ZipOutputStream(outputStream).use { zos ->
                // 1. Write translator.json
                val jsonContent = CustomTranslator.toJson(translator)
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
        }
    }

    /**
     * Exports a [CustomTranslator] into a cached .htrans file ready for sharing.
     */
    suspend fun exportTranslatorToFile(translator: CustomTranslator): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()

            val sanitizedName = sanitizeFilename(translator.meta.name.ifBlank { translator.id })
            val targetFile = File(exportDir, "$sanitizedName.htrans")

            targetFile.outputStream().use { fos ->
                val exportResult = exportTranslatorToZip(translator, fos)
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
    suspend fun shareTranslator(context: Context, translator: CustomTranslator): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val file = exportTranslatorToFile(translator).getOrThrow()
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
        return name.replace(Regex("[^a-zA-Z0-9._-]"), "_").trim('_')
    }
}
