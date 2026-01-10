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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.TouchApp
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.ui.screens.theme.content.ActionsDetailContent
import com.d4viddf.hyperbridge.ui.screens.theme.content.AppsDetailContent
import com.d4viddf.hyperbridge.ui.screens.theme.content.CallStyleSheetContent
import com.d4viddf.hyperbridge.ui.screens.theme.content.ColorsDetailContent
import com.d4viddf.hyperbridge.ui.screens.theme.content.IconsDetailContent
import com.d4viddf.hyperbridge.ui.screens.theme.content.SharedThemePreview
import com.d4viddf.hyperbridge.ui.screens.theme.content.safeParseColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class CreatorRoute {
    MAIN_MENU, COLORS, ICONS, CALLS, ACTIONS, APPS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeCreatorScreen(
    editThemeId: String? = null,
    onBack: () -> Unit,
    onThemeCreated: () -> Unit
) {
    val viewModel: ThemeViewModel = viewModel()
    val activeThemeId by viewModel.activeThemeId.collectAsState()

    // [SWITCH] If editing app -> Show AppThemeEditorScreen
    if (viewModel.editingAppPackage != null) {
        AppThemeEditor(viewModel)
    } else {
        // --- GLOBAL THEME CREATOR ---
        var currentRoute by remember { mutableStateOf(CreatorRoute.MAIN_MENU) }
        var showSettingsSheet by remember { mutableStateOf(false) }
        var showSaveDialog by remember { mutableStateOf(false) }

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
                            text = when (currentRoute) {
                                CreatorRoute.MAIN_MENU -> if (editThemeId == null) stringResource(R.string.creator_title_new) else stringResource(R.string.creator_title_edit)
                                CreatorRoute.COLORS -> stringResource(R.string.creator_nav_colors)
                                CreatorRoute.ICONS -> stringResource(R.string.creator_nav_icons)
                                CreatorRoute.CALLS -> stringResource(R.string.creator_nav_calls)
                                CreatorRoute.ACTIONS -> stringResource(R.string.creator_nav_actions)
                                CreatorRoute.APPS -> stringResource(R.string.creator_nav_apps)
                            },
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        FilledTonalIconButton(
                            onClick = {
                                if (currentRoute != CreatorRoute.MAIN_MENU) currentRoute = CreatorRoute.MAIN_MENU else onBack()
                            },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    },
                    actions = {
                        if (currentRoute == CreatorRoute.MAIN_MENU) {
                            Button(
                                onClick = {
                                    if (editThemeId != null && editThemeId == activeThemeId) {
                                        viewModel.saveTheme(editThemeId)
                                        onThemeCreated()
                                    } else {
                                        showSaveDialog = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                                modifier = Modifier.padding(end = 8.dp)
                            ) { Text(stringResource(R.string.creator_action_save), fontWeight = FontWeight.Bold) }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            },
            containerColor = MaterialTheme.colorScheme.surface
        ) { padding ->
            Box(modifier = Modifier.padding(top = padding.calculateTopPadding()).fillMaxSize()) {
                AnimatedContent(
                    targetState = currentRoute,
                    transitionSpec = {
                        if (targetState == CreatorRoute.MAIN_MENU) slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                        else slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
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
                            previewContent = {
                                SharedThemePreview(
                                    highlightColorHex = viewModel.selectedColorHex,
                                    useAppColors = viewModel.useAppColors,
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
                                    useAppColors = viewModel.useAppColors,
                                    onColorSelected = { viewModel.selectedColorHex = it },
                                    onUseAppColorsChanged = { viewModel.useAppColors = it }
                                )
                            }
                        )
                        CreatorRoute.ICONS -> DetailScreenShell(
                            previewContent = {
                                SharedThemePreview(
                                    highlightColorHex = viewModel.selectedColorHex,
                                    useAppColors = viewModel.useAppColors,
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
                                    useAppColors = viewModel.useAppColors,
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
                            AppsDetailContent(viewModel)
                        }
                    }
                }
            }

            if (showSettingsSheet) ThemeMetadataSheet(viewModel) { showSettingsSheet = false }
            if (showSaveDialog) SaveDialog(viewModel, editThemeId, activeThemeId, onThemeCreated) { showSaveDialog = false }
        }
    }
}

@Composable
fun SaveDialog(viewModel: ThemeViewModel, editThemeId: String?, activeThemeId: String?, onThemeCreated: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.creator_dialog_apply_title)) },
        text = { Text(stringResource(R.string.creator_dialog_apply_desc)) },
        confirmButton = { Button(onClick = { onDismiss(); viewModel.saveTheme(editThemeId, apply = true); onThemeCreated() }) { Text(stringResource(R.string.creator_dialog_action_save_apply)) } },
        dismissButton = { TextButton(onClick = { onDismiss(); viewModel.saveTheme(editThemeId, apply = false); onThemeCreated() }) { Text(stringResource(R.string.creator_dialog_action_save_only)) } }
    )
}

@Composable
fun CreatorMainList(viewModel: ThemeViewModel, onNavigate: (CreatorRoute) -> Unit, onEditSettings: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // [FIX] Matched height (200dp) and padding (top = 8dp) to DetailScreenShell for alignment
        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 12.dp)) {
                    SharedThemePreview(
                        viewModel.selectedColorHex, viewModel.useAppColors, viewModel.selectedShapeId, viewModel.iconPaddingPercent,
                        viewModel.callAnswerColor, viewModel.callDeclineColor, viewModel.callAnswerShapeId, viewModel.callDeclineShapeId
                    )
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Button(onClick = onEditSettings, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(24.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer), elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)) {
                Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.creator_btn_edit_info))
            }
            Spacer(Modifier.height(16.dp))

            // [FIX] Spacing set to 2.dp as requested
            val menuItems = CreatorRoute.entries.filter { it != CreatorRoute.MAIN_MENU }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                menuItems.forEachIndexed { index, route ->
                    val shape = getExpressiveShape(menuItems.size, index, ShapeStyle.Large)
                    val icon = when(route) { CreatorRoute.COLORS -> Icons.Outlined.ColorLens; CreatorRoute.ICONS -> Icons.Outlined.Widgets; CreatorRoute.CALLS -> Icons.Outlined.Call; CreatorRoute.ACTIONS -> Icons.Outlined.TouchApp; CreatorRoute.APPS -> Icons.Outlined.Apps; else -> Icons.Outlined.Image }
                    val title = stringResource(when(route) { CreatorRoute.COLORS -> R.string.creator_nav_colors; CreatorRoute.ICONS -> R.string.creator_nav_icons; CreatorRoute.CALLS -> R.string.creator_nav_calls; CreatorRoute.ACTIONS -> R.string.creator_nav_actions; CreatorRoute.APPS -> R.string.creator_nav_apps; else -> R.string.app_name })
                    val sub = stringResource(when(route) { CreatorRoute.COLORS -> R.string.creator_sub_colors; CreatorRoute.ICONS -> R.string.creator_sub_icons; CreatorRoute.CALLS -> R.string.creator_sub_calls; CreatorRoute.ACTIONS -> R.string.creator_sub_actions; CreatorRoute.APPS -> R.string.creator_sub_apps; else -> R.string.app_name })

                    val trailing: (@Composable () -> Unit)? = if (route == CreatorRoute.COLORS) {
                        { Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(safeParseColor(viewModel.selectedColorHex)).border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)) }
                    } else if (route == CreatorRoute.ICONS) {
                        { Icon(Icons.Outlined.Image, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)) }
                    } else null

                    CreatorOptionCard(title, sub, icon, shape, { onNavigate(route) }, trailing)
                }
            }
            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
fun CreatorOptionCard(title: String, subtitle: String, icon: ImageVector, shape: Shape, onClick: () -> Unit, trailingContent: (@Composable () -> Unit)? = null) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), shape = shape, modifier = Modifier.fillMaxWidth().heightIn(min = 88.dp)) {
        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (trailingContent != null) trailingContent() else Icon(Icons.AutoMirrored.Rounded.ArrowForwardIos, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        }
    }
}

@Composable
fun DetailScreenShell(previewContent: @Composable () -> Unit, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
            Surface(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 12.dp)) { previewContent() }
            }
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) { content() }
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
        Column(modifier = Modifier.padding(24.dp).navigationBarsPadding().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.meta_title), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(32.dp))
            Box(modifier = Modifier.size(120.dp).clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp)).clickable { iconLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, contentAlignment = Alignment.Center) {
                if (iconBitmap != null) { Image(bitmap = iconBitmap!!, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                else { Icon(Icons.Outlined.Image, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp)) }
            }
            Spacer(Modifier.height(24.dp))
            if (viewModel.themeIconUri != null) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalButton(onClick = { viewModel.themeIconUri = null }, modifier = Modifier.size(50.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer), contentPadding = PaddingValues(0.dp)) { Icon(Icons.Rounded.Delete, null, modifier = Modifier.size(20.dp)) }
                    Button(onClick = { iconLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)) { Icon(Icons.Rounded.Edit, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.change_icon)) }
                }
            } else {
                Button(onClick = { iconLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)) { Icon(Icons.Rounded.Image, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.select_icon)) }
            }
            Spacer(Modifier.height(32.dp))
            OutlinedTextField(value = viewModel.themeName, onValueChange = { viewModel.themeName = it }, label = { Text(stringResource(R.string.meta_label_name)) }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardActions = KeyboardActions(onDone = { fm.clearFocus() }))
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(value = viewModel.themeAuthor, onValueChange = { viewModel.themeAuthor = it }, label = { Text(stringResource(R.string.meta_label_author)) }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardActions = KeyboardActions(onDone = { fm.clearFocus() }))
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(value = viewModel.themeDescription, onValueChange = { viewModel.themeDescription = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 5)
            Spacer(Modifier.height(32.dp))
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(50.dp), shape = ButtonDefaults.shape) { Text(stringResource(R.string.meta_action_done)) }
            Spacer(Modifier.height(24.dp))
        }
    }
}
