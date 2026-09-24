package com.d4viddf.hyperbridge.ui.screens.design

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.outlined.DashboardCustomize
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.carousel.HorizontalCenteredHeroCarousel
import androidx.compose.material3.carousel.rememberCarouselState
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toDrawable
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.data.theme.ThemeRepository
import com.d4viddf.hyperbridge.data.widget.WidgetManager
import com.d4viddf.hyperbridge.models.theme.CallModule
import com.d4viddf.hyperbridge.models.theme.GlobalConfig
import com.d4viddf.hyperbridge.models.theme.HyperTheme
import com.d4viddf.hyperbridge.models.theme.ResourceType
import com.d4viddf.hyperbridge.models.theme.ThemeMetadata
import com.d4viddf.hyperbridge.util.DocumentationUrls
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

// --- 1. STATEFUL COMPOSABLE (Logic Layer) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesignScreen(
    onNavigateToWidgets: () -> Unit,
    onNavigateToThemes: () -> Unit,
    onEditTheme: (String) -> Unit,
    onNavigateToDesigns: () -> Unit = {},
    onNavigateToTranslators: () -> Unit = {},
    onCreateTranslator: () -> Unit = {},
    onLaunchPicker: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val context = LocalContext.current
    val preferences = remember { AppPreferences(context.applicationContext) }
    val themeRepo = remember { ThemeRepository(context.applicationContext) }
    val translatorViewModel: com.d4viddf.hyperbridge.ui.screens.translators.TranslatorViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

    val activeThemeId by preferences.activeThemeIdFlow.collectAsState(initial = null)
    val savedWidgetIds by preferences.savedWidgetIdsFlow.collectAsState(initial = emptyList())
    val allTranslators by translatorViewModel.allTranslators.collectAsState()

    var availableThemes by remember { mutableStateOf<List<HyperTheme>>(emptyList()) }
    var widgetIcons by remember { mutableStateOf<List<Drawable>>(emptyList()) }
    // [NEW] State for cached theme icons
    var themeIcons by remember { mutableStateOf<Map<String, ImageBitmap?>>(emptyMap()) }

    var showBottomSheet by remember { mutableStateOf(false) }
    var showAddDesign by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    // A design is a translator that renders through a template (or, later, a custom widget island).
    val designs = allTranslators.filter {
        it.presentation.mode == com.d4viddf.hyperbridge.models.translator.PresentationMode.TEMPLATE ||
            it.presentation.mode == com.d4viddf.hyperbridge.models.translator.PresentationMode.WIDGET
    }

    LaunchedEffect(Unit) {
        val themes = themeRepo.getAvailableThemes()
        availableThemes = themes

        // [NEW] Load custom icons for themes
        withContext(Dispatchers.IO) {
            val iconMap = mutableMapOf<String, ImageBitmap?>()
            themes.forEach { theme ->
                val iconRes = theme.meta.customIcon
                if (iconRes != null && iconRes.type == ResourceType.LOCAL_FILE) {
                    try {
                        val file = File(themeRepo.getThemesDir(), "${theme.id}/${iconRes.value}")
                        if (file.exists()) {
                            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                            if (bitmap != null) {
                                iconMap[theme.id] = bitmap.asImageBitmap()
                            }
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }
            themeIcons = iconMap
        }
    }

    LaunchedEffect(savedWidgetIds) {
        if (savedWidgetIds.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                val icons = savedWidgetIds.take(5).mapNotNull { id ->
                    val info = WidgetManager.getWidgetInfo(context, id)
                    try {
                        val pkg = info?.provider?.packageName
                        if (pkg != null) context.packageManager.getApplicationIcon(pkg) else null
                    } catch (_: Exception) { null }
                }
                widgetIcons = icons
            }
        } else {
            widgetIcons = emptyList()
        }
    }

    // Call the stateless composable with loaded data
    DesignScreenContent(
        activeThemeId = activeThemeId,
        availableThemes = availableThemes,
        // [NEW] Pass icons down
        savedWidgetCount = savedWidgetIds.size,
        translators = allTranslators,
        designs = designs,
        onAddDesign = { showAddDesign = true },
        onNavigateToDesigns = onNavigateToDesigns,
        onNavigateToWidgets = onNavigateToWidgets,
        onNavigateToThemes = onNavigateToThemes,
        onEditTheme = onEditTheme,
        onNavigateToTranslators = onNavigateToTranslators,
        onCreateTranslator = onCreateTranslator,
        onFabClick = { showBottomSheet = true },
        onSettingsClick = onSettingsClick
    )

    if (showAddDesign) {
        AddDesignFlow(
            onDismiss = { showAddDesign = false },
            onDesignCreated = { design ->
                showAddDesign = false
                translatorViewModel.saveTranslator(design)
                Toast.makeText(context, R.string.design_design_created, Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 48.dp, start = 16.dp, end = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.design_add_to_island),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                Button(
                    onClick = {
                        showBottomSheet = false
                        onLaunchPicker()
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Rounded.Widgets, null, modifier = Modifier.padding(end = 8.dp))
                    Text(stringResource(R.string.design_system_widget_beta), style = MaterialTheme.typography.titleMedium)
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        showBottomSheet = false
                        onNavigateToThemes()
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Rounded.Palette, null, modifier = Modifier.padding(end = 8.dp))
                    Text(stringResource(R.string.design_get_themes), style = MaterialTheme.typography.titleMedium)
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        showBottomSheet = false
                        onCreateTranslator()
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Extension, null, modifier = Modifier.padding(end = 8.dp))
                    Text(stringResource(R.string.design_action_create_translator), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

// --- 2. STATELESS COMPOSABLE (UI Layer - Previewable) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesignScreenContent(
    activeThemeId: String?,
    availableThemes: List<HyperTheme>,
    savedWidgetCount: Int,
    translators: List<com.d4viddf.hyperbridge.models.translator.CustomTranslator> = emptyList(),
    designs: List<com.d4viddf.hyperbridge.models.translator.CustomTranslator> = emptyList(),
    onAddDesign: () -> Unit = {},
    onNavigateToDesigns: () -> Unit = {},
    onNavigateToWidgets: () -> Unit,
    onNavigateToThemes: () -> Unit,
    onEditTheme: (String) -> Unit,
    onNavigateToTranslators: () -> Unit = {},
    onCreateTranslator: () -> Unit = {},
    onFabClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                },
                actions = {
                    Surface(
                        modifier = Modifier
                            .size(40.dp)
                            .padding(end = 8.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        onClick = onSettingsClick
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Settings, null, modifier = Modifier.size(20.dp))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onFabClick,
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.design_add_design))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Top HeroSection carousel
            HeroSection()

            Spacer(Modifier.height(16.dp))

            // Bento Grid on common background surface with rounded top corners covering remaining height
            DesignVariantBento(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                availableThemes = availableThemes,
                activeThemeId = activeThemeId,
                savedWidgetCount = savedWidgetCount,
                designs = designs,
                translators = translators,
                onNavigateToThemes = onNavigateToThemes,
                onCreateTheme = { onEditTheme("") },
                onNavigateToWidgets = onNavigateToWidgets,
                onAddWidget = onFabClick,
                onNavigateToDesigns = onNavigateToDesigns,
                onAddDesign = onAddDesign,
                onNavigateToTranslators = onNavigateToTranslators,
                onCreateTranslator = onCreateTranslator
            )
        }
    }
}

// --- SECTIONS ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeroSection() {
    val uriHandler = LocalUriHandler.current
    val items = listOf(
        HeroItem(stringResource(R.string.design_hero_customization_title), stringResource(R.string.design_hero_customization_subtitle), Color(0xFF4CAF50)) {
            uriHandler.openUri(DocumentationUrls.CUSTOMIZATION_DOCS)
        },
        HeroItem(stringResource(R.string.design_hero_pro_title), stringResource(R.string.design_hero_pro_subtitle), Color(0xFF2196F3)) {
            uriHandler.openUri(DocumentationUrls.THEME_CREATOR_DOCS)
        },
        HeroItem(stringResource(R.string.design_hero_community_title), stringResource(R.string.design_hero_community_subtitle), Color(0xFF9C27B0)
        ) { uriHandler.openUri("https://github.com/D4vidDf/HyperBridge/discussions") }
    )

    val state = rememberCarouselState { items.size }

    HorizontalCenteredHeroCarousel(
        modifier = Modifier.fillMaxWidth(),
        state = state,
        itemSpacing = 8.dp,
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) { i ->
        val item = items[i]
        HeroCard(
            item,
            Modifier.maskClip(MaterialTheme.shapes.extraLarge)
        )
    }
}


// --- BENTO GRID LAYOUT ---

@Composable
fun DesignVariantBento(
    modifier: Modifier = Modifier,
    availableThemes: List<HyperTheme>,
    activeThemeId: String?,
    savedWidgetCount: Int,
    designs: List<com.d4viddf.hyperbridge.models.translator.CustomTranslator>,
    translators: List<com.d4viddf.hyperbridge.models.translator.CustomTranslator>,
    onNavigateToThemes: () -> Unit,
    onCreateTheme: () -> Unit,
    onNavigateToWidgets: () -> Unit,
    onAddWidget: () -> Unit,
    onNavigateToDesigns: () -> Unit,
    onAddDesign: () -> Unit,
    onNavigateToTranslators: () -> Unit,
    onCreateTranslator: () -> Unit
) {
    val activeThemeName = availableThemes.find { it.id == activeThemeId }?.meta?.name
    val activeDesignsCount = designs.count { it.isEnabled }
    val activeTranslatorsCount = translators.count { it.isEnabled }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Row 1: Themes & Widgets
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                BentoCell(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.design_section_themes),
                    status = activeThemeName ?: stringResource(R.string.design_card_themes_active_default),
                    icon = Icons.Rounded.Palette,
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    iconColor = MaterialTheme.colorScheme.primary,
                    onCardClick = onNavigateToThemes,
                    onActionClick = onCreateTheme
                )

                BentoCell(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.design_section_widgets),
                    status = if (savedWidgetCount > 0) stringResource(R.string.design_card_widgets_saved, savedWidgetCount) else stringResource(R.string.design_card_widgets_empty),
                    icon = Icons.Rounded.Widgets,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                    iconColor = MaterialTheme.colorScheme.secondary,
                    onCardClick = onNavigateToWidgets,
                    onActionClick = onAddWidget
                )
            }

            // Row 2: Designs & Translators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                BentoCell(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.design_section_designs),
                    status = stringResource(R.string.design_status_active, activeDesignsCount),
                    icon = Icons.Outlined.DashboardCustomize,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
                    iconColor = MaterialTheme.colorScheme.tertiary,
                    onCardClick = onNavigateToDesigns,
                    onActionClick = onAddDesign
                )

                BentoCell(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.design_section_translators),
                    status = stringResource(R.string.design_status_active, activeTranslatorsCount),
                    icon = Icons.Default.Extension,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    iconColor = MaterialTheme.colorScheme.onSurface,
                    onCardClick = onNavigateToTranslators,
                    onActionClick = onCreateTranslator
                )
            }
        }
    }
}

@Composable
private fun BentoCell(
    modifier: Modifier = Modifier,
    title: String,
    status: String,
    icon: ImageVector,
    containerColor: Color,
    iconColor: Color,
    onCardClick: () -> Unit,
    onActionClick: () -> Unit
) {
    Card(
        onClick = onCardClick,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = modifier.height(170.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    modifier = Modifier.size(38.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = iconColor)
                    }
                }

                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    onClick = onActionClick
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp), tint = iconColor)
                    }
                }
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// --- CARDS & COMPONENTS ---

data class HeroItem(val title: String, val subtitle: String, val color: Color, val onClick: () -> Unit)

@Composable
fun HeroCard(item: HeroItem, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .height(200.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        onClick = item.onClick
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(item.color.copy(alpha = 0.1f))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 20.dp, y = 20.dp)
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(item.color.copy(alpha = 0.2f))
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(24.dp)
            ) {
                Text(item.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(item.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// --- 3. PREVIEWS ---

@SuppressLint("UseKtx")
@Preview(showBackground = true)
@Composable
private fun DesignScreenPreview() {
    val mockThemes = listOf(
        HyperTheme(
            id = "1",
            meta = ThemeMetadata("Neon City", "David", 1),
            global = GlobalConfig(
                highlightColor = "#00FF00",
                backgroundColor = "#000000",
                textColor = "#FFFFFF",
                useAppColors = false,
                iconShapeId = "circle",
                iconPaddingPercent = 10
            ),
            callConfig = CallModule(null, null, "#00FF00", "#FF0000")
        ),
        HyperTheme(
            id = "2",
            meta = ThemeMetadata("Sunset", "Alice", 1),
            global = GlobalConfig(
                highlightColor = "#FF5722",
                backgroundColor = "#202124",
                textColor = "#FFFFFF",
                useAppColors = false,
                iconShapeId = "square",
                iconPaddingPercent = 15
            ),
            callConfig = CallModule(null, null, "#00FF00", "#FF0000")
        )
    )

    MaterialTheme {
        DesignScreenContent(
            activeThemeId = "1",
            availableThemes = mockThemes,
            savedWidgetCount = 2,
            onNavigateToWidgets = {},
            onNavigateToThemes = {},
            onEditTheme = {},
            onFabClick = {},
            onSettingsClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DesignScreenEmptyPreview() {
    MaterialTheme {
        DesignScreenContent(
            activeThemeId = null,
            availableThemes = emptyList(),
            savedWidgetCount = 0,
            onNavigateToWidgets = {},
            onNavigateToThemes = {},
            onEditTheme = {},
            onFabClick = {},
            onSettingsClick = {}
        )
    }
}



@Preview(name = "Bento Grid", showBackground = true)
@Composable
private fun DesignVariantBentoPreview() {
    val mockThemes = listOf(
        HyperTheme(
            id = "1",
            meta = ThemeMetadata("Sunset Glow", "Alice", 1),
            global = GlobalConfig(highlightColor = "#FF5722", backgroundColor = "#202124", textColor = "#FFFFFF", useAppColors = false, iconShapeId = "square", iconPaddingPercent = 15),
            callConfig = CallModule(null, null, "#FF5722", "#D32F2F")
        )
    )
    MaterialTheme {
        DesignVariantBento(
            availableThemes = mockThemes,
            activeThemeId = "1",
            savedWidgetCount = 4,
            designs = emptyList(),
            translators = emptyList(),
            onNavigateToThemes = {},
            onCreateTheme = {},
            onNavigateToWidgets = {},
            onAddWidget = {},
            onNavigateToDesigns = {},
            onAddDesign = {},
            onNavigateToTranslators = {},
            onCreateTranslator = {}
        )
    }
}