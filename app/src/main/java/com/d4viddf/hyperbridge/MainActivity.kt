package com.d4viddf.hyperbridge

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.BackHandler
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
import androidx.navigation3.ui.NavDisplay
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.data.db.AppDatabase
import com.d4viddf.hyperbridge.ui.components.ChangelogSheet
import com.d4viddf.hyperbridge.ui.components.PriorityEducationDialog
import com.d4viddf.hyperbridge.ui.navigation.Navigator
import com.d4viddf.hyperbridge.ui.navigation.Screen
import com.d4viddf.hyperbridge.ui.navigation.mainNavGraph
import com.d4viddf.hyperbridge.ui.navigation.rememberNavigationState
import com.d4viddf.hyperbridge.ui.navigation.toEntries
import com.d4viddf.hyperbridge.ui.theme.HyperBridgeTheme
import com.d4viddf.hyperbridge.util.BackupManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HyperBridgeTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainRootNavigation(onExit = { finish() })
                }
            }
        }
    }
}

@Composable
fun MainRootNavigation(onExit: () -> Unit) {
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

    if (isSetupComplete == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        MainNavigationContent(
            isSetupComplete = isSetupComplete!!,
            context = context,
            scope = scope,
            preferences = preferences,
            backupManager = backupManager,
            currentVersionCode = currentVersionCode,
            currentVersionName = currentVersionName,
            onExit = onExit
        )
    }
}

@Composable
private fun MainNavigationContent(
    isSetupComplete: Boolean,
    context: Context,
    scope: CoroutineScope,
    preferences: AppPreferences,
    backupManager: BackupManager,
    currentVersionCode: Int,
    currentVersionName: String,
    onExit: () -> Unit
) {
    val lastSeenVersion by preferences.lastSeenVersion.collectAsState(initial = currentVersionCode)
    val isPriorityEduShown by preferences.isPriorityEduShown.collectAsState(initial = true)

    var showChangelog by remember { mutableStateOf(false) }
    var showPriorityEdu by remember { mutableStateOf(false) }

    val startRoute = if (isSetupComplete) Screen.Home else Screen.Onboarding
    val navigationState = rememberNavigationState(
        startRoute = startRoute,
        topLevelRoutes = setOf(Screen.Onboarding, Screen.Home)
    )
    val navigator = remember(navigationState) { Navigator(navigationState) }

    LaunchedEffect(isSetupComplete) {
        if (isSetupComplete) {
            if (currentVersionCode > lastSeenVersion) {
                showChangelog = true
            } else if (!isPriorityEduShown && !showChangelog) {
                showPriorityEdu = true
            }
        }
    }

    val entryProvider = mainNavGraph(
        context = context,
        scope = scope,
        preferences = preferences,
        navigator = navigator,
        backupManager = backupManager,
        currentVersionCode = currentVersionCode,
        onExit = onExit
    )

    BackHandler {
        if (!navigator.goBack()) {
            onExit()
        }
    }

    NavDisplay(
        entries = navigationState.toEntries(entryProvider),
        onBack = {
            if (!navigator.goBack()) {
                onExit()
            }
        }
    )

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
