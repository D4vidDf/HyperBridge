package com.d4viddf.hyperbridge.ui.screens.settings

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.createBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.data.widget.WidgetManager
import com.d4viddf.hyperbridge.models.CallStage
import com.d4viddf.hyperbridge.models.IslandConfig
import com.d4viddf.hyperbridge.models.NotificationType
import com.d4viddf.hyperbridge.models.WidgetConfig
import com.d4viddf.hyperbridge.models.WidgetSize
import com.d4viddf.hyperbridge.ui.AppInfo
import com.d4viddf.hyperbridge.ui.AppListViewModel
import com.d4viddf.hyperbridge.ui.components.BlocklistEditor
import com.d4viddf.hyperbridge.ui.components.IslandSettingsControl
import com.d4viddf.hyperbridge.ui.theme.HyperBridgeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppConfigScreen(
    packageName: String,
    viewModel: AppListViewModel = viewModel(),
    onBack: () -> Unit,
    onNavConfigClick: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = remember { AppPreferences(context.applicationContext) }

    val effectiveConfig by viewModel.getEffectiveAppConfigFlow(packageName).collectAsState(initial = null)
    val activeTypes = effectiveConfig?.activeTypes ?: emptySet()
    val activeCallStages = effectiveConfig?.activeCallStages ?: CallStage.entries.toSet()
    val isManagedByTheme = effectiveConfig?.isManagedByTheme == true

    val appIslandConfig by viewModel.getAppIslandConfig(packageName).collectAsState(initial = IslandConfig())
    val globalConfig by viewModel.globalConfigFlow.collectAsState(
        initial = IslandConfig(isFloat = true, isShowShade = true, timeout = 5)
    )

    val blockedTerms by viewModel.getAppBlockedTerms(packageName).collectAsState(initial = emptySet())

    val allowedPackages by preferences.allowedPackagesFlow.collectAsState(initial = emptySet())
    val isBridged = allowedPackages.contains(packageName)

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
    var showAddWidgetSheet by remember { mutableStateOf(false) }
    var pendingWidgetId by remember { mutableIntStateOf(-1) }

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

    val bindLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && pendingWidgetId != -1) {
            val boundId = pendingWidgetId
            scope.launch {
                preferences.saveWidgetConfig(boundId, WidgetConfig())
                refreshWidgetTrigger.intValue++
            }
            pendingWidgetId = -1
        } else if (pendingWidgetId != -1) {
            WidgetManager.deleteId(context, pendingWidgetId)
            pendingWidgetId = -1
        }
    }

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
        savedWidgetIds = appSavedWidgetIds,
        availableProviders = availableProviders,
        onBack = onBack,
        onToggleBridged = { enabled -> viewModel.toggleApp(packageName, enabled) },
        onToggleType = { type, enabled -> viewModel.updateAppConfig(packageName, type, enabled) },
        onToggleCallStage = { stage, enabled -> viewModel.updateAppCallStage(packageName, stage, enabled) },
        onUpdateIslandConfig = { config -> viewModel.updateAppIslandConfig(packageName, config) },
        onUpdateBlockedTerms = { terms -> viewModel.updateAppBlockedTerms(packageName, terms) },
        onNavConfigClick = { onNavConfigClick(packageName) },
        onAddWidgetClick = { showAddWidgetSheet = true },
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
        }
    )

    if (showAddWidgetSheet) {
        AddAppWidgetSheet(
            packageName = packageName,
            appName = appInfo?.name ?: packageName,
            availableProviders = availableProviders,
            onDismiss = { showAddWidgetSheet = false },
            onSelectProvider = { provider ->
                showAddWidgetSheet = false
                val widgetId = WidgetManager.allocateId(context)
                val allowed = WidgetManager.bindWidget(context, widgetId, provider.provider)
                if (allowed) {
                    scope.launch {
                        preferences.saveWidgetConfig(widgetId, WidgetConfig())
                        refreshWidgetTrigger.intValue++
                    }
                } else {
                    pendingWidgetId = widgetId
                    val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider.provider)
                    }
                    bindLauncher.launch(intent)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
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
    savedWidgetIds: List<Int>,
    availableProviders: List<AppWidgetProviderInfo>,
    onBack: () -> Unit,
    onToggleBridged: (Boolean) -> Unit,
    onToggleType: (NotificationType, Boolean) -> Unit,
    onToggleCallStage: (CallStage, Boolean) -> Unit,
    onUpdateIslandConfig: (IslandConfig) -> Unit,
    onUpdateBlockedTerms: (Set<String>) -> Unit,
    onNavConfigClick: () -> Unit,
    onAddWidgetClick: () -> Unit,
    onDeleteWidget: (Int) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val navEditDesc = stringResource(R.string.cd_nav_edit)
    val activeDesc = stringResource(R.string.cd_app_state_active)
    val inactiveDesc = stringResource(R.string.cd_app_state_inactive)

    val activeCount = NotificationType.configurableEntries.count { activeTypes.contains(it.name) }
    val activeSubtitle = stringResource(R.string.active_notifications_subtitle, activeCount)
    val blockedCount = blockedTerms.size
    val blockedBadge = if (blockedCount > 0) stringResource(R.string.blocked_terms_active_count, blockedCount) else null

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = appName,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = stringResource(R.string.app_config_subtitle),
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
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- HEADER OVERVIEW CARD ---
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

            // --- SECTION 1: NOTIFICATION TYPES ---
            item {
                AppConfigExpandableCard(
                    title = stringResource(R.string.active_notifications_title),
                    icon = Icons.Default.Notifications,
                    subtitle = activeSubtitle,
                    initiallyExpanded = true
                ) {
                    AppNotificationTypesContent(
                        packageName = packageName,
                        activeTypes = activeTypes,
                        activeCallStages = activeCallStages,
                        onToggleType = onToggleType,
                        onToggleCallStage = onToggleCallStage,
                        onNavConfigClick = onNavConfigClick,
                        navEditDesc = navEditDesc
                    )
                }
            }

            // --- SECTION 2: ISLAND APPEARANCE ---
            item {
                AppConfigExpandableCard(
                    title = stringResource(R.string.island_appearance),
                    icon = Icons.Default.Palette
                ) {
                    AppAppearanceContent(
                        appConfig = appIslandConfig,
                        globalConfig = globalConfig,
                        onUpdate = onUpdateIslandConfig,
                        activeDesc = activeDesc,
                        inactiveDesc = inactiveDesc
                    )
                }
            }

            // --- SECTION 3: BLOCKED TERMS ---
            item {
                AppConfigExpandableCard(
                    title = stringResource(R.string.blocked_terms),
                    icon = Icons.Default.Block,
                    subtitle = blockedBadge
                ) {
                    BlocklistEditor(
                        terms = blockedTerms,
                        onUpdate = onUpdateBlockedTerms
                    )
                }
            }

            // --- SECTION 4: APP WIDGETS ---
            item {
                AppWidgetsSectionCard(
                    savedWidgetIds = savedWidgetIds,
                    availableProviders = availableProviders,
                    onAddWidget = onAddWidgetClick,
                    onDeleteWidget = onDeleteWidget
                )
            }

            // --- SECTION 5: CUSTOM DESIGN (Placeholder) ---
            item {
                FutureFeaturePlaceholderCard(
                    title = stringResource(R.string.custom_design_title),
                    badge = stringResource(R.string.custom_design_badge),
                    description = stringResource(R.string.custom_design_desc),
                    icon = Icons.Default.Brush
                )
            }

            // --- SECTION 6: CUSTOM TRANSLATORS (Placeholder) ---
            item {
                FutureFeaturePlaceholderCard(
                    title = stringResource(R.string.custom_translators_title),
                    badge = stringResource(R.string.custom_translators_badge),
                    description = stringResource(R.string.custom_translators_desc),
                    icon = Icons.Default.Extension
                )
            }

            // Extra spacer at bottom
            item {
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------
// SUBCOMPONENTS
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
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (appIcon != null) {
                    Image(
                        bitmap = appIcon.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(56.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Android,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

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

                    Spacer(Modifier.height(6.dp))

                    Surface(
                        color = if (isBridged) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isBridged) stringResource(R.string.app_status_bridged) else stringResource(R.string.app_status_not_bridged),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isBridged) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Switch(
                    checked = isBridged,
                    onCheckedChange = onToggleBridged
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
                            Icons.Outlined.Info,
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

@Composable
fun AppConfigExpandableCard(
    title: String,
    icon: ImageVector,
    subtitle: String? = null,
    initiallyExpanded: Boolean = false,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "rotation")

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.rotate(rotation)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    HorizontalDivider(
                        modifier = Modifier.padding(bottom = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(0.3f)
                    )
                    content()
                }
            }
        }
    }
}

@Composable
fun AppNotificationTypesContent(
    packageName: String,
    activeTypes: Set<String>,
    activeCallStages: Set<CallStage>,
    onToggleType: (NotificationType, Boolean) -> Unit,
    onToggleCallStage: (CallStage, Boolean) -> Unit,
    onNavConfigClick: () -> Unit,
    navEditDesc: String
) {
    Column {
        NotificationType.configurableEntries.forEach { type ->
            val isChecked = activeTypes.contains(type.name)
            val typeLabel = stringResource(type.labelRes)
            val switchDesc = if (isChecked) stringResource(R.string.cd_disable_type, typeLabel)
            else stringResource(R.string.cd_enable_type, typeLabel)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleType(type, !isChecked) }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = typeLabel,
                        style = MaterialTheme.typography.bodyLarge,
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, bottom = 8.dp)
                ) {
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
                    CallStage.entries.forEach { stage ->
                        val stageEnabled = stage in activeCallStages
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleCallStage(stage, !stageEnabled) }
                                .padding(vertical = 8.dp),
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
                            Spacer(Modifier.width(8.dp))
                            Switch(
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

@Composable
fun AppAppearanceContent(
    appConfig: IslandConfig,
    globalConfig: IslandConfig,
    onUpdate: (IslandConfig) -> Unit,
    activeDesc: String,
    inactiveDesc: String
) {
    val isUsingGlobal = appConfig.isFloat == null

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (isUsingGlobal) onUpdate(globalConfig)
                    else onUpdate(IslandConfig(null, null, null))
                }
                .padding(vertical = 8.dp)
                .semantics { stateDescription = if (isUsingGlobal) activeDesc else inactiveDesc },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = isUsingGlobal, onCheckedChange = null)
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.use_global_default), style = MaterialTheme.typography.bodyLarge)
        }

        if (!isUsingGlobal) {
            Spacer(Modifier.height(8.dp))
            IslandSettingsControl(
                config = appConfig,
                onUpdate = onUpdate
            )
        } else {
            Text(
                text = stringResource(R.string.appearance_use_defaults_desc),
                color = Color.Gray,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp, start = 8.dp)
            )
        }
    }
}

@Composable
fun AppWidgetsSectionCard(
    savedWidgetIds: List<Int>,
    availableProviders: List<AppWidgetProviderInfo>,
    onAddWidget: () -> Unit,
    onDeleteWidget: (Int) -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Widgets,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.app_widgets_section_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.app_widgets_section_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            if (savedWidgetIds.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_widgets_for_app),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                savedWidgetIds.forEachIndexed { index, widgetId ->
                    if (index > 0) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(0.2f),
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                    AppConfigWidgetChildItem(
                        widgetId = widgetId,
                        onDelete = { onDeleteWidget(widgetId) }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            FilledTonalButton(
                onClick = onAddWidget,
                enabled = availableProviders.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.add_island_widget))
            }

            if (availableProviders.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_available_widgets_for_app),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

@Composable
fun AppConfigWidgetChildItem(
    widgetId: Int,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val preferences = remember { AppPreferences(context.applicationContext) }
    val config by preferences.getWidgetConfigFlow(widgetId).collectAsState(initial = null)

    val viewHeightDp = when (config?.size) {
        WidgetSize.SMALL -> 100
        WidgetSize.MEDIUM -> 180
        WidgetSize.LARGE -> 280
        WidgetSize.XLARGE -> 380
        else -> 180
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.widget_id_fmt, widgetId),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error)
            }
        }

        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(viewHeightDp.dp + 32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    val wrapper = FrameLayout(ctx)
                    val hostView = WidgetManager.createPreview(ctx, widgetId)
                    if (hostView != null) {
                        val info = WidgetManager.getWidgetInfo(ctx, widgetId)
                        hostView.setAppWidget(widgetId, info)
                        wrapper.addView(hostView)

                        val density = ctx.resources.displayMetrics.density
                        val w = (300 * density).toInt()
                        val h = (viewHeightDp * density).toInt()

                        hostView.measure(
                            View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                            View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.AT_MOST)
                        )
                        hostView.layout(0, 0, hostView.measuredWidth, hostView.measuredHeight)
                    }
                    wrapper
                },
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxSize()
            )
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                val intent = Intent(context, com.d4viddf.hyperbridge.service.WidgetOverlayService::class.java).apply {
                    action = "ACTION_TEST_WIDGET"
                    putExtra("WIDGET_ID", widgetId)
                }
                context.startService(intent)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.filledTonalButtonColors()
        ) {
            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.show_on_island))
        }
    }
}

@Composable
fun FutureFeaturePlaceholderCard(
    title: String,
    badge: String,
    description: String,
    icon: ImageVector
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(8.dp))
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
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAppWidgetSheet(
    packageName: String,
    appName: String,
    availableProviders: List<AppWidgetProviderInfo>,
    onDismiss: () -> Unit,
    onSelectProvider: (AppWidgetProviderInfo) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = stringResource(R.string.select_app_widget),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = appName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            if (availableProviders.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_available_widgets_for_app),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(availableProviders) { provider ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectProvider(provider) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Widgets,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    val label = provider.loadLabel(context.packageManager)
                                    Text(
                                        text = if (label.isNullOrEmpty()) provider.provider.shortClassName else label,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${provider.targetCellWidth} x ${provider.targetCellHeight}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
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

@Preview(showBackground = true)
@Composable
fun AppConfigScreenPreview() {
    HyperBridgeTheme {
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
            savedWidgetIds = emptyList(),
            availableProviders = emptyList(),
            onBack = {},
            onToggleBridged = {},
            onToggleType = { _, _ -> },
            onToggleCallStage = { _, _ -> },
            onUpdateIslandConfig = {},
            onUpdateBlockedTerms = {},
            onNavConfigClick = {},
            onAddWidgetClick = {},
            onDeleteWidget = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AppConfigScreenManagedByThemePreview() {
    HyperBridgeTheme {
        AppConfigContent(
            appName = "Google Maps",
            packageName = "com.google.android.apps.maps",
            appIcon = null,
            isBridged = true,
            isManagedByTheme = true,
            activeTypes = setOf(NotificationType.NAVIGATION.name),
            activeCallStages = CallStage.entries.toSet(),
            appIslandConfig = IslandConfig(),
            globalConfig = IslandConfig(isFloat = true, isShowShade = true, timeout = 5),
            blockedTerms = emptySet(),
            savedWidgetIds = listOf(101),
            availableProviders = emptyList(),
            onBack = {},
            onToggleBridged = {},
            onToggleType = { _, _ -> },
            onToggleCallStage = { _, _ -> },
            onUpdateIslandConfig = {},
            onUpdateBlockedTerms = {},
            onNavConfigClick = {},
            onAddWidgetClick = {},
            onDeleteWidget = {}
        )
    }
}
