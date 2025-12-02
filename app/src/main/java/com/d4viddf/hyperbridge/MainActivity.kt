package com.d4viddf.hyperbridge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.ui.components.ChangelogDialog
import com.d4viddf.hyperbridge.ui.components.PriorityEducationDialog
import com.d4viddf.hyperbridge.ui.screens.home.HomeScreen
import com.d4viddf.hyperbridge.ui.screens.onboarding.OnboardingScreen
import com.d4viddf.hyperbridge.ui.screens.settings.*
import com.d4viddf.hyperbridge.ui.theme.HyperBridgeTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
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

enum class Screen(val depth: Int) {
    ONBOARDING(0), HOME(1), INFO(2), SETUP(3), LICENSES(3), BEHAVIOR(3), GLOBAL_SETTINGS(3), HISTORY(3), NAV_CUSTOMIZATION(4), APP_PRIORITY(4), GLOBAL_BLOCKLIST(4),
    BLOCKLIST_APPS(5)
}

@Composable
fun MainRootNavigation() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = remember { AppPreferences(context) }

    val packageInfo = remember { try { context.packageManager.getPackageInfo(context.packageName, 0) } catch (e: Exception) { null } }
    @Suppress("DEPRECATION")
    val currentVersionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) packageInfo?.longVersionCode?.toInt() ?: 0 else packageInfo?.versionCode ?: 0
    val currentVersionName = packageInfo?.versionName ?: "0.3.0"

    // --- 1. ROBUST DATA COLLECTION ---
    // We use produceState to initialize as NULL.
    // This ensures we show the Loading Spinner until the Flow emits its first real value from disk.
    val isSetupComplete by produceState<Boolean?>(initialValue = null) {
        preferences.isSetupComplete.collect { value = it }
    }

    val lastSeenVersion by preferences.lastSeenVersion.collectAsState(initial = currentVersionCode)
    val isPriorityEduShown by preferences.isPriorityEduShown.collectAsState(initial = true)

    var currentScreen by remember { mutableStateOf<Screen?>(null) }
    var showChangelog by remember { mutableStateOf(false) }
    var showPriorityEdu by remember { mutableStateOf(false) }
    var navConfigPackage by remember { mutableStateOf<String?>(null) }

    // --- 2. ROUTING LOGIC ---
    LaunchedEffect(isSetupComplete) {
        // Only run logic when we have a valid boolean from disk
        if (isSetupComplete != null) {
            if (currentScreen == null) {
                // Route based on saved state
                currentScreen = if (isSetupComplete == true) Screen.HOME else Screen.ONBOARDING
            }

            // If setup is done, check for modals
            if (isSetupComplete == true) {
                if (currentVersionCode > lastSeenVersion) {
                    showChangelog = true
                } else if (!isPriorityEduShown && !showChangelog) {
                    showPriorityEdu = true
                }
            }
        }
    }

    BackHandler(enabled = currentScreen != Screen.HOME && currentScreen != Screen.ONBOARDING) {
        currentScreen = when (currentScreen) {
            Screen.NAV_CUSTOMIZATION -> if (navConfigPackage != null) Screen.HOME else Screen.GLOBAL_SETTINGS
            Screen.GLOBAL_SETTINGS -> Screen.INFO
            Screen.APP_PRIORITY -> Screen.BEHAVIOR
            Screen.HISTORY -> Screen.INFO
            Screen.BEHAVIOR, Screen.SETUP, Screen.LICENSES -> Screen.INFO
            Screen.INFO -> Screen.HOME
            else -> Screen.HOME
        }
    }

    // --- 3. LOADING STATE ---
    // If setup state is null, or we haven't routed yet -> Show Spinner
    if (isSetupComplete == null || currentScreen == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        AnimatedContent(
            targetState = currentScreen!!,
            transitionSpec = {
                if (targetState.depth > initialState.depth) {
                    (slideInHorizontally { width -> width } + fadeIn(tween(400))).togetherWith(slideOutHorizontally { width -> -width / 3 } + fadeOut(tween(400)))
                } else {
                    (slideInHorizontally { width -> -width } + fadeIn(tween(400))).togetherWith(slideOutHorizontally { width -> width / 3 } + fadeOut(tween(400)))
                } using SizeTransform(clip = false)
            },
            label = "ScreenTransition"
        ) { target ->
            when (target) {
                Screen.ONBOARDING -> OnboardingScreen {
                    scope.launch {
                        preferences.setSetupComplete(true)
                        preferences.setLastSeenVersion(currentVersionCode)
                        preferences.setPriorityEduShown(true)
                        currentScreen = Screen.HOME
                    }
                }
                Screen.HOME -> HomeScreen(
                    onSettingsClick = { currentScreen = Screen.INFO },
                    onNavConfigClick = { pkg -> navConfigPackage = pkg; currentScreen = Screen.NAV_CUSTOMIZATION }
                )
                Screen.INFO -> InfoScreen(
                    onBack = { currentScreen = Screen.HOME },
                    onSetupClick = { currentScreen = Screen.SETUP },
                    onLicensesClick = { currentScreen = Screen.LICENSES },
                    onBehaviorClick = { currentScreen = Screen.BEHAVIOR },
                    onGlobalSettingsClick = { currentScreen = Screen.GLOBAL_SETTINGS },
                    onHistoryClick = { currentScreen = Screen.HISTORY },
                    onBlocklistClick = { currentScreen = Screen.GLOBAL_BLOCKLIST }
                )
                Screen.GLOBAL_SETTINGS -> GlobalSettingsScreen(onBack = { currentScreen = Screen.INFO }, onNavSettingsClick = { navConfigPackage = null; currentScreen = Screen.NAV_CUSTOMIZATION })
                Screen.NAV_CUSTOMIZATION -> NavCustomizationScreen(onBack = { currentScreen = if (navConfigPackage != null) Screen.HOME else Screen.GLOBAL_SETTINGS }, packageName = navConfigPackage)
                Screen.SETUP -> SetupHealthScreen(onBack = { currentScreen = Screen.INFO })
                Screen.LICENSES -> LicensesScreen(onBack = { currentScreen = Screen.INFO })
                Screen.BEHAVIOR -> PrioritySettingsScreen(onBack = { currentScreen = Screen.INFO }, onNavigateToPriorityList = { currentScreen = Screen.APP_PRIORITY })
                Screen.APP_PRIORITY -> AppPriorityScreen(onBack = { currentScreen = Screen.BEHAVIOR })
                Screen.HISTORY -> ChangelogHistoryScreen(onBack = { currentScreen = Screen.INFO })
                Screen.GLOBAL_BLOCKLIST -> GlobalBlocklistScreen(
                    onBack = { currentScreen = Screen.INFO },
                    onNavigateToAppList = { currentScreen = Screen.BLOCKLIST_APPS }
                )

                Screen.BLOCKLIST_APPS -> BlocklistAppListScreen(
                    onBack = { currentScreen = Screen.GLOBAL_BLOCKLIST }
                )
            }
        }
    }

    if (showChangelog) {
        ChangelogDialog(currentVersionName = currentVersionName, changelogText = stringResource(R.string.changelog_0_3_0)) {
            showChangelog = false
            scope.launch {
                preferences.setLastSeenVersion(currentVersionCode)
                if (!isPriorityEduShown) showPriorityEdu = true
            }
        }
    }

    if (showPriorityEdu) {
        PriorityEducationDialog(
            onDismiss = { showPriorityEdu = false; scope.launch { preferences.setPriorityEduShown(true) } },
            onConfigure = {
                showPriorityEdu = false
                scope.launch { preferences.setPriorityEduShown(true) }
                currentScreen = Screen.BEHAVIOR
            }
        )
    }
}