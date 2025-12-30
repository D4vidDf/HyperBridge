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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.ui.screens.theme.content.ColorsDetailContent
import com.d4viddf.hyperbridge.ui.screens.theme.content.IconsDetailContent

enum class CreatorRoute {
    MAIN_MENU, COLORS, ICONS, CALLS, APPS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeCreatorScreen(
    editThemeId: String? = null,
    onBack: () -> Unit,
    onThemeCreated: () -> Unit
) {
    val viewModel: ThemeViewModel = viewModel()
    var currentRoute by remember { mutableStateOf(CreatorRoute.MAIN_MENU) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    LaunchedEffect(editThemeId) {
        if (editThemeId != null) viewModel.loadThemeForEditing(editThemeId)
        else viewModel.clearCreatorState()
    }

    BackHandler(enabled = currentRoute != CreatorRoute.MAIN_MENU) {
        currentRoute = CreatorRoute.MAIN_MENU
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when(currentRoute) {
                            CreatorRoute.MAIN_MENU -> if (editThemeId == null) stringResource(R.string.creator_title_new) else stringResource(R.string.creator_title_edit)
                            CreatorRoute.COLORS -> stringResource(R.string.creator_nav_colors)
                            CreatorRoute.ICONS -> stringResource(R.string.creator_nav_icons)
                            CreatorRoute.CALLS -> stringResource(R.string.creator_nav_calls)
                            CreatorRoute.APPS -> stringResource(R.string.creator_nav_apps)
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    FilledTonalIconButton(
                        onClick = onBack,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            viewModel.saveTheme(editThemeId)
                            onThemeCreated()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(stringResource(R.string.creator_action_save), fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            AnimatedContent(
                targetState = currentRoute,
                transitionSpec = {
                    if (targetState == CreatorRoute.MAIN_MENU) {
                        slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                    } else {
                        slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                    }
                },
                label = "CreatorNav"
            ) { route ->
                when (route) {
                    CreatorRoute.MAIN_MENU -> CreatorMainList(
                        viewModel = viewModel,
                        onNavigate = { currentRoute = it },
                        onEditSettings = { showSettingsSheet = true }
                    )

                    CreatorRoute.COLORS -> DetailScreenShell(
                        previewContent = { ThemeCarouselPreview(viewModel) },
                        content = { ColorsDetailContent(viewModel) }
                    )

                    CreatorRoute.ICONS -> DetailScreenShell(
                        previewContent = { IconsSpecificPreview(viewModel) },
                        content = { IconsDetailContent(viewModel) }
                    )

                    CreatorRoute.CALLS -> DetailScreenShell(
                        previewContent = { CallSpecificPreview(viewModel) },
                        content = { CallStyleSheetContent(viewModel) }
                    )

                    CreatorRoute.APPS -> DetailScreenShell(
                        previewContent = { ThemeCarouselPreview(viewModel) },
                        content = { AppsDetailContent(viewModel) }
                    )
                }
            }
        }

        if (showSettingsSheet) {
            ThemeMetadataSheet(viewModel) { showSettingsSheet = false }
        }
    }
}

@Composable
private fun CreatorMainList(
    viewModel: ThemeViewModel,
    onNavigate: (CreatorRoute) -> Unit,
    onEditSettings: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.35f),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 24.dp)) {
                    ThemeCarouselPreview(viewModel)
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(0.65f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onEditSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.creator_btn_edit_info))
            }

            Spacer(Modifier.height(16.dp))

            val menuItems = listOf(
                CreatorRoute.COLORS,
                CreatorRoute.ICONS,
                CreatorRoute.CALLS,
                CreatorRoute.APPS
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                menuItems.forEachIndexed { index, route ->
                    val shape = getExpressiveShape(menuItems.size, index, ShapeStyle.Large)

                    when(route) {
                        CreatorRoute.COLORS -> CreatorOptionCard(
                            title = stringResource(R.string.creator_nav_colors),
                            subtitle = stringResource(R.string.creator_sub_colors),
                            icon = Icons.Outlined.ColorLens,
                            shape = shape,
                            onClick = { onNavigate(route) },
                            trailingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(safeParseColor(viewModel.selectedColorHex))
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                )
                            }
                        )
                        CreatorRoute.ICONS -> CreatorOptionCard(
                            title = stringResource(R.string.creator_nav_icons),
                            subtitle = stringResource(R.string.creator_sub_icons),
                            icon = Icons.Outlined.Widgets,
                            shape = shape,
                            onClick = { onNavigate(route) },
                            trailingContent = {
                                Icon(Icons.Outlined.Image, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            }
                        )
                        CreatorRoute.CALLS -> CreatorOptionCard(
                            title = stringResource(R.string.creator_nav_calls),
                            subtitle = stringResource(R.string.creator_sub_calls),
                            icon = Icons.Outlined.Call,
                            shape = shape,
                            onClick = { onNavigate(route) }
                        )
                        CreatorRoute.APPS -> CreatorOptionCard(
                            title = stringResource(R.string.creator_nav_apps),
                            subtitle = stringResource(R.string.creator_sub_apps),
                            icon = Icons.Outlined.Apps,
                            shape = shape,
                            onClick = { onNavigate(route) }
                        )
                        else -> {}
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CreatorOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    shape: Shape,
    onClick: () -> Unit,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = shape,
        modifier = Modifier.fillMaxWidth().heightIn(min = 88.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (trailingContent != null) {
                trailingContent()
            } else {
                Icon(Icons.AutoMirrored.Rounded.ArrowForwardIos, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
private fun DetailScreenShell(
    previewContent: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(vertical = 24.dp)
                ) {
                    previewContent()
                }
            }
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeMetadataSheet(viewModel: ThemeViewModel, onDismiss: () -> Unit) {
    val fm = LocalFocusManager.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(24.dp).navigationBarsPadding()) {
            Text(stringResource(R.string.meta_title), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = viewModel.themeName,
                onValueChange = { viewModel.themeName = it },
                label = { Text(stringResource(R.string.meta_label_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardActions = KeyboardActions(onDone = { fm.clearFocus() })
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = viewModel.themeAuthor,
                onValueChange = { viewModel.themeAuthor = it },
                label = { Text(stringResource(R.string.meta_label_author)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardActions = KeyboardActions(onDone = { fm.clearFocus() })
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text(stringResource(R.string.meta_action_done))
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

sealed class ShapeStyle(
    val topRadius: Dp,
    val bottomRadius: Dp
) {
    data object None : ShapeStyle(topRadius = 0.dp, bottomRadius = 0.dp)
    data object ExtraSmall : ShapeStyle(topRadius = 2.dp, bottomRadius = 1.dp)
    data object Small : ShapeStyle(topRadius = 4.dp, bottomRadius = 2.dp)
    data object Medium : ShapeStyle(topRadius = 15.dp, bottomRadius = 5.dp)
    data object Large : ShapeStyle(topRadius = 24.dp, bottomRadius = 4.dp)
    data object ExtraLarge : ShapeStyle(topRadius = 48.dp, bottomRadius = 16.dp)
}

fun getExpressiveShape(
    groupSize: Int,
    index: Int,
    style: ShapeStyle = ShapeStyle.Large
): Shape {
    if (groupSize <= 1) return RoundedCornerShape(style.topRadius)

    val large = style.topRadius
    val small = style.bottomRadius

    return when (index) {
        0 -> RoundedCornerShape(
            topStart = large,
            topEnd = large,
            bottomEnd = small,
            bottomStart = small
        )
        groupSize - 1 -> RoundedCornerShape(
            topStart = small,
            topEnd = small,
            bottomEnd = large,
            bottomStart = large
        )
        else -> RoundedCornerShape(small)
    }
}