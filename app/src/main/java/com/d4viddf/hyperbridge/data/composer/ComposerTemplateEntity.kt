package com.d4viddf.hyperbridge.data.composer

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A user-authored Phase 4 island template (issue #272), persisted in Room. [definitionJson] is
 * an encoded [com.d4viddf.hyperbridge.models.composer.IslandTemplateDefinition]; the rule fields
 * are kept as real columns (mirroring [com.d4viddf.hyperbridge.models.theme.RuleConditions])
 * rather than nested in the JSON, so future filtering/indexing doesn't need to decode every row.
 */
@Entity(tableName = "composer_templates")
data class ComposerTemplateEntity(
    @PrimaryKey val id: String,
    val name: String,
    val definitionJson: String,
    val packageNameRegex: String?,
    val titleRegex: String?,
    val textRegex: String?,
    val priority: Int,
    val enabled: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)
