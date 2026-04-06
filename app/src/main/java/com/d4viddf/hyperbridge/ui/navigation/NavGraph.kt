package com.d4viddf.hyperbridge.ui.navigation

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.entryProvider
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.ui.screens.home.HomeScreen
import com.d4viddf.hyperbridge.ui.screens.onboarding.OnboardingScreen
import com.d4viddf.hyperbridge.ui.screens.settings.AppPriorityScreen
import com.d4viddf.hyperbridge.ui.screens.settings.BackupSettingsScreen
import com.d4viddf.hyperbridge.ui.screens.settings.BlocklistAppListScreen
import com.d4viddf.hyperbridge.ui.screens.settings.ChangelogHistoryScreen
import com.d4viddf.hyperbridge.ui.screens.settings.EngineSettingsScreen
import com.d4viddf.hyperbridge.ui.screens.settings.GlobalBlocklistScreen
import com.d4viddf.hyperbridge.ui.screens.settings.GlobalSettingsScreen
import com.d4viddf.hyperbridge.ui.screens.settings.ImportPreviewScreen
import com.d4viddf.hyperbridge.ui.screens.settings.InfoScreen
import com.d4viddf.hyperbridge.ui.screens.settings.IslandSettingsScreen
import com.d4viddf.hyperbridge.ui.screens.settings.LicensesScreen
import com.d4viddf.hyperbridge.ui.screens.settings.NavCustomizationScreen
import com.d4viddf.hyperbridge.ui.screens.settings.PrioritySettingsScreen
import com.d4viddf.hyperbridge.ui.screens.settings.SetupHealthScreen
import com.d4viddf.hyperbridge.util.BackupManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun mainNavGraph(
    context: Context,
    scope: CoroutineScope,
    preferences: AppPreferences,
    navigator: Navigator<Screen>,
    backupManager: BackupManager,
    currentVersionCode: Int,
    onExit: () -> Unit
) = entryProvider {
    entry<Screen.Onboarding> {
        OnboardingScreen {
            scope.launch {
                preferences.setSetupComplete(true)
                preferences.setLastSeenVersion(currentVersionCode)

                navigator.finishOnboarding(Screen.Home)
            }
        }
    }
    entry<Screen.Home> {
        HomeScreen(
            onSettingsClick = { navigator.navigate(Screen.Info) },
            onNavConfigClick = { pkg -> navigator.navigate(Screen.NavCustomization(pkg)) }
        )
    }
    entry<Screen.Info> {
        InfoScreen(
            onBack = { if (!navigator.goBack()) onExit() },
            onSetupClick = { navigator.navigate(Screen.Setup) },
            onLicensesClick = { navigator.navigate(Screen.Licenses) },
            onBehaviorClick = { navigator.navigate(Screen.Behavior) },
            onGlobalSettingsClick = { navigator.navigate(Screen.GlobalSettings) },
            onHistoryClick = { navigator.navigate(Screen.History) },
            onBlocklistClick = { navigator.navigate(Screen.GlobalBlocklist) },
            onBackupClick = { navigator.navigate(Screen.Backup) }
        )
    }
    entry<Screen.GlobalSettings> {
        GlobalSettingsScreen(
            onBack = { navigator.goBack() },
            onNavSettingsClick = { navigator.navigate(Screen.NavCustomization(null)) },
            onIslandSettingsClick = { navigator.navigate(Screen.IslandSettings) },
            onEngineSettingsClick = { navigator.navigate(Screen.EngineSettings) }
        )
    }
    entry<Screen.NavCustomization> { key ->
        NavCustomizationScreen(
            onBack = { navigator.goBack() },
            packageName = key.packageName
        )
    }
    entry<Screen.EngineSettings> {
        EngineSettingsScreen(onBack = { navigator.goBack() })
    }
    entry<Screen.Setup> {
        SetupHealthScreen(onBack = { navigator.goBack() })
    }
    entry<Screen.Licenses> {
        LicensesScreen(onBack = { navigator.goBack() })
    }
    entry<Screen.Behavior> {
        PrioritySettingsScreen(
            onBack = { navigator.goBack() },
            onNavigateToPriorityList = { navigator.navigate(Screen.AppPriority) }
        )
    }
    entry<Screen.AppPriority> {
        AppPriorityScreen(onBack = { navigator.goBack() })
    }
    entry<Screen.History> {
        ChangelogHistoryScreen(onBack = { navigator.goBack() })
    }
    entry<Screen.GlobalBlocklist> {
        GlobalBlocklistScreen(
            onBack = { navigator.goBack() },
            onNavigateToAppList = { navigator.navigate(Screen.BlocklistApps) }
        )
    }
    entry<Screen.BlocklistApps> {
        BlocklistAppListScreen(onBack = { navigator.goBack() })
    }
    entry<Screen.Backup> {
        BackupSettingsScreen(
            onBack = { navigator.goBack() },
            backupManager = backupManager,
            onBackupFileLoaded = { backup ->
                navigator.navigate(Screen.ImportPreview(backup))
            }
        )
    }
    entry<Screen.ImportPreview> { key ->
        val importSuccessMsg = stringResource(R.string.import_success)
        val importFailedMsg = stringResource(R.string.import_failed)
        ImportPreviewScreen(
            backupData = key.backup,
            onBack = { navigator.goBack() },
            onConfirmRestore = { selection ->
                scope.launch {
                    val result = backupManager.restoreBackup(key.backup, selection)
                    if (result.isSuccess) {
                        Toast.makeText(context, importSuccessMsg, Toast.LENGTH_LONG).show()
                        navigator.navigate(Screen.Home)
                    } else {
                        val error = result.exceptionOrNull()?.message ?: ""
                        Toast.makeText(context, importFailedMsg.format(error), Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }
    entry<Screen.IslandSettings> {
        IslandSettingsScreen(onBack = { navigator.goBack() })
    }
}
