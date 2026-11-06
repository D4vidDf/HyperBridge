package com.d4viddf.hyperbridge.ui.screens.theme

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.DisplaySettings
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.models.theme.ColorMode
import com.d4viddf.hyperbridge.ui.screens.settings.NavCustomizationScreen
import com.d4viddf.hyperbridge.ui.screens.theme.content.ActionsDetailContent
import com.d4viddf.hyperbridge.ui.screens.theme.content.AppsDetailContent
import com.d4viddf.hyperbridge.ui.screens.theme.content.BehaviourMenuContent
import com.d4viddf.hyperbridge.ui.screens.theme.content.CallStyleSheetContent
import com.d4viddf.hyperbridge.ui.screens.theme.content.ColorsDetailContent
import com.d4viddf.hyperbridge.ui.screens.theme.content.EngineThemeContent
import com.d4viddf.hyperbridge.ui.screens.theme.content.IconsDetailContent
import com.d4viddf.hyperbridge.ui.screens.theme.content.NotificationTypesContent
import com.d4viddf.hyperbridge.ui.screens.theme.content.ReplyStyleSheetContent
import com.d4viddf.hyperbridge.ui.screens.theme.content.ThemeBehaviourContent
import com.d4viddf.hyperbridge.ui.screens.theme.content.safeParseColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val CREATOR_SPLIT_MIN_WIDTH = 560.dp

enum class CreatorRoute {
    MAIN_MENU,
    BEHAVIOR_MENU,
    BEHAVIOR_ENGINE,
    BEHAVIOR_ISLAND,
    BEHAVIOR_TYPES,
    COLORS,
    ICONS,
    CALLS,
    NAVIGATION,
    REPLY,
    ACTIONS,
    APPS,
    // Cascaded Per-App Override Routes
    APP_MENU,
    APP_BEHAVIOR_MENU,
    APP_BEHAVIOR_ENGINE,
    APP_BEHAVIOR_ISLAND,
    APP_BEHAVIOR_TYPES,
    APP_COLORS,
    APP_ICONS,
    APP_CALLS,
    APP_NAVIGATION,
    APP_REPLY,
    APP_ACTIONS
}

private fun CreatorRoute.isAppRoute(): Boolean = when (this) {
    CreatorRoute.APP_MENU,
    CreatorRoute.APP_BEHAVIOR_MENU,
    CreatorRoute.APP_BEHAVIOR_ENGINE,
    CreatorRoute.APP_BEHAVIOR_ISLAND,
    CreatorRoute.APP_BEHAVIOR_TYPES,
    CreatorRoute.APP_COLORS,
    CreatorRoute.APP_ICONS,
    CreatorRoute.APP_CALLS,
    CreatorRoute.APP_NAVIGATION,
    CreatorRoute.APP_REPLY,
    CreatorRoute.APP_ACTIONS -> true
    else -> false
}

private fun CreatorRoute.toAppEditorRouteOrNull(): AppEditorRoute? = when (this) {
    CreatorRoute.APP_MENU -> AppEditorRoute.MENU
    CreatorRoute.APP_BEHAVIOR_MENU -> AppEditorRoute.BEHAVIOR_MENU
    CreatorRoute.APP_BEHAVIOR_ENGINE -> AppEditorRoute.BEHAVIOR_ENGINE
    CreatorRoute.APP_BEHAVIOR_ISLAND -> AppEditorRoute.BEHAVIOR_ISLAND
    CreatorRoute.APP_BEHAVIOR_TYPES -> AppEditorRoute.BEHAVIOR_TYPES
    CreatorRoute.APP_COLORS -> AppEditorRoute.COLORS
    CreatorRoute.APP_ICONS -> AppEditorRoute.ICONS
    CreatorRoute.APP_CALLS -> AppEditorRoute.CALLS
    CreatorRoute.APP_NAVIGATION -> AppEditorRoute.NAVIGATION
    CreatorRoute.APP_REPLY -> AppEditorRoute.REPLY
    CreatorRoute.APP_ACTIONS -> AppEditorRoute.ACTIONS
    else -> null
}

private fun AppEditorRoute.toCreatorRoute(): CreatorRoute = when (this) {
    AppEditorRoute.MENU -> CreatorRoute.APP_MENU
    AppEditorRoute.BEHAVIOR_MENU -> CreatorRoute.APP_BEHAVIOR_MENU
    AppEditorRoute.BEHAVIOR_ENGINE -> CreatorRoute.APP_BEHAVIOR_ENGINE
    AppEditorRoute.BEHAVIOR_ISLAND -> CreatorRoute.APP_BEHAVIOR_ISLAND
    AppEditorRoute.BEHAVIOR_TYPES -> CreatorRoute.APP_BEHAVIOR_TYPES
    AppEditorRoute.COLORS -> CreatorRoute.APP_COLORS
    AppEditorRoute.ICONS -> CreatorRoute.APP_ICONS
    AppEditorRoute.CALLS -> CreatorRoute.APP_CALLS
    AppEditorRoute.NAVIGATION -> CreatorRoute.APP_NAVIGATION
    AppEditorRoute.REPLY -> CreatorRoute.APP_REPLY
    AppEditorRoute.ACTIONS -> CreatorRoute.APP_ACTIONS
}

@Composable
fun ThemeCreatorScreen(
    editThemeId: String? = null,
    onBack: () -> Unit,
    onThemeCreated: () -> Unit
) {
    val viewModel: ThemeViewModel = viewModel()
    val activeThemeId by viewModel.activeThemeId.collectAsState()
    val isNative by viewModel.useNativeLiveUpdates.collectAsState()

    var stackNames by rememberSaveable {
        mutableStateOf(listOf(CreatorRoute.MAIN_MENU.name))
    }
    var previousStackSize by rememberSaveable { mutableIntStateOf(1) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showAppUnsavedDialog by remember { mutableStateOf(false) }

    val stack = remember(stackNames) {
        stackNames.mapNotNull { name ->
            runCatching { CreatorRoute.valueOf(name) }.getOrNull()
        }.ifEmpty { listOf(CreatorRoute.MAIN_MENU) }
    }

    LaunchedEffect(editThemeId) {
        if (editThemeId != null) {
            if (viewModel.currentEditingThemeId != editThemeId) {
                viewModel.loadThemeForEditing(editThemeId)
            }
        } else {
            if (viewModel.currentEditingThemeId == null) {
                viewModel.clearCreatorState()
            }
        }
    }

    // Keep stack synchronized if editingAppPackage was cleared externally
    LaunchedEffect(viewModel.editingAppPackage, stack) {
        if (viewModel.editingAppPackage == null && stack.any { it.isAppRoute() }) {
            val trimmed = stack.takeWhile { !it.isAppRoute() }.ifEmpty { listOf(CreatorRoute.MAIN_MENU) }
            previousStackSize = stack.size
            stackNames = trimmed.map { it.name }
        }
    }

    fun navigateFrom(source: CreatorRoute, target: CreatorRoute) {
        // If navigating from a non-app route while an app is open, close app editing cleanly
        if (!source.isAppRoute() && !target.isAppRoute() && viewModel.editingAppPackage != null) {
            viewModel.cancelAppEditing()
        }
        val sourceIndex = stack.lastIndexOf(source)
        previousStackSize = stack.size
        stackNames = if (sourceIndex != -1) {
            if (stack.getOrNull(sourceIndex + 1) == target && stack.size == sourceIndex + 2) {
                // Tapping the already-open option in the left pane deselects it and returns to source
                if (target == CreatorRoute.APP_MENU) {
                    showAppUnsavedDialog = true
                    stackNames
                } else {
                    stack.take(sourceIndex + 1).map { it.name }
                }
            } else {
                (stack.take(sourceIndex + 1) + target).map { it.name }
            }
        } else {
            (stack + target).map { it.name }
        }
    }

    fun popFrom(route: CreatorRoute) {
        if (route == CreatorRoute.MAIN_MENU) {
            viewModel.currentEditingThemeId = null
            onBack()
            return
        }
        if (route == CreatorRoute.APP_MENU) {
            showAppUnsavedDialog = true
            return
        }
        val idx = stack.lastIndexOf(route)
        previousStackSize = stack.size
        if (idx > 0) {
            val newStack = stack.take(idx)
            if (newStack.none { it.isAppRoute() } && viewModel.editingAppPackage != null) {
                viewModel.cancelAppEditing()
            }
            stackNames = newStack.map { it.name }
        } else if (stack.size > 1) {
            stackNames = stack.dropLast(1).map { it.name }
        }
    }

    val handleSaveTheme = {
        if (editThemeId != null && editThemeId == activeThemeId) {
            viewModel.saveTheme(existingId = editThemeId, apply = true)
            viewModel.currentEditingThemeId = null
            onThemeCreated()
        } else {
            showSaveDialog = true
        }
    }

    val handleSaveAppChanges = {
        viewModel.saveAppChanges()
        val trimmed = stack.takeWhile { !it.isAppRoute() }.ifEmpty { listOf(CreatorRoute.MAIN_MENU) }
        previousStackSize = stack.size
        stackNames = trimmed.map { it.name }
    }

    BackHandler(enabled = true) {
        popFrom(stack.last())
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        val isSplitMode = maxWidth >= CREATOR_SPLIT_MIN_WIDTH

        if (isSplitMode) {
            val leftRoute = if (stack.size >= 2) stack[stack.size - 2] else CreatorRoute.MAIN_MENU
            val rightRoute: CreatorRoute? = if (stack.size >= 2) stack.last() else null
            val isForward = stack.size >= previousStackSize

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // LEFT PANE: Always shows MAIN_MENU at depth 1 & 2, cascades to parent route at depth 3+
                Box(
                    modifier = Modifier
                        .weight(0.44f)
                        .fillMaxHeight()
                ) {
                    AnimatedContent(
                        targetState = leftRoute,
                        transitionSpec = {
                            if (isForward) {
                                slideInHorizontally { it / 2 } + fadeIn() togetherWith
                                    slideOutHorizontally { -it / 2 } + fadeOut()
                            } else {
                                slideInHorizontally { -it / 2 } + fadeIn() togetherWith
                                    slideOutHorizontally { it / 2 } + fadeOut()
                            }
                        },
                        label = "CreatorLeftCascade"
                    ) { route ->
                        val selectedInLeft = if (stack.size >= 2 && route == leftRoute) rightRoute else null
                        if (route == CreatorRoute.MAIN_MENU) {
                            CreatorPaneScaffold(
                                route = route,
                                editThemeId = editThemeId,
                                editingAppLabel = viewModel.editingAppLabel,
                                isLeftPane = true,
                                onBackClick = { popFrom(route) },
                                onSaveTheme = handleSaveTheme,
                                onDoneApp = handleSaveAppChanges
                            ) {
                                CreatorNodeBody(
                                    route = route,
                                    selectedChild = selectedInLeft,
                                    viewModel = viewModel,
                                    isNative = isNative,
                                    onNavigateFrom = { src, dst -> navigateFrom(src, dst) },
                                    onPopRoute = { popFrom(it) },
                                    onEditSettings = { showSettingsSheet = true }
                                )
                            }
                        } else {
                            CreatorDetailPaneSurface {
                                CreatorPaneScaffold(
                                    route = route,
                                    editThemeId = editThemeId,
                                    editingAppLabel = viewModel.editingAppLabel,
                                    isLeftPane = true,
                                    onBackClick = { popFrom(route) },
                                    onSaveTheme = handleSaveTheme,
                                    onDoneApp = handleSaveAppChanges
                                ) {
                                    CreatorNodeBody(
                                        route = route,
                                        selectedChild = selectedInLeft,
                                        viewModel = viewModel,
                                        isNative = isNative,
                                        onNavigateFrom = { src, dst -> navigateFrom(src, dst) },
                                        onPopRoute = { popFrom(it) },
                                        onEditSettings = { showSettingsSheet = true }
                                    )
                                }
                            }
                        }
                    }
                }

                // RIGHT PANE: Shows "Nothing is selected" at depth 1, or active detail/sub-menu at depth 2+
                Box(
                    modifier = Modifier
                        .weight(0.56f)
                        .fillMaxHeight()
                ) {
                    CreatorDetailPaneSurface {
                        AnimatedContent(
                            targetState = rightRoute,
                            transitionSpec = {
                                if (targetState == null) {
                                    fadeIn() togetherWith slideOutHorizontally { it / 3 } + fadeOut()
                                } else if (initialState == null || isForward) {
                                    slideInHorizontally { it / 2 } + fadeIn() togetherWith
                                        slideOutHorizontally { -it / 3 } + fadeOut()
                                } else {
                                    slideInHorizontally { -it / 3 } + fadeIn() togetherWith
                                        slideOutHorizontally { it / 2 } + fadeOut()
                                }
                            },
                            label = "CreatorRightDetail"
                        ) { route ->
                            if (route == null) {
                                CreatorNothingSelectedPane()
                            } else {
                                CreatorPaneScaffold(
                                    route = route,
                                    editThemeId = editThemeId,
                                    editingAppLabel = viewModel.editingAppLabel,
                                    isLeftPane = false,
                                    onBackClick = { popFrom(route) },
                                    onSaveTheme = handleSaveTheme,
                                    onDoneApp = handleSaveAppChanges
                                ) {
                                    CreatorNodeBody(
                                        route = route,
                                        selectedChild = null,
                                        viewModel = viewModel,
                                        isNative = isNative,
                                        onNavigateFrom = { src, dst -> navigateFrom(src, dst) },
                                        onPopRoute = { popFrom(it) },
                                        onEditSettings = { showSettingsSheet = true }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // COMPACT / PHONE MODE: Single full-screen pane showing stack.last()
            val currentRoute = stack.last()
            val isForward = stack.size >= previousStackSize
            AnimatedContent(
                targetState = currentRoute,
                transitionSpec = {
                    if (isForward && targetState != CreatorRoute.MAIN_MENU) {
                        slideInHorizontally { it } + fadeIn() togetherWith
                            slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith
                            slideOutHorizontally { it } + fadeOut()
                    }
                },
                label = "CreatorCompactNav"
            ) { route ->
                CreatorPaneScaffold(
                    route = route,
                    editThemeId = editThemeId,
                    editingAppLabel = viewModel.editingAppLabel,
                    isLeftPane = true,
                    onBackClick = { popFrom(route) },
                    onSaveTheme = handleSaveTheme,
                    onDoneApp = handleSaveAppChanges
                ) {
                    CreatorNodeBody(
                        route = route,
                        selectedChild = null,
                        viewModel = viewModel,
                        isNative = isNative,
                        onNavigateFrom = { src, dst -> navigateFrom(src, dst) },
                        onPopRoute = { popFrom(it) },
                        onEditSettings = { showSettingsSheet = true }
                    )
                }
            }
        }

        if (showSettingsSheet) {
            ThemeMetadataSheet(viewModel) { showSettingsSheet = false }
        }
        if (showSaveDialog) {
            SaveDialog(viewModel, editThemeId, activeThemeId, onThemeCreated) { showSaveDialog = false }
        }
        if (showAppUnsavedDialog) {
            AlertDialog(
                onDismissRequest = { showAppUnsavedDialog = false },
                title = { Text(stringResource(R.string.unsaved_changes)) },
                text = { Text(stringResource(R.string.unsaved_changes_desc)) },
                confirmButton = {
                    Button(
                        onClick = {
                            showAppUnsavedDialog = false
                            handleSaveAppChanges()
                        }
                    ) {
                        Text(stringResource(R.string.save))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showAppUnsavedDialog = false
                            viewModel.cancelAppEditing()
                            val trimmed = stack.takeWhile { !it.isAppRoute() }.ifEmpty { listOf(CreatorRoute.MAIN_MENU) }
                            previousStackSize = stack.size
                            stackNames = trimmed.map { it.name }
                        }
                    ) {
                        Text(stringResource(R.string.discard))
                    }
                }
            )
        }
    }
}

@Composable
private fun CreatorDetailPaneSurface(
    content: @Composable () -> Unit
) {
    val baseScheme = MaterialTheme.colorScheme
    val detailScheme = remember(baseScheme) {
        baseScheme.copy(
            background = baseScheme.surfaceContainerLow,
            surface = baseScheme.surfaceContainerLow,
            surfaceContainerLow = baseScheme.surfaceContainer
        )
    }
    MaterialTheme(colorScheme = detailScheme) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            color = baseScheme.surfaceContainerLow
        ) {
            content()
        }
    }
}

@Composable
fun CreatorNothingSelectedPane() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.width(340.dp)
        ) {
            Surface(
                modifier = Modifier.size(88.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.creator_nothing_selected_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.creator_nothing_selected_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreatorPaneScaffold(
    route: CreatorRoute,
    editThemeId: String?,
    editingAppLabel: String,
    isLeftPane: Boolean,
    onBackClick: () -> Unit,
    onSaveTheme: () -> Unit,
    onDoneApp: () -> Unit,
    content: @Composable () -> Unit
) {
    val titleText = when (route) {
        CreatorRoute.MAIN_MENU -> if (editThemeId == null) stringResource(R.string.creator_title_new) else stringResource(R.string.creator_title_edit)
        CreatorRoute.BEHAVIOR_MENU, CreatorRoute.APP_BEHAVIOR_MENU -> stringResource(R.string.behaviour_triggers)
        CreatorRoute.BEHAVIOR_ENGINE, CreatorRoute.APP_BEHAVIOR_ENGINE -> stringResource(R.string.engine)
        CreatorRoute.BEHAVIOR_ISLAND, CreatorRoute.APP_BEHAVIOR_ISLAND -> stringResource(R.string.island_behavior_title)
        CreatorRoute.BEHAVIOR_TYPES, CreatorRoute.APP_BEHAVIOR_TYPES -> stringResource(R.string.active_notifications_title)
        CreatorRoute.COLORS, CreatorRoute.APP_COLORS -> stringResource(R.string.creator_nav_colors)
        CreatorRoute.ICONS, CreatorRoute.APP_ICONS -> stringResource(R.string.creator_nav_icons)
        CreatorRoute.CALLS, CreatorRoute.APP_CALLS -> stringResource(R.string.creator_nav_calls)
        CreatorRoute.NAVIGATION, CreatorRoute.APP_NAVIGATION -> stringResource(R.string.nav_layout_title)
        CreatorRoute.REPLY, CreatorRoute.APP_REPLY -> stringResource(R.string.inline_reply_title)
        CreatorRoute.ACTIONS, CreatorRoute.APP_ACTIONS -> stringResource(R.string.creator_nav_actions)
        CreatorRoute.APPS -> stringResource(R.string.creator_nav_apps)
        CreatorRoute.APP_MENU -> stringResource(R.string.edit_app)
    }

    val subtitleText: String? = when {
        route.isAppRoute() && editingAppLabel.isNotBlank() -> editingAppLabel
        else -> null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = titleText,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (subtitleText != null) {
                            Text(
                                text = subtitleText,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    FilledTonalIconButton(
                        onClick = onBackClick,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    ) {
                        Icon(
                            imageVector = if (isLeftPane) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Close,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    if (route == CreatorRoute.MAIN_MENU) {
                        Button(
                            onClick = onSaveTheme,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
                        }
                    } else if (route == CreatorRoute.APP_MENU) {
                        Button(
                            onClick = onDoneApp,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(stringResource(R.string.done), fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(top = padding.calculateTopPadding())
                .fillMaxSize()
        ) {
            content()
        }
    }
}

@Composable
private fun CreatorNodeBody(
    route: CreatorRoute,
    selectedChild: CreatorRoute?,
    viewModel: ThemeViewModel,
    isNative: Boolean,
    onNavigateFrom: (source: CreatorRoute, target: CreatorRoute) -> Unit,
    onPopRoute: (CreatorRoute) -> Unit,
    onEditSettings: () -> Unit
) {
    val effHighlight = viewModel.appHighlightColor ?: viewModel.selectedColorHex
    val effShape = viewModel.appShapeId ?: viewModel.selectedShapeId
    val effPadding = viewModel.appPaddingPercent ?: viewModel.iconPaddingPercent
    val effAnswerColor = viewModel.appCallAnswerColor ?: viewModel.callAnswerColor
    val effDeclineColor = viewModel.appCallDeclineColor ?: viewModel.callDeclineColor
    val effAnswerShape = viewModel.appCallAnswerShapeId ?: viewModel.callAnswerShapeId
    val effDeclineShape = viewModel.appCallDeclineShapeId ?: viewModel.callDeclineShapeId

    when (route) {
        CreatorRoute.MAIN_MENU -> CreatorMainList(
            viewModel = viewModel,
            selectedRoute = selectedChild,
            onNavigate = { target -> onNavigateFrom(CreatorRoute.MAIN_MENU, target) },
            onEditSettings = onEditSettings
        )
        CreatorRoute.BEHAVIOR_MENU -> Box(Modifier.fillMaxSize()) {
            BehaviourMenuContent(
                selectedRoute = selectedChild,
                onNavigate = { target -> onNavigateFrom(CreatorRoute.BEHAVIOR_MENU, target) }
            )
        }
        CreatorRoute.BEHAVIOR_ENGINE -> {
            EngineThemeContent(
                isNative = isNative,
                showDefaultOption = false,
                onEngineChange = { viewModel.setUseNativeLiveUpdates(it ?: false) }
            )
        }
        CreatorRoute.BEHAVIOR_ISLAND -> Box(Modifier.fillMaxSize()) {
            ThemeBehaviourContent()
        }
        CreatorRoute.BEHAVIOR_TYPES -> Box(Modifier.fillMaxSize()) {
            NotificationTypesContent()
        }
        CreatorRoute.NAVIGATION -> Box(Modifier.fillMaxSize()) {
            NavCustomizationScreen(
                onBack = { onPopRoute(CreatorRoute.NAVIGATION) },
                packageName = null,
                showTopBar = false
            )
        }
        CreatorRoute.REPLY -> Box(Modifier.fillMaxSize()) {
            ReplyStyleSheetContent(viewModel = viewModel)
        }
        CreatorRoute.COLORS -> DetailScreenShell(
            previewContent = {
                SharedThemePreview(
                    highlightColorHex = viewModel.selectedColorHex,
                    useAppColors = (viewModel.colorMode == ColorMode.APP_ICON),
                    shapeId = viewModel.selectedShapeId,
                    paddingPercent = viewModel.iconPaddingPercent,
                    answerColorHex = viewModel.callAnswerColor,
                    declineColorHex = viewModel.callDeclineColor,
                    answerShapeId = viewModel.callAnswerShapeId,
                    declineShapeId = viewModel.callDeclineShapeId
                )
            },
            content = {
                ColorsDetailContent(
                    selectedColorHex = viewModel.selectedColorHex,
                    colorMode = viewModel.colorMode,
                    onColorSelected = { viewModel.selectedColorHex = it },
                    onColorModeChanged = { viewModel.colorMode = it }
                )
            }
        )
        CreatorRoute.ICONS -> DetailScreenShell(
            previewContent = {
                SharedThemePreview(
                    highlightColorHex = viewModel.selectedColorHex,
                    useAppColors = (viewModel.colorMode == ColorMode.APP_ICON),
                    shapeId = viewModel.selectedShapeId,
                    paddingPercent = viewModel.iconPaddingPercent,
                    answerColorHex = viewModel.callAnswerColor,
                    declineColorHex = viewModel.callDeclineColor,
                    answerShapeId = viewModel.callAnswerShapeId,
                    declineShapeId = viewModel.callDeclineShapeId
                )
            },
            content = {
                IconsDetailContent(
                    iconPaddingPercent = viewModel.iconPaddingPercent,
                    selectedShapeId = viewModel.selectedShapeId,
                    onPaddingChange = { viewModel.iconPaddingPercent = it },
                    onShapeChange = { viewModel.selectedShapeId = it },
                    onStageAsset = { key, uri -> viewModel.stageAsset(key, uri) }
                )
            }
        )
        CreatorRoute.CALLS -> DetailScreenShell(
            previewContent = {
                SharedThemePreview(
                    highlightColorHex = viewModel.selectedColorHex,
                    useAppColors = (viewModel.colorMode == ColorMode.APP_ICON),
                    shapeId = viewModel.selectedShapeId,
                    paddingPercent = viewModel.iconPaddingPercent,
                    answerColorHex = viewModel.callAnswerColor,
                    declineColorHex = viewModel.callDeclineColor,
                    answerShapeId = viewModel.callAnswerShapeId,
                    declineShapeId = viewModel.callDeclineShapeId
                )
            },
            content = {
                CallStyleSheetContent(
                    answerColor = viewModel.callAnswerColor,
                    declineColor = viewModel.callDeclineColor,
                    answerShapeId = viewModel.callAnswerShapeId,
                    declineShapeId = viewModel.callDeclineShapeId,
                    onAnswerColorChange = { viewModel.callAnswerColor = it },
                    onDeclineColorChange = { viewModel.callDeclineColor = it },
                    onAnswerShapeChange = { viewModel.callAnswerShapeId = it },
                    onDeclineShapeChange = { viewModel.callDeclineShapeId = it },
                    onAnswerIconSelected = { viewModel.callAnswerUri = it; viewModel.stageAsset("call_answer", it) },
                    onDeclineIconSelected = { viewModel.callDeclineUri = it; viewModel.stageAsset("call_decline", it) }
                )
            }
        )
        CreatorRoute.ACTIONS -> Box(Modifier.fillMaxSize()) {
            ActionsDetailContent(
                actions = viewModel.themeDefaultActions,
                onUpdateAction = { k, v -> viewModel.updateDefaultAction(k, v) },
                onRemoveAction = { k -> viewModel.removeDefaultAction(k) }
            )
        }
        CreatorRoute.APPS -> Box(Modifier.fillMaxSize()) {
            AppsDetailContent(
                viewModel = viewModel,
                selectedPackageName = if (selectedChild == CreatorRoute.APP_MENU) viewModel.editingAppPackage else null,
                onAppSelected = { pkg, label ->
                    val alreadyEditingSameApp =
                        viewModel.editingAppPackage == pkg && selectedChild == CreatorRoute.APP_MENU
                    if (alreadyEditingSameApp) {
                        onNavigateFrom(CreatorRoute.APPS, CreatorRoute.APP_MENU)
                    } else {
                        viewModel.loadAppForEditing(pkg, label)
                        onNavigateFrom(CreatorRoute.APPS, CreatorRoute.APP_MENU)
                    }
                },
                onAppDeleted = { pkg ->
                    if (viewModel.editingAppPackage == pkg) {
                        viewModel.cancelAppEditing()
                    }
                    viewModel.removeAppOverride(pkg)
                }
            )
        }
        // --- CASCADED PER-APP ROUTES ---
        CreatorRoute.APP_MENU -> AppEditorMenu(
            viewModel = viewModel,
            selectedRoute = selectedChild?.toAppEditorRouteOrNull(),
            onNavigate = { appRoute ->
                onNavigateFrom(CreatorRoute.APP_MENU, appRoute.toCreatorRoute())
            }
        )
        CreatorRoute.APP_BEHAVIOR_MENU -> Box(Modifier.fillMaxSize()) {
            BehaviourMenuContent(
                selectedRoute = selectedChild,
                onNavigate = { target ->
                    val mapped = when (target) {
                        CreatorRoute.BEHAVIOR_ENGINE -> CreatorRoute.APP_BEHAVIOR_ENGINE
                        CreatorRoute.BEHAVIOR_ISLAND -> CreatorRoute.APP_BEHAVIOR_ISLAND
                        CreatorRoute.BEHAVIOR_TYPES -> CreatorRoute.APP_BEHAVIOR_TYPES
                        else -> CreatorRoute.APP_BEHAVIOR_MENU
                    }
                    onNavigateFrom(CreatorRoute.APP_BEHAVIOR_MENU, mapped)
                }
            )
        }
        CreatorRoute.APP_BEHAVIOR_ENGINE -> Box(Modifier.fillMaxSize()) {
            EngineThemeContent(
                isNative = viewModel.appUseNativeLiveUpdates,
                onEngineChange = { viewModel.appUseNativeLiveUpdates = it }
            )
        }
        CreatorRoute.APP_BEHAVIOR_ISLAND -> Box(Modifier.fillMaxSize()) {
            ThemeBehaviourContent()
        }
        CreatorRoute.APP_BEHAVIOR_TYPES -> Box(Modifier.fillMaxSize()) {
            AppNotificationTypesEditor(viewModel = viewModel)
        }
        CreatorRoute.APP_COLORS -> DetailScreenShell(
            previewContent = {
                SharedThemePreview(
                    effHighlight,
                    false,
                    effShape,
                    effPadding,
                    effAnswerColor,
                    effDeclineColor,
                    effAnswerShape,
                    effDeclineShape
                )
            },
            content = {
                AppColorEditor(viewModel)
            }
        )
        CreatorRoute.APP_ICONS -> DetailScreenShell(
            previewContent = {
                SharedThemePreview(
                    effHighlight,
                    false,
                    effShape,
                    effPadding,
                    effAnswerColor,
                    effDeclineColor,
                    effAnswerShape,
                    effDeclineShape
                )
            },
            content = {
                AppIconEditor(viewModel)
            }
        )
        CreatorRoute.APP_CALLS -> DetailScreenShell(
            previewContent = {
                SharedThemePreview(
                    effHighlight,
                    false,
                    effShape,
                    effPadding,
                    effAnswerColor,
                    effDeclineColor,
                    effAnswerShape,
                    effDeclineShape
                )
            },
            content = {
                AppCallEditor(viewModel)
            }
        )
        CreatorRoute.APP_NAVIGATION -> Box(Modifier.fillMaxSize()) {
            NavCustomizationScreen(
                onBack = { onPopRoute(CreatorRoute.APP_NAVIGATION) },
                packageName = viewModel.editingAppPackage,
                showTopBar = false
            )
        }
        CreatorRoute.APP_REPLY -> Box(Modifier.fillMaxSize()) {
            ReplyStyleSheetContent(viewModel = viewModel)
        }
        CreatorRoute.APP_ACTIONS -> Box(Modifier.fillMaxSize()) {
            AppActionEditor(viewModel)
        }
    }
}

@Composable
fun SaveDialog(viewModel: ThemeViewModel, editThemeId: String?, activeThemeId: String?, onThemeCreated: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.creator_dialog_apply_title)) },
        text = { Text(stringResource(R.string.creator_dialog_apply_desc)) },
        confirmButton = {
            Button(onClick = {
                onDismiss()
                viewModel.saveTheme(existingId = editThemeId, apply = true)
                viewModel.currentEditingThemeId = null
                onThemeCreated()
            }) { Text(stringResource(R.string.creator_dialog_action_save_apply)) }
        },
        dismissButton = {
            TextButton(onClick = {
                onDismiss()
                viewModel.saveTheme(existingId = editThemeId, apply = false)
                viewModel.currentEditingThemeId = null
                onThemeCreated()
            }) { Text(stringResource(R.string.creator_dialog_action_save_only)) }
        }
    )
}

@Composable
fun CreatorMainList(
    viewModel: ThemeViewModel,
    selectedRoute: CreatorRoute? = null,
    onNavigate: (CreatorRoute) -> Unit,
    onEditSettings: () -> Unit
) {
    Column(modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())) {
        Box(modifier = Modifier
            .fillMaxWidth()
            .height(200.dp), contentAlignment = Alignment.Center) {
            Surface(modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 12.dp)) {
                    SharedThemePreview(
                        viewModel.selectedColorHex,
                        (viewModel.colorMode == ColorMode.APP_ICON),
                        viewModel.selectedShapeId, viewModel.iconPaddingPercent,
                        viewModel.callAnswerColor, viewModel.callDeclineColor, viewModel.callAnswerShapeId, viewModel.callDeclineShapeId
                    )
                }
            }
        }

        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)) {
            Button(onClick = onEditSettings, modifier = Modifier
                .fillMaxWidth()
                .height(52.dp), shape = RoundedCornerShape(24.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer), elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)) {
                Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.creator_btn_edit_info))
            }
            Spacer(Modifier.height(16.dp))

            val menuItems = listOf(
                CreatorRoute.BEHAVIOR_MENU,
                CreatorRoute.COLORS,
                CreatorRoute.ICONS,
                CreatorRoute.CALLS,
                CreatorRoute.NAVIGATION,
                CreatorRoute.REPLY,
                CreatorRoute.ACTIONS,
                CreatorRoute.APPS
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                menuItems.forEachIndexed { index, route ->
                    val isSelected = selectedRoute == route
                    val shape = if (isSelected) {
                        RoundedCornerShape(20.dp)
                    } else {
                        getExpressiveShape(menuItems.size, index, ShapeStyle.Large)
                    }

                    val icon = when(route) {
                        CreatorRoute.BEHAVIOR_MENU -> Icons.Outlined.DisplaySettings
                        CreatorRoute.COLORS -> Icons.Outlined.ColorLens
                        CreatorRoute.ICONS -> Icons.Outlined.Widgets
                        CreatorRoute.CALLS -> Icons.Outlined.Call
                        CreatorRoute.NAVIGATION -> Icons.Outlined.Map
                        CreatorRoute.REPLY -> Icons.Outlined.Edit
                        CreatorRoute.ACTIONS -> Icons.Outlined.TouchApp
                        CreatorRoute.APPS -> Icons.Outlined.Apps
                        else -> Icons.Outlined.Image
                    }
                    val title = when(route) {
                        CreatorRoute.BEHAVIOR_MENU -> stringResource(R.string.engine)
                        CreatorRoute.COLORS -> stringResource(R.string.creator_nav_colors)
                        CreatorRoute.ICONS -> stringResource(R.string.creator_nav_icons)
                        CreatorRoute.CALLS -> stringResource(R.string.creator_nav_calls)
                        CreatorRoute.NAVIGATION -> stringResource(R.string.nav_layout_title)
                        CreatorRoute.REPLY -> stringResource(R.string.inline_reply_title)
                        CreatorRoute.ACTIONS -> stringResource(R.string.creator_nav_actions)
                        CreatorRoute.APPS -> stringResource(R.string.creator_nav_apps)
                        else -> stringResource(R.string.app_name)
                    }
                    val sub = when(route) {
                        CreatorRoute.BEHAVIOR_MENU -> stringResource(R.string.engine_timeouts_triggers)
                        CreatorRoute.COLORS -> stringResource(R.string.creator_sub_colors)
                        CreatorRoute.ICONS -> stringResource(R.string.creator_sub_icons)
                        CreatorRoute.CALLS -> stringResource(R.string.creator_sub_calls)
                        CreatorRoute.NAVIGATION -> stringResource(R.string.nav_layout_desc)
                        CreatorRoute.REPLY -> stringResource(R.string.customize_inline_reply)
                        CreatorRoute.ACTIONS -> stringResource(R.string.creator_sub_actions)
                        CreatorRoute.APPS -> stringResource(R.string.creator_sub_apps)
                        else -> stringResource(R.string.app_name)
                    }

                    val trailing: (@Composable () -> Unit)? = when (route) {
                        CreatorRoute.COLORS -> {
                            {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(safeParseColor(viewModel.selectedColorHex))
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                )
                            }
                        }
                        CreatorRoute.ICONS -> {
                            {
                                Icon(
                                    Icons.Outlined.Image,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        else -> null
                    }

                    CreatorOptionCard(
                        title = title,
                        subtitle = sub,
                        icon = icon,
                        shape = shape,
                        onClick = { onNavigate(route) },
                        trailingContent = trailing,
                        selected = isSelected
                    )
                }
            }
            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
fun CreatorOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    shape: Shape,
    onClick: () -> Unit,
    trailingContent: (@Composable () -> Unit)? = null,
    selected: Boolean = false
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        shape = shape,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 76.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 76.dp)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (trailingContent != null) {
                trailingContent()
            } else {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowForwardIos,
                    null,
                    modifier = Modifier.size(16.dp),
                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun DetailScreenShell(previewContent: @Composable () -> Unit, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier
            .fillMaxWidth()
            .height(200.dp), contentAlignment = Alignment.Center) {
            Surface(modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 12.dp)) { previewContent() }
            }
        }
        Box(modifier = Modifier
            .weight(1f)
            .fillMaxWidth()) { content() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeMetadataSheet(viewModel: ThemeViewModel, onDismiss: () -> Unit) {
    val fm = LocalFocusManager.current
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val iconLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.PickVisualMedia()) { uri: Uri? -> if (uri != null) { viewModel.themeIconUri = uri } }
    var iconBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(viewModel.themeIconUri) {
        if (viewModel.themeIconUri != null) {
            withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(viewModel.themeIconUri!!)?.use { stream ->
                        val bmp = BitmapFactory.decodeStream(stream)
                        iconBitmap = bmp?.asImageBitmap()
                    }
                } catch (e: Exception) { e.printStackTrace(); iconBitmap = null }
            }
        } else { iconBitmap = null }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier
            .padding(24.dp)
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.meta_title), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(32.dp))
            Box(modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
                .clickable { iconLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, contentAlignment = Alignment.Center) {
                if (iconBitmap != null) { Image(bitmap = iconBitmap!!, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                else { Icon(Icons.Outlined.Image, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp)) }
            }
            Spacer(Modifier.height(24.dp))
            if (viewModel.themeIconUri != null) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalButton(onClick = { viewModel.themeIconUri = null }, modifier = Modifier.size(50.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer), contentPadding = PaddingValues(0.dp)) { Icon(Icons.Rounded.Delete, null, modifier = Modifier.size(20.dp)) }
                    Button(onClick = { iconLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, modifier = Modifier
                        .weight(1f)
                        .height(50.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)) { Icon(Icons.Rounded.Edit, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.change_icon)) }
                }
            } else {
                Button(onClick = { iconLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)) { Icon(Icons.Rounded.Image, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.select_icon)) }
            }
            Spacer(Modifier.height(32.dp))
            OutlinedTextField(value = viewModel.themeName, onValueChange = { viewModel.themeName = it }, label = { Text(stringResource(R.string.meta_label_name)) }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardActions = KeyboardActions(onDone = { fm.clearFocus() }))
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(value = viewModel.themeAuthor, onValueChange = { viewModel.themeAuthor = it }, label = { Text(stringResource(R.string.meta_label_author)) }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardActions = KeyboardActions(onDone = { fm.clearFocus() }))
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(value = viewModel.themeDescription, onValueChange = { viewModel.themeDescription = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 5)
            Spacer(Modifier.height(32.dp))
            Button(onClick = onDismiss, modifier = Modifier
                .fillMaxWidth()
                .height(50.dp), shape = ButtonDefaults.shape) { Text(stringResource(R.string.done)) }
            Spacer(Modifier.height(24.dp))
        }
    }
}