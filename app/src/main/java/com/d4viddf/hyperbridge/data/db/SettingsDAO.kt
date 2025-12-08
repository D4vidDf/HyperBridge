package com.d4viddf.hyperbridge.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    // Get value stream (returns null if key doesn't exist)
    @Query("SELECT value FROM settings WHERE `key` = :key")
    fun getSettingFlow(key: String): Flow<String?>

    // Sync read for migration check
    @Query("SELECT value FROM settings WHERE `key` = :key")
    suspend fun getSetting(key: String): String?

    // Insert or Update
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(setting: AppSetting)

    @Query("DELETE FROM settings WHERE `key` = :key")
    suspend fun delete(key: String)

    @Query("SELECT * FROM settings")
    suspend fun getAllSync(): List<AppSetting> // Needed for Export

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(settings: List<AppSetting>) // Needed for Restore
}