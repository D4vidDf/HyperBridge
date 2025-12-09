package com.d4viddf.hyperbridge.data.model

import com.google.gson.annotations.SerializedName

/**
 * Root Container
 */
data class HyperBridgeBackup(
    @SerializedName("metadata") val metadata: BackupMetadata,
    @SerializedName("settings") val settings: List<AppSettingBackup>
)

data class BackupMetadata(
    @SerializedName("package_name") val packageName: String,
    @SerializedName("version_code") val versionCode: Int,
    @SerializedName("version_name") val versionName: String,
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("device_model") val deviceModel: String
)

data class AppSettingBackup(
    @SerializedName("key") val key: String,
    @SerializedName("value") val value: String
)