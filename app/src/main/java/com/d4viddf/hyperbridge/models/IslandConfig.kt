package com.d4viddf.hyperbridge.models

data class IslandConfig(
    val isFloat: Boolean? = null,
    val isShowShade: Boolean? = null,
    val timeout: Long? = null
) {
    // Merges this config (App) with a default config (Global)
    fun mergeWith(global: IslandConfig): IslandConfig {
        return IslandConfig(
            isFloat = this.isFloat ?: global.isFloat ?: true,
            isShowShade = this.isShowShade ?: global.isShowShade ?: true,
            timeout = this.timeout ?: global.timeout ?: 5000L
        )
    }
}