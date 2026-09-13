package com.d4viddf.hyperbridge.models

data class IslandConfig(
    val isFloat: Boolean? = null,
    val isShowShade: Boolean? = null,
    val timeout: Int? = null,
    val floatTimeout: Int? = null,
    val removeOriginalNotification: Boolean? = null,
    val dismissWithOriginal: Boolean? = null,
    val enableInlineReply: Boolean? = null,
    // Global-only for now; carried here so translators get it alongside the rest of the config.
    val smartActions: SmartActionsConfig? = null,
) {
    // Merges this config (App) with a default config (Global)
    fun mergeWith(global: IslandConfig): IslandConfig {
        return IslandConfig(
            isFloat = this.isFloat ?: global.isFloat ?: false,
            isShowShade = this.isShowShade ?: global.isShowShade ?: false,
            timeout = this.timeout ?: global.timeout ?: 10,
            floatTimeout = this.floatTimeout ?: global.floatTimeout ?: 5,
            removeOriginalNotification = this.removeOriginalNotification ?: global.removeOriginalNotification ?: false,
            dismissWithOriginal = this.dismissWithOriginal ?: global.dismissWithOriginal ?: true,
            enableInlineReply = this.enableInlineReply ?: global.enableInlineReply ?: true,
            smartActions = this.smartActions ?: global.smartActions ?: SmartActionsConfig.DISABLED,
        )
    }
}