package com.d4viddf.hyperbridge.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.d4viddf.hyperbridge.models.translator.CustomTranslator

@Entity(
    tableName = "custom_translators",
    indices = [
        Index(value = ["priority"]),
        Index(value = ["isEnabled"]),
        Index(value = ["targetScope"])
    ]
)
data class TranslatorEntity(
    @PrimaryKey val id: String,
    val name: String,
    val targetScope: String,
    val targetPackages: String, // Comma-separated or JSON list for filtering
    val targetNotificationTypes: String,
    val priority: Int,
    val isEnabled: Boolean,
    val jsonContent: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toCustomTranslator(): Result<CustomTranslator> {
        return CustomTranslator.fromJson(jsonContent)
    }

    companion object {
        fun fromCustomTranslator(translator: CustomTranslator): TranslatorEntity {
            return TranslatorEntity(
                id = translator.id,
                name = translator.meta.name,
                targetScope = translator.targetScope.name,
                targetPackages = translator.targetPackages.joinToString(","),
                targetNotificationTypes = translator.targetNotificationTypes.joinToString(","),
                priority = translator.priority,
                isEnabled = translator.isEnabled,
                jsonContent = CustomTranslator.toJson(translator),
                updatedAt = System.currentTimeMillis()
            )
        }
    }
}
