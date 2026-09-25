package com.d4viddf.hyperbridge.ui.screens.settings

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.DashboardCustomize
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.DisplaySettings
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.Preview
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.createBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.data.widget.WidgetManager
import com.d4viddf.hyperbridge.models.AppSmartActionsOverride
import com.d4viddf.hyperbridge.models.CallStage
import com.d4viddf.hyperbridge.models.IslandConfig
import com.d4viddf.hyperbridge.models.NotificationType
import com.d4viddf.hyperbridge.models.SmartActionType
import com.d4viddf.hyperbridge.models.SmartActionsConfig
import com.d4viddf.hyperbridge.models.WidgetConfig
import com.d4viddf.hyperbridge.models.WidgetSize
import com.d4viddf.hyperbridge.ui.AppInfo
import com.d4viddf.hyperbridge.ui.AppListViewModel
import com.d4viddf.hyperbridge.ui.components.IslandSettingsControl
import com.d4viddf.hyperbridge.ui.screens.design.DesignPreviewCardItem
import com.d4viddf.hyperbridge.ui.screens.design.WidgetConfigScreen
import com.d4viddf.hyperbridge.ui.screens.design.WidgetPickerScreen
import com.d4viddf.hyperbridge.ui.screens.theme.ShapeStyle
import com.d4viddf.hyperbridge.ui.screens.theme.getExpressiveShape
import com.d4viddf.hyperbridge.ui.screens.translators.TRANSLATOR_OUTLINED_ICONS
import com.d4viddf.hyperbridge.ui.theme.HyperBridgeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Subscreens available within the App Configuration flow.
 * Group 1: Notification Types, Island Behavior, Blocked Terms
 * Group 2: Island Widgets
 * Group 3: Custom Design, Custom Translators
 */
enum class AppConfigSubscreen {
    NOTIFICATION_TYPES,
    ISLAND_BEHAVIOR,
    SMART_ACTIONS,
    BLOCKED_TERMS,
    ISLAND_WIDGETS,
    CUSTOM_DESIGN,
    CUSTOM_TRANSLATORS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppConfigScreen(
    packageName: String,
    viewModel: AppListViewModel = viewModel(),
    translatorViewModel: com.d4viddf.hyperbridge.ui.screens.translators.TranslatorViewModel = viewModel(),
    onBack: () -> Unit,
    onNavConfigClick: (String) -> Unit,
    onCreateTranslator: (String) -> Unit = {},
    onEditTranslator: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = remember { AppPreferences(context.applicationContext) }

    var currentSubscreen by remember { mutableStateOf<AppConfigSubscreen?>(null) }

    val effectiveConfig by viewModel.getEffectiveAppConfigFlow(packageName).collectAsState(initial = null)
    val activeTypes = effectiveConfig?.activeTypes ?: emptySet()
    val activeCallStages = effectiveConfig?.activeCallStages ?: CallStage.entries.toSet()
    val isManagedByTheme = effectiveConfig?.isManagedByTheme == true

    val appIslandConfig by viewModel.getAppIslandConfig(packageName).collectAsState(initial = IslandConfig())
    val globalConfig by viewModel.globalConfigFlow.collectAsState(
        initial = IslandConfig(isFloat = true, isShowShade = true, timeout = 5)
    )

    val blockedTerms by viewModel.getAppBlockedTerms(packageName).collectAsState(initial = emptySet())

    val globalSmartActionsConfig by viewModel.smartActionsConfigFlow.collectAsState(initial = SmartActionsConfig.DISABLED)

    val allowedPackages by preferences.allowedPackagesFlow.collectAsState(initial = emptySet())
    val isBridged = allowedPackages.contains(packageName)
    val appTranslators by translatorViewModel.getTranslatorsForApp(packageName).collectAsState(initial = emptyList())
    val appDesigns by translatorViewModel.getDesignsForApp(packageName).collectAsState(initial = emptyList())

    var appInfo by remember { mutableStateOf<AppInfo?>(null) }
    LaunchedEffect(packageName) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            try {
                val ai = pm.getApplicationInfo(packageName, 0)
                val label = pm.getApplicationLabel(ai).toString()
                val iconDrawable = pm.getApplicationIcon(ai)
                val iconBmp = drawableToBitmap(iconDrawable)
                appInfo = AppInfo(
                    name = label,
                    packageName = packageName,
                    icon = iconBmp,
                    isBridged = isBridged,
                    isInstalled = true
                )
            } catch (_: Exception) {
                appInfo = AppInfo(
                    name = packageName,
                    packageName = packageName,
                    icon = null,
                    isBridged = isBridged,
                    isInstalled = false
                )
            }
        }
    }

    val savedWidgetIds by preferences.savedWidgetIdsFlow.collectAsState(initial = emptyList())
    val refreshWidgetTrigger = remember { mutableIntStateOf(0) }
    var appSavedWidgetIds by remember { mutableStateOf<List<Int>>(emptyList()) }
    var availableProviders by remember { mutableStateOf<List<AppWidgetProviderInfo>>(emptyList()) }
    var isPickingWidget by remember { mutableStateOf(false) }
    var editingWidgetId by remember { mutableStateOf<Int?>(null) }
    var showAddDesign by remember { mutableStateOf(false) }

    BackHandler(enabled = editingWidgetId != null || isPickingWidget || showAddDesign || currentSubscreen != null) {
        when {
            showAddDesign -> showAddDesign = false
            editingWidgetId != null -> editingWidgetId = null
            isPickingWidget -> isPickingWidget = false
            currentSubscreen != null -> currentSubscreen = null
        }
    }

    LaunchedEffect(savedWidgetIds, refreshWidgetTrigger.intValue, packageName) {
        withContext(Dispatchers.IO) {
            val filteredIds = savedWidgetIds.filter { id ->
                val info = WidgetManager.getWidgetInfo(context, id)
                info?.provider?.packageName == packageName
            }
            appSavedWidgetIds = filteredIds

            val manager = AppWidgetManager.getInstance(context)
            val allProviders = manager.installedProviders
            availableProviders = allProviders.filter { it.provider.packageName == packageName }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AppConfigContent(
            appName = appInfo?.name ?: packageName,
            packageName = packageName,
            appIcon = appInfo?.icon,
            isBridged = isBridged,
            isManagedByTheme = isManagedByTheme,
            activeTypes = activeTypes,
            activeCallStages = activeCallStages,
            appIslandConfig = appIslandConfig,
            globalConfig = globalConfig,
            blockedTerms = blockedTerms,
            globalSmartActionsConfig = globalSmartActionsConfig,
            savedWidgetIds = appSavedWidgetIds,
            availableProviders = availableProviders,
            translators = appTranslators,
            designs = appDesigns,
            currentSubscreen = currentSubscreen,
            onNavigateSubscreen = { currentSubscreen = it },
            onBack = onBack,
            onToggleBridged = { enabled -> viewModel.toggleApp(packageName, enabled) },
            onToggleType = { type, enabled -> viewModel.updateAppConfig(packageName, type, enabled) },
            onToggleCallStage = { stage, enabled -> viewModel.updateAppCallStage(packageName, stage, enabled) },
            onUpdateIslandConfig = { config -> viewModel.updateAppIslandConfig(packageName, config) },
            onUpdateBlockedTerms = { terms -> viewModel.updateAppBlockedTerms(packageName, terms) },
            onSmartActionExcludedChange = { excluded -> viewModel.setSmartActionExcluded(packageName, excluded) },
            onSmartActionTypeOverride = { type, enabled -> viewModel.setAppSmartActionTypeOverride(packageName, type, enabled) },
            onClearSmartActionsOverride = { viewModel.clearAppSmartActionsOverride(packageName) },
            onNavConfigClick = { onNavConfigClick(packageName) },
            onAddWidgetClick = { isPickingWidget = true },
            onEditWidget = { widgetId -> editingWidgetId = widgetId },
            onDeleteWidget = { widgetId ->
                val killIntent = Intent(context, com.d4viddf.hyperbridge.service.WidgetOverlayService::class.java).apply {
                    action = "ACTION_KILL_WIDGET"
                    putExtra("WIDGET_ID", widgetId)
                }
                context.startService(killIntent)
                scope.launch {
                    preferences.removeWidgetId(widgetId)
                    refreshWidgetTrigger.intValue++
                }
            },
            onToggleTranslator = { id, enabled -> translatorViewModel.toggleTranslator(id, enabled) },
            onCreateTranslator = { onCreateTranslator(packageName) },
            onEditTranslator = onEditTranslator,
            onToggleDesign = { designId, enabled -> translatorViewModel.toggleDesignForApp(designId, packageName, enabled) },
            onCreateDesign = { showAddDesign = true }
        )

        if (showAddDesign) {
            com.d4viddf.hyperbridge.ui.screens.design.AddDesignFlow(
                onDismiss = { showAddDesign = false },
                onDesignCreated = { design ->
                    showAddDesign = false
                    val targetedDesign = design.copy(
                        targetScope = com.d4viddf.hyperbridge.models.translator.TargetScope.SPECIFIC_APPS,
                        targetPackages = listOf(packageName)
                    )
                    translatorViewModel.saveTranslator(targetedDesign)
                }
            )
        }

        // --- OVERLAYS ---
        AnimatedVisibility(
            visible = isPickingWidget,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(400)
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(400)
            ) + fadeOut(),
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            WidgetPickerScreen(
                targetPackageName = packageName,
                onBack = { isPickingWidget = false },
                onWidgetSelected = { newId ->
                    isPickingWidget = false
                    scope.launch {
                        preferences.saveWidgetConfig(newId, WidgetConfig())
                        refreshWidgetTrigger.intValue++
                        editingWidgetId = newId
                    }
                }
            )
        }

        AnimatedVisibility(
            visible = editingWidgetId != null,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(400)
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(400)
            ) + fadeOut(),
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            if (editingWidgetId != null) {
                WidgetConfigScreen(
                    widgetId = editingWidgetId!!,
                    onBack = {
                        editingWidgetId = null
                        refreshWidgetTrigger.intValue++
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppConfigContent(
    appName: String,
    packageName: String,
    appIcon: Bitmap?,
    isBridged: Boolean,
    isManagedByTheme: Boolean,
    activeTypes: Set<String>,
    activeCallStages: Set<CallStage>,
    appIslandConfig: IslandConfig,
    globalConfig: IslandConfig,
    blockedTerms: Set<String>,
    globalSmartActionsConfig: SmartActionsConfig,
    savedWidgetIds: List<Int>,
    availableProviders: List<AppWidgetProviderInfo>,
    translators: List<com.d4viddf.hyperbridge.models.translator.CustomTranslator> = emptyList(),
    designs: List<com.d4viddf.hyperbridge.models.translator.CustomTranslator> = emptyList(),
    currentSubscreen: AppConfigSubscreen? = null,
    onNavigateSubscreen: (AppConfigSubscreen?) -> Unit = {},
    onBack: () -> Unit,
    onToggleBridged: (Boolean) -> Unit,
    onToggleType: (NotificationType, Boolean) -> Unit,
    onToggleCallStage: (CallStage, Boolean) -> Unit,
    onUpdateIslandConfig: (IslandConfig) -> Unit,
    onUpdateBlockedTerms: (Set<String>) -> Unit,
    onSmartActionExcludedChange: (Boolean) -> Unit,
    onSmartActionTypeOverride: (SmartActionType, Boolean?) -> Unit,
    onClearSmartActionsOverride: () -> Unit,
    onNavConfigClick: () -> Unit,
    onAddWidgetClick: () -> Unit,
    onEditWidget: (Int) -> Unit,
    onDeleteWidget: (Int) -> Unit,
    onToggleTranslator: (String, Boolean) -> Unit = { _, _ -> },
    onCreateTranslator: () -> Unit = {},
    onEditTranslator: (String) -> Unit = {},
    onToggleDesign: (String, Boolean) -> Unit = { _, _ -> },
    onCreateDesign: () -> Unit = {}
) {
    val activeDesc = stringResource(R.string.cd_app_state_active)
    val inactiveDesc = stringResource(R.string.cd_app_state_inactive)
    val navEditDesc = stringResource(R.string.cd_nav_edit)
    val activeTypesSubtitle = stringResource(R.string.active_notifications_subtitle, activeTypes.size)
    val isUsingGlobal = appIslandConfig.isFloat == null
    val behaviorSubtitle = if (isUsingGlobal) {
        stringResource(R.string.use_global_default)
    } else {
        "${if (appIslandConfig.isFloat == true) activeDesc else inactiveDesc} • ${appIslandConfig.timeout ?: 5}s"
    }
    val blockedSubtitle = stringResource(R.string.blocked_terms_count, blockedTerms.size)
    val appSmartActionsOverride = appIslandConfig.smartActionsOverride ?: AppSmartActionsOverride()
    val isSmartActionsExcluded = packageName in globalSmartActionsConfig.excludedPackages
    val smartActionsSubtitle = when {
        !globalSmartActionsConfig.enabled -> stringResource(R.string.app_smart_actions_subtitle_global_off)
        isSmartActionsExcluded -> stringResource(R.string.app_smart_actions_subtitle_off)
        else -> stringResource(R.string.app_smart_actions_subtitle_on)
    }
    val widgetsSubtitle = if (savedWidgetIds.isNotEmpty()) {
        "${savedWidgetIds.size} configured"
    } else {
        stringResource(R.string.no_widgets_for_app)
    }
    val translatorsSubtitle = if (translators.isNotEmpty()) {
        stringResource(R.string.design_translators_carousel_desc, translators.count { it.isEnabled })
    } else {
        stringResource(R.string.custom_translators_desc)
    }
    val activeDesignsCount = designs.count { design ->
        design.isEnabled && !design.excludedPackages.any { it.equals(packageName, ignoreCase = true) }
    }
    val designsSubtitle = if (designs.isNotEmpty()) {
        stringResource(R.string.app_designs_subtitle, activeDesignsCount)
    } else {
        stringResource(R.string.custom_design_desc)
    }

    AnimatedContent(
        targetState = currentSubscreen,
        transitionSpec = {
            if (targetState != null && initialState == null) {
                (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> -width / 3 } + fadeOut()
                )
            } else if (targetState == null && initialState != null) {
                (slideInHorizontally { width -> -width / 3 } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> width } + fadeOut()
                )
            } else {
                fadeIn().togetherWith(fadeOut())
            }
        },
        label = "AppConfigSubscreenTransition"
    ) { subscreen ->
        if (subscreen == null) {
            // =========================================================================
            // MAIN OVERVIEW SCREEN
            // =========================================================================
            val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    LargeTopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = stringResource(R.string.app_config_title),
                                    maxLines = 1,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = stringResource(R.string.app_config_subtitle),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        navigationIcon = {
                            FilledTonalIconButton(
                                onClick = onBack,
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.back)
                                )
                            }
                        },
                        scrollBehavior = scrollBehavior
                    )
                }
            ) { padding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Overview Card
                    item {
                        AppHeaderCard(
                            appName = appName,
                            packageName = packageName,
                            appIcon = appIcon,
                            isBridged = isBridged,
                            isManagedByTheme = isManagedByTheme,
                            onToggleBridged = onToggleBridged
                        )
                    }

                    // --- GROUP 1: NOTIFICATIONS & BEHAVIOR (3 connected items) ---
                    item {
                        val group1Items = listOf(
                            AppConfigSubscreen.NOTIFICATION_TYPES,
                            AppConfigSubscreen.ISLAND_BEHAVIOR,
                            AppConfigSubscreen.SMART_ACTIONS,
                            AppConfigSubscreen.BLOCKED_TERMS
                        )

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            group1Items.forEachIndexed { index, route ->
                                val shape = getExpressiveShape(group1Items.size, index, ShapeStyle.Large)

                                when (route) {
                                    AppConfigSubscreen.NOTIFICATION_TYPES -> AppConfigOptionCard(
                                        title = stringResource(R.string.active_notifications_title),
                                        subtitle = activeTypesSubtitle,
                                        icon = Icons.Default.Notifications,
                                        shape = shape,
                                        onClick = { onNavigateSubscreen(AppConfigSubscreen.NOTIFICATION_TYPES) }
                                    )
                                    AppConfigSubscreen.ISLAND_BEHAVIOR -> AppConfigOptionCard(
                                        title = stringResource(R.string.island_behavior_title),
                                        subtitle = behaviorSubtitle,
                                        icon = Icons.Outlined.DisplaySettings,
                                        shape = shape,
                                        onClick = { onNavigateSubscreen(AppConfigSubscreen.ISLAND_BEHAVIOR) }
                                    )
                                    AppConfigSubscreen.SMART_ACTIONS -> AppConfigOptionCard(
                                        title = stringResource(R.string.app_smart_actions_title),
                                        subtitle = smartActionsSubtitle,
                                        icon = Icons.Outlined.AutoAwesome,
                                        shape = shape,
                                        onClick = { onNavigateSubscreen(AppConfigSubscreen.SMART_ACTIONS) }
                                    )
                                    AppConfigSubscreen.BLOCKED_TERMS -> AppConfigOptionCard(
                                        title = stringResource(R.string.blocked_terms),
                                        subtitle = blockedSubtitle,
                                        icon = Icons.Default.Block,
                                        shape = shape,
                                        onClick = { onNavigateSubscreen(AppConfigSubscreen.BLOCKED_TERMS) }
                                    )
                                    else -> {}
                                }
                            }
                        }
                    }

                    // --- GROUP 2: ISLAND WIDGETS (1 standalone item) ---
                    item {
                        val group2Items = listOf(
                            AppConfigSubscreen.ISLAND_WIDGETS
                        )

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            group2Items.forEachIndexed { index, _ ->
                                val shape = getExpressiveShape(group2Items.size, index, ShapeStyle.Large)

                                AppConfigOptionCard(
                                    title = stringResource(R.string.app_widgets_section_title),
                                    subtitle = widgetsSubtitle,
                                    icon = Icons.Outlined.Widgets,
                                    shape = shape,
                                    onClick = { onNavigateSubscreen(AppConfigSubscreen.ISLAND_WIDGETS) }
                                )
                            }
                        }
                    }

                    // --- GROUP 3: FUTURE EXTENSIONS (2 connected items) ---
                    item {
                        val group3Items = listOf(
                            AppConfigSubscreen.CUSTOM_DESIGN,
                            AppConfigSubscreen.CUSTOM_TRANSLATORS
                        )

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            group3Items.forEachIndexed { index, route ->
                                val shape = getExpressiveShape(group3Items.size, index, ShapeStyle.Large)

                                when (route) {
                                    AppConfigSubscreen.CUSTOM_DESIGN -> AppConfigOptionCard(
                                        title = stringResource(R.string.custom_design_title),
                                        subtitle = designsSubtitle,
                                        badge = null,
                                        icon = Icons.Default.Brush,
                                        shape = shape,
                                        onClick = { onNavigateSubscreen(AppConfigSubscreen.CUSTOM_DESIGN) }
                                    )
                                    AppConfigSubscreen.CUSTOM_TRANSLATORS -> AppConfigOptionCard(
                                        title = stringResource(R.string.custom_translators_title),
                                        subtitle = translatorsSubtitle,
                                        badge = null,
                                        icon = Icons.Default.Extension,
                                        shape = shape,
                                        onClick = { onNavigateSubscreen(AppConfigSubscreen.CUSTOM_TRANSLATORS) }
                                    )
                                    else -> {}
                                }
                            }
                        }
                    }

                    item {
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        } else {
            // =========================================================================
            // DEDICATED SUBSCREEN
            // =========================================================================
            when (subscreen) {
                AppConfigSubscreen.NOTIFICATION_TYPES -> {
                    SubscreenScaffold(
                        title = stringResource(R.string.active_notifications_title),
                        appName = appName,
                        onBack = { onNavigateSubscreen(null) }
                    ) {
                        AppNotificationTypesContent(
                            activeTypes = activeTypes,
                            activeCallStages = activeCallStages,
                            onToggleType = onToggleType,
                            onToggleCallStage = onToggleCallStage,
                            onNavConfigClick = onNavConfigClick,
                            navEditDesc = navEditDesc
                        )
                    }
                }

                AppConfigSubscreen.ISLAND_BEHAVIOR -> {
                    SubscreenScaffold(
                        title = stringResource(R.string.island_behavior_title),
                        appName = appName,
                        onBack = { onNavigateSubscreen(null) }
                    ) {
                        AppBehaviorContent(
                            appConfig = appIslandConfig,
                            globalConfig = globalConfig,
                            onUpdate = onUpdateIslandConfig,
                            activeDesc = activeDesc,
                            inactiveDesc = inactiveDesc
                        )
                    }
                }

                AppConfigSubscreen.SMART_ACTIONS -> {
                    SubscreenScaffold(
                        title = stringResource(R.string.app_smart_actions_title),
                        appName = appName,
                        onBack = { onNavigateSubscreen(null) }
                    ) {
                        AppSmartActionsContent(
                            global = globalSmartActionsConfig,
                            override = appSmartActionsOverride,
                            packageName = packageName,
                            onExcludedChange = onSmartActionExcludedChange,
                            onTypeOverride = onSmartActionTypeOverride,
                            onClearOverrides = onClearSmartActionsOverride
                        )
                    }
                }

                AppConfigSubscreen.BLOCKED_TERMS -> {
                    SubscreenScaffold(
                        title = stringResource(R.string.blocked_terms),
                        appName = appName,
                        onBack = { onNavigateSubscreen(null) }
                    ) {
                        AppConfigBlockedTermsContent(
                            terms = blockedTerms,
                            onUpdate = onUpdateBlockedTerms
                        )
                    }
                }

                AppConfigSubscreen.ISLAND_WIDGETS -> {
                    SubscreenScaffold(
                        title = stringResource(R.string.app_widgets_section_title),
                        appName = appName,
                        onBack = { onNavigateSubscreen(null) },
                        floatingActionButton = {
                            FloatingActionButton(
                                onClick = onAddWidgetClick,
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = stringResource(R.string.add_island_widget)
                                )
                            }
                        }
                    ) {
                        AppWidgetsSectionCard(
                            savedWidgetIds = savedWidgetIds,
                            availableProviders = availableProviders,
                            onAddWidget = onAddWidgetClick,
                            onEditWidget = onEditWidget,
                            onDeleteWidget = onDeleteWidget
                        )
                    }
                }

                AppConfigSubscreen.CUSTOM_DESIGN -> {
                    SubscreenScaffold(
                        title = stringResource(R.string.custom_design_title),
                        appName = appName,
                        onBack = { onNavigateSubscreen(null) },
                        floatingActionButton = {
                            FloatingActionButton(
                                onClick = onCreateDesign,
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = stringResource(R.string.app_designs_add_design)
                                )
                            }
                        }
                    ) {
                        AppDesignsSectionCard(
                            packageName = packageName,
                            designs = designs,
                            onToggleDesign = onToggleDesign,
                            onEditDesign = onEditTranslator,
                            onCreateDesign = onCreateDesign
                        )
                    }
                }

                AppConfigSubscreen.CUSTOM_TRANSLATORS -> {
                    SubscreenScaffold(
                        title = stringResource(R.string.custom_translators_title),
                        appName = appName,
                        onBack = { onNavigateSubscreen(null) },
                        floatingActionButton = {
                            FloatingActionButton(
                                onClick = onCreateTranslator,
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = stringResource(R.string.app_translators_create_for_app)
                                )
                            }
                        }
                    ) {
                        AppTranslatorsSectionCard(
                            translators = translators,
                            onToggleTranslator = onToggleTranslator,
                            onEditTranslator = onEditTranslator,
                            onCreateTranslator = onCreateTranslator
                        )
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------
// REUSABLE SUBSCREEN CONTAINER
// ------------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscreenScaffold(
    title: String,
    appName: String,
    onBack: () -> Unit,
    actions: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = appName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    FilledTonalIconButton(
                        onClick = onBack,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = { actions() },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = floatingActionButton
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                content()
            }
            item {
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------
// EXPRESSIVE OPTION CARD (Replicating ThemeCreatorScreen style with variable corners)
// ------------------------------------------------------------------------------------------------

@Composable
fun AppConfigOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    shape: Shape,
    badge: String? = null,
    onClick: () -> Unit,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = shape,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 88.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (badge != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            if (trailingContent != null) {
                trailingContent()
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------
// HEADER OVERVIEW CARD
// ------------------------------------------------------------------------------------------------

@Composable
fun AppHeaderCard(
    appName: String,
    packageName: String,
    appIcon: Bitmap?,
    isBridged: Boolean,
    isManagedByTheme: Boolean,
    onToggleBridged: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (appIcon != null) {
                    Image(
                        bitmap = appIcon.asImageBitmap(),
                        contentDescription = appName,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Android,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = appName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        color = if (isBridged) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isBridged) stringResource(R.string.app_status_bridged) else stringResource(R.string.app_status_not_bridged),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isBridged) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Switch(
                    checked = isBridged,
                    onCheckedChange = onToggleBridged,
                    modifier = Modifier.semantics {
                        contentDescription = "Toggle bridging for $appName"
                    }
                )
            }

            if (isManagedByTheme) {
                Spacer(Modifier.height(14.dp))
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.theme_managed_banner),
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------
// NOTIFICATION TYPES CONTENT (Each in a clean separate container card)
// ------------------------------------------------------------------------------------------------

@Composable
fun AppNotificationTypesContent(
    activeTypes: Set<String>,
    activeCallStages: Set<CallStage>,
    onToggleType: (NotificationType, Boolean) -> Unit,
    onToggleCallStage: (CallStage, Boolean) -> Unit,
    onNavConfigClick: () -> Unit,
    navEditDesc: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        NotificationType.configurableEntries.forEach { type ->
            val isChecked = activeTypes.contains(type.name)
            val typeLabel = stringResource(type.labelRes)
            val switchDesc = if (isChecked) stringResource(R.string.cd_disable_type, typeLabel)
            else stringResource(R.string.cd_enable_type, typeLabel)

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleType(type, !isChecked) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = typeLabel,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (type == NotificationType.NAVIGATION) {
                            IconButton(
                                onClick = onNavConfigClick,
                                modifier = Modifier.semantics { contentDescription = navEditDesc }
                            ) {
                                Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        Switch(
                            checked = isChecked,
                            onCheckedChange = { onToggleType(type, it) },
                            modifier = Modifier.semantics { contentDescription = switchDesc }
                        )
                    }

                    if (type == NotificationType.CALL && isChecked) {
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(0.3f))
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.call_stage_settings),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.call_stage_settings_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        CallStage.entries.forEach { stage ->
                            val stageEnabled = stage in activeCallStages
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onToggleCallStage(stage, !stageEnabled) }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(stage.labelRes),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = stringResource(stage.descriptionRes),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Checkbox(
                                    checked = stageEnabled,
                                    onCheckedChange = { onToggleCallStage(stage, it) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------
// ISLAND BEHAVIOR CONTENT
// Shows current config (global or custom), any change auto-switches to custom,
// and has the "Use Global Defaults" toggle at the END in its own container card.
// ------------------------------------------------------------------------------------------------

@Composable
fun AppBehaviorContent(
    appConfig: IslandConfig,
    globalConfig: IslandConfig,
    onUpdate: (IslandConfig) -> Unit,
    activeDesc: String,
    inactiveDesc: String
) {
    val isUsingGlobal = appConfig.isFloat == null

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // "Use Global Defaults" toggle as the FIRST option in its own separate container Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (isUsingGlobal) {
                            // Turn off global: copy current global values into custom
                            onUpdate(globalConfig.copy(isFloat = globalConfig.isFloat ?: false))
                        } else {
                            // Turn on global: reset custom values to null
                            onUpdate(IslandConfig(null, null, null, null, null, null, null))
                        }
                    }
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.use_global_default),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (isUsingGlobal) {
                            stringResource(R.string.appearance_use_defaults_desc)
                        } else {
                            stringResource(R.string.custom_behavior_desc)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = isUsingGlobal,
                    onCheckedChange = { useGlobal ->
                        if (useGlobal) {
                            onUpdate(IslandConfig(null, null, null, null, null, null, null))
                        } else {
                            onUpdate(globalConfig.copy(isFloat = globalConfig.isFloat ?: false))
                        }
                    },
                    modifier = Modifier.semantics {
                        stateDescription = if (isUsingGlobal) activeDesc else inactiveDesc
                    }
                )
            }
        }

        // Elements show current configuration (global values if using global, or custom if customized)
        // If user moves or changes anything, onUpdate is called with isFloat set to non-null,
        // automatically turning the toggle to custom and saving as custom.
        IslandSettingsControl(
            config = appConfig,
            defaultConfig = globalConfig,
            onUpdate = { updatedConfig ->
                val customConfig = updatedConfig.copy(
                    isFloat = updatedConfig.isFloat ?: globalConfig.isFloat ?: false
                )
                onUpdate(customConfig)
            }
        )
    }
}

// ------------------------------------------------------------------------------------------------
// SMART ACTIONS CONTENT (per-app)
// Master on/off (backed by SmartActionsConfig.excludedPackages), per-type overrides layered on
// top of the global config, and a "Use Global Defaults" action to clear all per-app overrides.
// ------------------------------------------------------------------------------------------------

@Composable
fun AppSmartActionsContent(
    global: SmartActionsConfig,
    override: AppSmartActionsOverride,
    packageName: String,
    onExcludedChange: (Boolean) -> Unit,
    onTypeOverride: (SmartActionType, Boolean?) -> Unit,
    onClearOverrides: () -> Unit
) {
    val isExcluded = packageName in global.excludedPackages
    val controlsEnabled = global.enabled && !isExcluded

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!global.enabled) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = stringResource(R.string.app_smart_actions_global_off_banner),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        // Master switch: on/off for this app is tracked by SmartActionsConfig.excludedPackages,
        // not by a field of its own.
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (global.enabled) 1f else 0.55f)
        ) {
            SettingsSwitchItem(
                icon = Icons.Outlined.AutoAwesome,
                title = stringResource(R.string.app_smart_actions_enable),
                subtitle = stringResource(R.string.app_smart_actions_enable_desc),
                checked = !isExcluded,
                onCheckedChange = if (global.enabled) {
                    { checked -> onExcludedChange(!checked) }
                } else {
                    { _ -> }
                }
            )
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.app_smart_actions_types_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.app_smart_actions_types_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (controlsEnabled) 1f else 0.55f)
        ) {
            Column(Modifier.padding(vertical = 4.dp)) {
                SettingsSwitchItem(
                    icon = Icons.Outlined.Password,
                    title = stringResource(R.string.setting_smart_actions_otp),
                    subtitle = stringResource(R.string.setting_smart_actions_otp_desc),
                    checked = override.otp ?: global.otp,
                    onCheckedChange = if (controlsEnabled) {
                        { value -> onTypeOverride(SmartActionType.OTP, value) }
                    } else {
                        { _ -> }
                    }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
                SettingsSwitchItem(
                    icon = Icons.Outlined.Link,
                    title = stringResource(R.string.setting_smart_actions_url),
                    subtitle = stringResource(R.string.setting_smart_actions_url_desc),
                    checked = override.url ?: global.url,
                    onCheckedChange = if (controlsEnabled) {
                        { value -> onTypeOverride(SmartActionType.URL, value) }
                    } else {
                        { _ -> }
                    }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
                SettingsSwitchItem(
                    icon = Icons.Outlined.Call,
                    title = stringResource(R.string.setting_smart_actions_phone),
                    subtitle = stringResource(R.string.setting_smart_actions_phone_desc),
                    checked = override.phone ?: global.phone,
                    onCheckedChange = if (controlsEnabled) {
                        { value -> onTypeOverride(SmartActionType.PHONE, value) }
                    } else {
                        { _ -> }
                    }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
                SettingsSwitchItem(
                    icon = Icons.Outlined.LocalShipping,
                    title = stringResource(R.string.setting_smart_actions_tracking),
                    subtitle = stringResource(R.string.setting_smart_actions_tracking_desc),
                    checked = override.tracking ?: global.tracking,
                    onCheckedChange = if (controlsEnabled) {
                        { value -> onTypeOverride(SmartActionType.TRACKING, value) }
                    } else {
                        { _ -> }
                    }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
                SettingsSwitchItem(
                    icon = Icons.Outlined.Directions,
                    title = stringResource(R.string.setting_smart_actions_navigation),
                    subtitle = stringResource(R.string.setting_smart_actions_navigation_desc),
                    checked = override.navigation ?: global.navigation,
                    onCheckedChange = if (controlsEnabled) {
                        { value -> onTypeOverride(SmartActionType.NAVIGATION, value) }
                    } else {
                        { _ -> }
                    }
                )
            }
        }

        // "Use Global Defaults" action, mirroring AppBehaviorContent's global-default card:
        // a dedicated container with the switch state reflecting whether any per-app override
        // is currently set, clearing all of them in one tap.
        val isUsingGlobalDefaults = override.isEmpty
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (controlsEnabled) 1f else 0.55f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = controlsEnabled && !isUsingGlobalDefaults) { onClearOverrides() }
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.use_global_default),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (isUsingGlobalDefaults) {
                            stringResource(R.string.appearance_use_defaults_desc)
                        } else {
                            stringResource(R.string.custom_behavior_desc)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = isUsingGlobalDefaults,
                    enabled = controlsEnabled && !isUsingGlobalDefaults,
                    onCheckedChange = { onClearOverrides() }
                )
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------
// BLOCKED TERMS CONTENT
// Input box container on top, and vertical list of blocked terms below in separate containers.
// ------------------------------------------------------------------------------------------------

@Composable
fun AppConfigBlockedTermsContent(
    terms: Set<String>,
    onUpdate: (Set<String>) -> Unit
) {
    var text by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Container 1: Input Box Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(R.string.blocked_terms),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.blocked_terms_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.add_blocked_word)) },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (text.isNotBlank()) {
                                    onUpdate(terms + text.trim())
                                    text = ""
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = stringResource(R.string.add),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (text.isNotBlank()) {
                            onUpdate(terms + text.trim())
                            text = ""
                            keyboardController?.hide()
                        }
                    }),
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }

        // Container 2: Vertical list of blocked terms below the box
        val termsList = terms.toList()
        if (termsList.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp, horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.no_blocked_terms),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.no_blocked_terms_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                termsList.forEachIndexed { index, term ->
                    val shape = getExpressiveShape(termsList.size, index, ShapeStyle.Medium)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        shape = shape,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.errorContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Block,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(Modifier.width(14.dp))

                            Text(
                                text = term,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )

                            IconButton(
                                onClick = { onUpdate(terms - term) },
                                colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = stringResource(R.string.remove)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------
// ISLAND WIDGETS SECTION
// ------------------------------------------------------------------------------------------------

@Composable
fun AppWidgetsSectionCard(
    savedWidgetIds: List<Int>,
    availableProviders: List<AppWidgetProviderInfo>,
    onAddWidget: () -> Unit,
    onEditWidget: (Int) -> Unit,
    onDeleteWidget: (Int) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.app_widgets_section_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(14.dp))

            if (savedWidgetIds.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.Widgets,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.no_widgets_for_app),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    savedWidgetIds.forEach { widgetId ->
                        AppConfigWidgetChildItem(
                            widgetId = widgetId,
                            onEdit = { onEditWidget(widgetId) },
                            onDelete = { onDeleteWidget(widgetId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppConfigWidgetChildItem(
    widgetId: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val preferences = remember { AppPreferences(context.applicationContext) }
    val config by preferences.getWidgetConfigFlow(widgetId).collectAsState(initial = WidgetConfig())
    val providerInfo = remember(widgetId) { WidgetManager.getWidgetInfo(context, widgetId) }

    val viewHeightDp = when (config.size) {
        WidgetSize.ORIGINAL -> 160
        WidgetSize.SMALL -> 100
        WidgetSize.MEDIUM -> 160
        WidgetSize.LARGE -> 240
        WidgetSize.XLARGE -> 320
    }
    val previewContainerHeight = (viewHeightDp + 24).dp

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val label = providerInfo?.loadLabel(context.packageManager)
                    Text(
                        text = if (!label.isNullOrEmpty()) label else "Widget #$widgetId",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = config.size.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = config.renderMode.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.configure),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = {
                        val testIntent = Intent(context, com.d4viddf.hyperbridge.service.WidgetOverlayService::class.java).apply {
                            action = "ACTION_TEST_WIDGET"
                            putExtra("WIDGET_ID", widgetId)
                        }
                        context.startService(testIntent)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Test Widget",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = onDelete,
                    colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete Widget"
                    )
                }
            }

            // Live widget preview container with size adapted to widget configuration
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(previewContainerHeight)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { ctx ->
                        FrameLayout(ctx).apply {
                            val hostView = WidgetManager.createPreview(ctx, widgetId)
                            if (hostView != null) {
                                val info = WidgetManager.getWidgetInfo(ctx, widgetId)
                                hostView.setAppWidget(widgetId, info)
                                addView(hostView)

                                val density = ctx.resources.displayMetrics.density
                                val w = (300 * density).toInt()
                                val h = (viewHeightDp * density).toInt()

                                hostView.measure(
                                    View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                                    View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.AT_MOST)
                                )
                                hostView.layout(0, 0, hostView.measuredWidth, hostView.measuredHeight)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                )
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------
// CUSTOM DESIGNS SECTION (App-specific)
// ------------------------------------------------------------------------------------------------

private enum class AppDesignFilterScope {
    ALL,
    ACTIVE,
    INACTIVE,
    GLOBAL,
    APP_SPECIFIC
}

private data class AppDesignFilterState(
    val statusScope: AppDesignFilterScope = AppDesignFilterScope.ALL,
    val selectedNotificationTypes: Set<String> = emptySet(),
    val selectedTemplates: Set<String> = emptySet(),
    val selectedIcons: Set<String> = emptySet()
) {
    val isCustomFilterActive: Boolean
        get() = statusScope != AppDesignFilterScope.ALL ||
                selectedNotificationTypes.isNotEmpty() ||
                selectedTemplates.isNotEmpty() ||
                selectedIcons.isNotEmpty()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AppDesignFilterSheet(
    filterState: AppDesignFilterState,
    availableDesigns: List<com.d4viddf.hyperbridge.models.translator.CustomTranslator>,
    packageName: String,
    onDismiss: () -> Unit,
    onApply: (AppDesignFilterState) -> Unit,
    onReset: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var draftState by remember { mutableStateOf(filterState) }

    // Designs filtered by the currently selected Status & Scope
    val scopeFilteredDesigns = remember(availableDesigns, draftState.statusScope, packageName) {
        when (draftState.statusScope) {
            AppDesignFilterScope.ALL -> availableDesigns
            AppDesignFilterScope.ACTIVE -> availableDesigns.filter { design ->
                design.isEnabled && !design.excludedPackages.any { it.equals(packageName, ignoreCase = true) }
            }
            AppDesignFilterScope.INACTIVE -> availableDesigns.filter { design ->
                !design.isEnabled || design.excludedPackages.any { it.equals(packageName, ignoreCase = true) }
            }
            AppDesignFilterScope.GLOBAL -> availableDesigns.filter { design ->
                design.targetScope == com.d4viddf.hyperbridge.models.translator.TargetScope.GLOBAL ||
                        design.targetScope == com.d4viddf.hyperbridge.models.translator.TargetScope.NOTIFICATION_TYPE
            }
            AppDesignFilterScope.APP_SPECIFIC -> availableDesigns.filter { design ->
                (design.targetScope == com.d4viddf.hyperbridge.models.translator.TargetScope.SPECIFIC_APPS ||
                        design.targetScope == com.d4viddf.hyperbridge.models.translator.TargetScope.SYSTEM_APPS) &&
                        design.targetPackages.any { it.equals(packageName, ignoreCase = true) }
            }
        }
    }

    // Dynamic aggregations from scope-filtered designs
    val availableNotificationTypes = remember(scopeFilteredDesigns) {
        scopeFilteredDesigns.flatMap { it.targetNotificationTypes }.distinct().sorted()
    }
    val availableTemplates = remember(scopeFilteredDesigns) {
        scopeFilteredDesigns.mapNotNull { it.presentation.templateId }.filter { it.isNotBlank() }.distinct().sorted()
    }
    val availableIcons = remember(scopeFilteredDesigns) {
        scopeFilteredDesigns.mapNotNull { it.meta.iconName }.filter { it.isNotBlank() }.distinct().sorted()
    }

    val sanitizedSelectedTypes = draftState.selectedNotificationTypes.filter { it in availableNotificationTypes }.toSet()
    val sanitizedSelectedTemplates = draftState.selectedTemplates.filter { it in availableTemplates }.toSet()
    val sanitizedSelectedIcons = draftState.selectedIcons.filter { it in availableIcons }.toSet()

    val currentDraftState = draftState.copy(
        selectedNotificationTypes = sanitizedSelectedTypes,
        selectedTemplates = sanitizedSelectedTemplates,
        selectedIcons = sanitizedSelectedIcons
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header (fixed)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 8.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.app_designs_filter_sheet_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.app_designs_filter_sheet_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider()

            // Scrollable filter options
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 1. Status & Scope Section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.translators_filter_status_section),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val scopeOptions = listOf(
                            AppDesignFilterScope.ALL to R.string.app_designs_filter_all,
                            AppDesignFilterScope.ACTIVE to R.string.app_designs_filter_active,
                            AppDesignFilterScope.INACTIVE to R.string.app_designs_filter_inactive,
                            AppDesignFilterScope.GLOBAL to R.string.app_designs_filter_global,
                            AppDesignFilterScope.APP_SPECIFIC to R.string.app_designs_filter_app_specific
                        )
                        scopeOptions.forEach { (scope, labelRes) ->
                            val isSelected = currentDraftState.statusScope == scope
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    draftState = currentDraftState.copy(statusScope = scope)
                                },
                                label = { Text(stringResource(labelRes)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }

                // 2. Notification Types Section
                if (availableNotificationTypes.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.translators_filter_types_section),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            availableNotificationTypes.forEach { type ->
                                val isSelected = type in currentDraftState.selectedNotificationTypes
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        val newTypes = if (isSelected) {
                                            currentDraftState.selectedNotificationTypes - type
                                        } else {
                                            currentDraftState.selectedNotificationTypes + type
                                        }
                                        draftState = currentDraftState.copy(selectedNotificationTypes = newTypes)
                                    },
                                    label = { Text(type) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                    }
                }

                // 3. Templates Section
                if (availableTemplates.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.app_designs_filter_templates_section),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            availableTemplates.forEach { templateId ->
                                val isSelected = templateId in currentDraftState.selectedTemplates
                                val templateObj = com.d4viddf.hyperbridge.models.translator.IslandTemplateCatalog.find(templateId)
                                val labelText = if (templateObj != null) stringResource(templateObj.nameRes) else templateId

                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        val newTemplates = if (isSelected) {
                                            currentDraftState.selectedTemplates - templateId
                                        } else {
                                            currentDraftState.selectedTemplates + templateId
                                        }
                                        draftState = currentDraftState.copy(selectedTemplates = newTemplates)
                                    },
                                    label = { Text(labelText) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                    }
                }

                // 4. Icons Section
                if (availableIcons.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.translators_filter_icons_section),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            availableIcons.forEach { iconName ->
                                val isSelected = iconName in currentDraftState.selectedIcons
                                val iconOption = TRANSLATOR_OUTLINED_ICONS.firstOrNull { it.id.equals(iconName, ignoreCase = true) }
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        val newIcons = if (isSelected) {
                                            currentDraftState.selectedIcons - iconName
                                        } else {
                                            currentDraftState.selectedIcons + iconName
                                        }
                                        draftState = currentDraftState.copy(selectedIcons = newIcons)
                                    },
                                    leadingIcon = iconOption?.let { opt ->
                                        {
                                            Icon(
                                                imageVector = opt.icon,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    },
                                    label = { Text(iconOption?.label ?: iconName) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Fixed bottom actions bar with divider
            HorizontalDivider()

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            draftState = AppDesignFilterState()
                            onReset()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.RestartAlt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.translators_filter_reset))
                    }

                    Button(
                        onClick = { onApply(currentDraftState) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.translators_filter_apply))
                    }
                }
            }
        }
    }
}

@Composable
fun AppDesignsSectionCard(
    packageName: String,
    designs: List<com.d4viddf.hyperbridge.models.translator.CustomTranslator>,
    onToggleDesign: (String, Boolean) -> Unit,
    onEditDesign: (String) -> Unit,
    onCreateDesign: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterState by remember { mutableStateOf(AppDesignFilterState()) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showPreviewView by remember { mutableStateOf(true) }

    val filteredDesigns = remember(designs, searchQuery, filterState, packageName) {
        designs.filter { design ->
            val isAppEnabled = design.isEnabled && !design.excludedPackages.any { it.equals(packageName, ignoreCase = true) }
            val isAppSpecific = (design.targetScope == com.d4viddf.hyperbridge.models.translator.TargetScope.SPECIFIC_APPS ||
                    design.targetScope == com.d4viddf.hyperbridge.models.translator.TargetScope.SYSTEM_APPS) &&
                    design.targetPackages.any { it.equals(packageName, ignoreCase = true) }
            val isGlobal = design.targetScope == com.d4viddf.hyperbridge.models.translator.TargetScope.GLOBAL ||
                    design.targetScope == com.d4viddf.hyperbridge.models.translator.TargetScope.NOTIFICATION_TYPE

            val matchesScope = when (filterState.statusScope) {
                AppDesignFilterScope.ALL -> true
                AppDesignFilterScope.ACTIVE -> isAppEnabled
                AppDesignFilterScope.INACTIVE -> !isAppEnabled
                AppDesignFilterScope.GLOBAL -> isGlobal
                AppDesignFilterScope.APP_SPECIFIC -> isAppSpecific
            }

            val matchesTypes = filterState.selectedNotificationTypes.isEmpty() ||
                    design.targetNotificationTypes.any { it in filterState.selectedNotificationTypes }

            val matchesTemplates = filterState.selectedTemplates.isEmpty() ||
                    (design.presentation.templateId != null && design.presentation.templateId in filterState.selectedTemplates)

            val matchesIcons = filterState.selectedIcons.isEmpty() ||
                    design.meta.iconName in filterState.selectedIcons

            val matchesSearch = searchQuery.isBlank() ||
                    design.meta.name.contains(searchQuery, ignoreCase = true) ||
                    design.meta.description.contains(searchQuery, ignoreCase = true) ||
                    design.presentation.templateId?.contains(searchQuery, ignoreCase = true) == true ||
                    design.targetNotificationTypes.any { it.contains(searchQuery, ignoreCase = true) }

            matchesScope && matchesTypes && matchesTemplates && matchesIcons && matchesSearch
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. HEADER INFO CARD ---
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.app_designs_section_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // --- 2. SEARCH & FILTER CONTROLS ---
        if (designs.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Search Field & Preview Toggle Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.app_designs_search_hint),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingIcon = {
                                AnimatedVisibility(visible = searchQuery.isNotBlank()) {
                                    FilledTonalIconButton(
                                        onClick = { searchQuery = "" },
                                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = stringResource(R.string.clear)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    FilledTonalIconButton(
                        onClick = { showPreviewView = !showPreviewView },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = if (showPreviewView) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    ) {
                        Icon(
                            imageVector = if (showPreviewView) Icons.Outlined.Preview else Icons.Outlined.Widgets,
                            contentDescription = stringResource(R.string.design_toggle_preview_cd),
                            tint = if (showPreviewView) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Filter Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isAllActive = !filterState.isCustomFilterActive

                    FilterChip(
                        selected = isAllActive,
                        onClick = { filterState = AppDesignFilterState() },
                        label = { Text(stringResource(R.string.app_designs_filter_all)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )

                    val activeCount = (if (filterState.statusScope != AppDesignFilterScope.ALL) 1 else 0) +
                            filterState.selectedNotificationTypes.size +
                            filterState.selectedTemplates.size +
                            filterState.selectedIcons.size

                    FilterChip(
                        selected = filterState.isCustomFilterActive,
                        onClick = { showFilterSheet = true },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.FilterList,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = if (activeCount > 0) {
                            {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "$activeCount",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        } else null,
                        label = { Text(stringResource(R.string.translators_filter_btn)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )

                    FilterChip(
                        selected = filterState.statusScope == AppDesignFilterScope.ACTIVE,
                        onClick = {
                            filterState = if (filterState.statusScope == AppDesignFilterScope.ACTIVE) {
                                filterState.copy(statusScope = AppDesignFilterScope.ALL)
                            } else {
                                filterState.copy(statusScope = AppDesignFilterScope.ACTIVE)
                            }
                        },
                        label = { Text(stringResource(R.string.app_designs_filter_active)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                    FilterChip(
                        selected = filterState.statusScope == AppDesignFilterScope.INACTIVE,
                        onClick = {
                            filterState = if (filterState.statusScope == AppDesignFilterScope.INACTIVE) {
                                filterState.copy(statusScope = AppDesignFilterScope.ALL)
                            } else {
                                filterState.copy(statusScope = AppDesignFilterScope.INACTIVE)
                            }
                        },
                        label = { Text(stringResource(R.string.app_designs_filter_inactive)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                    FilterChip(
                        selected = filterState.statusScope == AppDesignFilterScope.GLOBAL,
                        onClick = {
                            filterState = if (filterState.statusScope == AppDesignFilterScope.GLOBAL) {
                                filterState.copy(statusScope = AppDesignFilterScope.ALL)
                            } else {
                                filterState.copy(statusScope = AppDesignFilterScope.GLOBAL)
                            }
                        },
                        label = { Text(stringResource(R.string.app_designs_filter_global)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                    FilterChip(
                        selected = filterState.statusScope == AppDesignFilterScope.APP_SPECIFIC,
                        onClick = {
                            filterState = if (filterState.statusScope == AppDesignFilterScope.APP_SPECIFIC) {
                                filterState.copy(statusScope = AppDesignFilterScope.ALL)
                            } else {
                                filterState.copy(statusScope = AppDesignFilterScope.APP_SPECIFIC)
                            }
                        },
                        label = { Text(stringResource(R.string.app_designs_filter_app_specific)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }

        if (showFilterSheet) {
            AppDesignFilterSheet(
                filterState = filterState,
                availableDesigns = designs,
                packageName = packageName,
                onDismiss = { showFilterSheet = false },
                onApply = { newState ->
                    filterState = newState
                    showFilterSheet = false
                },
                onReset = {
                    filterState = AppDesignFilterState()
                    showFilterSheet = false
                }
            )
        }

        // --- 3. SEPARATE DESIGN ITEMS / EMPTY STATES ---
        if (designs.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.DashboardCustomize,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.app_designs_empty_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.app_designs_empty_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(0.85f)
                        )


                    }
                }
            }
        } else if (filteredDesigns.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.app_designs_no_results),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                filteredDesigns.forEach { design ->
                    val isAppEnabled = design.isEnabled && !design.excludedPackages.any { it.equals(packageName, ignoreCase = true) }

                    DesignPreviewCardItem(
                        design = design,
                        shape = RoundedCornerShape(20.dp),
                        isChecked = isAppEnabled,
                        showPreview = showPreviewView,
                        onToggle = { isChecked ->
                            onToggleDesign(design.id, isChecked)
                        },
                        onClick = {
                            onEditDesign(design.id)
                        }
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------
// FUTURE FEATURE PLACEHOLDER (Custom Translators)
// ------------------------------------------------------------------------------------------------

@Composable
fun FutureFeaturePlaceholderCard(
    title: String,
    badge: String,
    icon: ImageVector,
    description: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(14.dp))

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Track progress on GitHub: Issues #271, #272, #273.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------
// CUSTOM TRANSLATORS SECTION (App-specific)
// ------------------------------------------------------------------------------------------------

@Composable
fun AppTranslatorsSectionCard(
    translators: List<com.d4viddf.hyperbridge.models.translator.CustomTranslator>,
    onToggleTranslator: (String, Boolean) -> Unit,
    onEditTranslator: (String) -> Unit,
    onCreateTranslator: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.app_translators_section_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(14.dp))

            if (translators.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Extension,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.app_translators_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = onCreateTranslator,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.app_translators_create_for_app))
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    translators.forEach { translator ->
                        AppConfigTranslatorChildItem(
                            translator = translator,
                            onToggle = { isChecked -> onToggleTranslator(translator.id, isChecked) },
                            onEdit = { onEditTranslator(translator.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppConfigTranslatorChildItem(
    translator: com.d4viddf.hyperbridge.models.translator.CustomTranslator,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit
) {
    Surface(
        onClick = onEdit,
        shape = RoundedCornerShape(16.dp),
        color = if (translator.isEnabled) MaterialTheme.colorScheme.surfaceContainerHigh
        else MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Outlined Icon Badge
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = if (translator.isEnabled) {
                        when (translator.targetScope) {
                            com.d4viddf.hyperbridge.models.translator.TargetScope.GLOBAL -> MaterialTheme.colorScheme.primaryContainer
                            com.d4viddf.hyperbridge.models.translator.TargetScope.SPECIFIC_APPS -> MaterialTheme.colorScheme.secondaryContainer
                            com.d4viddf.hyperbridge.models.translator.TargetScope.SYSTEM_APPS -> MaterialTheme.colorScheme.errorContainer
                            com.d4viddf.hyperbridge.models.translator.TargetScope.NOTIFICATION_TYPE -> MaterialTheme.colorScheme.tertiaryContainer
                        }
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = com.d4viddf.hyperbridge.ui.screens.translators.getTranslatorOutlinedIcon(translator.meta.iconName),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = if (translator.isEnabled) {
                                when (translator.targetScope) {
                                    com.d4viddf.hyperbridge.models.translator.TargetScope.GLOBAL -> MaterialTheme.colorScheme.onPrimaryContainer
                                    com.d4viddf.hyperbridge.models.translator.TargetScope.SPECIFIC_APPS -> MaterialTheme.colorScheme.onSecondaryContainer
                                    com.d4viddf.hyperbridge.models.translator.TargetScope.SYSTEM_APPS -> MaterialTheme.colorScheme.onErrorContainer
                                    com.d4viddf.hyperbridge.models.translator.TargetScope.NOTIFICATION_TYPE -> MaterialTheme.colorScheme.onTertiaryContainer
                                }
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            }
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = translator.meta.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (translator.meta.description.isNotEmpty()) {
                        Text(
                            text = translator.meta.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.translators_priority_label, translator.priority),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.translators_action_edit),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = translator.isEnabled,
                    onCheckedChange = onToggle
                )
            }
        }
    }
}

private fun drawableToBitmap(drawable: Drawable): Bitmap {
    if (drawable is BitmapDrawable) return drawable.bitmap
    val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 1
    val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 1
    val bitmap = createBitmap(width, height)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}

// ------------------------------------------------------------------------------------------------
// PREVIEWS
// ------------------------------------------------------------------------------------------------

@Preview(name = "Overview", showBackground = true)
@Composable
fun AppConfigOverviewPreview() {
    HyperBridgeTheme {
        SampleAppConfigContent(currentSubscreen = null)
    }
}

@Preview(name = "Subscreen: Notification Types", showBackground = true)
@Composable
fun AppConfigNotificationTypesPreview() {
    HyperBridgeTheme {
        SampleAppConfigContent(currentSubscreen = AppConfigSubscreen.NOTIFICATION_TYPES)
    }
}

@Preview(name = "Subscreen: Island Behavior", showBackground = true)
@Composable
fun AppConfigIslandBehaviorPreview() {
    HyperBridgeTheme {
        SampleAppConfigContent(currentSubscreen = AppConfigSubscreen.ISLAND_BEHAVIOR)
    }
}

@Preview(name = "Subscreen: Smart Actions", showBackground = true)
@Composable
fun AppConfigSmartActionsPreview() {
    HyperBridgeTheme {
        SampleAppConfigContent(currentSubscreen = AppConfigSubscreen.SMART_ACTIONS)
    }
}

@Preview(name = "Subscreen: Blocked Terms", showBackground = true)
@Composable
fun AppConfigBlockedTermsPreview() {
    HyperBridgeTheme {
        SampleAppConfigContent(currentSubscreen = AppConfigSubscreen.BLOCKED_TERMS)
    }
}

@Preview(name = "Subscreen: Island Widgets", showBackground = true)
@Composable
fun AppConfigIslandWidgetsPreview() {
    HyperBridgeTheme {
        SampleAppConfigContent(currentSubscreen = AppConfigSubscreen.ISLAND_WIDGETS)
    }
}

@Preview(name = "Subscreen: Custom Design", showBackground = true)
@Composable
fun AppConfigCustomDesignPreview() {
    HyperBridgeTheme {
        SampleAppConfigContent(currentSubscreen = AppConfigSubscreen.CUSTOM_DESIGN)
    }
}

@Preview(name = "Subscreen: Custom Translators", showBackground = true)
@Composable
fun AppConfigCustomTranslatorsPreview() {
    HyperBridgeTheme {
        SampleAppConfigContent(currentSubscreen = AppConfigSubscreen.CUSTOM_TRANSLATORS)
    }
}

@Composable
private fun SampleAppConfigContent(currentSubscreen: AppConfigSubscreen?) {
    AppConfigContent(
        appName = "Spotify",
        packageName = "com.spotify.music",
        appIcon = null,
        isBridged = true,
        isManagedByTheme = false,
        activeTypes = setOf(NotificationType.MEDIA.name, NotificationType.MESSAGE.name),
        activeCallStages = CallStage.entries.toSet(),
        appIslandConfig = IslandConfig(isFloat = true, isShowShade = true, timeout = 5),
        globalConfig = IslandConfig(isFloat = true, isShowShade = true, timeout = 5),
        blockedTerms = setOf("Ad", "Promo"),
        globalSmartActionsConfig = SmartActionsConfig(enabled = true),
        savedWidgetIds = emptyList(),
        availableProviders = emptyList(),
        currentSubscreen = currentSubscreen,
        onNavigateSubscreen = {},
        onBack = {},
        onToggleBridged = {},
        onToggleType = { _, _ -> },
        onToggleCallStage = { _, _ -> },
        onUpdateIslandConfig = {},
        onUpdateBlockedTerms = {},
        onSmartActionExcludedChange = {},
        onSmartActionTypeOverride = { _, _ -> },
        onClearSmartActionsOverride = {},
        onNavConfigClick = {},
        onAddWidgetClick = {},
        onEditWidget = {},
        onDeleteWidget = {}
    )
}
