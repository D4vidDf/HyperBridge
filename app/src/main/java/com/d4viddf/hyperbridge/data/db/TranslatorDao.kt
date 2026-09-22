package com.d4viddf.hyperbridge.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TranslatorDao {
    @Query("SELECT * FROM custom_translators ORDER BY priority DESC")
    fun getAllTranslatorsFlow(): Flow<List<TranslatorEntity>>

    @Query("SELECT * FROM custom_translators WHERE isEnabled = 1 ORDER BY priority DESC")
    fun getActiveTranslatorsFlow(): Flow<List<TranslatorEntity>>

    @Query("SELECT * FROM custom_translators WHERE isEnabled = 1 ORDER BY priority DESC")
    suspend fun getActiveTranslators(): List<TranslatorEntity>

    @Query("SELECT * FROM custom_translators WHERE id = :id LIMIT 1")
    suspend fun getTranslatorById(id: String): TranslatorEntity?

    @Query("SELECT * FROM custom_translators WHERE id = :id LIMIT 1")
    fun getTranslatorByIdFlow(id: String): Flow<TranslatorEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranslator(translator: TranslatorEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(translators: List<TranslatorEntity>)

    @Update
    suspend fun updateTranslator(translator: TranslatorEntity)

    @Query("UPDATE custom_translators SET isEnabled = :isEnabled, updatedAt = :timestamp WHERE id = :id")
    suspend fun setTranslatorEnabled(id: String, isEnabled: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE custom_translators SET priority = :priority, updatedAt = :timestamp WHERE id = :id")
    suspend fun updatePriority(id: String, priority: Int, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM custom_translators WHERE id = :id")
    suspend fun deleteTranslatorById(id: String)

    @Query("DELETE FROM custom_translators")
    suspend fun deleteAll()
}
