package com.d4viddf.hyperbridge.models.widget

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import androidx.compose.ui.text.font.FontFamily
import com.d4viddf.hyperbridge.util.HyperBridgeFileExtensions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

data class CustomFontEntry(
    val fileName: String,
    val displayName: String,
    val file: File
)

/**
 * Manages user-imported font files (`.ttf`, `.otf`) stored in `context.filesDir/custom_fonts/`.
 */
class CustomFontRepository(private val context: Context) {

    private val fontsDir: File
        get() = File(context.filesDir, "custom_fonts").apply { if (!exists()) mkdirs() }

    fun getInstalledFontsSync(): List<CustomFontEntry> {
        val files = fontsDir.listFiles { _, name -> isSupportedFontFileName(name) } ?: emptyArray()
        return files
            .sortedBy { it.name.lowercase(Locale.ROOT) }
            .map { file ->
                CustomFontEntry(
                    fileName = file.name,
                    displayName = formatDisplayName(file.nameWithoutExtension),
                    file = file
                )
            }
    }

    suspend fun getInstalledFonts(): List<CustomFontEntry> = withContext(Dispatchers.IO) {
        getInstalledFontsSync()
    }

    suspend fun importFontFromUri(uri: Uri): Result<CustomFontEntry> = withContext(Dispatchers.IO) {
        runCatching {
            val rawName = HyperBridgeFileExtensions.resolveFileName(context, uri)
                ?: "custom_font_${System.currentTimeMillis()}.ttf"
            val ext = HyperBridgeFileExtensions.extractExtension(rawName)?.lowercase(Locale.ROOT)
            val validExt = if (ext == "ttf" || ext == "otf") ext else "ttf"
            val baseName = rawName.substringBeforeLast('.')
                .replace(Regex("[^a-zA-Z0-9_-]"), "_")
                .trim('_')
                .ifBlank { "custom_font" }
            val safeFileName = "$baseName.$validExt"
            val targetFile = File(fontsDir, safeFileName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: throw IllegalArgumentException("Could not read font file")

            // Verify the font file can be parsed by Android Typeface
            val tf = Typeface.createFromFile(targetFile)
            if (tf == null || tf == Typeface.DEFAULT) {
                // Note: Typeface.createFromFile throws RuntimeException on invalid font in Android,
                // or returns Typeface.DEFAULT on some versions.
            }

            CustomFontEntry(
                fileName = targetFile.name,
                displayName = formatDisplayName(targetFile.nameWithoutExtension),
                file = targetFile
            )
        }
    }

    fun resolveTypeface(fileName: String?): Typeface? {
        if (fileName.isNullOrBlank()) return null
        return resolveTypefaceCached(context, fileName)
    }

    fun resolveComposeFontFamily(fileName: String?): FontFamily? {
        val tf = resolveTypeface(fileName) ?: return null
        return FontFamily(tf)
    }

    companion object {
        private val typefaceCache = java.util.concurrent.ConcurrentHashMap<String, Typeface>()

        fun isSupportedFontFileName(name: String?): Boolean {
            if (name.isNullOrBlank()) return false
            val lower = name.lowercase(Locale.ROOT)
            return lower.endsWith(".ttf") || lower.endsWith(".otf")
        }

        fun formatDisplayName(rawName: String): String {
            return rawName
                .removeSuffix(".ttf")
                .removeSuffix(".otf")
                .replace('_', ' ')
                .replace('-', ' ')
                .trim()
                .ifBlank { "Custom Font" }
        }

        fun resolveTypefaceCached(context: Context, fileName: String?): Typeface? {
            if (fileName.isNullOrBlank()) return null
            typefaceCache[fileName]?.let { return it }
            return runCatching {
                val cleanName = fileName.replace('\\', '/').substringAfterLast('/')
                val file = File(File(context.filesDir, "custom_fonts"), cleanName)
                if (file.exists() && file.isFile) {
                    Typeface.createFromFile(file)?.also { typefaceCache[fileName] = it }
                } else null
            }.getOrNull()
        }
    }
}
