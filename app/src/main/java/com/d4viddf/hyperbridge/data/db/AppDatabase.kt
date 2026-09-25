package com.d4viddf.hyperbridge.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        AppSetting::class,
        TranslatorEntity::class,
        SourceAppEntity::class,
        SourceValueEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun settingsDao(): SettingsDao
    abstract fun translatorDao(): TranslatorDao
    abstract fun sourceDao(): SourceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // v2 (0.6.0-beta1): the custom translator table. Kept byte-identical to dev/0_6_0 so a
        // database created there opens here without an identity mismatch. MUST stay registered:
        // an unhandled version bump falls through to fallbackToDestructiveMigration and wipes the
        // entire `settings` table (every user preference) on the next update.
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `custom_translators` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `targetScope` TEXT NOT NULL,
                        `targetPackages` TEXT NOT NULL,
                        `targetNotificationTypes` TEXT NOT NULL,
                        `priority` INTEGER NOT NULL,
                        `isEnabled` INTEGER NOT NULL,
                        `jsonContent` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_custom_translators_priority` ON `custom_translators` (`priority`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_custom_translators_isEnabled` ON `custom_translators` (`isEnabled`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_custom_translators_targetScope` ON `custom_translators` (`targetScope`)")
            }
        }

        // v3 adds the island content source tables (Phase 5, #273).
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

        // v4 exists for the test builds of this branch only. Those shipped a v2/v3 schema whose
        // version 2 was the composer's `composer_templates` table, not `custom_translators`
        // (#272 dropped that store). Reaching v4 through a migration rather than matching an
        // existing version is what keeps Room from refusing to open those databases with
        // "Room cannot verify the data integrity" — the crash reported in #328.
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `composer_templates`")
                MIGRATION_1_2.migrate(db)
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
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
