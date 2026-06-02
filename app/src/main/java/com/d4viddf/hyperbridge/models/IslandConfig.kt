package com.d4viddf.hyperbridge.models

data class IslandConfig(
    val isFloat: Boolean? = null,
    val isShowShade: Boolean? = null,
    val timeout: Int? = null,
    val floatTimeout: Int? = null,
    val removeOriginalNotification: Boolean? = null,

) {
    // Merges this config (App) with a default config (Global)
    fun mergeWith(global: IslandConfig): IslandConfig {
        return IslandConfig(
            isFloat = this.isFloat ?: global.isFloat ?: false,
            isShowShade = this.isShowShade ?: global.isShowShade ?: false,
            timeout = this.timeout ?: global.timeout ?: 10,
            floatTimeout = this.floatTimeout ?: global.floatTimeout ?: 5,
            removeOriginalNotification = this.removeOriginalNotification ?: global.removeOriginalNotification ?: false,
        )
    }
}