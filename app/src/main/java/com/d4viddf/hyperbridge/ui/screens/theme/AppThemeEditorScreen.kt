package com.d4viddf.hyperbridge.ui.screens.theme

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.DisplaySettings
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.models.NotificationType
import com.d4viddf.hyperbridge.ui.components.SettingsToggleCard
import com.d4viddf.hyperbridge.ui.screens.settings.NavCustomizationScreen
import com.d4viddf.hyperbridge.ui.screens.theme.content.ActionConfigSheet
import com.d4viddf.hyperbridge.ui.screens.theme.content.ActionsDetailContent
import com.d4viddf.hyperbridge.ui.screens.theme.content.BehaviourMenuContent
import com.d4viddf.hyperbridge.ui.screens.theme.content.CallStyleSheetContent
import com.d4viddf.hyperbridge.ui.screens.theme.content.ColorsDetailContent
import com.d4viddf.hyperbridge.ui.screens.theme.content.EngineThemeContent
import com.d4viddf.hyperbridge.ui.screens.theme.content.IconsDetailContent
import com.d4viddf.hyperbridge.ui.screens.theme.content.NotificationTypesContent
import com.d4viddf.hyperbridge.ui.screens.theme.content.SharedThemePreview
import com.d4viddf.hyperbridge.ui.screens.theme.content.ThemeBehaviourContent
import com.d4viddf.hyperbridge.ui.screens.theme.content.safeParseColor
import com.d4viddf.hyperbridge.ui.theme.HyperBridgeTheme

enum class AppEditorRoute {
    MENU, BEHAVIOR_MENU, BEHAVIOR_ENGINE, BEHAVIOR_ISLAND, BEHAVIOR_TYPES, COLORS, ICONS, CALLS, NAVIGATION, ACTIONS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppThemeEditor(viewModel: ThemeViewModel) {
    var currentRoute by remember { mutableStateOf(AppEditorRoute.MENU) }
    var showUnsavedDialog by remember { mutableStateOf(false) }

    val isNative by viewModel.useNativeLiveUpdates.collectAsState()

    // Logic to handle "Back" request
    val handleBack = {
        when (currentRoute) {
            AppEditorRoute.BEHAVIOR_ENGINE,
            AppEditorRoute.BEHAVIOR_ISLAND,
            AppEditorRoute.BEHAVIOR_TYPES -> currentRoute = AppEditorRoute.BEHAVIOR_MENU // Go back to sub-menu
            AppEditorRoute.MENU -> showUnsavedDialog = true // Ask to save before exiting
            else -> currentRoute = AppEditorRoute.MENU // Go back to main app menu
        }
    }

    // Intercept System Back Gesture
    BackHandler {
        handleBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (currentRoute == AppEditorRoute.MENU) stringResource(R.string.edit_app) else stringResource(
                                when (currentRoute) {
                                    AppEditorRoute.BEHAVIOR_MENU -> R.string.behaviour_triggers
                                    AppEditorRoute.BEHAVIOR_ENGINE -> R.string.engine
                                    AppEditorRoute.BEHAVIOR_ISLAND -> R.string.island_behavior
                                    AppEditorRoute.BEHAVIOR_TYPES -> R.string.active_notifications_title
                                    AppEditorRoute.COLORS -> R.string.creator_nav_colors
                                    AppEditorRoute.ICONS -> R.string.creator_nav_icons
                                    AppEditorRoute.CALLS -> R.string.creator_nav_calls
                                    AppEditorRoute.NAVIGATION -> R.string.nav_layout_title
                                    AppEditorRoute.ACTIONS -> R.string.creator_nav_actions
                                    else -> R.string.app_name
                                }
                            ),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (currentRoute == AppEditorRoute.MENU) {
                            Text(
                                text = viewModel.editingAppLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    FilledTonalIconButton(
                        onClick = { handleBack() }, // Use the unified back logic
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (currentRoute == AppEditorRoute.MENU) {
                        Button(
                            onClick = { viewModel.saveAppChanges() },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(stringResource(R.string.done))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Box(modifier = Modifier
            .padding(top = padding.calculateTopPadding())
            .fillMaxSize()) {
            AnimatedContent(
                targetState = currentRoute,
                transitionSpec = {
                    if (targetState == AppEditorRoute.MENU || targetState == AppEditorRoute.BEHAVIOR_MENU && initialState != AppEditorRoute.MENU) {
                        slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                    } else {
                        slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                    }
                },
                label = "AppEditorNav"
            ) { route ->
                val effHighlight = viewModel.appHighlightColor ?: viewModel.selectedColorHex
                val effShape = viewModel.appShapeId ?: viewModel.selectedShapeId
                val effPadding = viewModel.appPaddingPercent ?: viewModel.iconPaddingPercent
                val effAnswerColor = viewModel.appCallAnswerColor ?: viewModel.callAnswerColor
                val effDeclineColor = viewModel.appCallDeclineColor ?: viewModel.callDeclineColor
                val effAnswerShape = viewModel.appCallAnswerShapeId ?: viewModel.callAnswerShapeId
                val effDeclineShape = viewModel.appCallDeclineShapeId ?: viewModel.callDeclineShapeId

                when (route) {
                    AppEditorRoute.MENU -> AppEditorMenu(
                        viewModel = viewModel,
                        onNavigate = { currentRoute = it }
                    )
                    AppEditorRoute.BEHAVIOR_MENU -> Box(Modifier.fillMaxSize()) {
                        BehaviourMenuContent(onNavigate = {
                            currentRoute = when (it) {
                                CreatorRoute.BEHAVIOR_ENGINE -> AppEditorRoute.BEHAVIOR_ENGINE
                                CreatorRoute.BEHAVIOR_ISLAND -> AppEditorRoute.BEHAVIOR_ISLAND
                                CreatorRoute.BEHAVIOR_TYPES -> AppEditorRoute.BEHAVIOR_TYPES
                                else -> AppEditorRoute.BEHAVIOR_MENU
                            }
                        })
                    }
                    AppEditorRoute.BEHAVIOR_ENGINE -> Box(Modifier.fillMaxSize()) {
                        EngineThemeContent(
                            isNative = viewModel.appUseNativeLiveUpdates,
                            onEngineChange = { viewModel.appUseNativeLiveUpdates = it }
                        )
                    }
                    AppEditorRoute.BEHAVIOR_ISLAND -> Box(Modifier.fillMaxSize()) {
                        ThemeBehaviourContent() // Still uses standard island behavior
                    }
                    AppEditorRoute.BEHAVIOR_TYPES -> Box(Modifier.fillMaxSize()) {
                        AppNotificationTypesEditor(viewModel = viewModel)
                    }
                    AppEditorRoute.COLORS -> DetailScreenShell(
                        previewContent = { SharedThemePreview(effHighlight, false, effShape, effPadding, effAnswerColor, effDeclineColor, effAnswerShape, effDeclineShape) },
                        content = {
                            AppColorEditor(viewModel)
                        }
                    )
                    AppEditorRoute.ICONS -> DetailScreenShell(
                        previewContent = { SharedThemePreview(effHighlight, false, effShape, effPadding, effAnswerColor, effDeclineColor, effAnswerShape, effDeclineShape) },
                        content = {
                            AppIconEditor(viewModel)
                        }
                    )
                    AppEditorRoute.CALLS -> DetailScreenShell(
                        previewContent = { SharedThemePreview(effHighlight, false, effShape, effPadding, effAnswerColor, effDeclineColor, effAnswerShape, effDeclineShape) },
                        content = {
                            AppCallEditor(viewModel)
                        }
                    )
                    AppEditorRoute.NAVIGATION -> Box(Modifier.fillMaxSize()) {
                        NavCustomizationScreen(
                            onBack = { currentRoute = AppEditorRoute.MENU },
                            packageName = viewModel.editingAppPackage,
                            showTopBar = false
                        )
                    }
                    AppEditorRoute.ACTIONS -> Box(Modifier.fillMaxSize()) {
                        AppActionEditor(viewModel)
                    }
                }
            }
        }
    }

    // --- UNSAVED CHANGES DIALOG ---
    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedDialog = false }, // Cancel/Stay
            title = { Text(stringResource(R.string.unsaved_changes)) },
            text = { Text(stringResource(R.string.unsaved_changes_desc)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveAppChanges() // Saves and closes editor
                        showUnsavedDialog = false
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.cancelAppEditing() // Discards and closes editor
                        showUnsavedDialog = false
                    }
                ) {
                    Text(stringResource(R.string.discard))
                }
            }
        )
    }
}

@Composable
fun AppEditorMenu(
    viewModel: ThemeViewModel,
    onNavigate: (AppEditorRoute) -> Unit
) {
    val effHighlight = viewModel.appHighlightColor ?: viewModel.selectedColorHex
    val effShape = viewModel.appShapeId ?: viewModel.selectedShapeId
    val effPadding = viewModel.appPaddingPercent ?: viewModel.iconPaddingPercent
    val effAnswerColor = viewModel.appCallAnswerColor ?: viewModel.callAnswerColor
    val effDeclineColor = viewModel.appCallDeclineColor ?: viewModel.callDeclineColor
    val effAnswerShape = viewModel.appCallAnswerShapeId ?: viewModel.callAnswerShapeId
    val effDeclineShape = viewModel.appCallDeclineShapeId ?: viewModel.callDeclineShapeId

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 12.dp)) {
                    SharedThemePreview(effHighlight, false, effShape, effPadding, effAnswerColor, effDeclineColor, effAnswerShape, effDeclineShape)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            val items = listOf(
                AppEditorRoute.BEHAVIOR_MENU,
                AppEditorRoute.COLORS,
                AppEditorRoute.ICONS,
                AppEditorRoute.CALLS,
                AppEditorRoute.NAVIGATION,
                AppEditorRoute.ACTIONS
            )

            items.forEachIndexed { index, route ->
                val shape = getExpressiveShape(items.size, index, ShapeStyle.Large)
                when(route) {
                    AppEditorRoute.BEHAVIOR_MENU -> CreatorOptionCard(
                        title = stringResource(R.string.engine),
                        subtitle = stringResource(R.string.engine_timeouts_triggers),
                        icon = Icons.Outlined.DisplaySettings,
                        shape = shape,
                        onClick = { onNavigate(route) }
                    )
                    AppEditorRoute.COLORS -> CreatorOptionCard(
                        title = stringResource(R.string.creator_nav_colors),
                        subtitle = if (viewModel.appHighlightColor != null) stringResource(R.string.creator_sub_colors_custom) else stringResource(R.string.creator_sub_colors_default),
                        icon = Icons.Outlined.ColorLens,
                        shape = shape,
                        onClick = { onNavigate(route) },
                        trailingContent = {
                            Box(modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(safeParseColor(effHighlight))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape))
                        }
                    )
                    AppEditorRoute.ICONS -> CreatorOptionCard(
                        title = stringResource(R.string.creator_nav_icons),
                        subtitle = if (viewModel.appShapeId != null || viewModel.appPaddingPercent != null) stringResource(R.string.creator_sub_icons_custom) else stringResource(R.string.creator_sub_icons_default),
                        icon = Icons.Outlined.Widgets,
                        shape = shape,
                        onClick = { onNavigate(route) }
                    )
                    AppEditorRoute.CALLS -> CreatorOptionCard(
                        title = stringResource(R.string.creator_nav_calls),
                        subtitle = stringResource(R.string.creator_sub_calls),
                        icon = Icons.Outlined.Call,
                        shape = shape,
                        onClick = { onNavigate(route) }
                    )
                    AppEditorRoute.NAVIGATION -> CreatorOptionCard(
                        title = stringResource(R.string.nav_layout_title),
                        subtitle = stringResource(R.string.nav_layout_desc),
                        icon = Icons.Outlined.Map,
                        shape = shape,
                        onClick = { onNavigate(route) }
                    )
                    AppEditorRoute.ACTIONS -> CreatorOptionCard(
                        title = stringResource(R.string.creator_nav_actions),
                        subtitle = pluralStringResource(
                            id = R.plurals.creator_sub_actions_count,
                            count = viewModel.appActions.size,
                            viewModel.appActions.size
                        ),
                        icon = Icons.Outlined.TouchApp,
                        shape = shape,
                        onClick = { onNavigate(route) }
                    )
                    else -> {}
                }
            }
        }
        Spacer(Modifier.height(48.dp))
    }
}

// --- SUB-EDITORS ---

@Composable
fun AppColorEditor(viewModel: ThemeViewModel) {
    ColorsDetailContent(
        selectedColorHex = viewModel.appHighlightColor ?: viewModel.selectedColorHex,
        colorMode = viewModel.appColorMode ?: viewModel.colorMode,
        onColorSelected = { viewModel.appHighlightColor = it },
        onColorModeChanged = { viewModel.appColorMode = it }
    )
}

@Composable
fun AppIconEditor(viewModel: ThemeViewModel) {
    IconsDetailContent(
        iconPaddingPercent = viewModel.appPaddingPercent ?: viewModel.iconPaddingPercent,
        selectedShapeId = viewModel.appShapeId ?: viewModel.selectedShapeId,
        onPaddingChange = { viewModel.appPaddingPercent = it },
        onShapeChange = { viewModel.appShapeId = it },
        onStageAsset = { key, uri -> viewModel.stageAsset("app_${viewModel.editingAppPackage}_$key", uri) }
    )
}

@Composable
fun AppCallEditor(viewModel: ThemeViewModel) {
    CallStyleSheetContent(
        answerColor = viewModel.appCallAnswerColor ?: viewModel.callAnswerColor,
        declineColor = viewModel.appCallDeclineColor ?: viewModel.callDeclineColor,
        answerShapeId = viewModel.appCallAnswerShapeId ?: viewModel.callAnswerShapeId,
        declineShapeId = viewModel.appCallDeclineShapeId ?: viewModel.callDeclineShapeId,

        onAnswerColorChange = { viewModel.appCallAnswerColor = it },
        onDeclineColorChange = { viewModel.appCallDeclineColor = it },

        onAnswerShapeChange = { viewModel.appCallAnswerShapeId = it },
        onDeclineShapeChange = { viewModel.appCallDeclineShapeId = it },

        onAnswerIconSelected = { viewModel.appCallAnswerUri = it },
        onDeclineIconSelected = { viewModel.appCallDeclineUri = it }
    )
}

@Composable
fun AppActionEditor(viewModel: ThemeViewModel) {
    var showAddSheet by remember { mutableStateOf(false) }

    ActionsDetailContent(
        actions = viewModel.appActions,
        onUpdateAction = { k, v -> viewModel.updateAppAction(k, v) },
        onRemoveAction = { k -> viewModel.removeAppAction(k) }
    )

    if (showAddSheet) {
        ActionConfigSheet(
            initialKeyword = null,
            initialConfig = null,
            onDismiss = { showAddSheet = false },
            onSave = { key, config ->
                viewModel.updateAppAction(key, config)
                showAddSheet = false
            }
        )
    }
}


// ========================================================================
//                      APP-SPECIFIC BEHAVIOR EDITORS
// ========================================================================

@Composable
fun AppNotificationTypesEditor(viewModel: ThemeViewModel) {
    // If it's not null, it means the user is overriding the global setting
    var isOverride by remember { mutableStateOf(viewModel.appEnabledNotificationTypes != null) }
    val activeTypes = viewModel.appEnabledNotificationTypes ?: emptySet()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(stringResource(R.string.trigger_override), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.trigger_app_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))

        SettingsToggleCard(
            title = stringResource(R.string.override_global_triggers),
            subtitle = stringResource(R.string.override_global_trigger_desc),
            icon = Icons.Outlined.NotificationsActive,
            checked = isOverride,
            onCheckedChange = { checked ->
                isOverride = checked
                // If turning on, default to everything enabled for this app
                viewModel.appEnabledNotificationTypes = if (checked) NotificationType.entries.map { it.name }.toSet() else null
            },
            shape = RoundedCornerShape(24.dp)
        )

        AnimatedVisibility(
            visible = isOverride,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(Modifier.padding(top = 16.dp)) {
                NotificationType.entries.forEachIndexed { index, type ->
                    val shape = when {
                        NotificationType.entries.size == 1 -> RoundedCornerShape(24.dp)
                        index == 0 -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                        index == NotificationType.entries.size - 1 -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
                        else -> RoundedCornerShape(4.dp)
                    }

                    // You can map the exact icons and subtitles here as you did in ThemeSettingsContent
                    SettingsToggleCard(
                        title = stringResource(type.labelRes),
                        subtitle = stringResource(R.string.enable_triggers),
                        icon = Icons.Outlined.TouchApp, // Update this with your specific type icons if desired
                        checked = activeTypes.contains(type.name),
                        onCheckedChange = { isChecked ->
                            val newSet = if (isChecked) activeTypes + type.name else activeTypes - type.name
                            viewModel.appEnabledNotificationTypes = newSet
                        },
                        shape = shape
                    )

                    if (index < NotificationType.entries.size - 1) {
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                }
            }
        }
    }
}

// ========================================================================
//                             PREVIEWS
// ========================================================================

@Preview(showBackground = true, name = "1. App Editor Menu (Light)")
@Composable
fun PreviewAppEditorMenuLight() {
    HyperBridgeTheme(darkTheme = false) {
        Box(modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)) {
            BehaviourMenuContent(onNavigate = {})
        }
    }
}

@Preview(showBackground = true, name = "2. App Editor Menu (Dark)")
@Composable
fun PreviewAppEditorMenuDark() {
    HyperBridgeTheme(darkTheme = true) {
        Box(modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)) {
            BehaviourMenuContent(onNavigate = {})
        }
    }
}

@Preview(showBackground = true, name = "3. Notification Types (Light)")
@Composable
fun PreviewNotificationTypesContentLight() {
    HyperBridgeTheme(darkTheme = false) {
        Box(modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)) {
            NotificationTypesContent()
        }
    }
}

@Preview(showBackground = true, name = "4. Notification Types (Dark)")
@Composable
fun PreviewNotificationTypesContentDark() {
    HyperBridgeTheme(darkTheme = true) {
        Box(modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)) {
            NotificationTypesContent()
        }
    }
}

@Preview(showBackground = true, name = "5. Engine - Default Inherit (Light)")
@Composable
fun PreviewEngineThemeContentDefault() {
    HyperBridgeTheme(darkTheme = false) {
        Box(modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)) {
            EngineThemeContent(
                isNative = null, // Demonstrates the Default option selected
                onEngineChange = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "6. Engine - Native Live Updates (Dark)")
@Composable
fun PreviewEngineThemeContentNativeDark() {
    HyperBridgeTheme(darkTheme = true) {
        Box(modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)) {
            EngineThemeContent(
                isNative = true, // Demonstrates the Native option selected
                onEngineChange = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "7. Engine - Xiaomi Custom (Light)")
@Composable
fun PreviewEngineThemeContentXiaomiLight() {
    HyperBridgeTheme(darkTheme = false) {
        Box(modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)) {
            EngineThemeContent(
                isNative = false, // Demonstrates the Xiaomi option selected
                onEngineChange = {}
            )
        }
    }
}