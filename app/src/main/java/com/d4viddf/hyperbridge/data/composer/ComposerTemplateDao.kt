package com.d4viddf.hyperbridge.data.composer

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ComposerTemplateDao {

    @Query("SELECT * FROM composer_templates ORDER BY priority DESC")
    fun getAllFlow(): Flow<List<ComposerTemplateEntity>>

    @Query("SELECT * FROM composer_templates WHERE enabled = 1 ORDER BY priority DESC")
    suspend fun getEnabledSync(): List<ComposerTemplateEntity>

    @Query("SELECT * FROM composer_templates WHERE id = :id")
    suspend fun getById(id: String): ComposerTemplateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ComposerTemplateEntity)

    @Query("DELETE FROM composer_templates WHERE id = :id")
    suspend fun delete(id: String)
}
