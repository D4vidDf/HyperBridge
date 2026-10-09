package com.d4viddf.hyperbridge.models.colorscheme

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Manages installed and creator-provided YAML color schemes.
 * Stores schemes as `*.yaml` files in `context.filesDir/color_schemes/`.
 */
class ColorSchemeRepository(private val context: Context) {

    private val schemesDir: File
        get() = File(context.filesDir, "color_schemes").apply { if (!exists()) mkdirs() }

    suspend fun getInstalledSchemes(): List<ColorSchemeDefinition> = withContext(Dispatchers.IO) {
        val files = schemesDir.listFiles { _, name ->
            name.endsWith(".yaml", ignoreCase = true) || name.endsWith(".yml", ignoreCase = true)
        } ?: emptyArray()

        files.mapNotNull { file ->
            runCatching {
                val text = file.readText()
                YamlColorSchemeParser.parse(text)
            }.getOrNull()
        }
    }

    suspend fun getSchemeById(id: String): ColorSchemeDefinition? = withContext(Dispatchers.IO) {
        getInstalledSchemes().firstOrNull { it.id == id }
    }

    suspend fun saveScheme(def: ColorSchemeDefinition): File = withContext(Dispatchers.IO) {
        val safeName = def.name.replace(Regex("[^a-zA-Z0-9_-]"), "_").lowercase()
        val file = File(schemesDir, "${safeName}.hscheme.yaml")
        val yaml = YamlColorSchemeParser.serialize(def)
        file.writeText(yaml)
        file
    }

    suspend fun importSchemeFromYaml(yamlText: String): ColorSchemeDefinition = withContext(Dispatchers.IO) {
        val def = YamlColorSchemeParser.parse(yamlText)
        saveScheme(def)
        def
    }

    suspend fun deleteScheme(id: String): Boolean = withContext(Dispatchers.IO) {
        val files = schemesDir.listFiles { _, name ->
            name.endsWith(".yaml", ignoreCase = true) || name.endsWith(".yml", ignoreCase = true)
        } ?: emptyArray()

        var deleted = false
        files.forEach { file ->
            val def = runCatching { YamlColorSchemeParser.parse(file.readText()) }.getOrNull()
            if (def?.id == id) {
                if (file.delete()) deleted = true
            }
        }
        deleted
    }
}
