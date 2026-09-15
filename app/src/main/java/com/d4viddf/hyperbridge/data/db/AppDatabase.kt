package com.d4viddf.hyperbridge.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.d4viddf.hyperbridge.data.composer.ComposerTemplateDao
import com.d4viddf.hyperbridge.data.composer.ComposerTemplateEntity

@Database(entities = [AppSetting::class, ComposerTemplateEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun settingsDao(): SettingsDao
    abstract fun composerTemplateDao(): ComposerTemplateDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Adds the composer_templates table (Phase 4, #272). MUST stay registered: without it,
        // the version bump above falls through to fallbackToDestructiveMigration and wipes the
        // entire `settings` table (every user preference) on the next app update.
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `composer_templates` (" +
                        "`id` TEXT NOT NULL, `name` TEXT NOT NULL, `definitionJson` TEXT NOT NULL, " +
                        "`packageNameRegex` TEXT, `titleRegex` TEXT, `textRegex` TEXT, " +
                        "`priority` INTEGER NOT NULL, `enabled` INTEGER NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`))"
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            val storageContext = context.createDeviceProtectedStorageContext()
            val dbName = "hyperbridge_db"

            // Migration logic: Move DB from CE to DE storage if it exists in old location
            if (!storageContext.getDatabasePath(dbName).exists()) {
                val oldDb = context.getDatabasePath(dbName)
                if (oldDb.exists()) {
                    storageContext.moveDatabaseFrom(context, dbName)
                }
            }

            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    storageContext,
                    AppDatabase::class.java,
                    dbName
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration(false)
                    .build().also { INSTANCE = it }
            }
        }

        fun performMigration(context: Context, onProgress: (Int) -> Unit) {
            val storageContext = context.createDeviceProtectedStorageContext()
            val dbName = "hyperbridge_db"

            if (!storageContext.getDatabasePath(dbName).exists()) {
                val oldDb = context.getDatabasePath(dbName)
                if (oldDb.exists()) {
                    onProgress(10)
                    try {
                        storageContext.moveDatabaseFrom(context, dbName)
                        onProgress(100)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        onProgress(-1) // Error state
                    }
                } else {
                    onProgress(100) // Already in DE or fresh install
                }
            } else {
                onProgress(100) // Already in DE
            }
        }
    }
}