package com.d4viddf.hyperbridge.ui.screens.translators

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Subject
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.DisplaySettings
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhoneInTalk
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.ViewQuilt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.models.NotificationType
import com.d4viddf.hyperbridge.models.translator.ActionMatchBy
import com.d4viddf.hyperbridge.models.translator.ActionMatcher
import com.d4viddf.hyperbridge.models.translator.ActionSlotConfig
import com.d4viddf.hyperbridge.models.translator.ActionSource
import com.d4viddf.hyperbridge.models.translator.BehaviorOverride
import com.d4viddf.hyperbridge.models.translator.CallConditions
import com.d4viddf.hyperbridge.models.translator.CallTypeCondition
import com.d4viddf.hyperbridge.models.translator.CustomTranslator
import com.d4viddf.hyperbridge.models.translator.DataExtractionConfig
import com.d4viddf.hyperbridge.models.translator.EngineMode
import com.d4viddf.hyperbridge.models.translator.MediaConditions
import com.d4viddf.hyperbridge.models.translator.MessagingConditions
import com.d4viddf.hyperbridge.models.translator.NavigationConditions
import com.d4viddf.hyperbridge.models.translator.PresentationConfig
import com.d4viddf.hyperbridge.models.translator.PresentationMode
import com.d4viddf.hyperbridge.models.translator.ProgressConditions
import com.d4viddf.hyperbridge.models.translator.ProgressSlotConfig
import com.d4viddf.hyperbridge.models.translator.ProgressSlotType
import com.d4viddf.hyperbridge.models.translator.SmartActionType
import com.d4viddf.hyperbridge.models.translator.TargetScope
import com.d4viddf.hyperbridge.models.translator.TextSlotConfig
import com.d4viddf.hyperbridge.models.translator.TranslatorConditions
import com.d4viddf.hyperbridge.models.translator.TranslatorMetadata
import com.d4viddf.hyperbridge.models.translator.TypeSpecificConditions
import com.d4viddf.hyperbridge.ui.components.EmptyState
import com.d4viddf.hyperbridge.ui.screens.theme.AppItem
import com.d4viddf.hyperbridge.ui.screens.theme.ShapeStyle
import com.d4viddf.hyperbridge.ui.screens.theme.content.AppIcon
import com.d4viddf.hyperbridge.ui.screens.theme.content.AppSelectionSheet
import com.d4viddf.hyperbridge.ui.screens.theme.getExpressiveShape
import java.util.UUID

enum class TranslatorRoute {
    MAIN_MENU, CONDITIONS, PRESENTATION, PROGRESS, ACTIONS, BEHAVIOR, APPS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslatorEditorScreen(
    translatorId: String? = null,
    initialPackageName: String? = null,
    onBack: () -> Unit,
    viewModel: TranslatorViewModel = viewModel()
) {
    var translator by remember {
        mutableStateOf(
            CustomTranslator(
                id = translatorId ?: UUID.randomUUID().toString(),
                meta = TranslatorMetadata(
                    name = if (translatorId == null) "New Translator" else "",
                    author = "User",
                    description = ""
                ),
                targetScope = if (initialPackageName != null) TargetScope.SPECIFIC_APPS else TargetScope.GLOBAL,
                targetPackages = if (initialPackageName != null) listOf(initialPackageName) else emptyList()
            )
        )
    }

    val installedApps by viewModel.installedApps.collectAsState()

    LaunchedEffect(translatorId) {
        if (translatorId != null) {
            val loaded = viewModel.getTranslatorById(translatorId)
            if (loaded != null) {
                translator = loaded
            }
        }
    }

    TranslatorEditorContent(
        translator = translator,
        onTranslatorChange = { translator = it },
        installedApps = installedApps,
        onFetchChannels = { pkgs -> viewModel.getNotificationChannelsForPackages(pkgs) },
        onSave = {
            viewModel.saveTranslator(translator) {
                onBack()
            }
        },
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslatorEditorContent(
    translator: CustomTranslator,
    onTranslatorChange: (CustomTranslator) -> Unit,
    installedApps: List<AppItem>,
    onFetchChannels: suspend (List<String>) -> List<NotificationChannelInfo> = { emptyList() },
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    var currentRoute by remember { mutableStateOf(TranslatorRoute.MAIN_MENU) }
    var showMetaSheet by remember { mutableStateOf(false) }

    val handleBackNavigation = {
        if (currentRoute == TranslatorRoute.MAIN_MENU) {
            onBack()
        } else {
            currentRoute = TranslatorRoute.MAIN_MENU
        }
    }

    BackHandler(enabled = true) {
        handleBackNavigation()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (currentRoute) {
                            TranslatorRoute.MAIN_MENU -> if (translator.meta.name.isBlank()) stringResource(R.string.translator_editor_title_edit) else translator.meta.name
                            TranslatorRoute.CONDITIONS -> stringResource(R.string.translator_menu_conditions)
                            TranslatorRoute.PRESENTATION -> stringResource(R.string.translator_menu_presentation)
                            TranslatorRoute.PROGRESS -> stringResource(R.string.translator_menu_progress)
                            TranslatorRoute.ACTIONS -> stringResource(R.string.translator_menu_actions)
                            TranslatorRoute.BEHAVIOR -> stringResource(R.string.translator_menu_behavior)
                            TranslatorRoute.APPS -> stringResource(R.string.translator_menu_apps)
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    FilledTonalIconButton(
                        onClick = handleBackNavigation,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (currentRoute == TranslatorRoute.MAIN_MENU) {
                        Button(
                            onClick = onSave,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(stringResource(R.string.creator_action_save), fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(top = padding.calculateTopPadding())
                .fillMaxSize()
        ) {
            AnimatedContent(
                targetState = currentRoute,
                transitionSpec = {
                    if (targetState == TranslatorRoute.MAIN_MENU) {
                        slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                    } else {
                        slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                    }
                },
                label = "TranslatorNav"
            ) { route ->
                when (route) {
                    TranslatorRoute.MAIN_MENU -> TranslatorMainList(
                        translator = translator,
                        onNavigate = { currentRoute = it },
                        onEditMetadata = { showMetaSheet = true }
                    )
                    TranslatorRoute.CONDITIONS -> TranslatorDetailShell(
                        previewContent = { TranslatorLivePreviewBar(translator) }
                    ) {
                        TranslatorConditionsContent(
                            conditions = translator.conditions,
                            targetPackages = translator.targetPackages,
                            targetNotificationTypes = translator.targetNotificationTypes,
                            onFetchChannels = onFetchChannels,
                            onChange = { onTranslatorChange(translator.copy(conditions = it)) }
                        )
                    }
                    TranslatorRoute.PRESENTATION -> TranslatorDetailShell(
                        previewContent = { TranslatorLivePreviewBar(translator) }
                    ) {
                        TranslatorPresentationContent(
                            presentation = translator.presentation,
                            onChange = { onTranslatorChange(translator.copy(presentation = it)) }
                        )
                    }
                    TranslatorRoute.PROGRESS -> TranslatorDetailShell(
                        previewContent = { TranslatorLivePreviewBar(translator) }
                    ) {
                        TranslatorProgressContent(
                            progressSlot = translator.presentation.progressSlot,
                            dataExtraction = translator.dataExtraction,
                            onProgressSlotChange = { onTranslatorChange(translator.copy(presentation = translator.presentation.copy(progressSlot = it))) },
                            onDataExtractionChange = { onTranslatorChange(translator.copy(dataExtraction = it)) }
                        )
                    }
                    TranslatorRoute.ACTIONS -> TranslatorDetailShell(
                        previewContent = { TranslatorLivePreviewBar(translator) }
                    ) {
                        TranslatorActionsContent(
                            actionSlots = translator.presentation.actionSlots,
                            onChange = { onTranslatorChange(translator.copy(presentation = translator.presentation.copy(actionSlots = it))) }
                        )
                    }
                    TranslatorRoute.BEHAVIOR -> TranslatorDetailShell(
                        previewContent = { TranslatorLivePreviewBar(translator) }
                    ) {
                        TranslatorBehaviorContent(
                            behavior = translator.behaviorOverride,
                            engineMode = translator.engineMode,
                            onBehaviorChange = { onTranslatorChange(translator.copy(behaviorOverride = it)) },
                            onEngineModeChange = { onTranslatorChange(translator.copy(engineMode = it)) }
                        )
                    }
                    TranslatorRoute.APPS -> Box(Modifier.fillMaxSize()) {
                        TranslatorAppsContent(
                            translator = translator,
                            installedApps = installedApps,
                            onTargetScopeChange = { scope, packages ->
                                onTranslatorChange(translator.copy(targetScope = scope, targetPackages = packages))
                            },
                            onTargetNotificationTypesChange = { types ->
                                onTranslatorChange(translator.copy(targetNotificationTypes = types))
                            }
                        )
                    }
                }
            }
        }

        if (showMetaSheet) {
            TranslatorMetadataSheet(
                translator = translator,
                onTranslatorChange = onTranslatorChange,
                onDismiss = { showMetaSheet = false }
            )
        }
    }
}

@Composable
fun TranslatorMainList(
    translator: CustomTranslator,
    onNavigate: (TranslatorRoute) -> Unit,
    onEditMetadata: () -> Unit
) {
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
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(vertical = 12.dp)
                ) {
                    TranslatorLivePreviewBar(translator)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Button(
                onClick = onEditMetadata,
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
                Text(stringResource(R.string.translator_btn_edit_meta))
            }

            Spacer(Modifier.height(16.dp))

            val menuItems = listOf(
                TranslatorRoute.APPS,
                TranslatorRoute.CONDITIONS,
                TranslatorRoute.PRESENTATION,
                TranslatorRoute.PROGRESS,
                TranslatorRoute.ACTIONS,
                TranslatorRoute.BEHAVIOR
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                menuItems.forEachIndexed { index, route ->
                    val shape = getExpressiveShape(menuItems.size, index, ShapeStyle.Large)

                    val icon = when (route) {
                        TranslatorRoute.APPS -> Icons.Outlined.Apps
                        TranslatorRoute.CONDITIONS -> Icons.Outlined.FilterList
                        TranslatorRoute.PRESENTATION -> Icons.Outlined.ViewQuilt
                        TranslatorRoute.PROGRESS -> Icons.Outlined.Speed
                        TranslatorRoute.ACTIONS -> Icons.Outlined.TouchApp
                        TranslatorRoute.BEHAVIOR -> Icons.Outlined.DisplaySettings
                        else -> Icons.Outlined.AutoAwesome
                    }

                    val title = when (route) {
                        TranslatorRoute.APPS -> stringResource(R.string.translator_menu_apps)
                        TranslatorRoute.CONDITIONS -> stringResource(R.string.translator_menu_conditions)
                        TranslatorRoute.PRESENTATION -> stringResource(R.string.translator_menu_presentation)
                        TranslatorRoute.PROGRESS -> stringResource(R.string.translator_menu_progress)
                        TranslatorRoute.ACTIONS -> stringResource(R.string.translator_menu_actions)
                        TranslatorRoute.BEHAVIOR -> stringResource(R.string.translator_menu_behavior)
                        else -> ""
                    }

                    val subtitle = when (route) {
                        TranslatorRoute.APPS -> if (translator.targetScope == TargetScope.GLOBAL) {
                            stringResource(R.string.translator_scope_global)
                        } else {
                            stringResource(R.string.translator_scope_apps_fmt, translator.targetPackages.size)
                        }
                        TranslatorRoute.CONDITIONS -> stringResource(R.string.translator_menu_conditions_sub)
                        TranslatorRoute.PRESENTATION -> stringResource(R.string.translator_menu_presentation_sub)
                        TranslatorRoute.PROGRESS -> stringResource(R.string.translator_menu_progress_sub)
                        TranslatorRoute.ACTIONS -> stringResource(R.string.translator_menu_actions_sub)
                        TranslatorRoute.BEHAVIOR -> stringResource(R.string.translator_menu_behavior_sub)
                        else -> ""
                    }

                    TranslatorOptionCard(
                        title = title,
                        subtitle = subtitle,
                        icon = icon,
                        shape = shape,
                        onClick = { onNavigate(route) }
                    )
                }
            }

            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
fun TranslatorOptionCard(
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
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (trailingContent != null) {
                trailingContent()
            } else {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowForwardIos,
                    null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun TranslatorDetailShell(
    previewContent: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
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
                    previewContent()
                }
            }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            content()
        }
    }
}

@Composable
fun TranslatorLivePreviewBar(translator: CustomTranslator) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Black),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = translator.presentation.textSlot.titleTemplate
                        .replace("{notif.title}", "Sample Title")
                        .replace("{notif.text}", "Sample Notification Message"),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = translator.presentation.textSlot.subtitleTemplate
                        .replace("{notif.title}", "Sample Title")
                        .replace("{notif.text}", "Sample Notification Message"),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray,
                    maxLines = 1
                )
            }

            if (translator.presentation.progressSlot.type != ProgressSlotType.NONE) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Speed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            if (translator.presentation.actionSlots.isNotEmpty()) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun TranslatorConditionsContent(
    conditions: TranslatorConditions,
    targetPackages: List<String> = emptyList(),
    targetNotificationTypes: List<String> = emptyList(),
    onFetchChannels: suspend (List<String>) -> List<NotificationChannelInfo> = { emptyList() },
    onChange: (TranslatorConditions) -> Unit
) {
    var showAddPatternSheet by remember { mutableStateOf(false) }
    var showChannelPickerSheet by remember { mutableStateOf(false) }

    val typeConds = conditions.typeSpecificConditions ?: TypeSpecificConditions()
    val messaging = typeConds.messaging
    val media = typeConds.media
    val call = typeConds.call
    val nav = typeConds.navigation
    val progress = typeConds.progress

    val hasAnyConditions = conditions.titleRegex != null ||
            conditions.textRegex != null ||
            conditions.subtextRegex != null ||
            conditions.channelId != null ||
            conditions.category != null ||
            conditions.hasProgress != null ||
            conditions.hasActions != null ||
            messaging?.senderNameRegex != null ||
            messaging?.conversationTitleRegex != null ||
            messaging?.isGroupConversation != null ||
            media?.artistRegex != null ||
            media?.albumRegex != null ||
            media?.isPlaying != null ||
            call?.callerNameRegex != null ||
            call?.callType != null ||
            nav?.instructionRegex != null ||
            nav?.distanceRegex != null ||
            progress?.minProgressPercent != null ||
            progress?.maxProgressPercent != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.translator_conditions_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.translator_conditions_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Add Pattern Action Button
        Button(
            onClick = { showAddPatternSheet = true },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.translator_cond_add_btn), fontWeight = FontWeight.SemiBold)
        }

        if (!hasAnyConditions) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Outlined.FilterList,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = stringResource(R.string.translator_cond_empty_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.translator_cond_empty_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Standard Conditions
        if (conditions.titleRegex != null) {
            ConditionPatternCard(
                title = stringResource(R.string.translator_cond_title_regex),
                icon = Icons.Outlined.Title,
                onRemove = { onChange(conditions.copy(titleRegex = null)) }
            ) {
                OutlinedTextField(
                    value = conditions.titleRegex,
                    onValueChange = { onChange(conditions.copy(titleRegex = it)) },
                    placeholder = { Text(stringResource(R.string.translator_cond_title_regex_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        if (conditions.textRegex != null) {
            ConditionPatternCard(
                title = stringResource(R.string.translator_cond_text_regex),
                icon = Icons.AutoMirrored.Outlined.Subject,
                onRemove = { onChange(conditions.copy(textRegex = null)) }
            ) {
                OutlinedTextField(
                    value = conditions.textRegex,
                    onValueChange = { onChange(conditions.copy(textRegex = it)) },
                    placeholder = { Text(stringResource(R.string.translator_cond_text_regex_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        if (conditions.subtextRegex != null) {
            ConditionPatternCard(
                title = stringResource(R.string.translator_cond_subtext_regex),
                icon = Icons.Outlined.Subtitles,
                onRemove = { onChange(conditions.copy(subtextRegex = null)) }
            ) {
                OutlinedTextField(
                    value = conditions.subtextRegex,
                    onValueChange = { onChange(conditions.copy(subtextRegex = it)) },
                    placeholder = { Text(stringResource(R.string.translator_cond_subtext_regex_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        if (conditions.channelId != null) {
            ConditionPatternCard(
                title = stringResource(R.string.translator_cond_channel),
                icon = Icons.Outlined.Tune,
                onRemove = { onChange(conditions.copy(channelId = null)) }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { showChannelPickerSheet = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(Icons.Outlined.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (conditions.channelId.isNotBlank()) conditions.channelId else stringResource(R.string.translator_cond_channel_select_btn),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Text(
                        text = stringResource(R.string.translator_cond_channel_custom_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = conditions.channelId,
                        onValueChange = { onChange(conditions.copy(channelId = it)) },
                        placeholder = { Text(stringResource(R.string.translator_cond_channel_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        if (conditions.category != null) {
            ConditionPatternCard(
                title = stringResource(R.string.translator_cond_category_title),
                icon = Icons.Outlined.Category,
                onRemove = { onChange(conditions.copy(category = null)) }
            ) {
                val knownCategories = listOf(
                    "msg" to R.string.translator_cond_category_msg,
                    "email" to R.string.translator_cond_category_email,
                    "call" to R.string.translator_cond_category_call,
                    "alarm" to R.string.translator_cond_category_alarm,
                    "progress" to R.string.translator_cond_category_progress,
                    "transport" to R.string.translator_cond_category_transport,
                    "navigation" to R.string.translator_cond_category_navigation,
                    "promo" to R.string.translator_cond_category_promo,
                    "service" to R.string.translator_cond_category_service,
                    "event" to R.string.translator_cond_category_event,
                    "social" to R.string.translator_cond_category_social,
                    "reminder" to R.string.translator_cond_category_reminder,
                    "sys" to R.string.translator_cond_category_system
                )

                val isCustom = conditions.category.isNotBlank() && knownCategories.none { it.first == conditions.category }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.translator_cond_category_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        knownCategories.forEach { (catKey, labelRes) ->
                            FilterChip(
                                selected = conditions.category == catKey,
                                onClick = { onChange(conditions.copy(category = catKey)) },
                                label = { Text(stringResource(labelRes), style = MaterialTheme.typography.bodySmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = if (isCustom) conditions.category else "",
                        onValueChange = { onChange(conditions.copy(category = it)) },
                        placeholder = { Text(stringResource(R.string.translator_cond_category_custom_hint)) },
                        label = { Text(stringResource(R.string.translator_cond_category_custom)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        if (conditions.hasProgress != null) {
            ConditionPatternCard(
                title = stringResource(R.string.translator_cond_has_progress),
                icon = Icons.Outlined.Speed,
                onRemove = { onChange(conditions.copy(hasProgress = null)) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.translator_cond_has_progress_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = conditions.hasProgress == true,
                        onCheckedChange = { onChange(conditions.copy(hasProgress = it)) }
                    )
                }
            }
        }

        if (conditions.hasActions != null) {
            ConditionPatternCard(
                title = stringResource(R.string.translator_cond_has_actions),
                icon = Icons.Outlined.TouchApp,
                onRemove = { onChange(conditions.copy(hasActions = null)) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.translator_cond_has_actions_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = conditions.hasActions == true,
                        onCheckedChange = { onChange(conditions.copy(hasActions = it)) }
                    )
                }
            }
        }

        // Messaging Conditions (Person / Conversation)
        if (messaging?.senderNameRegex != null) {
            ConditionPatternCard(
                title = stringResource(R.string.translator_cond_sender_name),
                icon = Icons.Outlined.Person,
                onRemove = {
                    val updated = messaging.copy(senderNameRegex = null)
                    onChange(conditions.copy(typeSpecificConditions = typeConds.copy(messaging = updated)))
                }
            ) {
                OutlinedTextField(
                    value = messaging.senderNameRegex,
                    onValueChange = {
                        val updated = messaging.copy(senderNameRegex = it)
                        onChange(conditions.copy(typeSpecificConditions = typeConds.copy(messaging = updated)))
                    },
                    placeholder = { Text(stringResource(R.string.translator_cond_sender_name_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        if (messaging?.conversationTitleRegex != null) {
            ConditionPatternCard(
                title = stringResource(R.string.translator_cond_conv_title),
                icon = Icons.Outlined.Forum,
                onRemove = {
                    val updated = messaging.copy(conversationTitleRegex = null)
                    onChange(conditions.copy(typeSpecificConditions = typeConds.copy(messaging = updated)))
                }
            ) {
                OutlinedTextField(
                    value = messaging.conversationTitleRegex,
                    onValueChange = {
                        val updated = messaging.copy(conversationTitleRegex = it)
                        onChange(conditions.copy(typeSpecificConditions = typeConds.copy(messaging = updated)))
                    },
                    placeholder = { Text(stringResource(R.string.translator_cond_conv_title_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        if (messaging?.isGroupConversation != null) {
            ConditionPatternCard(
                title = stringResource(R.string.translator_cond_is_group),
                icon = Icons.Outlined.Group,
                onRemove = {
                    val updated = messaging.copy(isGroupConversation = null)
                    onChange(conditions.copy(typeSpecificConditions = typeConds.copy(messaging = updated)))
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.translator_cond_is_group_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = messaging.isGroupConversation == true,
                        onCheckedChange = {
                            val updated = messaging.copy(isGroupConversation = it)
                            onChange(conditions.copy(typeSpecificConditions = typeConds.copy(messaging = updated)))
                        }
                    )
                }
            }
        }

        // Media Conditions (Artist, Album, Playing)
        if (media?.artistRegex != null) {
            ConditionPatternCard(
                title = stringResource(R.string.translator_cond_artist),
                icon = Icons.Outlined.MusicNote,
                onRemove = {
                    val updated = media.copy(artistRegex = null)
                    onChange(conditions.copy(typeSpecificConditions = typeConds.copy(media = updated)))
                }
            ) {
                OutlinedTextField(
                    value = media.artistRegex,
                    onValueChange = {
                        val updated = media.copy(artistRegex = it)
                        onChange(conditions.copy(typeSpecificConditions = typeConds.copy(media = updated)))
                    },
                    placeholder = { Text(stringResource(R.string.translator_cond_artist_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        if (media?.albumRegex != null) {
            ConditionPatternCard(
                title = stringResource(R.string.translator_cond_album),
                icon = Icons.Outlined.Album,
                onRemove = {
                    val updated = media.copy(albumRegex = null)
                    onChange(conditions.copy(typeSpecificConditions = typeConds.copy(media = updated)))
                }
            ) {
                OutlinedTextField(
                    value = media.albumRegex,
                    onValueChange = {
                        val updated = media.copy(albumRegex = it)
                        onChange(conditions.copy(typeSpecificConditions = typeConds.copy(media = updated)))
                    },
                    placeholder = { Text(stringResource(R.string.translator_cond_album_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        if (media?.isPlaying != null) {
            ConditionPatternCard(
                title = stringResource(R.string.translator_cond_is_playing),
                icon = Icons.Outlined.PlayCircle,
                onRemove = {
                    val updated = media.copy(isPlaying = null)
                    onChange(conditions.copy(typeSpecificConditions = typeConds.copy(media = updated)))
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.translator_cond_is_playing_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = media.isPlaying == true,
                        onCheckedChange = {
                            val updated = media.copy(isPlaying = it)
                            onChange(conditions.copy(typeSpecificConditions = typeConds.copy(media = updated)))
                        }
                    )
                }
            }
        }

        // Call Conditions (Caller Name, Call Type)
        if (call?.callerNameRegex != null) {
            ConditionPatternCard(
                title = stringResource(R.string.translator_cond_caller_name),
                icon = Icons.Outlined.Call,
                onRemove = {
                    val updated = call.copy(callerNameRegex = null)
                    onChange(conditions.copy(typeSpecificConditions = typeConds.copy(call = updated)))
                }
            ) {
                OutlinedTextField(
                    value = call.callerNameRegex,
                    onValueChange = {
                        val updated = call.copy(callerNameRegex = it)
                        onChange(conditions.copy(typeSpecificConditions = typeConds.copy(call = updated)))
                    },
                    placeholder = { Text(stringResource(R.string.translator_cond_caller_name_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        if (call?.callType != null) {
            ConditionPatternCard(
                title = stringResource(R.string.translator_cond_call_type),
                icon = Icons.Outlined.PhoneInTalk,
                onRemove = {
                    val updated = call.copy(callType = null)
                    onChange(conditions.copy(typeSpecificConditions = typeConds.copy(call = updated)))
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CallTypeCondition.entries.forEach { type ->
                        val label = when (type) {
                            CallTypeCondition.INCOMING -> stringResource(R.string.translator_cond_call_type_incoming)
                            CallTypeCondition.ONGOING -> stringResource(R.string.translator_cond_call_type_ongoing)
                            CallTypeCondition.MISSED -> stringResource(R.string.translator_cond_call_type_missed)
                        }
                        FilterChip(
                            selected = call.callType == type,
                            onClick = {
                                val updated = call.copy(callType = type)
                                onChange(conditions.copy(typeSpecificConditions = typeConds.copy(call = updated)))
                            },
                            label = { Text(label, style = MaterialTheme.typography.bodySmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }
        }

        // Navigation Conditions (Instruction, Distance)
        if (nav?.instructionRegex != null) {
            ConditionPatternCard(
                title = stringResource(R.string.translator_cond_nav_instruction),
                icon = Icons.Outlined.Navigation,
                onRemove = {
                    val updated = nav.copy(instructionRegex = null)
                    onChange(conditions.copy(typeSpecificConditions = typeConds.copy(navigation = updated)))
                }
            ) {
                OutlinedTextField(
                    value = nav.instructionRegex,
                    onValueChange = {
                        val updated = nav.copy(instructionRegex = it)
                        onChange(conditions.copy(typeSpecificConditions = typeConds.copy(navigation = updated)))
                    },
                    placeholder = { Text(stringResource(R.string.translator_cond_nav_instruction_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        if (nav?.distanceRegex != null) {
            ConditionPatternCard(
                title = stringResource(R.string.translator_cond_nav_distance),
                icon = Icons.Outlined.NearMe,
                onRemove = {
                    val updated = nav.copy(distanceRegex = null)
                    onChange(conditions.copy(typeSpecificConditions = typeConds.copy(navigation = updated)))
                }
            ) {
                OutlinedTextField(
                    value = nav.distanceRegex,
                    onValueChange = {
                        val updated = nav.copy(distanceRegex = it)
                        onChange(conditions.copy(typeSpecificConditions = typeConds.copy(navigation = updated)))
                    },
                    placeholder = { Text(stringResource(R.string.translator_cond_nav_distance_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        // Progress Conditions (Min/Max percentage)
        if (progress?.minProgressPercent != null) {
            ConditionPatternCard(
                title = stringResource(R.string.translator_cond_min_progress),
                icon = Icons.Outlined.Speed,
                onRemove = {
                    val updated = progress.copy(minProgressPercent = null)
                    onChange(conditions.copy(typeSpecificConditions = typeConds.copy(progress = updated)))
                }
            ) {
                OutlinedTextField(
                    value = progress.minProgressPercent.toString(),
                    onValueChange = {
                        val num = it.filter { c -> c.isDigit() }.toIntOrNull()
                        val updated = progress.copy(minProgressPercent = num)
                        onChange(conditions.copy(typeSpecificConditions = typeConds.copy(progress = updated)))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        if (progress?.maxProgressPercent != null) {
            ConditionPatternCard(
                title = stringResource(R.string.translator_cond_max_progress),
                icon = Icons.Outlined.Speed,
                onRemove = {
                    val updated = progress.copy(maxProgressPercent = null)
                    onChange(conditions.copy(typeSpecificConditions = typeConds.copy(progress = updated)))
                }
            ) {
                OutlinedTextField(
                    value = progress.maxProgressPercent.toString(),
                    onValueChange = {
                        val num = it.filter { c -> c.isDigit() }.toIntOrNull()
                        val updated = progress.copy(maxProgressPercent = num)
                        onChange(conditions.copy(typeSpecificConditions = typeConds.copy(progress = updated)))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    }

    if (showAddPatternSheet) {
        AddConditionFieldSheet(
            conditions = conditions,
            targetNotificationTypes = targetNotificationTypes,
            onDismiss = { showAddPatternSheet = false },
            onConditionFieldAdded = { updatedConditions ->
                showAddPatternSheet = false
                onChange(updatedConditions)
            }
        )
    }

    if (showChannelPickerSheet) {
        NotificationChannelSelectionSheet(
            targetPackages = targetPackages,
            selectedChannelId = conditions.channelId,
            onFetchChannels = onFetchChannels,
            onChannelSelected = { selectedId ->
                onChange(conditions.copy(channelId = selectedId))
            },
            onDismiss = { showChannelPickerSheet = false }
        )
    }
}

@Composable
fun ConditionPatternCard(
    title: String,
    icon: ImageVector,
    onRemove: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.translators_action_delete),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            content()
        }
    }
}

enum class ConditionFieldKey {
    TITLE_REGEX,
    TEXT_REGEX,
    SUBTEXT_REGEX,
    CHANNEL_ID,
    CATEGORY,
    HAS_PROGRESS,
    HAS_ACTIONS,
    SENDER_NAME_REGEX,
    CONVERSATION_TITLE_REGEX,
    IS_GROUP_CONVERSATION,
    ARTIST_REGEX,
    ALBUM_REGEX,
    IS_PLAYING,
    CALLER_NAME_REGEX,
    CALL_TYPE,
    NAV_INSTRUCTION_REGEX,
    NAV_DISTANCE_REGEX,
    MIN_PROGRESS_PERCENT,
    MAX_PROGRESS_PERCENT
}

data class ConditionFieldOption(
    val key: ConditionFieldKey,
    val titleRes: Int,
    val icon: ImageVector,
    val groupRes: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddConditionFieldSheet(
    conditions: TranslatorConditions,
    targetNotificationTypes: List<String>,
    onDismiss: () -> Unit,
    onConditionFieldAdded: (TranslatorConditions) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        AddConditionFieldContent(
            conditions = conditions,
            targetNotificationTypes = targetNotificationTypes,
            onConditionFieldAdded = onConditionFieldAdded
        )
    }
}

@Composable
fun AddConditionFieldContent(
    conditions: TranslatorConditions,
    targetNotificationTypes: List<String>,
    onConditionFieldAdded: (TranslatorConditions) -> Unit
) {
    val typeConds = conditions.typeSpecificConditions ?: TypeSpecificConditions()
    val messaging = typeConds.messaging
    val media = typeConds.media
    val call = typeConds.call
    val nav = typeConds.navigation
    val progress = typeConds.progress

    val activeKeys = mutableSetOf<ConditionFieldKey>().apply {
        if (conditions.titleRegex != null) add(ConditionFieldKey.TITLE_REGEX)
        if (conditions.textRegex != null) add(ConditionFieldKey.TEXT_REGEX)
        if (conditions.subtextRegex != null) add(ConditionFieldKey.SUBTEXT_REGEX)
        if (conditions.channelId != null) add(ConditionFieldKey.CHANNEL_ID)
        if (conditions.category != null) add(ConditionFieldKey.CATEGORY)
        if (conditions.hasProgress != null) add(ConditionFieldKey.HAS_PROGRESS)
        if (conditions.hasActions != null) add(ConditionFieldKey.HAS_ACTIONS)
        if (messaging?.senderNameRegex != null) add(ConditionFieldKey.SENDER_NAME_REGEX)
        if (messaging?.conversationTitleRegex != null) add(ConditionFieldKey.CONVERSATION_TITLE_REGEX)
        if (messaging?.isGroupConversation != null) add(ConditionFieldKey.IS_GROUP_CONVERSATION)
        if (media?.artistRegex != null) add(ConditionFieldKey.ARTIST_REGEX)
        if (media?.albumRegex != null) add(ConditionFieldKey.ALBUM_REGEX)
        if (media?.isPlaying != null) add(ConditionFieldKey.IS_PLAYING)
        if (call?.callerNameRegex != null) add(ConditionFieldKey.CALLER_NAME_REGEX)
        if (call?.callType != null) add(ConditionFieldKey.CALL_TYPE)
        if (nav?.instructionRegex != null) add(ConditionFieldKey.NAV_INSTRUCTION_REGEX)
        if (nav?.distanceRegex != null) add(ConditionFieldKey.NAV_DISTANCE_REGEX)
        if (progress?.minProgressPercent != null) add(ConditionFieldKey.MIN_PROGRESS_PERCENT)
        if (progress?.maxProgressPercent != null) add(ConditionFieldKey.MAX_PROGRESS_PERCENT)
    }

    val allOptions = listOf(
        // Standard
        ConditionFieldOption(ConditionFieldKey.TITLE_REGEX, R.string.translator_cond_title_regex, Icons.Outlined.Title, R.string.translator_cond_group_standard),
        ConditionFieldOption(ConditionFieldKey.TEXT_REGEX, R.string.translator_cond_text_regex, Icons.AutoMirrored.Outlined.Subject, R.string.translator_cond_group_standard),
        ConditionFieldOption(ConditionFieldKey.SUBTEXT_REGEX, R.string.translator_cond_subtext_regex, Icons.Outlined.Subtitles, R.string.translator_cond_group_standard),
        ConditionFieldOption(ConditionFieldKey.CHANNEL_ID, R.string.translator_cond_channel, Icons.Outlined.Tune, R.string.translator_cond_group_standard),
        ConditionFieldOption(ConditionFieldKey.CATEGORY, R.string.translator_cond_category, Icons.Outlined.Category, R.string.translator_cond_group_standard),
        ConditionFieldOption(ConditionFieldKey.HAS_PROGRESS, R.string.translator_cond_has_progress, Icons.Outlined.Speed, R.string.translator_cond_group_standard),
        ConditionFieldOption(ConditionFieldKey.HAS_ACTIONS, R.string.translator_cond_has_actions, Icons.Outlined.TouchApp, R.string.translator_cond_group_standard),

        // Messaging
        ConditionFieldOption(ConditionFieldKey.SENDER_NAME_REGEX, R.string.translator_cond_sender_name, Icons.Outlined.Person, R.string.translator_cond_group_messaging),
        ConditionFieldOption(ConditionFieldKey.CONVERSATION_TITLE_REGEX, R.string.translator_cond_conv_title, Icons.Outlined.Forum, R.string.translator_cond_group_messaging),
        ConditionFieldOption(ConditionFieldKey.IS_GROUP_CONVERSATION, R.string.translator_cond_is_group, Icons.Outlined.Group, R.string.translator_cond_group_messaging),

        // Media
        ConditionFieldOption(ConditionFieldKey.ARTIST_REGEX, R.string.translator_cond_artist, Icons.Outlined.MusicNote, R.string.translator_cond_group_media),
        ConditionFieldOption(ConditionFieldKey.ALBUM_REGEX, R.string.translator_cond_album, Icons.Outlined.Album, R.string.translator_cond_group_media),
        ConditionFieldOption(ConditionFieldKey.IS_PLAYING, R.string.translator_cond_is_playing, Icons.Outlined.PlayCircle, R.string.translator_cond_group_media),

        // Call
        ConditionFieldOption(ConditionFieldKey.CALLER_NAME_REGEX, R.string.translator_cond_caller_name, Icons.Outlined.Call, R.string.translator_cond_group_call),
        ConditionFieldOption(ConditionFieldKey.CALL_TYPE, R.string.translator_cond_call_type, Icons.Outlined.PhoneInTalk, R.string.translator_cond_group_call),

        // Navigation
        ConditionFieldOption(ConditionFieldKey.NAV_INSTRUCTION_REGEX, R.string.translator_cond_nav_instruction, Icons.Outlined.Navigation, R.string.translator_cond_group_navigation),
        ConditionFieldOption(ConditionFieldKey.NAV_DISTANCE_REGEX, R.string.translator_cond_nav_distance, Icons.Outlined.NearMe, R.string.translator_cond_group_navigation),

        // Progress
        ConditionFieldOption(ConditionFieldKey.MIN_PROGRESS_PERCENT, R.string.translator_cond_min_progress, Icons.Outlined.Speed, R.string.translator_cond_group_progress),
        ConditionFieldOption(ConditionFieldKey.MAX_PROGRESS_PERCENT, R.string.translator_cond_max_progress, Icons.Outlined.Speed, R.string.translator_cond_group_progress)
    )

    val filteredOptions = allOptions.filter { opt ->
        if (activeKeys.contains(opt.key)) return@filter false
        if (targetNotificationTypes.isEmpty()) return@filter true

        when (opt.groupRes) {
            R.string.translator_cond_group_standard -> true
            R.string.translator_cond_group_messaging -> targetNotificationTypes.contains(NotificationType.MESSAGE.name)
            R.string.translator_cond_group_media -> targetNotificationTypes.contains(NotificationType.MEDIA.name)
            R.string.translator_cond_group_call -> targetNotificationTypes.contains(NotificationType.CALL.name)
            R.string.translator_cond_group_navigation -> targetNotificationTypes.contains(NotificationType.NAVIGATION.name)
            R.string.translator_cond_group_progress -> targetNotificationTypes.contains(NotificationType.PROGRESS.name) || targetNotificationTypes.contains(NotificationType.DOWNLOAD.name)
            else -> true
        }
    }

    val grouped = filteredOptions.groupBy { it.groupRes }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .navigationBarsPadding()
    ) {
        Text(
            text = stringResource(R.string.translator_cond_sheet_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            grouped.forEach { (groupRes, options) ->
                item {
                    Text(
                        text = stringResource(groupRes),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        options.forEach { opt ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                onClick = {
                                    val updated = when (opt.key) {
                                        ConditionFieldKey.TITLE_REGEX -> conditions.copy(titleRegex = "")
                                        ConditionFieldKey.TEXT_REGEX -> conditions.copy(textRegex = "")
                                        ConditionFieldKey.SUBTEXT_REGEX -> conditions.copy(subtextRegex = "")
                                        ConditionFieldKey.CHANNEL_ID -> conditions.copy(channelId = "")
                                        ConditionFieldKey.CATEGORY -> conditions.copy(category = "")
                                        ConditionFieldKey.HAS_PROGRESS -> conditions.copy(hasProgress = true)
                                        ConditionFieldKey.HAS_ACTIONS -> conditions.copy(hasActions = true)
                                        ConditionFieldKey.SENDER_NAME_REGEX -> conditions.copy(
                                            typeSpecificConditions = typeConds.copy(
                                                messaging = (messaging ?: MessagingConditions()).copy(senderNameRegex = "")
                                            )
                                        )
                                        ConditionFieldKey.CONVERSATION_TITLE_REGEX -> conditions.copy(
                                            typeSpecificConditions = typeConds.copy(
                                                messaging = (messaging ?: MessagingConditions()).copy(conversationTitleRegex = "")
                                            )
                                        )
                                        ConditionFieldKey.IS_GROUP_CONVERSATION -> conditions.copy(
                                            typeSpecificConditions = typeConds.copy(
                                                messaging = (messaging ?: MessagingConditions()).copy(isGroupConversation = true)
                                            )
                                        )
                                        ConditionFieldKey.ARTIST_REGEX -> conditions.copy(
                                            typeSpecificConditions = typeConds.copy(
                                                media = (media ?: MediaConditions()).copy(artistRegex = "")
                                            )
                                        )
                                        ConditionFieldKey.ALBUM_REGEX -> conditions.copy(
                                            typeSpecificConditions = typeConds.copy(
                                                media = (media ?: MediaConditions()).copy(albumRegex = "")
                                            )
                                        )
                                        ConditionFieldKey.IS_PLAYING -> conditions.copy(
                                            typeSpecificConditions = typeConds.copy(
                                                media = (media ?: MediaConditions()).copy(isPlaying = true)
                                            )
                                        )
                                        ConditionFieldKey.CALLER_NAME_REGEX -> conditions.copy(
                                            typeSpecificConditions = typeConds.copy(
                                                call = (call ?: CallConditions()).copy(callerNameRegex = "")
                                            )
                                        )
                                        ConditionFieldKey.CALL_TYPE -> conditions.copy(
                                            typeSpecificConditions = typeConds.copy(
                                                call = (call ?: CallConditions()).copy(callType = CallTypeCondition.INCOMING)
                                            )
                                        )
                                        ConditionFieldKey.NAV_INSTRUCTION_REGEX -> conditions.copy(
                                            typeSpecificConditions = typeConds.copy(
                                                navigation = (nav ?: NavigationConditions()).copy(instructionRegex = "")
                                            )
                                        )
                                        ConditionFieldKey.NAV_DISTANCE_REGEX -> conditions.copy(
                                            typeSpecificConditions = typeConds.copy(
                                                navigation = (nav ?: NavigationConditions()).copy(distanceRegex = "")
                                            )
                                        )
                                        ConditionFieldKey.MIN_PROGRESS_PERCENT -> conditions.copy(
                                            typeSpecificConditions = typeConds.copy(
                                                progress = (progress ?: ProgressConditions()).copy(minProgressPercent = 0)
                                            )
                                        )
                                        ConditionFieldKey.MAX_PROGRESS_PERCENT -> conditions.copy(
                                            typeSpecificConditions = typeConds.copy(
                                                progress = (progress ?: ProgressConditions()).copy(maxProgressPercent = 100)
                                            )
                                        )
                                    }
                                    onConditionFieldAdded(updated)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = opt.icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = stringResource(opt.titleRes),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(Modifier.weight(1f))
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TranslatorPresentationContent(
    presentation: PresentationConfig,
    onChange: (PresentationConfig) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.translator_presentation_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = stringResource(R.string.translator_pres_mode),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PresentationMode.entries.forEach { mode ->
                FilterChip(
                    selected = presentation.mode == mode,
                    onClick = { onChange(presentation.copy(mode = mode)) },
                    label = { Text(mode.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        OutlinedTextField(
            value = presentation.textSlot.titleTemplate,
            onValueChange = { onChange(presentation.copy(textSlot = presentation.textSlot.copy(titleTemplate = it))) },
            label = { Text(stringResource(R.string.translator_pres_title_tpl)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = presentation.textSlot.subtitleTemplate,
            onValueChange = { onChange(presentation.copy(textSlot = presentation.textSlot.copy(subtitleTemplate = it))) },
            label = { Text(stringResource(R.string.translator_pres_sub_tpl)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = presentation.textSlot.highlightTextTemplate ?: "",
            onValueChange = { onChange(presentation.copy(textSlot = presentation.textSlot.copy(highlightTextTemplate = it.ifBlank { null }))) },
            label = { Text(stringResource(R.string.translator_pres_highlight_tpl)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
fun TranslatorProgressContent(
    progressSlot: ProgressSlotConfig,
    dataExtraction: DataExtractionConfig,
    onProgressSlotChange: (ProgressSlotConfig) -> Unit,
    onDataExtractionChange: (DataExtractionConfig) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.translator_progress_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = stringResource(R.string.translator_prog_type),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ProgressSlotType.entries.forEach { type ->
                FilterChip(
                    selected = progressSlot.type == type,
                    onClick = { onProgressSlotChange(progressSlot.copy(type = type)) },
                    label = { Text(type.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.translator_prog_show_pct), style = MaterialTheme.typography.bodyLarge)
            }
            Switch(
                checked = progressSlot.showPercentage,
                onCheckedChange = { onProgressSlotChange(progressSlot.copy(showPercentage = it)) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.translator_prog_extract_text), style = MaterialTheme.typography.bodyLarge)
            }
            Switch(
                checked = dataExtraction.extractProgressFromText,
                onCheckedChange = { onDataExtractionChange(dataExtraction.copy(extractProgressFromText = it)) }
            )
        }

        if (dataExtraction.extractProgressFromText) {
            OutlinedTextField(
                value = dataExtraction.progressRegex ?: "",
                onValueChange = { onDataExtractionChange(dataExtraction.copy(progressRegex = it.ifBlank { null })) },
                label = { Text(stringResource(R.string.translator_prog_regex_tpl)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
fun TranslatorActionsContent(
    actionSlots: List<ActionSlotConfig>,
    onChange: (List<ActionSlotConfig>) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.translator_actions_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Button(
                onClick = {
                    val newSlot = ActionSlotConfig(
                        slotPosition = actionSlots.size,
                        source = ActionSource.NOTIFICATION_ACTION,
                        actionMatcher = ActionMatcher(
                            matchBy = ActionMatchBy.INDEX,
                            actionIndex = actionSlots.size
                        )
                    )
                    onChange(actionSlots + newSlot)
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.translator_actions_add_slot))
            }
        }

        if (actionSlots.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.TouchApp,
                title = stringResource(R.string.translator_actions_empty_title),
                description = stringResource(R.string.translator_actions_empty_desc),
                modifier = Modifier.padding(top = 32.dp)
            )
        } else {
            actionSlots.forEachIndexed { index, slot ->
                val shape = getExpressiveShape(actionSlots.size, index, ShapeStyle.Large)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = shape,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Slot #${slot.slotPosition + 1}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = {
                                    val updated = actionSlots.toMutableList().apply { removeAt(index) }
                                    onChange(updated)
                                }
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ActionSource.entries.forEach { source ->
                                FilterChip(
                                    selected = slot.source == source,
                                    onClick = {
                                        val updated = actionSlots.toMutableList()
                                        updated[index] = slot.copy(source = source)
                                        onChange(updated)
                                    },
                                    label = { Text(source.name) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }

                        if (slot.source == ActionSource.SMART_ACTION) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SmartActionType.entries.forEach { smartType ->
                                    FilterChip(
                                        selected = slot.smartActionType == smartType,
                                        onClick = {
                                            val updated = actionSlots.toMutableList()
                                            updated[index] = slot.copy(smartActionType = smartType)
                                            onChange(updated)
                                        },
                                        label = { Text(smartType.name) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = slot.customLabel ?: "",
                            onValueChange = {
                                val updated = actionSlots.toMutableList()
                                updated[index] = slot.copy(customLabel = it.ifBlank { null })
                                onChange(updated)
                            },
                            label = { Text(stringResource(R.string.translator_action_custom_label)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TranslatorBehaviorContent(
    behavior: BehaviorOverride,
    engineMode: EngineMode,
    onBehaviorChange: (BehaviorOverride) -> Unit,
    onEngineModeChange: (EngineMode) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.translator_behavior_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = stringResource(R.string.engine),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EngineMode.entries.forEach { mode ->
                FilterChip(
                    selected = engineMode == mode,
                    onClick = { onEngineModeChange(mode) },
                    label = { Text(mode.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.translator_behavior_float), style = MaterialTheme.typography.bodyLarge)
            }
            Switch(
                checked = behavior.isFloat == true,
                onCheckedChange = { onBehaviorChange(behavior.copy(isFloat = if (it) true else null)) }
            )
        }

        OutlinedTextField(
            value = behavior.timeoutSeconds?.toString() ?: "",
            onValueChange = { onBehaviorChange(behavior.copy(timeoutSeconds = it.toIntOrNull())) },
            label = { Text(stringResource(R.string.translator_behavior_timeout)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.translator_behavior_shade), style = MaterialTheme.typography.bodyLarge)
            }
            Switch(
                checked = behavior.isShowShade == true,
                onCheckedChange = { onBehaviorChange(behavior.copy(isShowShade = if (it) true else null)) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.translator_behavior_remove_orig), style = MaterialTheme.typography.bodyLarge)
            }
            Switch(
                checked = behavior.removeOriginalNotification == true,
                onCheckedChange = { onBehaviorChange(behavior.copy(removeOriginalNotification = if (it) true else null)) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.translator_behavior_dismiss_with_orig), style = MaterialTheme.typography.bodyLarge)
            }
            Switch(
                checked = behavior.dismissWithOriginal == true,
                onCheckedChange = { onBehaviorChange(behavior.copy(dismissWithOriginal = if (it) true else null)) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslatorAppsContent(
    translator: CustomTranslator,
    installedApps: List<AppItem>,
    onTargetScopeChange: (TargetScope, List<String>) -> Unit,
    onTargetNotificationTypesChange: (List<String>) -> Unit
) {
    var showScopeSheet by remember { mutableStateOf(false) }
    var showAppPicker by remember { mutableStateOf(false) }
    var showNotifTypePicker by remember { mutableStateOf(false) }

    val isFilterByNotifType = translator.targetNotificationTypes.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Card 1: Target Scope (Global vs Specific Apps)
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Apps,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.translator_target_scope_card_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (translator.targetScope == TargetScope.GLOBAL) {
                                stringResource(R.string.translator_target_scope_global_desc)
                            } else {
                                stringResource(R.string.translator_target_scope_apps_desc, translator.targetPackages.size)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showScopeSheet = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (translator.targetScope == TargetScope.GLOBAL) {
                                stringResource(R.string.translator_target_scope_global_title)
                            } else {
                                stringResource(R.string.translator_target_scope_apps_title)
                            }
                        )
                    }

                    if (translator.targetScope == TargetScope.SPECIFIC_APPS) {
                        Button(
                            onClick = { showAppPicker = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.translator_select_apps_action))
                        }
                    }
                }

                // Show selected apps chips / list if specific apps
                if (translator.targetScope == TargetScope.SPECIFIC_APPS && translator.targetPackages.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        translator.targetPackages.forEach { pkg ->
                            val label = installedApps.find { it.packageName == pkg }?.label ?: pkg
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AppIcon(packageName = pkg, modifier = Modifier.size(28.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                        Text(pkg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(
                                        onClick = {
                                            val updated = translator.targetPackages.toMutableList().apply { remove(pkg) }
                                            onTargetScopeChange(TargetScope.SPECIFIC_APPS, updated)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Card 2: Filter by Notification Type
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Outlined.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.translator_notif_types_filter_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.translator_notif_types_filter_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = isFilterByNotifType,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                showNotifTypePicker = true
                            } else {
                                onTargetNotificationTypesChange(emptyList())
                            }
                        }
                    )
                }

                if (isFilterByNotifType) {
                    Button(
                        onClick = { showNotifTypePicker = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(Icons.Outlined.Category, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (translator.targetNotificationTypes.isEmpty()) {
                                stringResource(R.string.translator_notif_types_select_action)
                            } else {
                                stringResource(R.string.translator_notif_types_count_fmt, translator.targetNotificationTypes.size)
                            }
                        )
                    }

                    // Show selected types chips
                    if (translator.targetNotificationTypes.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            translator.targetNotificationTypes.forEach { typeName ->
                                val notifType = NotificationType.entries.find { it.name == typeName }
                                val label = if (notifType != null) stringResource(notifType.labelRes) else typeName
                                FilterChip(
                                    selected = true,
                                    onClick = {
                                        val updated = translator.targetNotificationTypes.toMutableList().apply { remove(typeName) }
                                        onTargetNotificationTypesChange(updated)
                                    },
                                    label = { Text(label) },
                                    trailingIcon = {
                                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                                    },
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
        }
    }

    // Bottom Sheet: Target Scope Selector
    if (showScopeSheet) {
        TranslatorScopeSelectionSheet(
            currentScope = translator.targetScope,
            onDismiss = { showScopeSheet = false },
            onScopeSelected = { scope ->
                showScopeSheet = false
                if (scope == TargetScope.GLOBAL) {
                    onTargetScopeChange(TargetScope.GLOBAL, emptyList())
                } else {
                    onTargetScopeChange(TargetScope.SPECIFIC_APPS, translator.targetPackages)
                }
            }
        )
    }

    // Bottom Sheet: App Selection
    if (showAppPicker) {
        AppSelectionSheet(
            apps = installedApps,
            onDismiss = { showAppPicker = false },
            onAppSelected = { app ->
                showAppPicker = false
                if (!translator.targetPackages.contains(app.packageName)) {
                    val updated = translator.targetPackages + app.packageName
                    onTargetScopeChange(TargetScope.SPECIFIC_APPS, updated)
                }
            }
        )
    }

    // Bottom Sheet: Notification Types Selection
    if (showNotifTypePicker) {
        NotificationTypeSelectionSheet(
            selectedTypes = translator.targetNotificationTypes,
            onDismiss = { showNotifTypePicker = false },
            onTypesSelected = { types ->
                showNotifTypePicker = false
                onTargetNotificationTypesChange(types)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslatorScopeSelectionSheet(
    currentScope: TargetScope,
    onDismiss: () -> Unit,
    onScopeSelected: (TargetScope) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        TranslatorScopeSelectionContent(
            currentScope = currentScope,
            onScopeSelected = onScopeSelected
        )
    }
}

@Composable
fun TranslatorScopeSelectionContent(
    currentScope: TargetScope,
    onScopeSelected: (TargetScope) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.translator_scope_sheet_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (currentScope == TargetScope.GLOBAL) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
            ),
            onClick = { onScopeSelected(TargetScope.GLOBAL) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = currentScope == TargetScope.GLOBAL,
                    onClick = { onScopeSelected(TargetScope.GLOBAL) }
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.translator_target_scope_global_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.translator_target_scope_global_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (currentScope == TargetScope.SPECIFIC_APPS) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
            ),
            onClick = { onScopeSelected(TargetScope.SPECIFIC_APPS) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = currentScope == TargetScope.SPECIFIC_APPS,
                    onClick = { onScopeSelected(TargetScope.SPECIFIC_APPS) }
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.translator_target_scope_apps_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.translator_target_scope_apps_desc, 0),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationTypeSelectionSheet(
    selectedTypes: List<String>,
    onDismiss: () -> Unit,
    onTypesSelected: (List<String>) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        NotificationTypeSelectionContent(
            selectedTypes = selectedTypes,
            onTypesSelected = onTypesSelected
        )
    }
}

@Composable
fun NotificationTypeSelectionContent(
    selectedTypes: List<String>,
    onTypesSelected: (List<String>) -> Unit
) {
    var currentSelected by remember { mutableStateOf(selectedTypes.toSet()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .navigationBarsPadding()
    ) {
        Text(
            text = stringResource(R.string.translator_notif_types_sheet_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val types = NotificationType.configurableEntries
            items(types) { type ->
                val isChecked = currentSelected.contains(type.name)
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isChecked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceContainer
                    ),
                    onClick = {
                        currentSelected = if (isChecked) {
                            currentSelected - type.name
                        } else {
                            currentSelected + type.name
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                currentSelected = if (checked) {
                                    currentSelected + type.name
                                } else {
                                    currentSelected - type.name
                                }
                            }
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = stringResource(type.labelRes),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { onTypesSelected(currentSelected.toList()) },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(stringResource(R.string.done), fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationChannelSelectionSheet(
    targetPackages: List<String>,
    selectedChannelId: String?,
    onFetchChannels: suspend (List<String>) -> List<NotificationChannelInfo>,
    onChannelSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        NotificationChannelSelectionContent(
            targetPackages = targetPackages,
            selectedChannelId = selectedChannelId,
            onFetchChannels = onFetchChannels,
            onChannelSelected = {
                onChannelSelected(it)
                onDismiss()
            }
        )
    }
}

@Composable
fun NotificationChannelSelectionContent(
    targetPackages: List<String>,
    selectedChannelId: String?,
    onFetchChannels: suspend (List<String>) -> List<NotificationChannelInfo>,
    onChannelSelected: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var channels by remember { mutableStateOf<List<NotificationChannelInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(targetPackages) {
        isLoading = true
        channels = onFetchChannels(targetPackages)
        isLoading = false
    }

    val filtered = remember(channels, searchQuery) {
        if (searchQuery.isBlank()) {
            channels
        } else {
            channels.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.id.contains(searchQuery, ignoreCase = true) ||
                (it.packageName?.contains(searchQuery, ignoreCase = true) == true) ||
                (it.description?.contains(searchQuery, ignoreCase = true) == true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .navigationBarsPadding()
    ) {
        Text(
            text = stringResource(R.string.translator_cond_channel_sheet_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(stringResource(R.string.translator_cond_channel_search_hint)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cancel))
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            singleLine = true
        )

        Spacer(Modifier.height(16.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.translator_cond_channel_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered) { channel ->
                    val isSelected = selectedChannelId == channel.id
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceContainer
                        ),
                        onClick = { onChannelSelected(channel.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Tune,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = channel.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "ID: ${channel.id}${if (channel.packageName != null) " • ${channel.packageName}" else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (!channel.description.isNullOrBlank()) {
                                    Text(
                                        text = channel.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslatorMetadataSheet(
    translator: CustomTranslator,
    onTranslatorChange: (CustomTranslator) -> Unit,
    onDismiss: () -> Unit
) {
    val fm = LocalFocusManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.translator_meta_title), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = translator.meta.name,
                onValueChange = { onTranslatorChange(translator.copy(meta = translator.meta.copy(name = it))) },
                label = { Text(stringResource(R.string.meta_label_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardActions = KeyboardActions(onDone = { fm.clearFocus() })
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = translator.meta.author,
                onValueChange = { onTranslatorChange(translator.copy(meta = translator.meta.copy(author = it))) },
                label = { Text(stringResource(R.string.meta_label_author)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardActions = KeyboardActions(onDone = { fm.clearFocus() })
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = translator.priority.toString(),
                onValueChange = { onTranslatorChange(translator.copy(priority = it.toIntOrNull() ?: 100)) },
                label = { Text(stringResource(R.string.translators_priority_label, translator.priority)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                keyboardActions = KeyboardActions(onDone = { fm.clearFocus() })
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = translator.meta.description,
                onValueChange = { onTranslatorChange(translator.copy(meta = translator.meta.copy(description = it))) },
                label = { Text(stringResource(R.string.translator_field_description)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )
            Spacer(Modifier.height(32.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(stringResource(R.string.meta_action_done))
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TranslatorLivePreviewBarPreview() {
    MaterialTheme {
        TranslatorLivePreviewBar(
            translator = CustomTranslator(
                id = "preview",
                meta = TranslatorMetadata(name = "Preview Translator", author = "Author")
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TranslatorEditorMainListPreview() {
    MaterialTheme {
        TranslatorMainList(
            translator = CustomTranslator(
                id = "preview",
                meta = TranslatorMetadata(name = "Music & Nav Translator", author = "Author"),
                targetScope = TargetScope.GLOBAL
            ),
            onNavigate = {},
            onEditMetadata = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TranslatorConditionsContentPreview() {
    MaterialTheme {
        TranslatorDetailShell(
            previewContent = {
                TranslatorLivePreviewBar(
                    CustomTranslator(
                        id = "preview",
                        meta = TranslatorMetadata(name = "Preview", author = "Author")
                    )
                )
            }
        ) {
            TranslatorConditionsContent(
                conditions = TranslatorConditions(
                    titleRegex = "^OTP: (\\d{6})",
                    category = "msg",
                    hasActions = true,
                    typeSpecificConditions = TypeSpecificConditions(
                        messaging = MessagingConditions(
                            senderNameRegex = "^[A-Z][a-z]+"
                        )
                    )
                ),
                targetNotificationTypes = listOf("MESSAGE"),
                onChange = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddConditionFieldSheetPreview() {
    MaterialTheme {
        Surface {
            AddConditionFieldContent(
                conditions = TranslatorConditions(),
                targetNotificationTypes = listOf("MESSAGE", "MEDIA"),
                onConditionFieldAdded = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TranslatorPresentationContentPreview() {
    MaterialTheme {
        TranslatorDetailShell(
            previewContent = {
                TranslatorLivePreviewBar(
                    CustomTranslator(
                        id = "preview",
                        meta = TranslatorMetadata(name = "Preview", author = "Author")
                    )
                )
            }
        ) {
            TranslatorPresentationContent(
                presentation = PresentationConfig(
                    mode = PresentationMode.STANDARD,
                    textSlot = TextSlotConfig(
                        titleTemplate = "{notif.title} • Live",
                        subtitleTemplate = "{notif.text}"
                    )
                ),
                onChange = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TranslatorProgressContentPreview() {
    MaterialTheme {
        TranslatorDetailShell(
            previewContent = {
                TranslatorLivePreviewBar(
                    CustomTranslator(
                        id = "preview",
                        meta = TranslatorMetadata(name = "Preview", author = "Author")
                    )
                )
            }
        ) {
            TranslatorProgressContent(
                progressSlot = ProgressSlotConfig(
                    type = ProgressSlotType.PROGRESS_BAR,
                    showPercentage = true
                ),
                dataExtraction = DataExtractionConfig(
                    extractProgressFromText = true,
                    progressRegex = "(\\d+)%"
                ),
                onProgressSlotChange = {},
                onDataExtractionChange = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TranslatorActionsContentPreview() {
    MaterialTheme {
        TranslatorDetailShell(
            previewContent = {
                TranslatorLivePreviewBar(
                    CustomTranslator(
                        id = "preview",
                        meta = TranslatorMetadata(name = "Preview", author = "Author")
                    )
                )
            }
        ) {
            TranslatorActionsContent(
                actionSlots = listOf(
                    ActionSlotConfig(
                        slotPosition = 0,
                        source = ActionSource.SMART_ACTION,
                        smartActionType = SmartActionType.OTP_COPY,
                        customLabel = "Copy OTP"
                    ),
                    ActionSlotConfig(
                        slotPosition = 1,
                        source = ActionSource.NOTIFICATION_ACTION,
                        customLabel = "Reply"
                    )
                ),
                onChange = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TranslatorBehaviorContentPreview() {
    MaterialTheme {
        TranslatorDetailShell(
            previewContent = {
                TranslatorLivePreviewBar(
                    CustomTranslator(
                        id = "preview",
                        meta = TranslatorMetadata(name = "Preview", author = "Author")
                    )
                )
            }
        ) {
            TranslatorBehaviorContent(
                behavior = BehaviorOverride(
                    isFloat = true,
                    timeoutSeconds = 15,
                    isShowShade = true
                ),
                engineMode = EngineMode.CUSTOM_ISLAND,
                onBehaviorChange = {},
                onEngineModeChange = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TranslatorAppsContentPreview() {
    MaterialTheme {
        TranslatorAppsContent(
            translator = CustomTranslator(
                id = "preview",
                meta = TranslatorMetadata(name = "Preview", author = "Author"),
                targetScope = TargetScope.SPECIFIC_APPS,
                targetPackages = listOf("com.whatsapp", "com.spotify.music"),
                targetNotificationTypes = listOf("MESSAGE", "MEDIA")
            ),
            installedApps = listOf(
                AppItem("com.whatsapp", "WhatsApp"),
                AppItem("com.spotify.music", "Spotify")
            ),
            onTargetScopeChange = { _, _ -> },
            onTargetNotificationTypesChange = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TranslatorScopeSelectionSheetPreview() {
    MaterialTheme {
        Surface {
            TranslatorScopeSelectionContent(
                currentScope = TargetScope.SPECIFIC_APPS,
                onScopeSelected = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NotificationTypeSelectionSheetPreview() {
    MaterialTheme {
        Surface {
            NotificationTypeSelectionContent(
                selectedTypes = listOf("MESSAGE", "MEDIA", "CALL"),
                onTypesSelected = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NotificationChannelSelectionSheetPreview() {
    MaterialTheme {
        Surface {
            NotificationChannelSelectionContent(
                targetPackages = listOf("com.whatsapp"),
                selectedChannelId = "group_messages",
                onFetchChannels = {
                    listOf(
                        NotificationChannelInfo(
                            id = "group_messages",
                            name = "Group Messages",
                            description = "Notifications for group conversations",
                            packageName = "com.whatsapp"
                        ),
                        NotificationChannelInfo(
                            id = "direct_messages",
                            name = "Direct Messages",
                            description = "Notifications for individual chats",
                            packageName = "com.whatsapp"
                        ),
                        NotificationChannelInfo(
                            id = "call_notifications",
                            name = "Call Alerts",
                            description = "Incoming voice & video calls",
                            packageName = "com.whatsapp"
                        )
                    )
                },
                onChannelSelected = {}
            )
        }
    }
}





