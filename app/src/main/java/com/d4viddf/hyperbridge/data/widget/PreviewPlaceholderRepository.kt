package com.d4viddf.hyperbridge.data.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import com.d4viddf.hyperbridge.models.widget.PreviewPlaceholderConfig
import com.d4viddf.hyperbridge.models.widget.WidgetImportGuard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.max
import kotlin.math.roundToInt
import androidx.core.graphics.scale
import androidx.core.graphics.createBitmap

data class ResolvedPlaceholderBitmaps(
    val pictureBitmap: Bitmap? = null,
    val avatarBitmap: Bitmap? = null,
    val smallIconBitmap: Bitmap? = null
)

/**
 * Manages local sidecar persistence of [PreviewPlaceholderConfig] and custom placeholder images
 * under `filesDir/preview_placeholders/<designKey>/`.
 *
 * Also supports optional bundling/extraction under `preview_placeholders/` inside `.hwidget` and `.htrans`
 * archives when the creator explicitly opts in during Share/Export.
 */
class PreviewPlaceholderRepository(private val context: Context) {

    private val tag = "PreviewPlaceholderRepo"
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val rootDir = File(context.filesDir, "preview_placeholders")

    init {
        if (!rootDir.exists()) rootDir.mkdirs()
    }

    fun dirFor(designKey: String): File {
        val safeKey = sanitizeKey(designKey)
        return File(rootDir, safeKey)
    }

    suspend fun loadConfig(designKey: String): PreviewPlaceholderConfig? = withContext(Dispatchers.IO) {
        try {
            val configFile = File(dirFor(designKey), CONFIG_FILE_NAME)
            if (!configFile.exists() || configFile.length() > WidgetImportGuard.MAX_CONFIG_BYTES) {
                return@withContext null
            }
            json.decodeFromString(PreviewPlaceholderConfig.serializer(), configFile.readText(Charsets.UTF_8))
        } catch (e: Exception) {
            Log.w(tag, "Failed to load placeholder config for $designKey", e)
            null
        }
    }

    suspend fun saveConfig(designKey: String, config: PreviewPlaceholderConfig) = withContext(Dispatchers.IO) {
        try {
            val dir = dirFor(designKey)
            if (!dir.exists()) dir.mkdirs()
            val configFile = File(dir, CONFIG_FILE_NAME)
            configFile.writeText(json.encodeToString(PreviewPlaceholderConfig.serializer(), config), Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(tag, "Failed to save placeholder config for $designKey", e)
        }
    }

    suspend fun clearPlaceholders(designKey: String) = withContext(Dispatchers.IO) {
        try {
            dirFor(designKey).deleteRecursively()
        } catch (_: Exception) {}
    }

    suspend fun saveImageFromUri(
        designKey: String,
        slotFileName: String,
        uri: Uri,
        maxDimensionPx: Int = 512
    ): String? = withContext(Dispatchers.IO) {
        try {
            val rawBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            } ?: return@withContext null

            val scaled = downsampleIfNeeded(rawBitmap, maxDimensionPx)
            saveBitmapFile(designKey, slotFileName, scaled)
        } catch (e: Exception) {
            Log.e(tag, "Failed to save placeholder image from URI", e)
            null
        }
    }

    suspend fun saveBitmapFile(
        designKey: String,
        slotFileName: String,
        bitmap: Bitmap,
        maxDimensionPx: Int = 512
    ): String? = withContext(Dispatchers.IO) {
        try {
            val safeName = slotFileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val finalName = if (safeName.endsWith(".png", ignoreCase = true)) safeName else "$safeName.png"
            val dir = dirFor(designKey)
            if (!dir.exists()) dir.mkdirs()
            val outFile = File(dir, finalName)
            val scaled = downsampleIfNeeded(bitmap, maxDimensionPx)
            FileOutputStream(outFile).use { fos ->
                scaled.compress(Bitmap.CompressFormat.PNG, 95, fos)
            }
            finalName
        } catch (e: Exception) {
            Log.e(tag, "Failed to save placeholder bitmap", e)
            null
        }
    }

    suspend fun resolveBitmaps(
        designKey: String,
        config: PreviewPlaceholderConfig
    ): ResolvedPlaceholderBitmaps = withContext(Dispatchers.IO) {
        val dir = dirFor(designKey)
        val pictureBmp = resolveRefBitmap(dir, config.pictureRef, config.notifPackage, defaultSizePx = 320)
        val avatarBmp = resolveRefBitmap(dir, config.avatarRef, config.notifPackage, defaultSizePx = 192)
        val smallIconBmp = resolveRefBitmap(dir, config.smallIconRef, config.notifPackage, defaultSizePx = 128)
        ResolvedPlaceholderBitmaps(
            pictureBitmap = pictureBmp,
            avatarBitmap = avatarBmp,
            smallIconBitmap = smallIconBmp
        )
    }

    fun resolveRefBitmapSync(
        designKey: String,
        ref: String?,
        notifPackage: String?,
        defaultSizePx: Int = 256
    ): Bitmap? {
        return resolveRefBitmap(dirFor(designKey), ref, notifPackage, defaultSizePx)
    }

    private fun resolveRefBitmap(
        dir: File,
        ref: String?,
        notifPackage: String?,
        defaultSizePx: Int
    ): Bitmap? {
        if (ref.isNullOrBlank()) return null
        return try {
            when {
                ref == PreviewPlaceholderConfig.REF_APP_ICON -> {
                    val pkg = notifPackage?.takeIf { it.isNotBlank() } ?: return null
                    val drawable = context.packageManager.getApplicationIcon(pkg)
                    drawableToBitmap(drawable, defaultSizePx)
                }
                ref.startsWith("preset:") -> {
                    createPresetSampleBitmap(ref, defaultSizePx)
                }
                else -> {
                    val safeName = File(ref).name
                    val file = File(dir, safeName)
                    if (file.exists() && file.isFile) {
                        BitmapFactory.decodeFile(file.absolutePath)
                    } else null
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Writes `preview_placeholders/` entries into an exported `.hwidget` or `.htrans` ZIP stream.
     */
    fun writePlaceholdersToZip(designKey: String, zos: ZipOutputStream) {
        val dir = dirFor(designKey)
        if (!dir.exists() || !dir.isDirectory) return
        val configFile = File(dir, CONFIG_FILE_NAME)
        if (!configFile.exists()) return

        dir.walkTopDown().filter { it.isFile }.forEach { file ->
            val relName = file.relativeTo(dir).path.replace('\\', '/')
            if (!relName.contains("..")) {
                val entry = ZipEntry("$ZIP_FOLDER_NAME/$relName")
                zos.putNextEntry(entry)
                file.inputStream().use { input -> input.copyTo(zos) }
                zos.closeEntry()
            }
        }
    }

    /**
     * Extracts bundled `preview_placeholders/` from an unpacked archive temp directory into
     * local sidecar storage for [targetDesignKey], then deletes `preview_placeholders/` from
     * [unpackedTempDir] so widget directories remain clean.
     */
    fun extractBundledPlaceholders(unpackedTempDir: File, targetDesignKey: String) {
        val bundledDir = File(unpackedTempDir, ZIP_FOLDER_NAME)
        if (!bundledDir.exists() || !bundledDir.isDirectory) return
        try {
            val bundledConfig = File(bundledDir, CONFIG_FILE_NAME)
            if (bundledConfig.exists() && bundledConfig.length() <= WidgetImportGuard.MAX_CONFIG_BYTES) {
                // Validate JSON structure before copying
                val parsed = json.decodeFromString(
                    PreviewPlaceholderConfig.serializer(),
                    bundledConfig.readText(Charsets.UTF_8)
                )
                val targetDir = dirFor(targetDesignKey)
                if (targetDir.exists()) targetDir.deleteRecursively()
                targetDir.mkdirs()

                File(targetDir, CONFIG_FILE_NAME).writeText(
                    json.encodeToString(PreviewPlaceholderConfig.serializer(), parsed),
                    Charsets.UTF_8
                )

                bundledDir.listFiles()?.forEach { f ->
                    if (f.isFile && f.extension.equals("png", ignoreCase = true) && f.length() <= 4 * 1024 * 1024) {
                        f.copyTo(File(targetDir, f.name), overwrite = true)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Ignoring invalid bundled preview_placeholders", e)
        } finally {
            bundledDir.deleteRecursively()
        }
    }

    private fun sanitizeKey(key: String): String {
        val cleaned = key.replace(Regex("[^a-zA-Z0-9._-]"), "_").trim('_')
        return cleaned.ifBlank { "default_preview" }
    }

    private fun downsampleIfNeeded(bitmap: Bitmap, maxDim: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxDim && h <= maxDim) return bitmap
        val ratio = maxDim.toFloat() / max(w, h).toFloat()
        val targetW = (w * ratio).roundToInt().coerceAtLeast(1)
        val targetH = (h * ratio).roundToInt().coerceAtLeast(1)
        return bitmap.scale(targetW, targetH)
    }

    companion object {
        const val CONFIG_FILE_NAME = "placeholders.json"
        const val ZIP_FOLDER_NAME = "preview_placeholders"
        const val SLOT_PICTURE = "picture.png"
        const val SLOT_AVATAR = "avatar.png"
        const val SLOT_SMALL_ICON = "small_icon.png"

        fun drawableToBitmap(drawable: Drawable, sizePx: Int = 128): Bitmap {
            if (drawable is BitmapDrawable && drawable.bitmap != null) {
                return drawable.bitmap
            }
            val w = drawable.intrinsicWidth.takeIf { it > 0 } ?: sizePx
            val h = drawable.intrinsicHeight.takeIf { it > 0 } ?: sizePx
            val bmp = createBitmap(w, h)
            val canvas = Canvas(bmp)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            return bmp
        }

        /**
         * Generates crisp, vibrant procedural sample bitmaps for album art and contact avatars
         * so creators can immediately test dynamic palette extraction and image nodes with zero external files.
         */
        fun createPresetSampleBitmap(presetId: String, sizePx: Int = 256): Bitmap {
            val s = sizePx.coerceIn(64, 512)
            val bmp = createBitmap(s, s)
            val canvas = Canvas(bmp)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)

            val colors: IntArray = when (presetId) {
                PreviewPlaceholderConfig.PRESET_ART_NEON -> intArrayOf(
                    0xFF7C3AED.toInt(), 0xFFEC4899.toInt(), 0xFFF59E0B.toInt()
                )
                PreviewPlaceholderConfig.PRESET_ART_CYBER -> intArrayOf(
                    0xFF0F172A.toInt(), 0xFF0284C7.toInt(), 0xFF22D3EE.toInt()
                )
                PreviewPlaceholderConfig.PRESET_ART_AURORA -> intArrayOf(
                    0xFF064E3B.toInt(), 0xFF10B981.toInt(), 0xFFA7F3D0.toInt()
                )
                PreviewPlaceholderConfig.PRESET_ART_SUNSET -> intArrayOf(
                    0xFF7F1D1D.toInt(), 0xFFEA580C.toInt(), 0xFFFDE047.toInt()
                )
                PreviewPlaceholderConfig.PRESET_AVATAR_CORAL -> intArrayOf(
                    0xFFF43F5E.toInt(), 0xFFFB7185.toInt(), 0xFFFDA4AF.toInt()
                )
                PreviewPlaceholderConfig.PRESET_AVATAR_SKY -> intArrayOf(
                    0xFF2563EB.toInt(), 0xFF38BDF8.toInt(), 0xFFBAE6FD.toInt()
                )
                PreviewPlaceholderConfig.PRESET_AVATAR_MINT -> intArrayOf(
                    0xFF059669.toInt(), 0xFF34D399.toInt(), 0xFFA7F3D0.toInt()
                )
                else -> intArrayOf(0xFF3B82F6.toInt(), 0xFF8B5CF6.toInt(), 0xFFEC4899.toInt())
            }

            paint.shader = LinearGradient(
                0f, 0f, s.toFloat(), s.toFloat(),
                colors, null, Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, s.toFloat(), s.toFloat(), paint)

            val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            glowPaint.shader = RadialGradient(
                s * 0.72f, s * 0.28f, s * 0.55f,
                intArrayOf(0x66FFFFFF, 0x00FFFFFF),
                null, Shader.TileMode.CLAMP
            )
            canvas.drawCircle(s * 0.72f, s * 0.28f, s * 0.55f, glowPaint)

            if (presetId.startsWith("preset:avatar_")) {
                // Draw clean minimalist avatar silhouette
                val fg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xE6FFFFFF.toInt() }
                canvas.drawCircle(s * 0.5f, s * 0.38f, s * 0.18f, fg)
                val shoulders = RectF(s * 0.22f, s * 0.62f, s * 0.78f, s * 1.08f)
                canvas.drawOval(shoulders, fg)
            } else {
                // Draw subtle vinyl / geometric rings for album artwork
                val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0x33FFFFFF
                    style = Paint.Style.STROKE
                    strokeWidth = (s * 0.02f).coerceAtLeast(2f)
                }
                canvas.drawCircle(s * 0.5f, s * 0.5f, s * 0.32f, ringPaint)
                canvas.drawCircle(s * 0.5f, s * 0.5f, s * 0.12f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0x44000000
                })
            }

            return bmp
        }
    }
}
