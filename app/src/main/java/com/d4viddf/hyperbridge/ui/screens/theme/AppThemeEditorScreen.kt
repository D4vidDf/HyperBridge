package com.d4viddf.hyperbridge.ui.screens.theme

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.ui.screens.theme.content.ActionConfigSheet
import com.d4viddf.hyperbridge.ui.screens.theme.content.ActionsDetailContent
import com.d4viddf.hyperbridge.ui.screens.theme.content.CallStyleSheetContent
import com.d4viddf.hyperbridge.ui.screens.theme.content.ColorsDetailContent
import com.d4viddf.hyperbridge.ui.screens.theme.content.IconsDetailContent
import com.d4viddf.hyperbridge.ui.screens.theme.content.SharedThemePreview
import com.d4viddf.hyperbridge.ui.screens.theme.content.safeParseColor

enum class AppEditorRoute {
    MENU, COLORS, ICONS, CALLS, ACTIONS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppThemeEditor(viewModel: ThemeViewModel) {
    var currentRoute by remember { mutableStateOf(AppEditorRoute.MENU) }

    BackHandler {
        if (currentRoute != AppEditorRoute.MENU) {
            currentRoute = AppEditorRoute.MENU
        } else {
            viewModel.cancelAppEditing()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (currentRoute == AppEditorRoute.MENU) stringResource(R.string.edit_app) else stringResource(
                                when (currentRoute) {
                                    AppEditorRoute.COLORS -> R.string.creator_nav_colors
                                    AppEditorRoute.ICONS -> R.string.creator_nav_icons
                                    AppEditorRoute.CALLS -> R.string.creator_nav_calls
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
                        onClick = {
                            if (currentRoute != AppEditorRoute.MENU) {
                                currentRoute = AppEditorRoute.MENU
                            } else {
                                viewModel.cancelAppEditing()
                            }
                        },
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
        Box(modifier = Modifier.padding(top = padding.calculateTopPadding()).fillMaxSize()) {
            AnimatedContent(
                targetState = currentRoute,
                transitionSpec = {
                    if (targetState == AppEditorRoute.MENU) {
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
                    AppEditorRoute.ACTIONS -> Box(Modifier.fillMaxSize()) {
                        AppActionEditor(viewModel)
                    }
                }
            }
        }
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
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
    ) {
        // [FIXED] Padding and Height to match DetailScreenShell
        Box(
            modifier = Modifier.fillMaxWidth().height(200.dp),
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
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp) // [FIXED] Proper separation spacing
        ) {
            val items = listOf(
                AppEditorRoute.COLORS,
                AppEditorRoute.ICONS,
                AppEditorRoute.CALLS,
                AppEditorRoute.ACTIONS
            )

            items.forEachIndexed { index, route ->
                val shape = getExpressiveShape(items.size, index, ShapeStyle.Large)
                when(route) {
                    AppEditorRoute.COLORS -> CreatorOptionCard(
                        title = stringResource(R.string.creator_nav_colors),
                        subtitle = if (viewModel.appHighlightColor != null) stringResource(R.string.creator_sub_colors_custom) else stringResource(R.string.creator_sub_colors_default),
                        icon = Icons.Outlined.ColorLens,
                        shape = shape,
                        onClick = { onNavigate(route) },
                        trailingContent = {
                            Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(safeParseColor(effHighlight)).border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape))
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
        // [FIX] Correctly bind to the app-specific usage flag
        useAppColors = viewModel.appUseAppColors == true,
        onColorSelected = {
            viewModel.appHighlightColor = it
            // If user manually picks a color, typically disable "use app colors"
            viewModel.appUseAppColors = false
        },
        onUseAppColorsChanged = { isEnabled ->
            viewModel.appUseAppColors = isEnabled
        }
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