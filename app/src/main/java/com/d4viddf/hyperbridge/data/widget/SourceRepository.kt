package com.d4viddf.hyperbridge.data.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.d4viddf.hyperbridge.data.db.AppDatabase
import com.d4viddf.hyperbridge.data.db.SourceAppEntity
import com.d4viddf.hyperbridge.data.db.SourceDao
import com.d4viddf.hyperbridge.data.db.SourceValueEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Backs the "island content sources" allow-list + latest values. A source app must first be
 * explicitly allowed (via [setAllowed], from Settings) before [update] is honored; a sighting
 * from a not-yet-allowed app is still recorded (so it shows up as pending in Settings) but its
 * value update is dropped by the caller (see `UpdateSourceReceiver`/`SourceUpdateService`).
 */
class SourceRepository(
    private val context: Context,
    private val dao: SourceDao = AppDatabase.getDatabase(context).sourceDao()
) {

    private val tag = "HyperBridgeSources"
    private val iconsDir = File(context.filesDir, "sources")

    /** Emits a sourceId every time [update] writes a new value, for live-refresh consumers. */
    val updates = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 8)

    fun allAppsFlow(): Flow<List<SourceAppEntity>> = dao.getAllAppsFlow()

    suspend fun recordSighting(pkg: String, displayName: String) {
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val existing = dao.getApp(pkg)
            dao.upsertApp(
                existing?.copy(lastSeenAt = now, displayName = displayName)
                    ?: SourceAppEntity(pkg, displayName, allowed = false, firstSeenAt = now, lastSeenAt = now)
            )
        }
    }

    suspend fun isAllowed(pkg: String): Boolean = withContext(Dispatchers.IO) {
        dao.getApp(pkg)?.allowed ?: false
    }

    suspend fun setAllowed(pkg: String, allowed: Boolean) {
        withContext(Dispatchers.IO) { dao.setAllowed(pkg, allowed) }
    }

    /**
     * Writes a new value for [sourceId] on behalf of [ownerPackage].
     *
     * A source id belongs to the first allowed package that writes it: a later write from a
     * different package is refused (returns false) so two allowed apps sharing an id cannot
     * clobber each other's value. The id is released when its current row expires or is deleted.
     */
    suspend fun update(sourceId: String, ownerPackage: String, text: String?, icon: Bitmap?, ttlMs: Long?): Boolean {
        return withContext(Dispatchers.IO) {
            val current = dao.getValue(sourceId)
            if (current != null && current.ownerPackage != ownerPackage) {
                val currentExpired = current.ttlMs != null && (current.updatedAt + current.ttlMs) < System.currentTimeMillis()
                if (!currentExpired) {
                    Log.w(tag, "Refused update of source '$sourceId' from $ownerPackage: owned by ${current.ownerPackage}")
                    return@withContext false
                }
            }
            var iconPath: String? = null
            if (icon != null) {
                try {
                    if (!iconsDir.exists()) iconsDir.mkdirs()
                    val file = File(iconsDir, "$sourceId.png")
                    FileOutputStream(file).use { icon.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    iconPath = file.absolutePath
                } catch (e: Exception) {
                    Log.w(tag, "Failed to persist source icon for $sourceId", e)
                }
            }
            dao.upsertValue(
                SourceValueEntity(
                    sourceId = sourceId,
                    ownerPackage = ownerPackage,
                    text = text,
                    iconPath = iconPath,
                    updatedAt = System.currentTimeMillis(),
                    ttlMs = ttlMs
                )
            )
            updates.tryEmit(sourceId)
            true
        }
    }

    /**
     * field is "text" or "icon" (returns a file path for "icon"). Expired values resolve to null,
     * and so do values whose owner package has since been revoked in Settings.
     */
    suspend fun lookup(sourceId: String, field: String): String? = withContext(Dispatchers.IO) {
        val value = dao.getValue(sourceId) ?: return@withContext null
        val expired = value.ttlMs != null && (value.updatedAt + value.ttlMs) < System.currentTimeMillis()
        if (expired) return@withContext null
        if (dao.getApp(value.ownerPackage)?.allowed != true) return@withContext null
        when (field) {
            "text" -> value.text
            "icon" -> value.iconPath
            else -> null
        }
    }

    fun loadIconBitmap(sourceId: String): Bitmap? {
        val file = File(iconsDir, "$sourceId.png")
        return if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
    }
}
