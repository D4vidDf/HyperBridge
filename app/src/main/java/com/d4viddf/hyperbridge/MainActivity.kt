package com.d4viddf.hyperbridge

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.data.db.AppDatabase
import com.d4viddf.hyperbridge.ui.components.ChangelogSheet
import com.d4viddf.hyperbridge.ui.components.PriorityEducationDialog
import com.d4viddf.hyperbridge.ui.navigation.NavigationState
import com.d4viddf.hyperbridge.ui.navigation.Navigator
import com.d4viddf.hyperbridge.ui.navigation.Screen
import com.d4viddf.hyperbridge.ui.navigation.rememberNavigationState
import com.d4viddf.hyperbridge.ui.navigation.toEntries
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
import com.d4viddf.hyperbridge.ui.theme.HyperBridgeTheme
import com.d4viddf.hyperbridge.util.BackupManager
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HyperBridgeTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainRootNavigation()
                }
            }
        }
    }
}

@Composable
fun MainRootNavigation() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = remember { AppPreferences(context) }

    val database = remember { AppDatabase.getDatabase(context) }
    val backupManager = remember { BackupManager(context, preferences, database) }

    val packageInfo = remember { try { context.packageManager.getPackageInfo(context.packageName, 0) } catch (e: Exception) { null } }
    @Suppress("DEPRECATION")
    val currentVersionCode = packageInfo?.longVersionCode?.toInt() ?: 0
    val currentVersionName = packageInfo?.versionName ?: "0.4.2"

    val isSetupComplete by produceState<Boolean?>(initialValue = null) {
        preferences.isSetupComplete.collect { value = it }
    }

    val lastSeenVersion by preferences.lastSeenVersion.collectAsState(initial = currentVersionCode)
    val isPriorityEduShown by preferences.isPriorityEduShown.collectAsState(initial = true)

    var showChangelog by remember { mutableStateOf(false) }
    var showPriorityEdu by remember { mutableStateOf(false) }

    val navigationState = rememberNavigationState(
        startRoute = Screen.Onboarding,
        topLevelRoutes = setOf(Screen.Onboarding, Screen.Home)
    )
    val navigator = remember { Navigator(navigationState) }

    LaunchedEffect(isSetupComplete) {
        if (isSetupComplete != null) {
            if (isSetupComplete == true && navigationState.topLevelRoute == Screen.Onboarding) {
                navigationState.topLevelRoute = Screen.Home
            } else if (isSetupComplete == false && navigationState.topLevelRoute == Screen.Home) {
                navigationState.topLevelRoute = Screen.Onboarding
            }

            if (isSetupComplete == true) {
                if (currentVersionCode > lastSeenVersion) {
                    showChangelog = true
                } else if (!isPriorityEduShown && !showChangelog) {
                    showPriorityEdu = true
                }
            }
        }
    }

    val entryProvider = entryProvider {
        entry<Screen.Onboarding> {
            OnboardingScreen {
                scope.launch {
                    preferences.setSetupComplete(true)
                    preferences.setLastSeenVersion(currentVersionCode)
                    preferences.setPriorityEduShown(true)
                    navigator.navigate(Screen.Home)
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
                onBack = { navigator.goBack() },
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

    if (isSetupComplete == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        NavDisplay(
            entries = navigationState.toEntries(entryProvider),
            onBack = { navigator.goBack() }
        )
    }

    if (showChangelog) {
        ChangelogSheet(
            currentVersionName = currentVersionName,
            changelogText = stringResource(R.string.changelog_0_4_2),
            onDismiss = {
                showChangelog = false
                scope.launch {
                    preferences.setLastSeenVersion(currentVersionCode)
                    if (!isPriorityEduShown) showPriorityEdu = true
                }
            }
        )
    }

    if (showPriorityEdu) {
        PriorityEducationDialog(
            onDismiss = { showPriorityEdu = false; scope.launch { preferences.setPriorityEduShown(true) } },
            onConfigure = {
                showPriorityEdu = false
                scope.launch { preferences.setPriorityEduShown(true) }
                navigator.navigate(Screen.Behavior)
            }
        )
    }
}
