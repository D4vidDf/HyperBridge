package com.d4viddf.hyperbridge.ui.screens.translators

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.outlined.Subject
import androidx.compose.material.icons.automirrored.outlined.ViewQuilt
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material.icons.outlined.DashboardCustomize
import androidx.compose.material.icons.outlined.DisplaySettings
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhoneInTalk
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Slider
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.rememberSliderState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.viewmodel.compose.viewModel
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.widget.CustomWidgetRepository
import com.d4viddf.hyperbridge.models.NotificationType
import com.d4viddf.hyperbridge.models.translator.ActionDisplayMode
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
import com.d4viddf.hyperbridge.models.translator.IslandTemplateCatalog
import com.d4viddf.hyperbridge.models.translator.MediaConditions
import com.d4viddf.hyperbridge.models.translator.MessagingConditions
import com.d4viddf.hyperbridge.models.translator.NavigationConditions
import com.d4viddf.hyperbridge.models.translator.PillLeftDesign
import com.d4viddf.hyperbridge.models.translator.PillMediaItem
import com.d4viddf.hyperbridge.models.translator.PillRightDesign
import com.d4viddf.hyperbridge.models.translator.PresentationConfig
import com.d4viddf.hyperbridge.models.translator.PresentationMode
import com.d4viddf.hyperbridge.models.translator.ProgressConditions
import com.d4viddf.hyperbridge.models.translator.ProgressSlotConfig
import com.d4viddf.hyperbridge.models.translator.ProgressSlotType
import com.d4viddf.hyperbridge.models.translator.SmartActionType
import com.d4viddf.hyperbridge.models.translator.TargetScope
import com.d4viddf.hyperbridge.models.translator.TextSlotConfig
import com.d4viddf.hyperbridge.models.translator.ThemeBinding
import com.d4viddf.hyperbridge.models.translator.TranslatorConditions
import com.d4viddf.hyperbridge.models.translator.TranslatorMetadata
import com.d4viddf.hyperbridge.models.translator.TypeSpecificConditions
import com.d4viddf.hyperbridge.models.translator.withResolvedTemplate
import com.d4viddf.hyperbridge.models.widget.CustomWidgetDocument
import com.d4viddf.hyperbridge.ui.components.EmptyState
import com.d4viddf.hyperbridge.ui.components.EnginePreview
import com.d4viddf.hyperbridge.ui.components.island.HyperOsIslandPreview
import com.d4viddf.hyperbridge.ui.components.island.IslandPreviewMode
import com.d4viddf.hyperbridge.ui.components.island.IslandTemplateGallery
import com.d4viddf.hyperbridge.ui.screens.design.DesignManagerHelpSheet
import com.d4viddf.hyperbridge.ui.screens.theme.AppItem
import com.d4viddf.hyperbridge.ui.screens.theme.ShapeStyle
import com.d4viddf.hyperbridge.ui.screens.theme.content.AppIcon
import com.d4viddf.hyperbridge.ui.screens.theme.content.AppSelectionSheet
import com.d4viddf.hyperbridge.ui.screens.theme.getExpressiveShape
import com.d4viddf.hyperbridge.ui.screens.theme.getShapeFromId
import com.d4viddf.hyperbridge.ui.screens.theme.safeParseColor
import com.d4viddf.hyperbridge.util.DocumentationUrls
import java.util.UUID
import kotlin.math.roundToInt

private val TRANSLATOR_SPLIT_MIN_WIDTH = 560.dp

enum class TranslatorRoute {
    MAIN_MENU,
    CONDITIONS,
    CONDITIONS_ADD,
    PRESENTATION,
    PRESENTATION_MODE,
    PRESENTATION_TEMPLATES,
    PRESENTATION_WIDGETS,
    PRESENTATION_THEMES,
    PRESENTATION_LEFT_ICON,
    PILL,
    PILL_LEFT,
    PILL_RIGHT,
    PILL_MEDIA_ORDER,
    PROGRESS,
    PROGRESS_TYPE,
    ACTIONS,
    BEHAVIOR,
    BEHAVIOR_ENGINE,
    APPS,
    APPS_SCOPE
}

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
    val systemApps by viewModel.systemApps.collectAsState()
    val installedThemes by viewModel.installedThemes.collectAsState()

    LaunchedEffect(translatorId) {
        if (translatorId != null) {
            val loaded = viewModel.getTranslatorById(translatorId)
            if (loaded != null) {
                // Edit concrete template slots, never a bare templateId (see effectivePresentation).
                translator = loaded.withResolvedTemplate()
            }
        }
    }

    TranslatorEditorContent(
        translator = translator,
        onTranslatorChange = { translator = it },
        installedApps = installedApps,
        systemApps = systemApps,
        installedThemes = installedThemes,
        onFetchChannels = { pkgs -> viewModel.getNotificationChannelsForPackages(pkgs) },
        onSave = {
            viewModel.saveTranslator(translator) {
                onBack()
            }
        },
        onBack = onBack
    )
}

@Composable
fun TranslatorEditorContent(
    translator: CustomTranslator,
    onTranslatorChange: (CustomTranslator) -> Unit,
    installedApps: List<AppItem>,
    systemApps: List<AppItem> = emptyList(),
    installedThemes: List<com.d4viddf.hyperbridge.models.theme.HyperTheme> = emptyList(),
    onFetchChannels: suspend (List<String>) -> List<NotificationChannelInfo> = { emptyList() },
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    var stackNames by rememberSaveable {
        mutableStateOf(listOf(TranslatorRoute.MAIN_MENU.name))
    }
    var previousStackSize by rememberSaveable { mutableIntStateOf(1) }
    var showMetaSheet by remember { mutableStateOf(false) }

    val stack = remember(stackNames) {
        stackNames.mapNotNull { name ->
            runCatching { TranslatorRoute.valueOf(name) }.getOrNull()
        }.ifEmpty { listOf(TranslatorRoute.MAIN_MENU) }
    }

    // Keep the stack consistent when presentation mode or pill design hides child routes
    LaunchedEffect(
        translator.presentation.mode,
        translator.presentation.templateId,
        translator.presentation.pill.leftDesign
    ) {
        val mode = translator.presentation.mode
        val validStack = stack.takeWhile { route ->
            when (route) {
                TranslatorRoute.MAIN_MENU,
                TranslatorRoute.APPS,
                TranslatorRoute.APPS_SCOPE,
                TranslatorRoute.CONDITIONS,
                TranslatorRoute.CONDITIONS_ADD,
                TranslatorRoute.BEHAVIOR,
                TranslatorRoute.BEHAVIOR_ENGINE -> true

                TranslatorRoute.PRESENTATION,
                TranslatorRoute.PRESENTATION_MODE -> mode != PresentationMode.RAW_PARAM_V2

                TranslatorRoute.PRESENTATION_TEMPLATES -> mode == PresentationMode.TEMPLATE
                TranslatorRoute.PRESENTATION_WIDGETS -> mode == PresentationMode.WIDGET
                TranslatorRoute.PRESENTATION_THEMES -> mode == PresentationMode.STANDARD || mode == PresentationMode.TEMPLATE
                TranslatorRoute.PRESENTATION_LEFT_ICON -> mode == PresentationMode.STANDARD ||
                    (mode == PresentationMode.TEMPLATE && !translator.presentation.templateId.isNullOrBlank())

                TranslatorRoute.PILL,
                TranslatorRoute.PILL_LEFT,
                TranslatorRoute.PILL_RIGHT -> mode != PresentationMode.RAW_PARAM_V2

                TranslatorRoute.PILL_MEDIA_ORDER -> mode != PresentationMode.RAW_PARAM_V2 &&
                    translator.presentation.pill.leftDesign == PillLeftDesign.MEDIA

                TranslatorRoute.PROGRESS,
                TranslatorRoute.PROGRESS_TYPE,
                TranslatorRoute.ACTIONS -> mode == PresentationMode.STANDARD || mode == PresentationMode.TEMPLATE
            }
        }.ifEmpty { listOf(TranslatorRoute.MAIN_MENU) }

        if (validStack.size != stack.size) {
            previousStackSize = stack.size
            stackNames = validStack.map { it.name }
        }
    }

    fun navigateFrom(source: TranslatorRoute, target: TranslatorRoute) {
        val sourceIndex = stack.lastIndexOf(source)
        previousStackSize = stack.size
        stackNames = if (sourceIndex != -1) {
            if (stack.getOrNull(sourceIndex + 1) == target && stack.size == sourceIndex + 2) {
                // Tapping the already-open option in the left pane deselects it and returns to source
                stack.take(sourceIndex + 1).map { it.name }
            } else {
                (stack.take(sourceIndex + 1) + target).map { it.name }
            }
        } else {
            (stack + target).map { it.name }
        }
    }

    fun popFrom(route: TranslatorRoute) {
        if (route == TranslatorRoute.MAIN_MENU) {
            onBack()
            return
        }
        val idx = stack.lastIndexOf(route)
        previousStackSize = stack.size
        if (idx > 0) {
            stackNames = stack.take(idx).map { it.name }
        } else if (stack.size > 1) {
            stackNames = stack.dropLast(1).map { it.name }
        }
    }

    BackHandler(enabled = true) {
        popFrom(stack.last())
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        val isSplitMode = maxWidth >= TRANSLATOR_SPLIT_MIN_WIDTH

        if (isSplitMode) {
            val leftRoute = if (stack.size >= 2) stack[stack.size - 2] else TranslatorRoute.MAIN_MENU
            val rightRoute: TranslatorRoute? = if (stack.size >= 2) stack.last() else null
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
                        label = "TranslatorLeftCascade"
                    ) { route ->
                        val selectedInLeft = if (stack.size >= 2 && route == leftRoute) rightRoute else null
                        if (route == TranslatorRoute.MAIN_MENU) {
                            TranslatorPaneScaffold(
                                route = route,
                                translatorName = translator.meta.name,
                                isLeftPane = true,
                                onBackClick = { popFrom(route) },
                                onSave = onSave
                            ) {
                                TranslatorNodeBody(
                                    route = route,
                                    selectedChild = selectedInLeft,
                                    isSplitMode = true,
                                    isLeftPane = true,
                                    translator = translator,
                                    onTranslatorChange = onTranslatorChange,
                                    installedApps = installedApps,
                                    systemApps = systemApps,
                                    installedThemes = installedThemes,
                                    onFetchChannels = onFetchChannels,
                                    onNavigateFrom = { src, dst -> navigateFrom(src, dst) },
                                    onPopRoute = { popFrom(it) },
                                    onEditMetadata = { showMetaSheet = true }
                                )
                            }
                        } else {
                            TranslatorDetailPaneSurface {
                                TranslatorPaneScaffold(
                                    route = route,
                                    translatorName = translator.meta.name,
                                    isLeftPane = true,
                                    onBackClick = { popFrom(route) },
                                    onSave = onSave
                                ) {
                                    TranslatorNodeBody(
                                        route = route,
                                        selectedChild = selectedInLeft,
                                        isSplitMode = true,
                                        isLeftPane = true,
                                        translator = translator,
                                        onTranslatorChange = onTranslatorChange,
                                        installedApps = installedApps,
                                        systemApps = systemApps,
                                        installedThemes = installedThemes,
                                        onFetchChannels = onFetchChannels,
                                        onNavigateFrom = { src, dst -> navigateFrom(src, dst) },
                                        onPopRoute = { popFrom(it) },
                                        onEditMetadata = { showMetaSheet = true }
                                    )
                                }
                            }
                        }
                    }
                }

                // RIGHT PANE: Shows "Nothing is selected" at depth 1, or active detail/sub-pane at depth 2+
                Box(
                    modifier = Modifier
                        .weight(0.56f)
                        .fillMaxHeight()
                ) {
                    TranslatorDetailPaneSurface {
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
                            label = "TranslatorRightDetail"
                        ) { route ->
                            if (route == null) {
                                TranslatorNothingSelectedPane()
                            } else {
                                TranslatorPaneScaffold(
                                    route = route,
                                    translatorName = translator.meta.name,
                                    isLeftPane = false,
                                    onBackClick = { popFrom(route) },
                                    onSave = onSave
                                ) {
                                    TranslatorNodeBody(
                                        route = route,
                                        selectedChild = null,
                                        isSplitMode = true,
                                        isLeftPane = false,
                                        translator = translator,
                                        onTranslatorChange = onTranslatorChange,
                                        installedApps = installedApps,
                                        systemApps = systemApps,
                                        installedThemes = installedThemes,
                                        onFetchChannels = onFetchChannels,
                                        onNavigateFrom = { src, dst -> navigateFrom(src, dst) },
                                        onPopRoute = { popFrom(it) },
                                        onEditMetadata = { showMetaSheet = true }
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
                    if (isForward && targetState != TranslatorRoute.MAIN_MENU) {
                        slideInHorizontally { it } + fadeIn() togetherWith
                            slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith
                            slideOutHorizontally { it } + fadeOut()
                    }
                },
                label = "TranslatorCompactNav"
            ) { route ->
                TranslatorPaneScaffold(
                    route = route,
                    translatorName = translator.meta.name,
                    isLeftPane = true,
                    onBackClick = { popFrom(route) },
                    onSave = onSave
                ) {
                    TranslatorNodeBody(
                        route = route,
                        selectedChild = null,
                        isSplitMode = false,
                        isLeftPane = true,
                        translator = translator,
                        onTranslatorChange = onTranslatorChange,
                        installedApps = installedApps,
                        systemApps = systemApps,
                        installedThemes = installedThemes,
                        onFetchChannels = onFetchChannels,
                        onNavigateFrom = { src, dst -> navigateFrom(src, dst) },
                        onPopRoute = { popFrom(it) },
                        onEditMetadata = { showMetaSheet = true }
                    )
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
private fun TranslatorDetailPaneSurface(
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
fun TranslatorNothingSelectedPane() {
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
                text = stringResource(R.string.translator_nothing_selected_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TranslatorPaneScaffold(
    route: TranslatorRoute,
    translatorName: String,
    isLeftPane: Boolean,
    onBackClick: () -> Unit,
    onSave: () -> Unit,
    content: @Composable () -> Unit
) {
    val titleText = when (route) {
        TranslatorRoute.MAIN_MENU -> translatorName.ifBlank { stringResource(R.string.translator_editor_title_edit) }
        TranslatorRoute.CONDITIONS -> stringResource(R.string.translator_conditions_title)
        TranslatorRoute.CONDITIONS_ADD -> stringResource(R.string.translator_cond_sheet_title)
        TranslatorRoute.PRESENTATION -> stringResource(R.string.translator_menu_presentation)
        TranslatorRoute.PRESENTATION_MODE -> stringResource(R.string.translator_pres_mode_sheet_title)
        TranslatorRoute.PRESENTATION_TEMPLATES -> stringResource(R.string.translator_pres_template_sheet_title)
        TranslatorRoute.PRESENTATION_WIDGETS -> stringResource(R.string.select_app_widget)
        TranslatorRoute.PRESENTATION_THEMES -> stringResource(R.string.translator_pres_theme_sheet_title)
        TranslatorRoute.PRESENTATION_LEFT_ICON -> stringResource(R.string.translator_pres_icon_sheet_title)
        TranslatorRoute.PILL -> stringResource(R.string.translator_menu_pill)
        TranslatorRoute.PILL_LEFT -> stringResource(R.string.translator_pill_left_sheet_title)
        TranslatorRoute.PILL_RIGHT -> stringResource(R.string.translator_pill_right_sheet_title)
        TranslatorRoute.PILL_MEDIA_ORDER -> stringResource(R.string.translator_pill_media_order_title)
        TranslatorRoute.PROGRESS -> stringResource(R.string.translator_menu_progress)
        TranslatorRoute.PROGRESS_TYPE -> stringResource(R.string.translator_prog_type_sheet_title)
        TranslatorRoute.ACTIONS -> stringResource(R.string.translator_actions_title)
        TranslatorRoute.BEHAVIOR -> stringResource(R.string.translator_menu_behavior)
        TranslatorRoute.BEHAVIOR_ENGINE -> stringResource(R.string.translator_behavior_engine_mode_sheet_title)
        TranslatorRoute.APPS -> stringResource(R.string.translator_select_apps)
        TranslatorRoute.APPS_SCOPE -> stringResource(R.string.translator_scope_sheet_title)
    }

    val subtitleText: String? = when (route) {
        TranslatorRoute.CONDITIONS_ADD -> stringResource(R.string.translator_conditions_title)
        TranslatorRoute.PRESENTATION_MODE,
        TranslatorRoute.PRESENTATION_TEMPLATES,
        TranslatorRoute.PRESENTATION_WIDGETS,
        TranslatorRoute.PRESENTATION_THEMES,
        TranslatorRoute.PRESENTATION_LEFT_ICON -> stringResource(R.string.translator_menu_presentation)
        TranslatorRoute.PILL_LEFT,
        TranslatorRoute.PILL_RIGHT,
        TranslatorRoute.PILL_MEDIA_ORDER -> stringResource(R.string.translator_menu_pill)
        TranslatorRoute.PROGRESS_TYPE -> stringResource(R.string.translator_menu_progress)
        TranslatorRoute.BEHAVIOR_ENGINE -> stringResource(R.string.translator_menu_behavior)
        TranslatorRoute.APPS_SCOPE -> stringResource(R.string.translator_select_apps)
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
                    if (route == TranslatorRoute.MAIN_MENU) {
                        Button(
                            onClick = onSave,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
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
            content()
        }
    }
}

@Composable
private fun TranslatorNodeBody(
    route: TranslatorRoute,
    selectedChild: TranslatorRoute?,
    isSplitMode: Boolean,
    isLeftPane: Boolean,
    translator: CustomTranslator,
    onTranslatorChange: (CustomTranslator) -> Unit,
    installedApps: List<AppItem>,
    systemApps: List<AppItem>,
    installedThemes: List<com.d4viddf.hyperbridge.models.theme.HyperTheme>,
    onFetchChannels: suspend (List<String>) -> List<NotificationChannelInfo>,
    onNavigateFrom: (source: TranslatorRoute, target: TranslatorRoute) -> Unit,
    onPopRoute: (TranslatorRoute) -> Unit,
    onEditMetadata: () -> Unit
) {
    // In split mode at Depth 2 (!isLeftPane), MAIN_MENU on the left already displays the live preview.
    // On phones (!isSplitMode) or when cascaded into the left pane at Depth 3+ (isLeftPane), keep the sticky preview.
    val showDetailShellPreview = !isSplitMode || isLeftPane
    val subRouteCallback: ((TranslatorRoute) -> Unit)? = if (isSplitMode) {
        { subRoute -> onNavigateFrom(route, subRoute) }
    } else {
        null
    }

    @Composable
    fun MaybeDetailShell(body: @Composable () -> Unit) {
        if (showDetailShellPreview) {
            TranslatorDetailShell(
                previewContent = {
                    HyperOsIslandPreview(
                        translator = translator,
                        installedThemes = installedThemes,
                        showChrome = !isSplitMode
                    )
                }
            ) {
                body()
            }
        } else {
            Box(Modifier.fillMaxSize()) {
                body()
            }
        }
    }

    when (route) {
        TranslatorRoute.MAIN_MENU -> TranslatorMainList(
            translator = translator,
            installedThemes = installedThemes,
            selectedRoute = selectedChild,
            onNavigate = { target -> onNavigateFrom(TranslatorRoute.MAIN_MENU, target) },
            onEditMetadata = onEditMetadata
        )

        TranslatorRoute.CONDITIONS -> Box(Modifier.fillMaxSize()) {
            TranslatorConditionsContent(
                conditions = translator.conditions,
                targetPackages = translator.targetPackages,
                targetNotificationTypes = translator.targetNotificationTypes,
                selectedSubRoute = selectedChild,
                onOpenSubRoute = subRouteCallback,
                onFetchChannels = onFetchChannels,
                onChange = { onTranslatorChange(translator.copy(conditions = it)) }
            )
        }
        TranslatorRoute.CONDITIONS_ADD -> Box(Modifier.fillMaxSize()) {
            AddConditionFieldContent(
                conditions = translator.conditions,
                targetNotificationTypes = translator.targetNotificationTypes,
                showHeader = false,
                onConditionFieldAdded = { updated ->
                    onTranslatorChange(translator.copy(conditions = updated))
                }
            )
        }

        TranslatorRoute.PRESENTATION -> MaybeDetailShell {
            TranslatorPresentationContent(
                presentation = translator.presentation,
                themeBinding = translator.themeBinding,
                installedThemes = installedThemes,
                selectedSubRoute = selectedChild,
                onOpenSubRoute = subRouteCallback,
                onPresentationChange = { onTranslatorChange(translator.copy(presentation = it)) },
                onThemeBindingChange = { onTranslatorChange(translator.copy(themeBinding = it)) }
            )
        }
        TranslatorRoute.PRESENTATION_MODE -> Box(Modifier.fillMaxSize()) {
            PresentationModeSelectionContent(
                currentMode = translator.presentation.mode,
                showHeader = false,
                onModeSelected = { newMode ->
                    onTranslatorChange(
                        translator.copy(
                            presentation = IslandTemplateCatalog.effectivePresentation(
                                translator.presentation.copy(mode = newMode)
                            )
                        )
                    )
                }
            )
        }
        TranslatorRoute.PRESENTATION_TEMPLATES -> Box(Modifier.fillMaxSize()) {
            TemplateSelectionContent(
                currentTemplateId = translator.presentation.templateId,
                showHeader = false,
                onTemplateSelected = { newTemplateId ->
                    onTranslatorChange(
                        translator.copy(
                            presentation = IslandTemplateCatalog.applyTemplate(
                                translator.presentation,
                                newTemplateId
                            )
                        )
                    )
                }
            )
        }
        TranslatorRoute.PRESENTATION_WIDGETS -> Box(Modifier.fillMaxSize()) {
            WidgetSelectionContent(
                currentWidgetId = translator.presentation.widgetId,
                showHeader = false,
                onWidgetSelected = { newWidgetId ->
                    onTranslatorChange(
                        translator.copy(
                            presentation = translator.presentation.copy(widgetId = newWidgetId)
                        )
                    )
                }
            )
        }
        TranslatorRoute.PRESENTATION_THEMES -> Box(Modifier.fillMaxSize()) {
            ThemeSelectionContent(
                currentThemeId = translator.themeBinding.themeId,
                installedThemes = installedThemes,
                showHeader = false,
                onThemeSelected = { newThemeId ->
                    onTranslatorChange(
                        translator.copy(
                            themeBinding = translator.themeBinding.copy(themeId = newThemeId)
                        )
                    )
                }
            )
        }
        TranslatorRoute.PRESENTATION_LEFT_ICON -> Box(Modifier.fillMaxSize()) {
            LeftIconSelectionContent(
                currentSource = translator.presentation.leftSlot.source,
                showHeader = false,
                onSourceSelected = { newSource ->
                    onTranslatorChange(
                        translator.copy(
                            presentation = translator.presentation.copy(
                                leftSlot = translator.presentation.leftSlot.copy(source = newSource)
                            )
                        )
                    )
                }
            )
        }

        TranslatorRoute.PILL -> Box(Modifier.fillMaxSize()) {
            TranslatorPillContent(
                pillConfig = translator.presentation.pill,
                installedThemes = installedThemes,
                themeBinding = translator.themeBinding,
                showInlinePreview = showDetailShellPreview,
                selectedSubRoute = selectedChild,
                onOpenSubRoute = subRouteCallback,
                onPillConfigChange = {
                    onTranslatorChange(
                        translator.copy(presentation = translator.presentation.copy(pill = it))
                    )
                }
            )
        }
        TranslatorRoute.PILL_LEFT -> Box(Modifier.fillMaxSize()) {
            TranslatorPillLeftSelectionContent(
                selected = translator.presentation.pill.leftDesign,
                showHeader = false,
                onSelect = { newLeft ->
                    onTranslatorChange(
                        translator.copy(
                            presentation = translator.presentation.copy(
                                pill = translator.presentation.pill.copy(leftDesign = newLeft)
                            )
                        )
                    )
                }
            )
        }
        TranslatorRoute.PILL_RIGHT -> Box(Modifier.fillMaxSize()) {
            TranslatorPillRightSelectionContent(
                selected = translator.presentation.pill.rightDesign,
                showHeader = false,
                onSelect = { newRight ->
                    onTranslatorChange(
                        translator.copy(
                            presentation = translator.presentation.copy(
                                pill = translator.presentation.pill.copy(rightDesign = newRight)
                            )
                        )
                    )
                }
            )
        }
        TranslatorRoute.PILL_MEDIA_ORDER -> Box(Modifier.fillMaxSize()) {
            TranslatorPillMediaOrderContent(
                mediaOrder = translator.presentation.pill.mediaOrder,
                showHeader = false,
                onUpdateOrder = { newOrder ->
                    onTranslatorChange(
                        translator.copy(
                            presentation = translator.presentation.copy(
                                pill = translator.presentation.pill.copy(mediaOrder = newOrder)
                            )
                        )
                    )
                }
            )
        }

        TranslatorRoute.PROGRESS -> MaybeDetailShell {
            TranslatorProgressContent(
                progressSlot = translator.presentation.progressSlot,
                dataExtraction = translator.dataExtraction,
                selectedSubRoute = selectedChild,
                onOpenSubRoute = subRouteCallback,
                onProgressSlotChange = {
                    onTranslatorChange(
                        translator.copy(presentation = translator.presentation.copy(progressSlot = it))
                    )
                },
                onDataExtractionChange = { onTranslatorChange(translator.copy(dataExtraction = it)) }
            )
        }
        TranslatorRoute.PROGRESS_TYPE -> Box(Modifier.fillMaxSize()) {
            ProgressSlotTypeSelectionContent(
                currentType = translator.presentation.progressSlot.type,
                showHeader = false,
                onTypeSelected = { newType ->
                    onTranslatorChange(
                        translator.copy(
                            presentation = translator.presentation.copy(
                                progressSlot = translator.presentation.progressSlot.copy(type = newType)
                            )
                        )
                    )
                }
            )
        }

        TranslatorRoute.ACTIONS -> MaybeDetailShell {
            TranslatorActionsContent(
                actionSlots = translator.presentation.actionSlots,
                onChange = {
                    onTranslatorChange(
                        translator.copy(presentation = translator.presentation.copy(actionSlots = it))
                    )
                }
            )
        }

        TranslatorRoute.BEHAVIOR -> Box(Modifier.fillMaxSize()) {
            TranslatorBehaviorContent(
                behavior = translator.behaviorOverride,
                engineMode = translator.engineMode,
                selectedSubRoute = selectedChild,
                onOpenSubRoute = subRouteCallback,
                onBehaviorChange = { onTranslatorChange(translator.copy(behaviorOverride = it)) },
                onEngineModeChange = { onTranslatorChange(translator.copy(engineMode = it)) }
            )
        }
        TranslatorRoute.BEHAVIOR_ENGINE -> Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(
                    text = stringResource(R.string.preview),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(10.dp))
                EnginePreview(isNative = translator.engineMode == EngineMode.NATIVE_LIVE_UPDATE)
                Spacer(Modifier.height(12.dp))
            }
            TranslatorEngineSelectionContent(
                currentMode = translator.engineMode,
                showHeader = false,
                onModeSelected = { newEngine ->
                    onTranslatorChange(translator.copy(engineMode = newEngine))
                }
            )
        }

        TranslatorRoute.APPS -> Box(Modifier.fillMaxSize()) {
            TranslatorAppsContent(
                translator = translator,
                installedApps = installedApps,
                systemApps = systemApps,
                selectedSubRoute = selectedChild,
                onOpenSubRoute = subRouteCallback,
                onTargetScopeChange = { scope, packages ->
                    onTranslatorChange(translator.copy(targetScope = scope, targetPackages = packages))
                },
                onTargetNotificationTypesChange = { types ->
                    onTranslatorChange(translator.copy(targetNotificationTypes = types))
                }
            )
        }
        TranslatorRoute.APPS_SCOPE -> Box(Modifier.fillMaxSize()) {
            TranslatorScopeSelectionContent(
                currentScope = translator.targetScope,
                showHeader = false,
                onScopeSelected = { newScope ->
                    val updatedPackages = if (newScope == TargetScope.GLOBAL || newScope == TargetScope.NOTIFICATION_TYPE) {
                        emptyList()
                    } else if (newScope != translator.targetScope) {
                        emptyList()
                    } else {
                        translator.targetPackages
                    }
                    onTranslatorChange(
                        translator.copy(
                            targetScope = newScope,
                            targetPackages = updatedPackages
                        )
                    )
                    if (!isSplitMode) {
                        onPopRoute(TranslatorRoute.APPS_SCOPE)
                    }
                }
            )
        }
    }
}

@Composable
fun TranslatorMainList(
    translator: CustomTranslator,
    installedThemes: List<com.d4viddf.hyperbridge.models.theme.HyperTheme> = emptyList(),
    selectedRoute: TranslatorRoute? = null,
    onNavigate: (TranslatorRoute) -> Unit,
    onEditMetadata: () -> Unit
) {
    var previewMode by remember { mutableStateOf(IslandPreviewMode.EXPANDED) }

    LaunchedEffect(selectedRoute) {
        when (selectedRoute) {
            TranslatorRoute.PILL -> previewMode = IslandPreviewMode.COMPACT_PILL
            TranslatorRoute.PRESENTATION,
            TranslatorRoute.PROGRESS,
            TranslatorRoute.ACTIONS -> previewMode = IslandPreviewMode.EXPANDED
            else -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            HyperOsIslandPreview(
                translator = translator,
                installedThemes = installedThemes,
                mode = previewMode,
                onModeChange = { previewMode = it }
            )
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
                Icon(
                    imageVector = getTranslatorOutlinedIcon(translator.meta.iconName),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.translator_btn_edit_meta),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(16.dp))

            val isRawParamV2 = translator.presentation.mode == PresentationMode.RAW_PARAM_V2

            if (isRawParamV2) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Construction,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = stringResource(R.string.translator_raw_param_banner_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                        Text(
                            text = stringResource(R.string.translator_raw_param_banner_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            val isWidget = translator.presentation.mode == PresentationMode.WIDGET

            val menuItems = when {
                isRawParamV2 -> listOf(
                    TranslatorRoute.APPS,
                    TranslatorRoute.CONDITIONS,
                    TranslatorRoute.BEHAVIOR
                )
                isWidget -> listOf(
                    TranslatorRoute.APPS,
                    TranslatorRoute.CONDITIONS,
                    TranslatorRoute.PRESENTATION,
                    TranslatorRoute.PILL,
                    TranslatorRoute.BEHAVIOR
                )
                else -> listOf(
                    TranslatorRoute.APPS,
                    TranslatorRoute.CONDITIONS,
                    TranslatorRoute.PRESENTATION,
                    TranslatorRoute.PILL,
                    TranslatorRoute.PROGRESS,
                    TranslatorRoute.ACTIONS,
                    TranslatorRoute.BEHAVIOR
                )
            }

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

                    val icon = when (route) {
                        TranslatorRoute.APPS -> Icons.Outlined.Apps
                        TranslatorRoute.CONDITIONS -> Icons.Outlined.FilterList
                        TranslatorRoute.PRESENTATION -> Icons.AutoMirrored.Outlined.ViewQuilt
                        TranslatorRoute.PILL -> Icons.Outlined.DashboardCustomize
                        TranslatorRoute.PROGRESS -> Icons.Outlined.Speed
                        TranslatorRoute.ACTIONS -> Icons.Outlined.TouchApp
                        TranslatorRoute.BEHAVIOR -> Icons.Outlined.DisplaySettings
                        else -> Icons.Outlined.AutoAwesome
                    }

                    val title = when (route) {
                        TranslatorRoute.APPS -> stringResource(R.string.translator_select_apps)
                        TranslatorRoute.CONDITIONS -> stringResource(R.string.translator_conditions_title)
                        TranslatorRoute.PRESENTATION -> stringResource(R.string.translator_menu_presentation)
                        TranslatorRoute.PILL -> stringResource(R.string.translator_menu_pill)
                        TranslatorRoute.PROGRESS -> stringResource(R.string.translator_menu_progress)
                        TranslatorRoute.ACTIONS -> stringResource(R.string.translator_actions_title)
                        TranslatorRoute.BEHAVIOR -> stringResource(R.string.translator_menu_behavior)
                        else -> ""
                    }

                    val subtitle = when (route) {
                        TranslatorRoute.APPS -> when (translator.targetScope) {
                            TargetScope.GLOBAL -> stringResource(R.string.translator_scope_global)
                            TargetScope.SYSTEM_APPS -> stringResource(R.string.translators_scope_system_apps, translator.targetPackages.size)
                            TargetScope.SPECIFIC_APPS -> stringResource(R.string.translator_scope_apps_fmt, translator.targetPackages.size)
                            TargetScope.NOTIFICATION_TYPE -> stringResource(R.string.translators_scope_types, translator.targetNotificationTypes.size)
                        }
                        TranslatorRoute.CONDITIONS -> stringResource(R.string.translator_menu_conditions_sub)
                        TranslatorRoute.PRESENTATION -> stringResource(R.string.translator_menu_presentation_sub)
                        TranslatorRoute.PILL -> stringResource(R.string.translator_menu_pill_sub)
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
                        onClick = { onNavigate(route) },
                        selected = isSelected
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
fun TranslatorDetailShell(
    previewContent: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            previewContent()
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TranslatorLivePreviewBar(
    translator: CustomTranslator,
    installedThemes: List<com.d4viddf.hyperbridge.models.theme.HyperTheme> = emptyList()
) {
    val linkedTheme = if (translator.themeBinding.themeId.isNotBlank() && translator.themeBinding.themeId != "active") {
        installedThemes.find { it.id == translator.themeBinding.themeId }
    } else {
        null
    }

    val highlightColor = if (linkedTheme?.global?.highlightColor != null) {
        safeParseColor(linkedTheme.global.highlightColor)
    } else {
        MaterialTheme.colorScheme.primary
    }

    val textColor = if (linkedTheme?.global?.textColor != null) {
        safeParseColor(linkedTheme.global.textColor)
    } else {
        Color.White
    }

    val iconShapeId = linkedTheme?.global?.iconShapeId ?: "circle"
    val iconShape = getShapeFromId(iconShapeId).toShape()

    val leftSlotSource = translator.presentation.leftSlot.source.uppercase()

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
            if (leftSlotSource != "HIDDEN") {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(iconShape)
                        .background(highlightColor),
                    contentAlignment = Alignment.Center
                ) {
                    if (leftSlotSource == "AVATAR") {
                        Icon(
                            Icons.Outlined.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Icon(
                            Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = translator.presentation.textSlot.titleTemplate
                        .replace("{notif.title}", "Sample Title")
                        .replace("{notif.text}", "Sample Notification Message")
                        .replace("{notif.subtext}", "Subtext")
                        .replace("{notif.sender}", "Alice")
                        .replace("{notif.conversation}", "Family Chat")
                        .replace("{notif.app}", "Messenger"),
                    style = MaterialTheme.typography.labelLarge,
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = translator.presentation.textSlot.subtitleTemplate
                        .replace("{notif.title}", "Sample Title")
                        .replace("{notif.text}", "Sample Notification Message")
                        .replace("{notif.subtext}", "Subtext")
                        .replace("{notif.sender}", "Alice")
                        .replace("{notif.conversation}", "Family Chat")
                        .replace("{notif.app}", "Messenger"),
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.7f),
                    maxLines = 1
                )
            }

            if (translator.presentation.progressSlot.type != ProgressSlotType.NONE) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(highlightColor.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Speed,
                        contentDescription = null,
                        tint = highlightColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            if (translator.presentation.actionSlots.isNotEmpty()) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = highlightColor,
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
    selectedSubRoute: TranslatorRoute? = null,
    onOpenSubRoute: ((TranslatorRoute) -> Unit)? = null,
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
        val isAddPatternSelected = selectedSubRoute == TranslatorRoute.CONDITIONS_ADD
        Button(
            onClick = {
                if (onOpenSubRoute != null) {
                    onOpenSubRoute(TranslatorRoute.CONDITIONS_ADD)
                } else {
                    showAddPatternSheet = true
                }
            },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            border = if (isAddPatternSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isAddPatternSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                },
                contentColor = if (isAddPatternSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                }
            )
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.translator_cond_sheet_title), fontWeight = FontWeight.SemiBold)
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
                            text = conditions.channelId.ifBlank { stringResource(R.string.translator_cond_channel_select_btn) },
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
                        checked = conditions.hasProgress,
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
                        checked = conditions.hasActions,
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
                        checked = messaging.isGroupConversation,
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
                        checked = media.isPlaying,
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
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )

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
    showHeader: Boolean = true,
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
        ConditionFieldOption(ConditionFieldKey.NAV_INSTRUCTION_REGEX, R.string.translator_cond_nav_instruction, Icons.Outlined.Navigation, R.string.cat_nav),
        ConditionFieldOption(ConditionFieldKey.NAV_DISTANCE_REGEX, R.string.translator_cond_nav_distance, Icons.Outlined.NearMe, R.string.cat_nav),

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
            R.string.cat_nav -> targetNotificationTypes.contains(NotificationType.NAVIGATION.name)
            R.string.translator_cond_group_progress -> targetNotificationTypes.contains(NotificationType.PROGRESS.name) || targetNotificationTypes.contains(NotificationType.DOWNLOAD.name)
            else -> true
        }
    }

    val grouped = filteredOptions.groupBy { it.groupRes }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (showHeader) Modifier.padding(24.dp).navigationBarsPadding() else Modifier.fillMaxSize().padding(16.dp))
    ) {
        if (showHeader) {
            Text(
                text = stringResource(R.string.translator_cond_sheet_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = !showHeader),
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
    themeBinding: ThemeBinding = ThemeBinding(),
    installedThemes: List<com.d4viddf.hyperbridge.models.theme.HyperTheme> = emptyList(),
    selectedSubRoute: TranslatorRoute? = null,
    onOpenSubRoute: ((TranslatorRoute) -> Unit)? = null,
    onPresentationChange: (PresentationConfig) -> Unit,
    onThemeBindingChange: (ThemeBinding) -> Unit
) {
    var showModeSheet by remember { mutableStateOf(false) }
    var showThemeSheet by remember { mutableStateOf(false) }
    var showTemplateSheet by remember { mutableStateOf(false) }
    var showWidgetSheet by remember { mutableStateOf(false) }
    var showLeftIconSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. PRESENTATION MODE CARD ---
        val currentModeName = when (presentation.mode) {
            PresentationMode.STANDARD -> stringResource(R.string.translator_pres_mode_standard)
            PresentationMode.TEMPLATE -> stringResource(R.string.translator_pres_mode_template)
            PresentationMode.WIDGET -> stringResource(R.string.translator_pres_mode_widget)
            PresentationMode.RAW_PARAM_V2 -> stringResource(R.string.translator_raw_param_badge)
        }
        val currentModeDesc = when (presentation.mode) {
            PresentationMode.STANDARD -> stringResource(R.string.translator_pres_mode_standard_desc)
            PresentationMode.TEMPLATE -> stringResource(R.string.translator_pres_mode_template_desc)
            PresentationMode.WIDGET -> stringResource(R.string.translator_pres_mode_widget_desc)
            PresentationMode.RAW_PARAM_V2 -> stringResource(R.string.translator_raw_param_banner_desc)
        }
        val currentModeIcon = when (presentation.mode) {
            PresentationMode.STANDARD -> Icons.AutoMirrored.Outlined.ViewQuilt
            PresentationMode.TEMPLATE -> Icons.Outlined.DashboardCustomize
            PresentationMode.WIDGET -> Icons.Outlined.Widgets
            PresentationMode.RAW_PARAM_V2 -> Icons.Outlined.Construction
        }

        Card(
            shape = RoundedCornerShape(24.dp),
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
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                currentModeIcon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.translator_pres_mode_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.translator_pres_mode_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                val isModeSelected = selectedSubRoute == TranslatorRoute.PRESENTATION_MODE
                val openModePicker = {
                    if (onOpenSubRoute != null) {
                        onOpenSubRoute(TranslatorRoute.PRESENTATION_MODE)
                    } else {
                        showModeSheet = true
                    }
                }
                Card(
                    shape = RoundedCornerShape(16.dp),
                    border = if (isModeSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                    colors = CardDefaults.cardColors(
                        containerColor = if (isModeSelected) {
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        }
                    ),
                    onClick = openModePicker,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            currentModeIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentModeName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = currentModeDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        FilledTonalButton(
                            onClick = openModePicker,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(stringResource(R.string.translator_pres_mode_select_btn), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        // --- 2. WIP / SELECTOR CARDS FOR TEMPLATE & WIDGET ---
        if (presentation.mode == PresentationMode.TEMPLATE) {
            val isTemplateSelected = selectedSubRoute == TranslatorRoute.PRESENTATION_TEMPLATES
            Card(
                shape = RoundedCornerShape(20.dp),
                border = if (isTemplateSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                colors = CardDefaults.cardColors(
                    containerColor = if (isTemplateSelected) {
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Outlined.DashboardCustomize,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.translator_pres_mode_template),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = stringResource(R.string.translator_pres_template_gallery_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = {
                            if (onOpenSubRoute != null) {
                                onOpenSubRoute(TranslatorRoute.PRESENTATION_TEMPLATES)
                            } else {
                                showTemplateSheet = true
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isTemplateSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                            contentColor = if (isTemplateSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onTertiary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.DashboardCustomize, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        val pickedTemplate = IslandTemplateCatalog.find(presentation.templateId)
                        Text(
                            text = when {
                                pickedTemplate != null -> stringResource(pickedTemplate.nameRes)
                                presentation.templateId.isNullOrBlank() ->
                                    stringResource(R.string.translator_pres_select_template_btn)
                                else -> presentation.templateId
                            }
                        )
                    }
                }
            }
        }

        if (presentation.mode == PresentationMode.WIDGET) {
            val context = LocalContext.current
            val repository = remember(context) { CustomWidgetRepository(context) }
            val selectedDoc by produceState<CustomWidgetDocument?>(initialValue = null, presentation.widgetId) {
                value = if (!presentation.widgetId.isNullOrBlank()) {
                    repository.getWidget(presentation.widgetId)
                } else null
            }

            val isWidgetSelected = selectedSubRoute == TranslatorRoute.PRESENTATION_WIDGETS
            Card(
                shape = RoundedCornerShape(20.dp),
                border = if (isWidgetSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                colors = CardDefaults.cardColors(
                    containerColor = if (isWidgetSelected) {
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Outlined.Widgets,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.custom_design_title),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = stringResource(R.string.translator_pres_widget_wip_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = {
                            if (onOpenSubRoute != null) {
                                onOpenSubRoute(TranslatorRoute.PRESENTATION_WIDGETS)
                            } else {
                                showWidgetSheet = true
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isWidgetSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                            contentColor = if (isWidgetSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onTertiary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.Widgets, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = selectedDoc?.canvas?.let { "${it.name} (${it.heightDp}dp)" }
                                ?: if (presentation.widgetId.isNullOrBlank()) {
                                    stringResource(R.string.select_app_widget)
                                } else {
                                    presentation.widgetId
                                }
                        )
                    }
                }
            }
        }

        // --- 3. THEME LINKING CARD ---
        // (Applies to STANDARD and TEMPLATE modes)
        if (presentation.mode != PresentationMode.WIDGET) {
            val currentThemeName = if (themeBinding.themeId.isBlank() || themeBinding.themeId == "active") {
                stringResource(R.string.translator_pres_theme_active)
            } else {
                installedThemes.find { it.id == themeBinding.themeId }?.meta?.name ?: themeBinding.themeId
            }

            Card(
                shape = RoundedCornerShape(24.dp),
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
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Outlined.Palette,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.translator_pres_theme_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    val isThemeSelected = selectedSubRoute == TranslatorRoute.PRESENTATION_THEMES
                    val openThemePicker = {
                        if (onOpenSubRoute != null) {
                            onOpenSubRoute(TranslatorRoute.PRESENTATION_THEMES)
                        } else {
                            showThemeSheet = true
                        }
                    }
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        border = if (isThemeSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                        colors = CardDefaults.cardColors(
                            containerColor = if (isThemeSelected) {
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            }
                        ),
                        onClick = openThemePicker,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Brush,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentThemeName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )

                            }
                            FilledTonalButton(
                                onClick = openThemePicker,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(stringResource(R.string.translator_pres_theme_select_btn), style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }

        // --- 4. SLOT BINDINGS: STANDARD, and TEMPLATE once a layout is picked ---
        // A template fixes the layout; each element is still bound to notification data here (#272).
        if (presentation.mode == PresentationMode.STANDARD ||
            (presentation.mode == PresentationMode.TEMPLATE && !presentation.templateId.isNullOrBlank())
        ) {
            // LEFT ICON SLOT SOURCE
            val currentIconSourceKey = presentation.leftSlot.source.uppercase()
            val currentIconSourceName = when (currentIconSourceKey) {
                "AVATAR" -> stringResource(R.string.translator_pres_icon_source_avatar)
                "HIDDEN" -> stringResource(R.string.translator_pres_icon_source_hidden)
                else -> stringResource(R.string.translator_pres_icon_source_app)
            }
            val currentIconVector = when (currentIconSourceKey) {
                "AVATAR" -> Icons.Outlined.Person
                "HIDDEN" -> Icons.Outlined.Image
                else -> Icons.Outlined.AutoAwesome
            }

            Card(
                shape = RoundedCornerShape(24.dp),
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
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Outlined.Image,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.translator_pres_icon_source_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.translator_pres_icon_source_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    val isLeftIconSelected = selectedSubRoute == TranslatorRoute.PRESENTATION_LEFT_ICON
                    val openLeftIconPicker = {
                        if (onOpenSubRoute != null) {
                            onOpenSubRoute(TranslatorRoute.PRESENTATION_LEFT_ICON)
                        } else {
                            showLeftIconSheet = true
                        }
                    }
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        border = if (isLeftIconSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                        colors = CardDefaults.cardColors(
                            containerColor = if (isLeftIconSelected) {
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            }
                        ),
                        onClick = openLeftIconPicker,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                currentIconVector,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentIconSourceName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Source: $currentIconSourceKey",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            FilledTonalButton(
                                onClick = openLeftIconPicker,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(stringResource(R.string.translator_pres_icon_select_btn), style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }

            // TEXT TEMPLATES & EXPRESSIVE VARIABLE CHIPS
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.TextFields,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.translator_pres_text_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.translator_pres_text_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    val availableTokens = listOf(
                        "{notif.title}",
                        "{notif.text}",
                        "{notif.subtext}",
                        "{notif.sender}",
                        "{notif.conversation}",
                        "{notif.app}",
                        "{regex.1}",
                        "{regex.2}"
                    )

                    // TITLE TEMPLATE
                    TranslatorTemplateFieldWithChips(
                        value = presentation.textSlot.titleTemplate,
                        label = stringResource(R.string.translator_pres_title_tpl),
                        availableTokens = availableTokens,
                        onValueChange = {
                            onPresentationChange(
                                presentation.copy(textSlot = presentation.textSlot.copy(titleTemplate = it))
                            )
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // SUBTITLE TEMPLATE
                    TranslatorTemplateFieldWithChips(
                        value = presentation.textSlot.subtitleTemplate,
                        label = stringResource(R.string.translator_pres_sub_tpl),
                        availableTokens = availableTokens,
                        onValueChange = {
                            onPresentationChange(
                                presentation.copy(textSlot = presentation.textSlot.copy(subtitleTemplate = it))
                            )
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // HIGHLIGHT TEXT TEMPLATE (OPTIONAL)
                    TranslatorTemplateFieldWithChips(
                        value = presentation.textSlot.highlightTextTemplate ?: "",
                        label = stringResource(R.string.translator_pres_highlight_tpl),
                        availableTokens = availableTokens,
                        onValueChange = {
                            onPresentationChange(
                                presentation.copy(
                                    textSlot = presentation.textSlot.copy(highlightTextTemplate = it.ifBlank { null })
                                )
                            )
                        }
                    )
                }
            }
        }
    }

    // --- BOTTOM SHEETS ---
    if (showModeSheet) {
        PresentationModeSelectionSheet(
            currentMode = presentation.mode,
            onDismiss = { showModeSheet = false },
            onModeSelected = { newMode ->
                // A bare templateId is materialized here, so later edits start from the preset.
                onPresentationChange(IslandTemplateCatalog.effectivePresentation(presentation.copy(mode = newMode)))
                showModeSheet = false
            }
        )
    }

    if (showThemeSheet) {
        ThemeSelectionSheet(
            currentThemeId = themeBinding.themeId,
            installedThemes = installedThemes,
            onDismiss = { showThemeSheet = false },
            onThemeSelected = { newThemeId ->
                onThemeBindingChange(themeBinding.copy(themeId = newThemeId))
                showThemeSheet = false
            }
        )
    }

    if (showTemplateSheet) {
        TemplateSelectionSheet(
            currentTemplateId = presentation.templateId,
            onDismiss = { showTemplateSheet = false },
            onTemplateSelected = { newTemplateId ->
                onPresentationChange(IslandTemplateCatalog.applyTemplate(presentation, newTemplateId))
                showTemplateSheet = false
            }
        )
    }

    if (showWidgetSheet) {
        WidgetSelectionSheet(
            currentWidgetId = presentation.widgetId,
            onDismiss = { showWidgetSheet = false },
            onWidgetSelected = { newWidgetId ->
                onPresentationChange(presentation.copy(widgetId = newWidgetId))
                showWidgetSheet = false
            }
        )
    }

    if (showLeftIconSheet) {
        LeftIconSelectionSheet(
            currentSource = presentation.leftSlot.source,
            onDismiss = { showLeftIconSheet = false },
            onSourceSelected = { newSource ->
                onPresentationChange(presentation.copy(leftSlot = presentation.leftSlot.copy(source = newSource)))
                showLeftIconSheet = false
            }
        )
    }
}

// --- TEMPLATE FIELD WITH DISMISSABLE VARIABLE CHIPS & SCROLLABLE INSERT ROW ---
@Composable
fun TranslatorTemplateFieldWithChips(
    value: String,
    label: String,
    availableTokens: List<String>,
    onValueChange: (String) -> Unit
) {
    // Extract tokens currently present in value
    val presentTokens = availableTokens.filter { value.contains(it) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            singleLine = true
        )

        // Dismissable chips for active tokens in the textfield
        if (presentTokens.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                presentTokens.forEach { token ->
                    InputChip(
                        selected = true,
                        onClick = {
                            val updated = value.replace(token, "").replace("  ", " ").trim()
                            onValueChange(updated)
                        },
                        label = { Text(token, style = MaterialTheme.typography.labelSmall) },
                        trailingIcon = {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove $token",
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = InputChipDefaults.inputChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }

        // Quick Insert Tokens scrollable horizontal row
        Text(
            text = stringResource(R.string.translator_pres_tags_hint),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            availableTokens.forEach { token ->
                SuggestionChip(
                    onClick = {
                        val separator = if (value.isNotBlank() && !value.endsWith(" ")) " " else ""
                        onValueChange((value + separator + token).trim())
                    },
                    label = { Text(token, style = MaterialTheme.typography.labelSmall) },
                    shape = RoundedCornerShape(8.dp),
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                )
            }
        }
    }
}

// --- PRESENTATION MODE SELECTION SHEET ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresentationModeSelectionSheet(
    currentMode: PresentationMode,
    onDismiss: () -> Unit,
    onModeSelected: (PresentationMode) -> Unit
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        PresentationModeSelectionContent(
            currentMode = currentMode,
            onModeSelected = onModeSelected
        )
    }
}

@Composable
fun PresentationModeSelectionContent(
    currentMode: PresentationMode,
    showHeader: Boolean = true,
    onModeSelected: (PresentationMode) -> Unit
) {
    val modes = listOf(
        Triple(
            PresentationMode.STANDARD,
            Pair(R.string.translator_pres_mode_standard, R.string.translator_pres_mode_standard_desc),
            Icons.AutoMirrored.Outlined.ViewQuilt
        ),
        Triple(
            PresentationMode.TEMPLATE,
            Pair(R.string.translator_pres_mode_template, R.string.translator_pres_mode_template_desc),
            Icons.Outlined.DashboardCustomize
        ),
        Triple(
            PresentationMode.WIDGET,
            Pair(R.string.translator_pres_mode_widget, R.string.translator_pres_mode_widget_desc),
            Icons.Outlined.Widgets
        )
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (showHeader) Modifier.padding(horizontal = 20.dp, vertical = 8.dp).padding(bottom = 32.dp) else Modifier.fillMaxSize().padding(16.dp)),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (showHeader) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.ViewQuilt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.translator_pres_mode_sheet_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Text(
            text = stringResource(R.string.translator_pres_mode_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = !showHeader),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(modes) { (mode, textResPair, icon) ->
                val (titleRes, descRes) = textResPair
                val isSelected = currentMode == mode
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
                    ),
                    onClick = { onModeSelected(mode) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    icon,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(titleRes),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(descRes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isSelected) {
                            Spacer(Modifier.width(12.dp))
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- THEME SELECTION SHEET ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSelectionSheet(
    currentThemeId: String,
    installedThemes: List<com.d4viddf.hyperbridge.models.theme.HyperTheme>,
    onDismiss: () -> Unit,
    onThemeSelected: (String) -> Unit
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        ThemeSelectionContent(
            currentThemeId = currentThemeId,
            installedThemes = installedThemes,
            onThemeSelected = onThemeSelected
        )
    }
}

@Composable
fun ThemeSelectionContent(
    currentThemeId: String,
    installedThemes: List<com.d4viddf.hyperbridge.models.theme.HyperTheme>,
    showHeader: Boolean = true,
    onThemeSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (showHeader) Modifier.padding(horizontal = 20.dp, vertical = 8.dp).padding(bottom = 32.dp) else Modifier.fillMaxSize().padding(16.dp)),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (showHeader) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Outlined.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.translator_pres_theme_sheet_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = !showHeader),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Option 1: Active Theme Default
            val isActiveSelected = currentThemeId == "active" || currentThemeId.isBlank()
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isActiveSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
                    ),
                    onClick = { onThemeSelected("active") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isActiveSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Outlined.AutoAwesome,
                                    contentDescription = null,
                                    tint = if (isActiveSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.translator_pres_theme_active),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.translator_pres_theme_active_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isActiveSelected) {
                            Spacer(Modifier.width(12.dp))
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Installed Themes
            items(installedThemes) { theme ->
                val isSelected = currentThemeId == theme.id
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
                    ),
                    onClick = { onThemeSelected(theme.id) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val colorHex = theme.global.highlightColor ?: "#6750A4"
                        val parsedColor = try {
                            Color(colorHex.toColorInt())
                        } catch (_: Exception) {
                            MaterialTheme.colorScheme.primary
                        }

                        Surface(
                            shape = CircleShape,
                            color = parsedColor,
                            modifier = Modifier.size(36.dp)
                        ) {}

                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = theme.meta.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${theme.meta.author} • v${theme.meta.version}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isSelected) {
                            Spacer(Modifier.width(12.dp))
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- TEMPLATE SELECTION SHEET ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateSelectionSheet(
    currentTemplateId: String?,
    onDismiss: () -> Unit,
    onTemplateSelected: (String) -> Unit
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        TemplateSelectionContent(
            currentTemplateId = currentTemplateId,
            onTemplateSelected = onTemplateSelected
        )
    }
}

@Composable
fun TemplateSelectionContent(
    currentTemplateId: String?,
    showHeader: Boolean = true,
    onTemplateSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (showHeader) Modifier.padding(horizontal = 20.dp, vertical = 8.dp).padding(bottom = 32.dp) else Modifier.fillMaxSize().padding(16.dp)),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (showHeader) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Outlined.DashboardCustomize,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.translator_pres_template_sheet_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Text(
            text = stringResource(R.string.translator_pres_template_gallery_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        IslandTemplateGallery(
            currentTemplateId = currentTemplateId,
            onTemplateSelected = onTemplateSelected,
            modifier = Modifier.weight(1f, fill = !showHeader)
        )
    }
}

// --- WIDGET SELECTION SHEET ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetSelectionSheet(
    currentWidgetId: String?,
    onDismiss: () -> Unit,
    onWidgetSelected: (String) -> Unit
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        WidgetSelectionContent(
            currentWidgetId = currentWidgetId,
            onWidgetSelected = onWidgetSelected
        )
    }
}

@Composable
fun WidgetSelectionContent(
    currentWidgetId: String?,
    showHeader: Boolean = true,
    onWidgetSelected: (String) -> Unit,
    widgets: List<CustomWidgetDocument>? = null
) {
    val context = LocalContext.current
    val repository = remember(context) { CustomWidgetRepository(context) }
    var loadedWidgets by remember { mutableStateOf(widgets) }

    LaunchedEffect(widgets) {
        if (widgets == null) {
            loadedWidgets = repository.getAvailableWidgets()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (showHeader) Modifier.padding(horizontal = 20.dp, vertical = 8.dp).padding(bottom = 32.dp) else Modifier.fillMaxSize().padding(16.dp)),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (showHeader) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Outlined.Widgets,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.select_app_widget),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        val widgetList = loadedWidgets
        if (widgetList == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.5.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else if (widgetList.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Outlined.Widgets,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(40.dp)
                )
                Text(
                    text = stringResource(R.string.design_card_widgets_empty),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.studio_empty),
                    style = MaterialTheme.typography.bodySmall,
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
                items(widgetList) { widgetDoc ->
                    val isSelected = currentWidgetId == widgetDoc.id
                    val iconName = widgetDoc.meta.icon.ifBlank { "Widgets" }
                    val iconVector = getTranslatorOutlinedIcon(iconName)

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
                        ),
                        onClick = { onWidgetSelected(widgetDoc.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        iconVector,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = widgetDoc.canvas.name.ifBlank { widgetDoc.id },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${widgetDoc.canvas.heightDp} dp · ID: ${widgetDoc.id.take(8)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isSelected) {
                                Spacer(Modifier.width(12.dp))
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- LEFT ICON SELECTION SHEET ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeftIconSelectionSheet(
    currentSource: String,
    onDismiss: () -> Unit,
    onSourceSelected: (String) -> Unit
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        LeftIconSelectionContent(
            currentSource = currentSource,
            onSourceSelected = onSourceSelected
        )
    }
}

@Composable
fun LeftIconSelectionContent(
    currentSource: String,
    showHeader: Boolean = true,
    onSourceSelected: (String) -> Unit
) {
    val options = listOf(
        Triple("APP_ICON", R.string.translator_pres_icon_source_app, Icons.Outlined.AutoAwesome),
        Triple("AVATAR", R.string.translator_pres_icon_source_avatar, Icons.Outlined.Person),
        Triple("HIDDEN", R.string.translator_pres_icon_source_hidden, Icons.Outlined.Image)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (showHeader) Modifier.padding(horizontal = 20.dp, vertical = 8.dp).padding(bottom = 32.dp) else Modifier.fillMaxSize().padding(16.dp)),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (showHeader) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Outlined.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.translator_pres_icon_sheet_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Text(
            text = stringResource(R.string.translator_pres_icon_source_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = !showHeader),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(options) { (sourceKey, nameRes, icon) ->
                val isSelected = currentSource.equals(sourceKey, ignoreCase = true)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
                    ),
                    onClick = { onSourceSelected(sourceKey) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    icon,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(nameRes),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Source: $sourceKey",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isSelected) {
                            Spacer(Modifier.width(12.dp))
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TranslatorProgressContent(
    progressSlot: ProgressSlotConfig,
    dataExtraction: DataExtractionConfig,
    selectedSubRoute: TranslatorRoute? = null,
    onOpenSubRoute: ((TranslatorRoute) -> Unit)? = null,
    onProgressSlotChange: (ProgressSlotConfig) -> Unit,
    onDataExtractionChange: (DataExtractionConfig) -> Unit
) {
    var showTypeSheet by remember { mutableStateOf(false) }
    var showLearnSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. LEARN HOW PROGRESS WORKS ACTION BUTTON CARD ---
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
            ),
            onClick = { showLearnSheet = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Outlined.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.translator_prog_info_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.documentation_title),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowForwardIos,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // --- 2. PROGRESS SLOT TYPE CARD ---
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            val typeIcon = when (progressSlot.type) {
                                ProgressSlotType.NONE -> Icons.Outlined.VisibilityOff
                                ProgressSlotType.PROGRESS_BAR -> Icons.Outlined.Speed
                                ProgressSlotType.WAYPOINT -> Icons.Outlined.Navigation
                                ProgressSlotType.TIMER -> Icons.Outlined.Timer
                            }
                            Icon(
                                typeIcon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.translator_prog_type_card_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.translator_prog_type_card_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                val isProgressTypeSelected = selectedSubRoute == TranslatorRoute.PROGRESS_TYPE
                val openProgressTypePicker = {
                    if (onOpenSubRoute != null) {
                        onOpenSubRoute(TranslatorRoute.PROGRESS_TYPE)
                    } else {
                        showTypeSheet = true
                    }
                }
                Card(
                    shape = RoundedCornerShape(16.dp),
                    border = if (isProgressTypeSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                    colors = CardDefaults.cardColors(
                        containerColor = if (isProgressTypeSelected) {
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        }
                    ),
                    onClick = openProgressTypePicker,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val (titleRes, descRes) = when (progressSlot.type) {
                            ProgressSlotType.NONE -> Pair(R.string.translator_prog_type_none, R.string.translator_prog_type_none_desc)
                            ProgressSlotType.PROGRESS_BAR -> Pair(R.string.translator_prog_type_bar, R.string.translator_prog_type_bar_desc)
                            ProgressSlotType.WAYPOINT -> Pair(R.string.translator_prog_type_waypoint, R.string.translator_prog_type_waypoint_desc)
                            ProgressSlotType.TIMER -> Pair(R.string.translator_prog_type_timer, R.string.translator_prog_type_timer_desc)
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(titleRes),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = stringResource(descRes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        FilledTonalButton(
                            onClick = openProgressTypePicker,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(R.string.translator_prog_type_select_btn))
                        }
                    }
                }
            }
        }

        // --- 3. SHOW PERCENTAGE TOGGLE (VISIBLE IF NOT NONE) ---
        if (progressSlot.type != ProgressSlotType.NONE) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Outlined.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.translator_prog_show_pct),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.translator_prog_show_pct_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Switch(
                        checked = progressSlot.showPercentage,
                        onCheckedChange = { onProgressSlotChange(progressSlot.copy(showPercentage = it)) }
                    )
                }
            }
        }

        // --- 4. TEXT EXTRACTION CONFIGURATION CARD (VISIBLE IF NOT NONE) ---
        if (progressSlot.type != ProgressSlotType.NONE) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Outlined.TextFields,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.translator_prog_extract_card_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.translator_prog_extract_card_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Switch(
                            checked = dataExtraction.extractProgressFromText,
                            onCheckedChange = { onDataExtractionChange(dataExtraction.copy(extractProgressFromText = it)) }
                        )
                    }

                    if (dataExtraction.extractProgressFromText) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = dataExtraction.progressRegex ?: "",
                                onValueChange = { onDataExtractionChange(dataExtraction.copy(progressRegex = it.ifBlank { null })) },
                                label = { Text(stringResource(R.string.translator_prog_regex_tpl)) },
                                placeholder = { Text("(\\d+)%") },
                                supportingText = { Text(stringResource(R.string.translator_prog_regex_desc)) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                singleLine = true
                            )

                            // Quick Insert Regex Preset Chips
                            Text(
                                text = stringResource(R.string.translator_prog_regex_chips_title),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            val regexPresets = listOf("(\\d+)%", "(\\d+)/(\\d+)", "(\\d+)\\s*of\\s*(\\d+)", "(\\d+)\\s*MB")

                            Row(
                                modifier = Modifier
                                .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                regexPresets.forEach { preset ->
                                    SuggestionChip(
                                        onClick = { onDataExtractionChange(dataExtraction.copy(progressRegex = preset)) },
                                        label = { Text(preset, style = MaterialTheme.typography.labelSmall) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                        )
                                    )
                                }
                            }

                            Spacer(Modifier.height(4.dp))

                            // Custom Max Progress Field
                            OutlinedTextField(
                                value = dataExtraction.customMaxProgress.toString(),
                                onValueChange = {
                                    val num = it.filter { c -> c.isDigit() }.toIntOrNull() ?: 100
                                    onDataExtractionChange(dataExtraction.copy(customMaxProgress = num))
                                },
                                label = { Text(stringResource(R.string.translator_prog_max_title)) },
                                supportingText = { Text(stringResource(R.string.translator_prog_max_desc)) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                singleLine = true
                            )
                        }
                    }
                }
            }
        }
    }

    if (showTypeSheet) {
        ProgressSlotTypeSelectionSheet(
            currentType = progressSlot.type,
            onDismiss = { showTypeSheet = false },
            onTypeSelected = { newType ->
                onProgressSlotChange(progressSlot.copy(type = newType))
                showTypeSheet = false
            }
        )
    }

    if (showLearnSheet) {
        ProgressGuideSheet(
            onDismiss = { showLearnSheet = false }
        )
    }
}

// --- PROGRESS GUIDE BOTTOM SHEET ---
@Composable
fun ProgressGuideSheet(
    onDismiss: () -> Unit
) {
    DesignManagerHelpSheet(
        onDismiss = onDismiss,
        title = stringResource(R.string.icons_group_progress),
        description = stringResource(R.string.feature_guides_redirect_desc),
        docsUrl = DocumentationUrls.CUSTOM_TRANSLATORS_DOCS,
        icon = Icons.Outlined.Speed
    )
}


// --- PROGRESS SLOT TYPE SELECTION SHEET ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressSlotTypeSelectionSheet(
    currentType: ProgressSlotType,
    onDismiss: () -> Unit,
    onTypeSelected: (ProgressSlotType) -> Unit
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        ProgressSlotTypeSelectionContent(
            currentType = currentType,
            onTypeSelected = onTypeSelected
        )
    }
}

@Composable
fun ProgressSlotTypeSelectionContent(
    currentType: ProgressSlotType,
    showHeader: Boolean = true,
    onTypeSelected: (ProgressSlotType) -> Unit
) {
    val types = listOf(
        Triple(
            ProgressSlotType.NONE,
            Pair(R.string.translator_prog_type_none, R.string.translator_prog_type_none_desc),
            Icons.Outlined.VisibilityOff
        ),
        Triple(
            ProgressSlotType.PROGRESS_BAR,
            Pair(R.string.translator_prog_type_bar, R.string.translator_prog_type_bar_desc),
            Icons.Outlined.Speed
        ),
        Triple(
            ProgressSlotType.WAYPOINT,
            Pair(R.string.translator_prog_type_waypoint, R.string.translator_prog_type_waypoint_desc),
            Icons.Outlined.Navigation
        ),
        Triple(
            ProgressSlotType.TIMER,
            Pair(R.string.translator_prog_type_timer, R.string.translator_prog_type_timer_desc),
            Icons.Outlined.Timer
        )
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (showHeader) Modifier.padding(horizontal = 20.dp, vertical = 8.dp).padding(bottom = 32.dp) else Modifier.fillMaxSize().padding(16.dp)),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (showHeader) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Outlined.Speed,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.translator_prog_type_sheet_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Text(
            text = stringResource(R.string.translator_prog_type_card_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = !showHeader),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(types) { (type, textResPair, icon) ->
                val (titleRes, descRes) = textResPair
                val isSelected = currentType == type
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
                    ),
                    onClick = { onTypeSelected(type) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    icon,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(titleRes),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(descRes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isSelected) {
                            Spacer(Modifier.width(12.dp))
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TranslatorActionsContent(
    actionSlots: List<ActionSlotConfig>,
    onChange: (List<ActionSlotConfig>) -> Unit
) {
    var showLearnSheet by remember { mutableStateOf(false) }
    var activeSlotForSourceSheet by remember { mutableStateOf<Int?>(null) }
    var activeSlotForSmartTypeSheet by remember { mutableStateOf<Int?>(null) }
    var activeSlotForDisplayModeSheet by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. HOW ACTIONS WORK / EDUCATIONAL BANNER CARD ---
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
            ),
            onClick = { showLearnSheet = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Outlined.TouchApp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.translator_actions_info_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.documentation_title),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowForwardIos,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // --- 2. HEADER BAR & ADD BUTTON ---
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
                        isVisible = true,
                        source = ActionSource.NOTIFICATION_ACTION,
                        actionMatcher = ActionMatcher(
                            matchBy = ActionMatchBy.INDEX,
                            actionIndex = actionSlots.size
                        ),
                        displayMode = ActionDisplayMode.ICON_ONLY
                    )
                    onChange(actionSlots + newSlot)
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.translator_actions_add_slot))
            }
        }

        // --- 3. EMPTY STATE OR SLOTS LIST ---
        if (actionSlots.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.TouchApp,
                title = stringResource(R.string.translator_actions_empty_title),
                description = stringResource(R.string.translator_actions_empty_desc),
                modifier = Modifier.padding(top = 24.dp)
            )
        } else {
            actionSlots.forEachIndexed { index, slot ->
                val shape = RoundedCornerShape(20.dp)

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (slot.isVisible) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.surfaceContainerLowest
                    ),
                    shape = shape,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Slot Header: Badge, Move Up/Down, Visibility Toggle, Delete
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (slot.isVisible) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest
                                ) {
                                    Text(
                                        text = stringResource(R.string.translator_actions_slot_badge, index + 1),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (slot.isVisible) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                if (!slot.isVisible) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.translator_actions_visibility_hidden),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                // Move Up
                                IconButton(
                                    onClick = {
                                        if (index > 0) {
                                            val updated = actionSlots.toMutableList()
                                            val item = updated.removeAt(index)
                                            updated.add(index - 1, item)
                                            val reindexed = updated.mapIndexed { idx, s -> s.copy(slotPosition = idx) }
                                            onChange(reindexed)
                                        }
                                    },
                                    enabled = index > 0,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ArrowUpward,
                                        contentDescription = "Move Up",
                                        tint = if (index > 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outlineVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                // Move Down
                                IconButton(
                                    onClick = {
                                        if (index < actionSlots.size - 1) {
                                            val updated = actionSlots.toMutableList()
                                            val item = updated.removeAt(index)
                                            updated.add(index + 1, item)
                                            val reindexed = updated.mapIndexed { idx, s -> s.copy(slotPosition = idx) }
                                            onChange(reindexed)
                                        }
                                    },
                                    enabled = index < actionSlots.size - 1,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ArrowDownward,
                                        contentDescription = "Move Down",
                                        tint = if (index < actionSlots.size - 1) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outlineVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                // Visibility Toggle
                                IconButton(
                                    onClick = {
                                        val updated = actionSlots.toMutableList()
                                        updated[index] = slot.copy(isVisible = !slot.isVisible)
                                        onChange(updated)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = if (slot.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (slot.isVisible) "Hide Action" else "Show Action",
                                        tint = if (slot.isVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Delete Slot
                                IconButton(
                                    onClick = {
                                        val updated = actionSlots.toMutableList().apply { removeAt(index) }
                                        val reindexed = updated.mapIndexed { idx, s -> s.copy(slotPosition = idx) }
                                        onChange(reindexed)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.translator_actions_remove_slot),
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        // Action Source Selector Row
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                            onClick = { activeSlotForSourceSheet = index },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val (sourceTitleRes, sourceIcon) = when (slot.source) {
                                    ActionSource.NOTIFICATION_ACTION -> Pair(R.string.translator_actions_source_notif_title, Icons.Outlined.TouchApp)
                                    ActionSource.SMART_ACTION -> Pair(R.string.translator_actions_source_smart_title, Icons.Outlined.AutoAwesome)
                                    ActionSource.INLINE_REPLY -> Pair(R.string.translator_actions_source_reply_title,
                                        Icons.AutoMirrored.Filled.Reply
                                    )
                                    ActionSource.CUSTOM_BROADCAST -> Pair(R.string.translator_actions_source_broadcast_title, Icons.Default.RssFeed)
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                sourceIcon,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = stringResource(R.string.translator_actions_source_title),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = stringResource(sourceTitleRes),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                FilledTonalButton(
                                    onClick = { activeSlotForSourceSheet = index },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(stringResource(R.string.translator_actions_source_change_btn), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        // Specific Configuration according to Source
                        when (slot.source) {
                            ActionSource.SMART_ACTION -> {
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                                    onClick = { activeSlotForSmartTypeSheet = index },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        val smartTitleRes = when (slot.smartActionType) {
                                            SmartActionType.OTP_COPY -> R.string.translator_actions_smart_otp
                                            SmartActionType.OPEN_URL -> R.string.translator_actions_smart_url
                                            SmartActionType.DIAL_NUMBER -> R.string.translator_actions_smart_dial
                                            SmartActionType.TRACK_PACKAGE -> R.string.translator_actions_smart_track
                                            null -> R.string.translator_actions_smart_otp
                                        }
                                        val smartIcon = when (slot.smartActionType) {
                                            SmartActionType.OTP_COPY -> Icons.Default.ContentCopy
                                            SmartActionType.OPEN_URL -> Icons.Default.OpenInBrowser
                                            SmartActionType.DIAL_NUMBER -> Icons.Default.Call
                                            SmartActionType.TRACK_PACKAGE -> Icons.Default.LocalShipping
                                            null -> Icons.Default.ContentCopy
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.secondaryContainer,
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        smartIcon,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                            Spacer(Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = stringResource(R.string.translator_actions_smart_type),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = stringResource(smartTitleRes),
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }

                                        Icon(
                                            Icons.AutoMirrored.Rounded.ArrowForwardIos,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                            ActionSource.NOTIFICATION_ACTION -> {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = stringResource(R.string.translator_actions_matcher_title),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FilterChip(
                                            selected = slot.actionMatcher.matchBy == ActionMatchBy.INDEX,
                                            onClick = {
                                                val updated = actionSlots.toMutableList()
                                                updated[index] = slot.copy(
                                                    actionMatcher = slot.actionMatcher.copy(matchBy = ActionMatchBy.INDEX)
                                                )
                                                onChange(updated)
                                            },
                                            label = { Text(stringResource(R.string.translator_actions_matcher_by_index)) }
                                        )
                                        FilterChip(
                                            selected = slot.actionMatcher.matchBy == ActionMatchBy.TITLE,
                                            onClick = {
                                                val updated = actionSlots.toMutableList()
                                                updated[index] = slot.copy(
                                                    actionMatcher = slot.actionMatcher.copy(matchBy = ActionMatchBy.TITLE)
                                                )
                                                onChange(updated)
                                            },
                                            label = { Text(stringResource(R.string.translator_actions_matcher_by_title)) }
                                        )
                                    }

                                    if (slot.actionMatcher.matchBy == ActionMatchBy.INDEX) {
                                        OutlinedTextField(
                                            value = (slot.actionMatcher.actionIndex ?: 0).toString(),
                                            onValueChange = {
                                                val idx = it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0
                                                val updated = actionSlots.toMutableList()
                                                updated[index] = slot.copy(
                                                    actionMatcher = slot.actionMatcher.copy(actionIndex = idx)
                                                )
                                                onChange(updated)
                                            },
                                            label = { Text(stringResource(R.string.translator_actions_matcher_index_label)) },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            singleLine = true
                                        )
                                    } else {
                                        OutlinedTextField(
                                            value = slot.actionMatcher.titleRegex ?: "",
                                            onValueChange = {
                                                val updated = actionSlots.toMutableList()
                                                updated[index] = slot.copy(
                                                    actionMatcher = slot.actionMatcher.copy(titleRegex = it.ifBlank { null })
                                                )
                                                onChange(updated)
                                            },
                                            label = { Text(stringResource(R.string.translator_actions_matcher_regex_label)) },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            singleLine = true
                                        )
                                    }
                                }
                            }
                            ActionSource.INLINE_REPLY -> {
                                Text(
                                    text = stringResource(R.string.translator_actions_source_reply_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            ActionSource.CUSTOM_BROADCAST -> {
                                Text(
                                    text = stringResource(R.string.translator_actions_source_broadcast_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Display Mode Selector Row (Icon only, Text only, Icon & Text)
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                            onClick = { activeSlotForDisplayModeSheet = index },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val displayTitleRes = when (slot.displayMode) {
                                    ActionDisplayMode.ICON_ONLY -> R.string.translator_actions_display_mode_icon
                                    ActionDisplayMode.TEXT_ONLY -> R.string.translator_actions_display_mode_text
                                    ActionDisplayMode.ICON_AND_TEXT -> R.string.translator_actions_display_mode_both
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.translator_actions_display_mode_title),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = stringResource(displayTitleRes),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Icon(
                                    Icons.AutoMirrored.Rounded.ArrowForwardIos,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        // Optional Custom Button Label
                        OutlinedTextField(
                            value = slot.customLabel ?: "",
                            onValueChange = {
                                val updated = actionSlots.toMutableList()
                                updated[index] = slot.copy(customLabel = it.ifBlank { null })
                                onChange(updated)
                            },
                            label = { Text(stringResource(R.string.translator_action_custom_label)) },
                            placeholder = { Text("Auto (From Notification or Preset)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }
                }
            }
        }
    }

    // --- BOTTOM SHEETS ---
    if (activeSlotForSourceSheet != null) {
        val slotIndex = activeSlotForSourceSheet!!
        val currentSlot = actionSlots.getOrNull(slotIndex)
        if (currentSlot != null) {
            ActionSourceSelectionSheet(
                currentSource = currentSlot.source,
                onDismiss = { activeSlotForSourceSheet = null },
                onSourceSelected = { newSource ->
                    val updated = actionSlots.toMutableList()
                    val defaultSmart = if (newSource == ActionSource.SMART_ACTION && currentSlot.smartActionType == null) {
                        SmartActionType.OTP_COPY
                    } else currentSlot.smartActionType
                    updated[slotIndex] = currentSlot.copy(source = newSource, smartActionType = defaultSmart)
                    onChange(updated)
                    activeSlotForSourceSheet = null
                }
            )
        }
    }

    if (activeSlotForSmartTypeSheet != null) {
        val slotIndex = activeSlotForSmartTypeSheet!!
        val currentSlot = actionSlots.getOrNull(slotIndex)
        if (currentSlot != null) {
            SmartActionTypeSelectionSheet(
                currentType = currentSlot.smartActionType ?: SmartActionType.OTP_COPY,
                onDismiss = { activeSlotForSmartTypeSheet = null },
                onTypeSelected = { newType ->
                    val updated = actionSlots.toMutableList()
                    updated[slotIndex] = currentSlot.copy(smartActionType = newType)
                    onChange(updated)
                    activeSlotForSmartTypeSheet = null
                }
            )
        }
    }

    if (activeSlotForDisplayModeSheet != null) {
        val slotIndex = activeSlotForDisplayModeSheet!!
        val currentSlot = actionSlots.getOrNull(slotIndex)
        if (currentSlot != null) {
            ActionDisplayModeSelectionSheet(
                currentMode = currentSlot.displayMode,
                onDismiss = { activeSlotForDisplayModeSheet = null },
                onModeSelected = { newMode ->
                    val updated = actionSlots.toMutableList()
                    updated[slotIndex] = currentSlot.copy(displayMode = newMode)
                    onChange(updated)
                    activeSlotForDisplayModeSheet = null
                }
            )
        }
    }

    if (showLearnSheet) {
        ActionGuideSheet(
            onDismiss = { showLearnSheet = false }
        )
    }
}

// --- ACTION GUIDE BOTTOM SHEET ---
@Composable
fun ActionGuideSheet(
    onDismiss: () -> Unit
) {
    DesignManagerHelpSheet(
        onDismiss = onDismiss,
        title = stringResource(R.string.translator_actions_title),
        description = stringResource(R.string.feature_guides_redirect_desc),
        docsUrl = DocumentationUrls.SMART_ACTIONS_DOCS,
        icon = Icons.Outlined.TouchApp
    )
}


// --- ACTION SOURCE SELECTION SHEET ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionSourceSelectionSheet(
    currentSource: ActionSource,
    onDismiss: () -> Unit,
    onSourceSelected: (ActionSource) -> Unit
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        ActionSourceSelectionContent(
            currentSource = currentSource,
            onSourceSelected = onSourceSelected
        )
    }
}

@Composable
fun ActionSourceSelectionContent(
    currentSource: ActionSource,
    onSourceSelected: (ActionSource) -> Unit
) {
    val options = listOf(
        Triple(
            ActionSource.NOTIFICATION_ACTION,
            Pair(R.string.translator_actions_source_notif_title, R.string.translator_actions_source_notif_desc),
            Icons.Outlined.TouchApp
        ),
        Triple(
            ActionSource.SMART_ACTION,
            Pair(R.string.translator_actions_source_smart_title, R.string.translator_actions_source_smart_desc),
            Icons.Outlined.AutoAwesome
        ),
        Triple(
            ActionSource.INLINE_REPLY,
            Pair(R.string.translator_actions_source_reply_title, R.string.translator_actions_source_reply_desc),
            Icons.AutoMirrored.Filled.Reply
        ),
        Triple(
            ActionSource.CUSTOM_BROADCAST,
            Pair(R.string.translator_actions_source_broadcast_title, R.string.translator_actions_source_broadcast_desc),
            Icons.Default.RssFeed
        )
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Outlined.TouchApp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.translator_actions_source_sheet_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(options) { (source, textResPair, icon) ->
                val (titleRes, descRes) = textResPair
                val isSelected = currentSource == source
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
                    ),
                    onClick = { onSourceSelected(source) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    icon,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(titleRes),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(descRes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isSelected) {
                            Spacer(Modifier.width(12.dp))
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- SMART ACTION TYPE SELECTION SHEET ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartActionTypeSelectionSheet(
    currentType: SmartActionType,
    onDismiss: () -> Unit,
    onTypeSelected: (SmartActionType) -> Unit
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        SmartActionTypeSelectionContent(
            currentType = currentType,
            onTypeSelected = onTypeSelected
        )
    }
}

@Composable
fun SmartActionTypeSelectionContent(
    currentType: SmartActionType,
    onTypeSelected: (SmartActionType) -> Unit
) {
    val options = listOf(
        Triple(
            SmartActionType.OTP_COPY,
            Pair(R.string.translator_actions_smart_otp, R.string.translator_actions_smart_otp_desc),
            Icons.Default.ContentCopy
        ),
        Triple(
            SmartActionType.OPEN_URL,
            Pair(R.string.translator_actions_smart_url, R.string.translator_actions_smart_url_desc),
            Icons.Default.OpenInBrowser
        ),
        Triple(
            SmartActionType.DIAL_NUMBER,
            Pair(R.string.translator_actions_smart_dial, R.string.translator_actions_smart_dial_desc),
            Icons.Default.Call
        ),
        Triple(
            SmartActionType.TRACK_PACKAGE,
            Pair(R.string.translator_actions_smart_track, R.string.translator_actions_smart_track_desc),
            Icons.Default.LocalShipping
        )
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.translator_actions_smart_type_sheet_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(options) { (smartType, textResPair, icon) ->
                val (titleRes, descRes) = textResPair
                val isSelected = currentType == smartType
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
                    ),
                    onClick = { onTypeSelected(smartType) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    icon,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(titleRes),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(descRes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isSelected) {
                            Spacer(Modifier.width(12.dp))
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- ACTION DISPLAY MODE SELECTION SHEET ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionDisplayModeSelectionSheet(
    currentMode: ActionDisplayMode,
    onDismiss: () -> Unit,
    onModeSelected: (ActionDisplayMode) -> Unit
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        ActionDisplayModeSelectionContent(
            currentMode = currentMode,
            onModeSelected = onModeSelected
        )
    }
}

@Composable
fun ActionDisplayModeSelectionContent(
    currentMode: ActionDisplayMode,
    onModeSelected: (ActionDisplayMode) -> Unit
) {
    val options = listOf(
        Triple(
            ActionDisplayMode.ICON_ONLY,
            R.string.translator_actions_display_mode_icon,
            Icons.Outlined.AutoAwesome
        ),
        Triple(
            ActionDisplayMode.TEXT_ONLY,
            R.string.translator_actions_display_mode_text,
            Icons.Outlined.TextFields
        ),
        Triple(
            ActionDisplayMode.ICON_AND_TEXT,
            R.string.translator_actions_display_mode_both,
            Icons.AutoMirrored.Outlined.ViewQuilt
        )
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Outlined.DisplaySettings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.translator_actions_display_mode_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(options) { (mode, titleRes, icon) ->
                val isSelected = currentMode == mode
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
                    ),
                    onClick = { onModeSelected(mode) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    icon,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(titleRes),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (isSelected) {
                            Spacer(Modifier.width(12.dp))
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
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
    selectedSubRoute: TranslatorRoute? = null,
    onOpenSubRoute: ((TranslatorRoute) -> Unit)? = null,
    onBehaviorChange: (BehaviorOverride) -> Unit,
    onEngineModeChange: (EngineMode) -> Unit
) {
    var showLearnSheet by remember { mutableStateOf(false) }
    var showEngineSheet by remember { mutableStateOf(false) }

    val timeoutSteps = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 20, 30, 45, 60, 300, 900, 1800, 3600)
    val timePopUpSteps = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 20, 30)

    val currentTimeout = behavior.timeoutSeconds ?: 10
    val isTimeoutEnabled = (behavior.timeoutSeconds != null && behavior.timeoutSeconds > 0)

    val isFloatEnabled = behavior.isFloat ?: true
    val floatOnlyOnFirst = behavior.floatOnlyOnFirst
    val currentFloatTimeout = behavior.floatTimeoutSeconds ?: 10
    val isShowShade = behavior.isShowShade ?: false
    val showOnLockscreen = behavior.showOnLockscreen

    val removeOriginalOn = behavior.removeOriginalNotification == true
    val dismissWithOriginal = behavior.dismissWithOriginal ?: false
    val enableInlineReply = behavior.enableInlineReply ?: true

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Educational Banner / Learn Sheet Trigger
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Outlined.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.translator_behavior_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.island_behavior_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = { showLearnSheet = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(Icons.Outlined.TouchApp, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.documentation_title),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // --- SECTION 1: ENGINE CONFIGURATION ---
        val isEngineSelected = selectedSubRoute == TranslatorRoute.BEHAVIOR_ENGINE
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.engine_config_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp)
            )

            Card(
                shape = RoundedCornerShape(24.dp),
                border = if (isEngineSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                colors = CardDefaults.cardColors(
                    containerColor = if (isEngineSelected) {
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Outlined.Tune,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.engine),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            val engineTitleRes = when (engineMode) {
                                EngineMode.INHERIT -> R.string.translator_behavior_engine_mode_inherit
                                EngineMode.CUSTOM_ISLAND -> R.string.engine_xiaomi_title
                                EngineMode.NATIVE_LIVE_UPDATE -> R.string.engine_native_title
                            }
                            Text(
                                text = stringResource(engineTitleRes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (onOpenSubRoute != null) {
                                onOpenSubRoute(TranslatorRoute.BEHAVIOR_ENGINE)
                            } else {
                                showEngineSheet = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(stringResource(R.string.translator_behavior_engine_change_btn))
                    }
                }
            }
        }

        // --- SECTION 2: GLOBAL BEHAVIOR & TIMEOUTS ---
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.global_behavior),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    // Header with Switch
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(20.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.auto_hide_island),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (isTimeoutEnabled) stringResource(R.string.hides_after_a_set_time) else stringResource(R.string.behavior_hide_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(20.dp))
                        Switch(
                            checked = isTimeoutEnabled,
                            onCheckedChange = { enabled ->
                                val newTimeout = if (enabled) 5 else null
                                onBehaviorChange(behavior.copy(timeoutSeconds = newTimeout))
                            }
                        )
                    }

                    // Expandable Slider Section
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isTimeoutEnabled,
                        enter = androidx.compose.animation.expandVertically(),
                        exit = androidx.compose.animation.shrinkVertically()
                    ) {
                        Column {
                            Spacer(Modifier.height(16.dp))

                            Text(
                                text = com.d4viddf.hyperbridge.ui.components.formatSeconds(currentTimeout),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )

                            val currentIndex = timeoutSteps.indexOf(currentTimeout).coerceAtLeast(0).toFloat()
                            val timeoutSliderState = rememberSliderState(
                                value = currentIndex,
                                steps = timeoutSteps.size - 2,
                                trackRange = 0f..(timeoutSteps.size - 1).toFloat()
                            )
                            SideEffect {
                                if (!timeoutSliderState.isDragging && timeoutSliderState.value != currentIndex) {
                                    timeoutSliderState.value = currentIndex
                                }
                            }

                            Slider(
                                state = timeoutSliderState,
                                onValueChange = { index ->
                                    timeoutSliderState.value = index
                                    val selectedSeconds = timeoutSteps[index.roundToInt().coerceIn(0, timeoutSteps.lastIndex)]
                                    onBehaviorChange(behavior.copy(timeoutSeconds = selectedSeconds))
                                }
                            )

                            Text(
                                text = stringResource(R.string.behavior_desc_hide_long),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // --- SECTION 3: XIAOMI FEATURED NOTIFICATIONS ---
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.xiaomi_featured_notifications),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
            )

            // Float Settings Card (Heads-up Popup)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            Icons.Default.Visibility,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(20.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.setting_float),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                stringResource(R.string.setting_float_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(20.dp))
                        Switch(
                            checked = isFloatEnabled,
                            onCheckedChange = { onBehaviorChange(behavior.copy(isFloat = it)) }
                        )
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = isFloatEnabled,
                        enter = androidx.compose.animation.expandVertically(),
                        exit = androidx.compose.animation.shrinkVertically()
                    ) {
                        Column {
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.seconds_suffix, currentFloatTimeout),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )

                            val currentIndexPop = timePopUpSteps.indexOf(currentFloatTimeout).coerceAtLeast(1).toFloat()
                            val floatTimeoutSliderState = rememberSliderState(
                                value = currentIndexPop,
                                steps = timePopUpSteps.size - 2,
                                trackRange = 0f..(timePopUpSteps.size - 1).toFloat()
                            )
                            SideEffect {
                                if (!floatTimeoutSliderState.isDragging && floatTimeoutSliderState.value != currentIndexPop) {
                                    floatTimeoutSliderState.value = currentIndexPop
                                }
                            }

                            Slider(
                                state = floatTimeoutSliderState,
                                onValueChange = { index ->
                                    floatTimeoutSliderState.value = index
                                    val selectedSeconds = timePopUpSteps[index.roundToInt().coerceIn(0, timePopUpSteps.lastIndex)]
                                    onBehaviorChange(behavior.copy(floatTimeoutSeconds = selectedSeconds))
                                }
                            )
                            Text(
                                text = stringResource(R.string.setting_float_timeout_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.setting_float_only_on_first),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = stringResource(R.string.setting_float_only_on_first_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.width(16.dp))
                                Switch(
                                    checked = floatOnlyOnFirst,
                                    onCheckedChange = { onBehaviorChange(behavior.copy(floatOnlyOnFirst = it)) }
                                )
                            }
                        }
                    }
                }
            }

            // Notification Shade Toggle Card
            Card(
                onClick = { onBehaviorChange(behavior.copy(isShowShade = !isShowShade)) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Layers,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(20.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.setting_shade),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            stringResource(R.string.setting_shade_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(20.dp))
                    Switch(
                        checked = isShowShade,
                        onCheckedChange = { onBehaviorChange(behavior.copy(isShowShade = it)) }
                    )
                }
            }

            // Show on Lock Screen Toggle Card
            Card(
                onClick = { onBehaviorChange(behavior.copy(showOnLockscreen = !showOnLockscreen)) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 24.dp, bottomEnd = 24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(20.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.charging_island_lockscreen),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            stringResource(R.string.translator_show_on_lockscreen_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(20.dp))
                    Switch(
                        checked = showOnLockscreen,
                        onCheckedChange = { onBehaviorChange(behavior.copy(showOnLockscreen = it)) }
                    )
                }
            }
        }

        // --- SECTION 4: NOTIFICATION MANAGEMENT ---
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.notification_management),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
            )

            // Remove Original Notification Card
            Card(
                onClick = { onBehaviorChange(behavior.copy(removeOriginalNotification = !removeOriginalOn)) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.DeleteSweep,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(20.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.remove_original_notification),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            stringResource(R.string.remove_original_notification_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(20.dp))
                    Switch(
                        checked = removeOriginalOn,
                        onCheckedChange = { onBehaviorChange(behavior.copy(removeOriginalNotification = it)) }
                    )
                }
            }

            // Dismiss With Original Notification Card
            Card(
                onClick = { if (!removeOriginalOn) onBehaviorChange(behavior.copy(dismissWithOriginal = !dismissWithOriginal)) },
                enabled = !removeOriginalOn,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.DeleteSweep,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(20.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.dismiss_with_original),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            stringResource(R.string.dismiss_with_original_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(20.dp))
                    Switch(
                        checked = dismissWithOriginal,
                        enabled = !removeOriginalOn,
                        onCheckedChange = { onBehaviorChange(behavior.copy(dismissWithOriginal = it)) }
                    )
                }
            }

            // Enable Inline Reply Overlay Card
            Card(
                onClick = { if (!removeOriginalOn) onBehaviorChange(behavior.copy(enableInlineReply = !enableInlineReply)) },
                enabled = !removeOriginalOn,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = if (removeOriginalOn) 4.dp else 24.dp, bottomEnd = if (removeOriginalOn) 4.dp else 24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Reply,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(20.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.enable_inline_reply),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            stringResource(R.string.enable_inline_reply_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(20.dp))
                    Switch(
                        checked = enableInlineReply,
                        enabled = !removeOriginalOn,
                        onCheckedChange = { onBehaviorChange(behavior.copy(enableInlineReply = it)) }
                    )
                }
            }

            // Warning Banner if Remove Original Notification is enabled
            androidx.compose.animation.AnimatedVisibility(
                visible = removeOriginalOn,
                enter = androidx.compose.animation.expandVertically(),
                exit = androidx.compose.animation.shrinkVertically()
            ) {
                Text(
                    text = stringResource(R.string.remove_original_notification_hidden_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp)
                )
            }
        }
    }

    // --- BOTTOM SHEETS ---
    if (showEngineSheet) {
        TranslatorEngineSelectionSheet(
            currentMode = engineMode,
            onDismiss = { showEngineSheet = false },
            onModeSelected = {
                onEngineModeChange(it)
                onBehaviorChange(behavior.copy(engineMode = it))
                showEngineSheet = false
            }
        )
    }

    if (showLearnSheet) {
        TranslatorBehaviorGuideSheet(
            onDismiss = { showLearnSheet = false }
        )
    }
}

// --- ENGINE MODE SELECTION BOTTOM SHEET ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslatorEngineSelectionSheet(
    currentMode: EngineMode,
    onDismiss: () -> Unit,
    onModeSelected: (EngineMode) -> Unit
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        TranslatorEngineSelectionContent(
            currentMode = currentMode,
            onModeSelected = onModeSelected
        )
    }
}

@Composable
fun TranslatorEngineSelectionContent(
    currentMode: EngineMode,
    showHeader: Boolean = true,
    onModeSelected: (EngineMode) -> Unit
) {
    val options = listOf(
        Triple(
            EngineMode.INHERIT,
            R.string.translator_behavior_engine_mode_inherit,
            R.string.translator_behavior_engine_mode_inherit_desc
        ),
        Triple(
            EngineMode.CUSTOM_ISLAND,
            R.string.engine_xiaomi_title,
            R.string.engine_xiaomi_desc
        ),
        Triple(
            EngineMode.NATIVE_LIVE_UPDATE,
            R.string.engine_native_title,
            R.string.engine_native_desc
        )
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (showHeader) Modifier.padding(horizontal = 20.dp, vertical = 8.dp).padding(bottom = 32.dp) else Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp)),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (showHeader) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Outlined.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.translator_behavior_engine_mode_sheet_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = !showHeader),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(options) { (mode, titleRes, descRes) ->
                val isSelected = currentMode == mode
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
                    ),
                    onClick = { onModeSelected(mode) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    when (mode) {
                                        EngineMode.INHERIT -> Icons.Outlined.Tune
                                        EngineMode.CUSTOM_ISLAND -> Icons.Outlined.DashboardCustomize
                                        EngineMode.NATIVE_LIVE_UPDATE -> Icons.Outlined.Notifications
                                    },
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(titleRes),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(descRes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isSelected) {
                            Spacer(Modifier.width(12.dp))
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- BEHAVIOR GUIDE BOTTOM SHEET ---
@Composable
fun TranslatorBehaviorGuideSheet(
    onDismiss: () -> Unit
) {
    DesignManagerHelpSheet(
        onDismiss = onDismiss,
        title = stringResource(R.string.island_behavior_title),
        description = stringResource(R.string.feature_guides_redirect_desc),
        docsUrl = DocumentationUrls.CUSTOM_TRANSLATORS_DOCS,
        icon = Icons.Outlined.TouchApp
    )
}


@Composable
fun TranslatorAppsContent(
    translator: CustomTranslator,
    installedApps: List<AppItem>,
    systemApps: List<AppItem> = emptyList(),
    selectedSubRoute: TranslatorRoute? = null,
    onOpenSubRoute: ((TranslatorRoute) -> Unit)? = null,
    onTargetScopeChange: (TargetScope, List<String>) -> Unit,
    onTargetNotificationTypesChange: (List<String>) -> Unit
) {
    var showScopeSheet by remember { mutableStateOf(false) }
    var showAppPicker by remember { mutableStateOf(false) }
    var showSystemAppPicker by remember { mutableStateOf(false) }
    var showNotifTypePicker by remember { mutableStateOf(false) }
    val isFilterByNotifType = translator.targetNotificationTypes.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Card 1: Target Scope (Global vs Specific Apps vs System Apps)
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
                            text = when (translator.targetScope) {
                                TargetScope.GLOBAL -> stringResource(R.string.translator_target_scope_global_desc)
                                TargetScope.SYSTEM_APPS -> stringResource(R.string.translator_target_scope_system_desc, translator.targetPackages.size)
                                TargetScope.SPECIFIC_APPS -> stringResource(R.string.translator_target_scope_apps_desc, translator.targetPackages.size)
                                TargetScope.NOTIFICATION_TYPE -> stringResource(R.string.translators_scope_types, translator.targetNotificationTypes.size)
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
                    val isScopeSelected = selectedSubRoute == TranslatorRoute.APPS_SCOPE
                    Button(
                        onClick = {
                            if (onOpenSubRoute != null) {
                                onOpenSubRoute(TranslatorRoute.APPS_SCOPE)
                            } else {
                                showScopeSheet = true
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = if (isScopeSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isScopeSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer
                            },
                            contentColor = if (isScopeSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            }
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = when (translator.targetScope) {
                                TargetScope.GLOBAL -> stringResource(R.string.translator_scope_global)
                                TargetScope.SYSTEM_APPS -> stringResource(R.string.translator_target_scope_system_title)
                                TargetScope.SPECIFIC_APPS -> stringResource(R.string.translator_target_scope_apps_title)
                                TargetScope.NOTIFICATION_TYPE -> stringResource(R.string.translators_filter_types)
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
                    } else if (translator.targetScope == TargetScope.SYSTEM_APPS) {
                        Button(
                            onClick = { showSystemAppPicker = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.translator_select_system_apps_action))
                        }
                    }
                }

                // Show selected apps chips / list if specific or system apps
                if ((translator.targetScope == TargetScope.SPECIFIC_APPS || translator.targetScope == TargetScope.SYSTEM_APPS) && translator.targetPackages.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        translator.targetPackages.forEach { pkg ->
                            val label = installedApps.find { it.packageName == pkg }?.label
                                ?: systemApps.find { it.packageName == pkg }?.label
                                ?: pkg
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
                                            onTargetScopeChange(translator.targetScope, updated)
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
                                stringResource(R.string.translator_notif_types_sheet_title)
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
                when (scope) {
                    TargetScope.GLOBAL -> onTargetScopeChange(TargetScope.GLOBAL, emptyList())
                    TargetScope.SYSTEM_APPS -> onTargetScopeChange(TargetScope.SYSTEM_APPS, if (translator.targetScope == TargetScope.SYSTEM_APPS) translator.targetPackages else emptyList())
                    TargetScope.SPECIFIC_APPS -> onTargetScopeChange(TargetScope.SPECIFIC_APPS, if (translator.targetScope == TargetScope.SPECIFIC_APPS) translator.targetPackages else emptyList())
                    TargetScope.NOTIFICATION_TYPE -> onTargetScopeChange(TargetScope.NOTIFICATION_TYPE, emptyList())
                }
            }
        )
    }

    // Bottom Sheet: Specific App Selection
    if (showAppPicker) {
        AppSelectionSheet(
            apps = installedApps,
            title = stringResource(R.string.translator_select_apps_action),
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

    // Bottom Sheet: System App Selection
    if (showSystemAppPicker) {
        AppSelectionSheet(
            apps = systemApps,
            title = stringResource(R.string.translator_select_system_apps_action),
            onDismiss = { showSystemAppPicker = false },
            onAppSelected = { app ->
                showSystemAppPicker = false
                if (!translator.targetPackages.contains(app.packageName)) {
                    val updated = translator.targetPackages + app.packageName
                    onTargetScopeChange(TargetScope.SYSTEM_APPS, updated)
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
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )

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
    showHeader: Boolean = true,
    onScopeSelected: (TargetScope) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (showHeader) Modifier.padding(24.dp).navigationBarsPadding() else Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (showHeader) {
            Text(
                text = stringResource(R.string.translator_scope_sheet_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

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
                        text = stringResource(R.string.translator_scope_global),
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

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (currentScope == TargetScope.SYSTEM_APPS) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
            ),
            onClick = { onScopeSelected(TargetScope.SYSTEM_APPS) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = currentScope == TargetScope.SYSTEM_APPS,
                    onClick = { onScopeSelected(TargetScope.SYSTEM_APPS) }
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.translator_target_scope_system_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.translator_target_scope_system_desc, 0),
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
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )

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
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )

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
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )
    var showIconSheet by remember { mutableStateOf(false) }
    var showPrioritySheet by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.translator_meta_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            // 1. Outlined Icon Selector Card (Opens Icon Sheet)
            Card(
                onClick = { showIconSheet = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = getTranslatorOutlinedIcon(translator.meta.iconName),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.translator_meta_icon_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = TRANSLATOR_OUTLINED_ICONS.firstOrNull { it.id.equals(translator.meta.iconName, ignoreCase = true) }?.label
                                ?: stringResource(R.string.translator_meta_icon_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    FilledTonalIconButton(
                        onClick = { showIconSheet = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = stringResource(R.string.translator_btn_edit_meta),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // 2. Name & Author Fields
            OutlinedTextField(
                value = translator.meta.name,
                onValueChange = { onTranslatorChange(translator.copy(meta = translator.meta.copy(name = it))) },
                label = { Text(stringResource(R.string.meta_label_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardActions = KeyboardActions(onDone = { fm.clearFocus() })
            )

            OutlinedTextField(
                value = translator.meta.author,
                onValueChange = { onTranslatorChange(translator.copy(meta = translator.meta.copy(author = it))) },
                label = { Text(stringResource(R.string.meta_label_author)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardActions = KeyboardActions(onDone = { fm.clearFocus() })
            )

            // 3. Priority Card (Opens Priority Sheet)
            Card(
                onClick = { showPrioritySheet = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Speed,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.translator_priority_sheet_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "${translator.priority}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = stringResource(R.string.translator_priority_card_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    FilledTonalIconButton(
                        onClick = { showPrioritySheet = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = stringResource(R.string.translator_btn_edit_meta),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // 4. Description Field
            OutlinedTextField(
                value = translator.meta.description,
                onValueChange = { onTranslatorChange(translator.copy(meta = translator.meta.copy(description = it))) },
                label = { Text(stringResource(R.string.translator_field_description)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(stringResource(R.string.done))
            }
        }
    }

    if (showIconSheet) {
        TranslatorIconSelectionSheet(
            selectedIconName = translator.meta.iconName,
            onIconSelected = { iconId ->
                onTranslatorChange(translator.copy(meta = translator.meta.copy(iconName = iconId)))
                showIconSheet = false
            },
            onDismiss = { showIconSheet = false }
        )
    }

    if (showPrioritySheet) {
        TranslatorPrioritySheet(
            priority = translator.priority,
            onPriorityChange = { newPriority ->
                onTranslatorChange(translator.copy(priority = newPriority))
            },
            onDismiss = { showPrioritySheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslatorIconSelectionSheet(
    selectedIconName: String,
    onIconSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column {
                Text(
                    text = stringResource(R.string.translator_icon_sheet_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.translator_icon_sheet_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 72.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
            ) {
                items(TRANSLATOR_OUTLINED_ICONS) { option ->
                    val isSelected = selectedIconName.equals(option.id, ignoreCase = true)
                    Surface(
                        onClick = { onIconSelected(option.id) },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
                        ) {
                            Icon(
                                imageVector = option.icon,
                                contentDescription = option.label,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(stringResource(R.string.done))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslatorPrioritySheet(
    priority: Int,
    onPriorityChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )
    var textValue by remember(priority) { mutableStateOf(priority.toString()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column {
                Text(
                    text = stringResource(R.string.translator_priority_sheet_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.translator_priority_sheet_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Stepper and Number Input Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.translator_priority_input_label),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    // [-] [Number Field] [+]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalIconButton(
                            onClick = {
                                val newPriority = (priority - 10).coerceAtLeast(0)
                                textValue = newPriority.toString()
                                onPriorityChange(newPriority)
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease Priority")
                        }

                        OutlinedTextField(
                            value = textValue,
                            onValueChange = { input ->
                                textValue = input
                                val num = input.filter { it.isDigit() }.toIntOrNull() ?: 0
                                onPriorityChange(num.coerceIn(0, 9999))
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.titleLarge.copy(
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )

                        FilledTonalIconButton(
                            onClick = {
                                val newPriority = (priority + 10).coerceAtMost(9999)
                                textValue = newPriority.toString()
                                onPriorityChange(newPriority)
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase Priority")
                        }
                    }

                    // Quick Preset Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val presets = listOf(
                            50 to stringResource(R.string.translator_priority_preset_low),
                            100 to stringResource(R.string.translator_priority_preset_default),
                            200 to stringResource(R.string.translator_priority_preset_high),
                            500 to stringResource(R.string.translator_priority_preset_urgent)
                        )
                        presets.forEach { (presetVal, presetLabel) ->
                            val isSelected = priority == presetVal
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    textValue = presetVal.toString()
                                    onPriorityChange(presetVal)
                                },
                                label = { Text(presetLabel, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Online Documentation Card
            val uriHandler = LocalUriHandler.current
            Card(
                onClick = { uriHandler.openUri(DocumentationUrls.CUSTOM_TRANSLATORS_DOCS) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Outlined.Lightbulb,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.documentation_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.feature_guides_redirect_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(stringResource(R.string.done))
            }
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
        Surface {
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
                themeBinding = ThemeBinding(
                    themeId = "active"
                ),
                installedThemes = listOf(
                    com.d4viddf.hyperbridge.models.theme.HyperTheme(
                        id = "hyper_neon",
                        meta = com.d4viddf.hyperbridge.models.theme.ThemeMetadata(
                            name = "Hyper Neon",
                            author = "Community",
                            version = 1
                        ),
                        global = com.d4viddf.hyperbridge.models.theme.GlobalConfig(
                            highlightColor = "#00E5FF"
                        )
                    )
                ),
                onPresentationChange = {},
                onThemeBindingChange = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ThemeSelectionSheetPreview() {
    MaterialTheme {
        Surface {
            ThemeSelectionContent(
                currentThemeId = "active",
                installedThemes = listOf(
                    com.d4viddf.hyperbridge.models.theme.HyperTheme(
                        id = "hyper_neon",
                        meta = com.d4viddf.hyperbridge.models.theme.ThemeMetadata(
                            name = "Hyper Neon",
                            author = "Community",
                            version = 1
                        ),
                        global = com.d4viddf.hyperbridge.models.theme.GlobalConfig(
                            highlightColor = "#00E5FF"
                        )
                    ),
                    com.d4viddf.hyperbridge.models.theme.HyperTheme(
                        id = "sunset_glow",
                        meta = com.d4viddf.hyperbridge.models.theme.ThemeMetadata(
                            name = "Sunset Glow",
                            author = "DesignHub",
                            version = 2
                        ),
                        global = com.d4viddf.hyperbridge.models.theme.GlobalConfig(
                            highlightColor = "#FF5722"
                        )
                    )
                ),
                onThemeSelected = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TemplateSelectionSheetPreview() {
    MaterialTheme {
        Surface {
            TemplateSelectionContent(
                currentTemplateId = "tpl_standard_notification",
                onTemplateSelected = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WidgetSelectionSheetPreview() {
    MaterialTheme {
        Surface {
            WidgetSelectionContent(
                currentWidgetId = "widget_system_status",
                onWidgetSelected = {}
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

@Preview(showBackground = true)
@Composable
fun LeftIconSelectionSheetPreview() {
    MaterialTheme {
        Surface {
            LeftIconSelectionContent(
                currentSource = "APP_ICON",
                onSourceSelected = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PresentationModeSelectionSheetPreview() {
    MaterialTheme {
        Surface {
            PresentationModeSelectionContent(
                currentMode = PresentationMode.STANDARD,
                onModeSelected = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProgressSlotTypeSelectionSheetPreview() {
    MaterialTheme {
        Surface {
            ProgressSlotTypeSelectionContent(
                currentType = ProgressSlotType.PROGRESS_BAR,
                onTypeSelected = {}
            )
        }
    }
}



@Preview(showBackground = true)
@Composable
fun ActionSourceSelectionSheetPreview() {
    MaterialTheme {
        Surface {
            ActionSourceSelectionContent(
                currentSource = ActionSource.NOTIFICATION_ACTION,
                onSourceSelected = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SmartActionTypeSelectionSheetPreview() {
    MaterialTheme {
        Surface {
            SmartActionTypeSelectionContent(
                currentType = SmartActionType.OTP_COPY,
                onTypeSelected = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ActionDisplayModeSelectionSheetPreview() {
    MaterialTheme {
        Surface {
            ActionDisplayModeSelectionContent(
                currentMode = ActionDisplayMode.ICON_ONLY,
                onModeSelected = {}
            )
        }
    }
}

// =========================================================================
// PILL CUSTOMIZATION CONTENT, PREVIEW & BOTTOM SHEETS
// =========================================================================

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TranslatorPillContent(
    pillConfig: com.d4viddf.hyperbridge.models.translator.CompactPillConfig,
    installedThemes: List<com.d4viddf.hyperbridge.models.theme.HyperTheme> = emptyList(),
    themeBinding: ThemeBinding = ThemeBinding(),
    showInlinePreview: Boolean = true,
    selectedSubRoute: TranslatorRoute? = null,
    onOpenSubRoute: ((TranslatorRoute) -> Unit)? = null,
    onPillConfigChange: (com.d4viddf.hyperbridge.models.translator.CompactPillConfig) -> Unit
) {
    var showLeftSheet by remember { mutableStateOf(false) }
    var showRightSheet by remember { mutableStateOf(false) }
    var showMediaOrderSheet by remember { mutableStateOf(false) }
    var showLearnSheet by remember { mutableStateOf(false) }

    val leftDesign = pillConfig.leftDesign
    val rightDesign = pillConfig.rightDesign

    val linkedTheme = if (themeBinding.themeId.isNotBlank() && themeBinding.themeId != "active") {
        installedThemes.find { it.id == themeBinding.themeId }
    } else {
        null
    }

    val highlightColor = if (linkedTheme?.global?.highlightColor != null) {
        safeParseColor(linkedTheme.global.highlightColor)
    } else {
        MaterialTheme.colorScheme.primary
    }

    val iconShapeId = linkedTheme?.global?.iconShapeId ?: "circle"
    val iconShape = getShapeFromId(iconShapeId).toShape()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- PREVIEW CARD ---
        if (showInlinePreview) {
            Text(
                stringResource(R.string.preview),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            TranslatorPillIslandPreview(
                left = leftDesign,
                right = rightDesign,
                highlightColor = highlightColor,
                iconShape = iconShape,
                mediaOrder = pillConfig.mediaOrder
            )
        }

        // --- LEARN HOW COMPACT PILL WORKS BUTTON ---
        Button(
            onClick = { showLearnSheet = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.documentation_title),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }

        // --- GROUP CONFIGURATION HEADER ---
        Text(
            stringResource(R.string.group_configuration),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        // --- SLOTS CARD ---
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(vertical = 4.dp)) {
                // Left Content Row
                val isLeftSelected = selectedSubRoute == TranslatorRoute.PILL_LEFT
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(
                            if (isLeftSelected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                            else Color.Transparent
                        )
                        .clickable {
                            if (onOpenSubRoute != null) {
                                onOpenSubRoute(TranslatorRoute.PILL_LEFT)
                            } else {
                                showLeftSheet = true
                            }
                        }
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                when (leftDesign) {
                                    PillLeftDesign.ICON_ONLY,
                                    PillLeftDesign.ICON_AND_TEXT -> Icons.Outlined.AutoAwesome
                                    PillLeftDesign.AVATAR -> Icons.Outlined.Person
                                    PillLeftDesign.TEXT_ONLY -> Icons.Outlined.TextFields
                                    PillLeftDesign.MEDIA -> Icons.Outlined.MusicNote
                                    PillLeftDesign.HIDDEN -> Icons.Outlined.VisibilityOff
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.left_content),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = when (leftDesign) {
                                PillLeftDesign.ICON_AND_TEXT -> stringResource(R.string.screen_recording_left_option_icon_and_text)
                                PillLeftDesign.ICON_ONLY -> stringResource(R.string.screen_recording_left_option_icon_only)
                                PillLeftDesign.TEXT_ONLY -> stringResource(R.string.screen_recording_left_option_text_only)
                                PillLeftDesign.AVATAR -> stringResource(R.string.translator_pill_left_avatar)
                                PillLeftDesign.MEDIA -> stringResource(R.string.translator_pill_left_media)
                                PillLeftDesign.HIDDEN -> stringResource(R.string.translator_pill_left_hidden)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )

                // Right Content Row
                val isRightSelected = selectedSubRoute == TranslatorRoute.PILL_RIGHT
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                        .background(
                            if (isRightSelected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                            else Color.Transparent
                        )
                        .clickable {
                            if (onOpenSubRoute != null) {
                                onOpenSubRoute(TranslatorRoute.PILL_RIGHT)
                            } else {
                                showRightSheet = true
                            }
                        }
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                when (rightDesign) {
                                    PillRightDesign.AUTO -> Icons.Outlined.Tune
                                    PillRightDesign.PROGRESS_PERCENT -> Icons.Outlined.Speed
                                    PillRightDesign.TIMER -> Icons.Outlined.Timer
                                    PillRightDesign.HIGHLIGHT_TEXT -> Icons.Outlined.Subtitles
                                    PillRightDesign.NONE -> Icons.Outlined.VisibilityOff
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.right_content),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = when (rightDesign) {
                                PillRightDesign.AUTO -> stringResource(R.string.translator_pill_right_auto)
                                PillRightDesign.PROGRESS_PERCENT -> stringResource(R.string.translator_pill_right_progress)
                                PillRightDesign.TIMER -> stringResource(R.string.fallback_timer)
                                PillRightDesign.HIGHLIGHT_TEXT -> stringResource(R.string.translator_pill_right_highlight)
                                PillRightDesign.NONE -> stringResource(R.string.nav_content_none)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // --- MEDIA CONFIGURATION CARD (VISIBLE ONLY WHEN LEFT IS MEDIA) ---
        if (leftDesign == PillLeftDesign.MEDIA) {
            Text(
                stringResource(R.string.translator_pill_media_preferences_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            val isMediaOrderSelected = selectedSubRoute == TranslatorRoute.PILL_MEDIA_ORDER
            Card(
                border = if (isMediaOrderSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                colors = CardDefaults.cardColors(
                    containerColor = if (isMediaOrderSelected) {
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    }
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .clickable {
                            if (onOpenSubRoute != null) {
                                onOpenSubRoute(TranslatorRoute.PILL_MEDIA_ORDER)
                            } else {
                                showMediaOrderSheet = true
                            }
                        }
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Outlined.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.translator_pill_media_order_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(2.dp))
                        val mediaOrderSummary = if (pillConfig.mediaOrder.isEmpty()) {
                            stringResource(R.string.nav_content_none)
                        } else {
                            val albumCoverLabel = stringResource(R.string.translator_pill_media_item_album_cover)
                            val trackLabel = stringResource(R.string.translator_pill_media_item_track)
                            val artistLabel = stringResource(R.string.translator_pill_media_item_artist)
                            pillConfig.mediaOrder.joinToString(" • ") { item ->
                                when (item) {
                                    PillMediaItem.ALBUM_COVER -> albumCoverLabel
                                    PillMediaItem.TRACK -> trackLabel
                                    PillMediaItem.ARTIST -> artistLabel
                                }
                            }
                        }
                        Text(
                            text = mediaOrderSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Left Design Bottom Sheet
    if (showLeftSheet) {
        TranslatorPillLeftSelectionSheet(
            selected = leftDesign,
            onSelect = {
                onPillConfigChange(pillConfig.copy(leftDesign = it))
                showLeftSheet = false
            },
            onDismiss = { showLeftSheet = false }
        )
    }

    // Right Design Bottom Sheet
    if (showRightSheet) {
        TranslatorPillRightSelectionSheet(
            selected = rightDesign,
            onSelect = {
                onPillConfigChange(pillConfig.copy(rightDesign = it))
                showRightSheet = false
            },
            onDismiss = { showRightSheet = false }
        )
    }

    // Media Order Bottom Sheet
    if (showMediaOrderSheet) {
        TranslatorPillMediaOrderSheet(
            mediaOrder = pillConfig.mediaOrder,
            onUpdateOrder = { newOrder ->
                onPillConfigChange(pillConfig.copy(mediaOrder = newOrder))
            },
            onDismiss = { showMediaOrderSheet = false }
        )
    }

    // Learn Guide Bottom Sheet
    if (showLearnSheet) {
        TranslatorPillGuideSheet(
            onDismiss = { showLearnSheet = false }
        )
    }
}

// --- PILL PREVIEW COMPONENT ---
@Composable
private fun TranslatorPillIslandPreview(
    left: PillLeftDesign,
    right: PillRightDesign,
    highlightColor: Color,
    iconShape: Shape,
    mediaOrder: List<PillMediaItem> = listOf(
        PillMediaItem.ALBUM_COVER,
        PillMediaItem.TRACK,
        PillMediaItem.ARTIST
    )
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 36.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            com.d4viddf.hyperbridge.ui.components.island.HyperOsCompactPill(
                leftDesign = left,
                rightDesign = right,
                title = "App Alert",
                rightText = if (right == PillRightDesign.TIMER) "00:05" else "00:05",
                progressPercent = 65,
                highlightColor = highlightColor,
                iconShape = iconShape,
                mediaOrder = mediaOrder,
                mediaTrack = "Starboy",
                mediaArtist = "The Weeknd"
            )
        }
    }
}

// --- LEFT PILL SELECTION BOTTOM SHEET ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslatorPillLeftSelectionSheet(
    selected: PillLeftDesign,
    onSelect: (PillLeftDesign) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        TranslatorPillLeftSelectionContent(
            selected = selected,
            onSelect = onSelect
        )
    }
}

@Composable
fun TranslatorPillLeftSelectionContent(
    selected: PillLeftDesign,
    showHeader: Boolean = true,
    onSelect: (PillLeftDesign) -> Unit
) {
    val options = listOf(
        Triple(PillLeftDesign.ICON_AND_TEXT, Pair(R.string.screen_recording_left_option_icon_and_text, R.string.translator_pill_left_icon_and_text_desc), Icons.Outlined.AutoAwesome),
        Triple(PillLeftDesign.ICON_ONLY, Pair(R.string.screen_recording_left_option_icon_only, R.string.translator_pill_left_icon_only_desc), Icons.Outlined.AutoAwesome),
        Triple(PillLeftDesign.TEXT_ONLY, Pair(R.string.screen_recording_left_option_text_only, R.string.translator_pill_left_text_only_desc), Icons.Outlined.TextFields),
        Triple(PillLeftDesign.AVATAR, Pair(R.string.translator_pill_left_avatar, R.string.translator_pill_left_avatar_desc), Icons.Outlined.Person),
        Triple(PillLeftDesign.MEDIA, Pair(R.string.translator_pill_left_media, R.string.translator_pill_left_media_desc), Icons.Outlined.MusicNote),
        Triple(PillLeftDesign.HIDDEN, Pair(R.string.translator_pill_left_hidden, R.string.translator_pill_left_hidden_desc), Icons.Outlined.VisibilityOff)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (showHeader) Modifier.padding(horizontal = 20.dp, vertical = 8.dp).padding(bottom = 32.dp) else Modifier.fillMaxSize().padding(16.dp)),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (showHeader) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Outlined.DashboardCustomize,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.translator_pill_left_sheet_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = !showHeader),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(options) { (option, textResPair, icon) ->
                val (titleRes, descRes) = textResPair
                val isSelected = selected == option
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
                    ),
                    onClick = { onSelect(option) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    icon,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(titleRes),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(descRes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isSelected) {
                            Spacer(Modifier.width(12.dp))
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- RIGHT PILL SELECTION BOTTOM SHEET ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslatorPillRightSelectionSheet(
    selected: PillRightDesign,
    onSelect: (PillRightDesign) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        TranslatorPillRightSelectionContent(
            selected = selected,
            onSelect = onSelect
        )
    }
}

@Composable
fun TranslatorPillRightSelectionContent(
    selected: PillRightDesign,
    showHeader: Boolean = true,
    onSelect: (PillRightDesign) -> Unit
) {
    val options = listOf(
        Triple(PillRightDesign.AUTO, Pair(R.string.translator_pill_right_auto, R.string.translator_pill_right_auto_desc), Icons.Outlined.Tune),
        Triple(PillRightDesign.PROGRESS_PERCENT, Pair(R.string.translator_pill_right_progress, R.string.translator_pill_right_progress_desc), Icons.Outlined.Speed),
        Triple(PillRightDesign.TIMER, Pair(R.string.fallback_timer, R.string.translator_pill_right_timer_desc), Icons.Outlined.Timer),
        Triple(PillRightDesign.HIGHLIGHT_TEXT, Pair(R.string.translator_pill_right_highlight, R.string.translator_pill_right_highlight_desc), Icons.Outlined.Subtitles),
        Triple(PillRightDesign.NONE, Pair(R.string.nav_content_none, R.string.translator_pill_right_none_desc), Icons.Outlined.VisibilityOff)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (showHeader) Modifier.padding(horizontal = 20.dp, vertical = 8.dp).padding(bottom = 32.dp) else Modifier.fillMaxSize().padding(16.dp)),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (showHeader) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Outlined.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.translator_pill_right_sheet_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = !showHeader),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(options) { (option, textResPair, icon) ->
                val (titleRes, descRes) = textResPair
                val isSelected = selected == option
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
                    ),
                    onClick = { onSelect(option) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    icon,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(titleRes),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(descRes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isSelected) {
                            Spacer(Modifier.width(12.dp))
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- PILL GUIDE BOTTOM SHEET ---
// --- PILL GUIDE BOTTOM SHEET ---
@Composable
fun TranslatorPillGuideSheet(
    onDismiss: () -> Unit
) {
    DesignManagerHelpSheet(
        onDismiss = onDismiss,
        title = stringResource(R.string.translator_menu_pill),
        description = stringResource(R.string.feature_guides_redirect_desc),
        docsUrl = DocumentationUrls.CUSTOM_TRANSLATORS_DOCS,
        icon = Icons.Outlined.AutoAwesome
    )
}

// --- MEDIA ORDER BOTTOM SHEET ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslatorPillMediaOrderSheet(
    mediaOrder: List<PillMediaItem>,
    onUpdateOrder: (List<PillMediaItem>) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        TranslatorPillMediaOrderContent(
            mediaOrder = mediaOrder,
            onUpdateOrder = onUpdateOrder
        )
    }
}

@Composable
fun TranslatorPillMediaOrderContent(
    mediaOrder: List<PillMediaItem>,
    showHeader: Boolean = true,
    onUpdateOrder: (List<PillMediaItem>) -> Unit
) {
    val allPossibleItems = remember { PillMediaItem.entries }
    var itemOrder by remember {
        mutableStateOf(mediaOrder + (allPossibleItems - mediaOrder.toSet()))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (showHeader) Modifier.padding(horizontal = 20.dp, vertical = 8.dp).padding(bottom = 32.dp) else Modifier.fillMaxSize().padding(16.dp)),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (showHeader) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Outlined.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.translator_pill_media_order_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.translator_pill_media_order_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Text(
                text = stringResource(R.string.translator_pill_media_order_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = !showHeader),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(itemOrder) { index, item ->
                val isEnabled = mediaOrder.contains(item)
                val (titleRes, descRes, icon) = when (item) {
                    PillMediaItem.ALBUM_COVER -> Triple(
                        R.string.translator_pill_media_item_album_cover,
                        R.string.translator_pill_media_item_album_cover_desc,
                        Icons.Outlined.Album
                    )
                    PillMediaItem.TRACK -> Triple(
                        R.string.translator_pill_media_item_track,
                        R.string.translator_pill_media_item_track_desc,
                        Icons.Outlined.MusicNote
                    )
                    PillMediaItem.ARTIST -> Triple(
                        R.string.translator_pill_media_item_artist,
                        R.string.translator_pill_media_item_artist_desc,
                        Icons.Outlined.Person
                    )
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isEnabled) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    icon,
                                    contentDescription = null,
                                    tint = if (isEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(titleRes),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(descRes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Move Up
                        IconButton(
                            onClick = {
                                if (index > 0) {
                                    val list = itemOrder.toMutableList()
                                    val temp = list.removeAt(index)
                                    list.add(index - 1, temp)
                                    itemOrder = list
                                    onUpdateOrder(list.filter { it in mediaOrder })
                                }
                            },
                            enabled = index > 0,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowUpward,
                                contentDescription = "Move Up",
                                tint = if (index > 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Move Down
                        IconButton(
                            onClick = {
                                if (index < itemOrder.lastIndex) {
                                    val list = itemOrder.toMutableList()
                                    val temp = list.removeAt(index)
                                    list.add(index + 1, temp)
                                    itemOrder = list
                                    onUpdateOrder(list.filter { it in mediaOrder })
                                }
                            },
                            enabled = index < itemOrder.lastIndex,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowDownward,
                                contentDescription = "Move Down",
                                tint = if (index < itemOrder.lastIndex) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(Modifier.width(4.dp))

                        // Enable / Disable Switch
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { checked ->
                                val newSelected = if (checked) {
                                    mediaOrder + item
                                } else {
                                    mediaOrder.filter { it != item }
                                }
                                onUpdateOrder(itemOrder.filter { it in newSelected })
                            }
                        )
                    }
                }
            }
        }
    }
}




