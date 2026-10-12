package com.d4viddf.hyperbridge.util

import com.d4viddf.hyperbridge.data.model.AppSettingBackup
import com.d4viddf.hyperbridge.data.model.BackupMetadata
import com.d4viddf.hyperbridge.data.model.CustomWidgetBackupItem
import com.d4viddf.hyperbridge.data.model.HyperBridgeBackup
import com.d4viddf.hyperbridge.data.model.TranslatorBackupItem
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupManagerTest {

    private val gson = Gson()

    @Test
    fun roundTripBackupWithTranslatorsAndWidgets() {
        val metadata = BackupMetadata(
            packageName = "com.d4viddf.hyperbridge",
            versionCode = 40,
            versionName = "0.6.0-beta3",
            timestamp = 1700000000000L,
            deviceModel = "Pixel 8"
        )
        val settings = listOf(
            AppSettingBackup("active_theme", "theme_dark"),
            AppSettingBackup("enable_island", "true")
        )
        val translators = listOf(
            TranslatorBackupItem(
                id = "trans-1",
                jsonContent = """{"id":"trans-1","meta":{"name":"Test Translator"}}""",
                isEnabled = true,
                priority = 100,
                icons = mapOf("icon.png" to "iVBORw0KGgoAAAANSUhEUg==")
            )
        )
        val widgets = listOf(
            CustomWidgetBackupItem(
                id = "widget-1",
                configJson = """{"id":"widget-1","meta":{"name":"Test Widget"}}""",
                translatorJson = """{"id":"bound-trans","meta":{"name":"Bound Translator"}}""",
                assets = mapOf("bg.png" to "iVBORw0KGgoAAAANSUhEUg==")
            )
        )

        val backup = HyperBridgeBackup(
            metadata = metadata,
            settings = settings,
            translators = translators,
            widgets = widgets
        )

        val json = gson.toJson(backup)
        val deserialized = gson.fromJson(json, HyperBridgeBackup::class.java)

        assertEquals("com.d4viddf.hyperbridge", deserialized.metadata.packageName)
        assertEquals(2, deserialized.settings.size)
        assertNotNull(deserialized.translators)
        assertEquals(1, deserialized.translators?.size)
        assertEquals("trans-1", deserialized.translators?.first()?.id)
        assertEquals(1, deserialized.translators?.first()?.icons?.size)

        assertNotNull(deserialized.widgets)
        assertEquals(1, deserialized.widgets?.size)
        assertEquals("widget-1", deserialized.widgets?.first()?.id)
        assertEquals(1, deserialized.widgets?.first()?.assets?.size)
    }

    @Test
    fun legacyBackupWithoutTranslatorsOrWidgetsDeserializesSafely() {
        val legacyJson = """
            {
              "metadata": {
                "package_name": "com.d4viddf.hyperbridge",
                "version_code": 39,
                "version_name": "0.5.9",
                "timestamp": 1690000000000,
                "device_model": "Xiaomi 13"
              },
              "settings": [
                {
                  "key": "active_theme",
                  "value": "default"
                }
              ]
            }
        """.trimIndent()

        val deserialized = gson.fromJson(legacyJson, HyperBridgeBackup::class.java)
        assertEquals("com.d4viddf.hyperbridge", deserialized.metadata.packageName)
        assertEquals(1, deserialized.settings.size)
        assertNull(deserialized.translators)
        assertNull(deserialized.widgets)
    }

    @Test
    fun backupSelectionDefaultsIncludeAll() {
        val selection = BackupSelection()
        assertTrue(selection.includeSettings)
        assertTrue(selection.includeBlocklist)
        assertTrue(selection.includePriorities)
        assertTrue(selection.includeTranslators)
        assertTrue(selection.includeCustomWidgets)
    }
}
