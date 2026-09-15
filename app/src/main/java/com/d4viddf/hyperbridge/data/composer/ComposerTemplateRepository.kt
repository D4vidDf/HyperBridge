package com.d4viddf.hyperbridge.data.composer

import android.util.Log
import com.d4viddf.hyperbridge.models.composer.ComposerTemplate
import com.d4viddf.hyperbridge.models.composer.IslandTemplateDefinition
import com.d4viddf.hyperbridge.models.composer.TemplateRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

/**
 * Room-backed persistence for Phase 4 composer templates (issue #272). Follows the same
 * kotlinx.serialization JSON idiom [com.d4viddf.hyperbridge.data.theme.ThemeRepository] uses for
 * [com.d4viddf.hyperbridge.models.theme.HyperTheme], but stores the encoded blob in a Room column
 * instead of a file.
 */
class ComposerTemplateRepository(private val dao: ComposerTemplateDao) {

    private val tag = "ComposerTemplateRepo"
    private val json = Json { ignoreUnknownKeys = true }

    val templatesFlow: Flow<List<ComposerTemplate>> = dao.getAllFlow().map { entities ->
        entities.mapNotNull { it.toModelOrNull() }
    }

    suspend fun getEnabledSync(): List<ComposerTemplate> =
        dao.getEnabledSync().mapNotNull { it.toModelOrNull() }

    suspend fun getById(id: String): ComposerTemplate? = dao.getById(id)?.toModelOrNull()

    suspend fun save(template: ComposerTemplate) {
        val now = System.currentTimeMillis()
        val toSave = template.copy(
            createdAt = if (template.createdAt == 0L) now else template.createdAt,
            updatedAt = now
        )
        dao.upsert(toSave.toEntity(json))
    }

    suspend fun delete(id: String) = dao.delete(id)

    private fun ComposerTemplateEntity.toModelOrNull(): ComposerTemplate? {
        return try {
            ComposerTemplate(
                id = id,
                name = name,
                definition = json.decodeFromString<IslandTemplateDefinition>(definitionJson),
                rule = TemplateRule(
                    packageNameRegex = packageNameRegex,
                    titleRegex = titleRegex,
                    textRegex = textRegex,
                    priority = priority
                ),
                enabled = enabled,
                createdAt = createdAt,
                updatedAt = updatedAt
            )
        } catch (e: Exception) {
            Log.e(tag, "Failed to decode composer template $id", e)
            null
        }
    }

    private fun ComposerTemplate.toEntity(json: Json): ComposerTemplateEntity = ComposerTemplateEntity(
        id = id,
        name = name,
        definitionJson = json.encodeToString(definition),
        packageNameRegex = rule.packageNameRegex,
        titleRegex = rule.titleRegex,
        textRegex = rule.textRegex,
        priority = rule.priority,
        enabled = enabled,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
