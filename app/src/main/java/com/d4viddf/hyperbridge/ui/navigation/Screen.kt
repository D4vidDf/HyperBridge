package com.d4viddf.hyperbridge.ui.navigation

import androidx.navigation3.runtime.NavKey
import com.d4viddf.hyperbridge.data.model.HyperBridgeBackup
import kotlinx.serialization.Serializable

@Serializable
sealed interface Screen : NavKey {
    @Serializable data object Onboarding : Screen
    @Serializable data object Home : Screen
    @Serializable data object Info : Screen
    @Serializable data object Setup : Screen
    @Serializable data object Licenses : Screen
    @Serializable data object Behavior : Screen
    @Serializable data object GlobalSettings : Screen
    @Serializable data object History : Screen
    @Serializable data object Backup : Screen
    @Serializable data class ImportPreview(val backup: HyperBridgeBackup) : Screen
    @Serializable data class NavCustomization(val packageName: String?) : Screen
    @Serializable data object EngineSettings : Screen
    @Serializable data object AppPriority : Screen
    @Serializable data object GlobalBlocklist : Screen
    @Serializable data object BlocklistApps : Screen
    @Serializable data object IslandSettings : Screen
}
