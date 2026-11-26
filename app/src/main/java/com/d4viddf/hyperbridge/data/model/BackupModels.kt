package com.d4viddf.hyperbridge.data.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

/**
 * Root Container
 */
@Serializable
data class HyperBridgeBackup(
    @SerializedName("metadata") val metadata: BackupMetadata,
    @SerializedName("settings") val settings: List<AppSettingBackup> = emptyList(),
    @SerializedName("translators") val translators: List<TranslatorBackupItem>? = null,
    @SerializedName("widgets") val widgets: List<CustomWidgetBackupItem>? = null
)

@Serializable
data class BackupMetadata(
    @SerializedName("package_name") val packageName: String,
    @SerializedName("version_code") val versionCode: Int,
    @SerializedName("version_name") val versionName: String,
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("device_model") val deviceModel: String
)

@Serializable
data class AppSettingBackup(
    @SerializedName("key") val key: String,
    @SerializedName("value") val value: String
)

@Serializable
data class TranslatorBackupItem(
    @SerializedName("id") val id: String,
    @SerializedName("json_content") val jsonContent: String,
    @SerializedName("is_enabled") val isEnabled: Boolean = true,
    @SerializedName("priority") val priority: Int = 100,
    @SerializedName("icons") val icons: Map<String, String>? = null
)

@Serializable
data class CustomWidgetBackupItem(
    @SerializedName("id") val id: String,
    @SerializedName("config_json") val configJson: String,
    @SerializedName("translator_json") val translatorJson: String? = null,
    @SerializedName("assets") val assets: Map<String, String>? = null
)
