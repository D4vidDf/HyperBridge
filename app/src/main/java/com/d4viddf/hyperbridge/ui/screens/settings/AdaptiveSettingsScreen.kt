package com.d4viddf.hyperbridge.ui.screens.settings

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.data.db.AppDatabase
import com.d4viddf.hyperbridge.data.model.HyperBridgeBackup
import com.d4viddf.hyperbridge.ui.screens.theme.GlobalReplyCustomizationScreen
import com.d4viddf.hyperbridge.util.BackupManager
import kotlinx.coroutines.launch

enum class SettingsDestination {
    ROOT,
    // Level 1 (opened from InfoScreen)
    GLOBAL_SETTINGS,
    FLOATING_SETUP,
    BEHAVIOR,
    GLOBAL_BLOCKLIST,
    BACKUP,
    SETUP_HEALTH,
    DIAGNOSTICS,
    BUG_REPORT,
    HISTORY,
    LICENSES,
    // Level 2+ (opened from inside a Level 1 detail pane, cascading previous right -> left)
    ENGINE_SETTINGS,
    ISLAND_SETTINGS,
    DND_SETTINGS,
    PERMANENT_ISLAND,
    SMART_ACTIONS,
    DISCOUNT_KEYWORDS,
    NAV_CUSTOMIZATION,
    REPLY_CUSTOMIZATION,
    APP_PRIORITY,
    BLOCKLIST_APPS,
    IMPORT_PREVIEW
}

private val SETTINGS_SIDE_BY_SIDE_MIN_WIDTH = 720.dp

/**
 * Adaptive Material 3 Expressive Settings host with cascading two-pane navigation:
 * - Depth 1 (`[ROOT]`): Displays `InfoScreen`.
 * - Depth 2 (`[ROOT, Option1]`): On wide screens (`>= 720.dp`), `InfoScreen` narrows into the Left Pane
 *   while `Option1` opens in the Right Detail Pane using a single synchronized spring progression.
 * - Depth 3+ (`[ROOT, Option1, Option2]`): Selecting `Option2` from inside `Option1` moves `Option1`
 *   to the Left Pane and opens `Option2` in the Right Detail Pane.
 * - On narrower screens (`< 720.dp`), any active detail pane expands to full width so it is never cramped.
 */
@Composable
fun AdaptiveSettingsScreen(
    showRootBackButton: Boolean = true,
    onExit: () -> Unit = {}
) {
    val context = LocalContext.current
    val backupManager = remember(context) {
        val appContext = context.applicationContext
        BackupManager(
            appContext,
            AppPreferences(appContext),
            AppDatabase.getDatabase(appContext)
        )
    }

    // Navigation stack stored as a list of destination names so it survives configuration changes
    var stackNames by rememberSaveable {
        mutableStateOf(listOf(SettingsDestination.ROOT.name))
    }
    var previousStackSize by rememberSaveable { mutableIntStateOf(1) }
    var pendingBackupData by remember { mutableStateOf<HyperBridgeBackup?>(null) }

    val stack = remember(stackNames) {
        stackNames.mapNotNull { name ->
            runCatching { SettingsDestination.valueOf(name) }.getOrNull()
        }.ifEmpty { listOf(SettingsDestination.ROOT) }
    }

    // Cache the last active right-pane destination so exit animations keep rendering content while sliding out
    var lastRightDestinationName by rememberSaveable {
        mutableStateOf(SettingsDestination.GLOBAL_SETTINGS.name)
    }
    if (stack.size >= 2) {
        lastRightDestinationName = stack.last().name
    }
    val renderedRightDestination = if (stack.size >= 2) {
        stack.last()
    } else {
        runCatching { SettingsDestination.valueOf(lastRightDestinationName) }
            .getOrDefault(SettingsDestination.GLOBAL_SETTINGS)
    }

    val leftDestination = if (stack.size >= 2) {
        stack[stack.size - 2]
    } else {
        SettingsDestination.ROOT
    }

    val selectedChildOfLeft: SettingsDestination? = if (stack.size >= 2) {
        stack.last()
    } else {
        null
    }

    fun navigateFrom(source: SettingsDestination, target: SettingsDestination) {
        val sourceIndex = stack.lastIndexOf(source)
        previousStackSize = stack.size
        stackNames = if (sourceIndex != -1) {
            if (stack.getOrNull(sourceIndex + 1) == target && stack.size == sourceIndex + 2) {
                // Tapping the already-open option in the left pane closes the detail pane back to source
                stack.take(sourceIndex + 1).map { it.name }
            } else {
                (stack.take(sourceIndex + 1) + target).map { it.name }
            }
        } else {
            (stack + target).map { it.name }
        }
    }

    fun popBack() {
        if (stack.size > 1) {
            previousStackSize = stack.size
            stackNames = stack.dropLast(1).map { it.name }
        } else {
            onExit()
        }
    }

    if (stack.size > 1) {
        BackHandler {
            popBack()
        }
    }

    val isMovingForward = stack.size >= previousStackSize
    val hasDetailPane = stack.size >= 2

    val detailProgress by animateFloatAsState(
        targetValue = if (hasDetailPane) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "SettingsDetailProgress"
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpanded = maxWidth >= 600.dp
        val canShowSideBySideDetail = maxWidth >= SETTINGS_SIDE_BY_SIDE_MIN_WIDTH

        val horizPad = if (isExpanded) lerp(0.dp, 20.dp, detailProgress) else 0.dp
        val vertPad = if (isExpanded) lerp(0.dp, 12.dp, detailProgress) else 0.dp
        val innerAvailableWidth = (maxWidth - horizPad * 2).coerceAtLeast(320.dp)
        val splitGap = 20.dp
        val targetLeftSplitWidth = (innerAvailableWidth - splitGap) * 0.45f
        val targetRightSplitWidth = (innerAvailableWidth - splitGap) * 0.55f

        val currentLeftWidth = if (canShowSideBySideDetail) {
            lerp(innerAvailableWidth, targetLeftSplitWidth, detailProgress)
        } else {
            innerAvailableWidth
        }
        val currentGapWidth = if (canShowSideBySideDetail) {
            lerp(0.dp, splitGap, detailProgress)
        } else {
            0.dp
        }
        val currentRightBoxWidth = if (canShowSideBySideDetail) {
            (innerAvailableWidth - currentLeftWidth - currentGapWidth).coerceAtLeast(0.dp)
        } else {
            innerAvailableWidth
        }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.statusBars
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .padding(horizontal = horizPad, vertical = vertPad)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // LEFT PANE: Shows ROOT at depth 1-2, and cascades to stack[stack.size - 2] at depth 3+
                    if (canShowSideBySideDetail || detailProgress < 0.999f) {
                        Box(
                            modifier = Modifier
                                .width(currentLeftWidth)
                                .fillMaxHeight()
                                .graphicsLayer {
                                    if (!canShowSideBySideDetail) {
                                        alpha = 1f - detailProgress
                                        translationX = -detailProgress * size.width * 0.15f
                                    }
                                }
                        ) {
                            AnimatedContent(
                                targetState = leftDestination,
                                transitionSpec = {
                                    if (isMovingForward) {
                                        // Previous right pane slides from right to left into the Left Pane
                                        (fadeIn(tween(180)) + slideInHorizontally(
                                            initialOffsetX = { it / 2 },
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        )).togetherWith(
                                            fadeOut(tween(120)) + slideOutHorizontally(
                                                targetOffsetX = { -it / 3 },
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                                    stiffness = Spring.StiffnessMedium
                                                )
                                            )
                                        )
                                    } else {
                                        // Parent pane slides back in from the left when popping
                                        (fadeIn(tween(180)) + slideInHorizontally(
                                            initialOffsetX = { -it / 3 },
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        )).togetherWith(
                                            fadeOut(tween(120)) + slideOutHorizontally(
                                                targetOffsetX = { it / 2 },
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                                    stiffness = Spring.StiffnessMedium
                                                )
                                            )
                                        )
                                    }
                                },
                                label = "SettingsLeftPaneCascade"
                            ) { currentLeft ->
                                if (currentLeft == SettingsDestination.ROOT) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        RenderSettingsDestination(
                                            destination = SettingsDestination.ROOT,
                                            selectedChild = selectedChildOfLeft,
                                            showRootBackButton = showRootBackButton,
                                            backupManager = backupManager,
                                            pendingBackupData = pendingBackupData,
                                            onBackupLoaded = { backup ->
                                                pendingBackupData = backup
                                                navigateFrom(SettingsDestination.BACKUP, SettingsDestination.IMPORT_PREVIEW)
                                            },
                                            onRestoreSuccess = {
                                                stackNames = listOf(SettingsDestination.ROOT.name)
                                            },
                                            onNavigate = { target ->
                                                navigateFrom(SettingsDestination.ROOT, target)
                                            },
                                            onBack = { popBack() }
                                        )
                                    }
                                } else {
                                    // Sub-screen that moved from the Right Detail Pane into the Left Pane
                                    SettingsDetailPaneSurface {
                                        RenderSettingsDestination(
                                            destination = currentLeft,
                                            selectedChild = selectedChildOfLeft,
                                            showRootBackButton = true,
                                            backupManager = backupManager,
                                            pendingBackupData = pendingBackupData,
                                            onBackupLoaded = { backup ->
                                                pendingBackupData = backup
                                                navigateFrom(currentLeft, SettingsDestination.IMPORT_PREVIEW)
                                            },
                                            onRestoreSuccess = {
                                                stackNames = listOf(SettingsDestination.ROOT.name)
                                            },
                                            onNavigate = { target ->
                                                navigateFrom(currentLeft, target)
                                            },
                                            onBack = { popBack() }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // RIGHT DETAIL PANE (Side-by-side on >= 720.dp, synchronized single-stage width progression)
                    if (canShowSideBySideDetail && detailProgress > 0.001f) {
                        Spacer(modifier = Modifier.width(currentGapWidth))

                        Box(
                            modifier = Modifier
                                .width(currentRightBoxWidth)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(28.dp)),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Box(
                                modifier = Modifier
                                    .requiredWidth(targetRightSplitWidth)
                                    .fillMaxHeight()
                                    .graphicsLayer {
                                        alpha = detailProgress
                                        translationX = (1f - detailProgress) * 48.dp.toPx()
                                    }
                            ) {
                                SettingsDetailPaneSurface(useRoundedCorners = true) {
                                    AnimatedContent(
                                        targetState = renderedRightDestination,
                                        transitionSpec = {
                                            if (isMovingForward) {
                                                (fadeIn(tween(180)) + slideInHorizontally(
                                                    initialOffsetX = { it / 3 },
                                                    animationSpec = spring(
                                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                                        stiffness = Spring.StiffnessMedium
                                                    )
                                                )).togetherWith(
                                                    fadeOut(tween(120)) + slideOutHorizontally(
                                                        targetOffsetX = { -it / 4 },
                                                        animationSpec = spring(
                                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                                            stiffness = Spring.StiffnessMedium
                                                        )
                                                    )
                                                )
                                            } else {
                                                (fadeIn(tween(180)) + slideInHorizontally(
                                                    initialOffsetX = { -it / 4 },
                                                    animationSpec = spring(
                                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                                        stiffness = Spring.StiffnessMedium
                                                    )
                                                )).togetherWith(
                                                    fadeOut(tween(120)) + slideOutHorizontally(
                                                        targetOffsetX = { it / 3 },
                                                        animationSpec = spring(
                                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                                            stiffness = Spring.StiffnessMedium
                                                        )
                                                    )
                                                )
                                            }
                                        },
                                        label = "SettingsRightDetailTransition"
                                    ) { targetDest ->
                                        RenderSettingsDestination(
                                            destination = targetDest,
                                            selectedChild = null,
                                            showRootBackButton = true,
                                            backupManager = backupManager,
                                            pendingBackupData = pendingBackupData,
                                            onBackupLoaded = { backup ->
                                                pendingBackupData = backup
                                                navigateFrom(targetDest, SettingsDestination.IMPORT_PREVIEW)
                                            },
                                            onRestoreSuccess = {
                                                stackNames = listOf(SettingsDestination.ROOT.name)
                                            },
                                            onNavigate = { nextTarget ->
                                                navigateFrom(targetDest, nextTarget)
                                            },
                                            onBack = { popBack() }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // FULL-WIDTH OVERLAY DETAIL PANE FOR NARROWER SCREENS (< 720.dp)
                if (!canShowSideBySideDetail && detailProgress > 0.001f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = detailProgress
                                translationX = (1f - detailProgress) * size.width * 0.2f
                            }
                    ) {
                        SettingsDetailPaneSurface(useRoundedCorners = isExpanded) {
                            AnimatedContent(
                                targetState = renderedRightDestination,
                                transitionSpec = {
                                    if (isMovingForward) {
                                        (fadeIn(tween(200)) + slideInHorizontally(tween(220)) { it / 3 })
                                            .togetherWith(fadeOut(tween(140)) + slideOutHorizontally(tween(180)) { -it / 4 })
                                    } else {
                                        (fadeIn(tween(200)) + slideInHorizontally(tween(220)) { -it / 4 })
                                            .togetherWith(fadeOut(tween(140)) + slideOutHorizontally(tween(180)) { it / 3 })
                                    }
                                },
                                label = "SettingsCompactDetailTransition"
                            ) { targetDest ->
                                RenderSettingsDestination(
                                    destination = targetDest,
                                    selectedChild = null,
                                    showRootBackButton = true,
                                    backupManager = backupManager,
                                    pendingBackupData = pendingBackupData,
                                    onBackupLoaded = { backup ->
                                        pendingBackupData = backup
                                        navigateFrom(targetDest, SettingsDestination.IMPORT_PREVIEW)
                                    },
                                    onRestoreSuccess = {
                                        stackNames = listOf(SettingsDestination.ROOT.name)
                                    },
                                    onNavigate = { nextTarget ->
                                        navigateFrom(targetDest, nextTarget)
                                    },
                                    onBack = { popBack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Wraps a Settings pane in a rounded `surfaceContainerLow` card and overrides `background` & `surface`
 * in the local `MaterialTheme` so the `TopAppBar` / `LargeTopAppBar` / `Scaffold` of every Settings
 * screen seamlessly shares the detail panel background color.
 */
@Composable
private fun SettingsDetailPaneSurface(
    useRoundedCorners: Boolean = true,
    content: @Composable () -> Unit
) {
    val baseColorScheme = MaterialTheme.colorScheme
    val detailColorScheme = remember(baseColorScheme) {
        baseColorScheme.copy(
            background = baseColorScheme.surfaceContainerLow,
            surface = baseColorScheme.surfaceContainerLow,
            surfaceContainerLow = baseColorScheme.surfaceContainer
        )
    }
    val shape = if (useRoundedCorners) RoundedCornerShape(28.dp) else RoundedCornerShape(0.dp)

    MaterialTheme(colorScheme = detailColorScheme) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape),
            shape = shape,
            color = baseColorScheme.surfaceContainerLow
        ) {
            content()
        }
    }
}

@Composable
private fun RenderSettingsDestination(
    destination: SettingsDestination,
    selectedChild: SettingsDestination?,
    showRootBackButton: Boolean,
    backupManager: BackupManager,
    pendingBackupData: HyperBridgeBackup?,
    onBackupLoaded: (HyperBridgeBackup) -> Unit,
    onRestoreSuccess: () -> Unit,
    onNavigate: (SettingsDestination) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    when (destination) {
        SettingsDestination.ROOT -> {
            InfoScreen(
                onBack = onBack,
                showBackButton = showRootBackButton,
                selectedItemKey = selectedChild?.name,
                onGlobalSettingsClick = { onNavigate(SettingsDestination.GLOBAL_SETTINGS) },
                onFloatingSetupClick = { onNavigate(SettingsDestination.FLOATING_SETUP) },
                onBehaviorClick = { onNavigate(SettingsDestination.BEHAVIOR) },
                onBlocklistClick = { onNavigate(SettingsDestination.GLOBAL_BLOCKLIST) },
                onBackupClick = { onNavigate(SettingsDestination.BACKUP) },
                onSetupClick = { onNavigate(SettingsDestination.SETUP_HEALTH) },
                onDiagnosticsClick = { onNavigate(SettingsDestination.DIAGNOSTICS) },
                onBugReportClick = { onNavigate(SettingsDestination.BUG_REPORT) },
                onHistoryClick = { onNavigate(SettingsDestination.HISTORY) },
                onLicensesClick = { onNavigate(SettingsDestination.LICENSES) }
            )
        }

        SettingsDestination.GLOBAL_SETTINGS -> {
            GlobalSettingsScreen(
                onBack = onBack,
                selectedItemKey = selectedChild?.name,
                onEngineSettingsClick = { onNavigate(SettingsDestination.ENGINE_SETTINGS) },
                onIslandSettingsClick = { onNavigate(SettingsDestination.ISLAND_SETTINGS) },
                onDndSettingsClick = { onNavigate(SettingsDestination.DND_SETTINGS) },
                onPermanentIslandClick = { onNavigate(SettingsDestination.PERMANENT_ISLAND) },
                onSmartActionsClick = { onNavigate(SettingsDestination.SMART_ACTIONS) },
                onDiscountKeywordsClick = { onNavigate(SettingsDestination.DISCOUNT_KEYWORDS) },
                onNavSettingsClick = { onNavigate(SettingsDestination.NAV_CUSTOMIZATION) },
                onInlineReplyClick = { onNavigate(SettingsDestination.REPLY_CUSTOMIZATION) }
            )
        }

        SettingsDestination.FLOATING_SETUP -> {
            FloatingNotificationSetupScreen(onBack = onBack)
        }

        SettingsDestination.BEHAVIOR -> {
            PrioritySettingsScreen(
                onBack = onBack,
                onNavigateToPriorityList = { onNavigate(SettingsDestination.APP_PRIORITY) }
            )
        }

        SettingsDestination.GLOBAL_BLOCKLIST -> {
            GlobalBlocklistScreen(
                onBack = onBack,
                onNavigateToAppList = { onNavigate(SettingsDestination.BLOCKLIST_APPS) }
            )
        }

        SettingsDestination.BACKUP -> {
            BackupSettingsScreen(
                onBack = onBack,
                backupManager = backupManager,
                onBackupFileLoaded = onBackupLoaded
            )
        }

        SettingsDestination.SETUP_HEALTH -> {
            SetupHealthScreen(
                onBack = onBack,
                onNavigateToBugReport = { onNavigate(SettingsDestination.BUG_REPORT) }
            )
        }

        SettingsDestination.DIAGNOSTICS -> {
            DiagnosticsScreen(
                onBack = onBack,
                onReportError = { onNavigate(SettingsDestination.BUG_REPORT) },
                onNavigateToHealth = { onNavigate(SettingsDestination.SETUP_HEALTH) }
            )
        }

        SettingsDestination.BUG_REPORT -> {
            BugReportScreen(
                onBack = onBack,
                onNavigateToDiagnostics = { onNavigate(SettingsDestination.DIAGNOSTICS) }
            )
        }

        SettingsDestination.HISTORY -> {
            ChangelogHistoryScreen(onBack = onBack)
        }

        SettingsDestination.LICENSES -> {
            LicensesScreen(onBack = onBack)
        }

        SettingsDestination.ENGINE_SETTINGS -> {
            EngineSettingsScreen(onBack = onBack)
        }

        SettingsDestination.ISLAND_SETTINGS -> {
            IslandSettingsScreen(onBack = onBack)
        }

        SettingsDestination.DND_SETTINGS -> {
            DndSettingsScreen(onBack = onBack)
        }

        SettingsDestination.PERMANENT_ISLAND -> {
            PermanentIslandConfigScreen(onBack = onBack)
        }

        SettingsDestination.SMART_ACTIONS -> {
            SmartActionsSettingsScreen(onBack = onBack)
        }

        SettingsDestination.DISCOUNT_KEYWORDS -> {
            ProgressDiscountKeywordsScreen(onBack = onBack)
        }

        SettingsDestination.NAV_CUSTOMIZATION -> {
            NavCustomizationScreen(
                onBack = onBack,
                packageName = null
            )
        }

        SettingsDestination.REPLY_CUSTOMIZATION -> {
            GlobalReplyCustomizationScreen(onBack = onBack)
        }

        SettingsDestination.APP_PRIORITY -> {
            AppPriorityScreen(onBack = onBack)
        }

        SettingsDestination.BLOCKLIST_APPS -> {
            BlocklistAppListScreen(onBack = onBack)
        }

        SettingsDestination.IMPORT_PREVIEW -> {
            val backup = pendingBackupData
            if (backup != null) {
                val importSuccessMsg = stringResource(R.string.import_success)
                val importFailedMsg = stringResource(R.string.import_failed)
                ImportPreviewScreen(
                    backupData = backup,
                    onBack = onBack,
                    onConfirmRestore = { selection ->
                        scope.launch {
                            val result = backupManager.restoreBackup(backup, selection)
                            if (result.isSuccess) {
                                Toast.makeText(context, importSuccessMsg, Toast.LENGTH_LONG).show()
                                onRestoreSuccess()
                            } else {
                                val error = result.exceptionOrNull()?.message ?: ""
                                Toast.makeText(context, importFailedMsg.format(error), Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                )
            } else {
                Box(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
