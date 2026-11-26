package com.d4viddf.hyperbridge.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * "Island content sources" (issue #273 add-on): apps other than HyperBridge can drive a
 * micro-widget's `{source.<id>.text}` / `{source.<id>.icon}` tokens by broadcasting
 * `com.d4viddf.hyperbridge.action.UPDATE_SOURCE`. [SourceAppEntity] is the per-package
 * allow-list (opt-in, managed from Settings); [SourceValueEntity] is the latest value received
 * for a given source id.
 */
@Entity(tableName = "source_apps")
data class SourceAppEntity(
    @PrimaryKey val packageName: String,
    val displayName: String,
    val allowed: Boolean = false,
    val firstSeenAt: Long,
    val lastSeenAt: Long
)

@Entity(tableName = "source_values")
data class SourceValueEntity(
    @PrimaryKey val sourceId: String,
    val ownerPackage: String,
    val text: String?,
    val iconPath: String?,
    val updatedAt: Long,
    val ttlMs: Long?
)

@Dao
interface SourceDao {
    @Query("SELECT * FROM source_apps ORDER BY lastSeenAt DESC")
    fun getAllAppsFlow(): Flow<List<SourceAppEntity>>

    @Upsert
    suspend fun upsertApp(app: SourceAppEntity)

    @Query("UPDATE source_apps SET allowed = :allowed WHERE packageName = :pkg")
    suspend fun setAllowed(pkg: String, allowed: Boolean)

    @Query("SELECT * FROM source_apps WHERE packageName = :pkg")
    suspend fun getApp(pkg: String): SourceAppEntity?

    @Upsert
    suspend fun upsertValue(value: SourceValueEntity)

    @Query("SELECT * FROM source_values WHERE sourceId = :id")
    suspend fun getValue(id: String): SourceValueEntity?

    @Query("SELECT * FROM source_values WHERE ttlMs IS NOT NULL AND (updatedAt + ttlMs) < :now")
    suspend fun getExpired(now: Long): List<SourceValueEntity>

    @Query("DELETE FROM source_values WHERE ttlMs IS NOT NULL AND (updatedAt + ttlMs) < :now")
    suspend fun deleteExpired(now: Long)
}
