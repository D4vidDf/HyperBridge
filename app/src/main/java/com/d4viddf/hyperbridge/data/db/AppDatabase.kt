package com.d4viddf.hyperbridge.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.d4viddf.hyperbridge.data.composer.ComposerTemplateDao
import com.d4viddf.hyperbridge.data.composer.ComposerTemplateEntity

@Database(
    entities = [
        AppSetting::class,
        ComposerTemplateEntity::class,
        SourceAppEntity::class,
        SourceValueEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun settingsDao(): SettingsDao
    abstract fun composerTemplateDao(): ComposerTemplateDao
    abstract fun sourceDao(): SourceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Version 2 adds the composer_templates table (Phase 4, #272). Kept byte-identical to the
        // composer branch so a v2 database created by that build opens here without an identity
        // mismatch. MUST stay registered: without it, the version bump above falls through to
        // fallbackToDestructiveMigration and wipes the entire `settings` table (every user
        // preference) on the next app update.
        val MIGRATION_1_2 = object : Migration(1, 2) {
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

        // Version 3 adds the island content source tables (Phase 5, #273) on top of the composer
        // schema, so both a v1 (dev/0_6_0) and a v2 (template-composer build) database upgrade
        // cleanly instead of crashing with "Room cannot verify the data integrity".
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `source_apps` (
                        `packageName` TEXT NOT NULL,
                        `displayName` TEXT NOT NULL,
                        `allowed` INTEGER NOT NULL,
                        `firstSeenAt` INTEGER NOT NULL,
                        `lastSeenAt` INTEGER NOT NULL,
                        PRIMARY KEY(`packageName`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `source_values` (
                        `sourceId` TEXT NOT NULL,
                        `ownerPackage` TEXT NOT NULL,
                        `text` TEXT,
                        `iconPath` TEXT,
                        `updatedAt` INTEGER NOT NULL,
                        `ttlMs` INTEGER,
                        PRIMARY KEY(`sourceId`)
                    )
                    """.trimIndent()
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
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
