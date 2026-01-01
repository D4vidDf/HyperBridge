package com.d4viddf.hyperbridge.ui.screens.design

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhoneAndroid
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
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.data.theme.ThemeRepository
import com.d4viddf.hyperbridge.data.widget.WidgetManager
import com.d4viddf.hyperbridge.models.theme.CallModule
import com.d4viddf.hyperbridge.models.theme.GlobalConfig
import com.d4viddf.hyperbridge.models.theme.HyperTheme
import com.d4viddf.hyperbridge.models.theme.ThemeMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// --- 1. STATEFUL COMPOSABLE (Logic Layer) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesignScreen(
    onNavigateToWidgets: () -> Unit,
    onNavigateToThemes: () -> Unit,
    onLaunchPicker: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val context = LocalContext.current
    val preferences = remember { AppPreferences(context.applicationContext) }
    val themeRepo = remember { ThemeRepository(context.applicationContext) }

    val activeThemeId by preferences.activeThemeIdFlow.collectAsState(initial = null)
    val savedWidgetIds by preferences.savedWidgetIdsFlow.collectAsState(initial = emptyList())

    var availableThemes by remember { mutableStateOf<List<HyperTheme>>(emptyList()) }
    var widgetIcons by remember { mutableStateOf<List<Drawable>>(emptyList()) }
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    LaunchedEffect(Unit) {
        availableThemes = themeRepo.getAvailableThemes()
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
        savedWidgetCount = savedWidgetIds.size,
        widgetIcons = widgetIcons,
        onNavigateToWidgets = onNavigateToWidgets,
        onNavigateToThemes = onNavigateToThemes,
        onFabClick = { showBottomSheet = true },
        onSettingsClick = onSettingsClick
    )

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
    widgetIcons: List<Drawable>,
    onNavigateToWidgets: () -> Unit,
    onNavigateToThemes: () -> Unit,
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
                        modifier = Modifier.size(40.dp),
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // --- 1. HERO CAROUSEL ---
            HeroSection()

            // --- 2. THEMES SECTION ---
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                SectionHeader(stringResource(R.string.design_section_themes), onNavigateToThemes)
                ThemesCarousel(
                    themes = availableThemes,
                    activeId = activeThemeId,
                    onNavigateToThemes = onNavigateToThemes
                )
            }

            // --- 3. WIDGETS SECTION ---
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                SectionHeader(stringResource(R.string.design_section_widgets), onNavigateToWidgets)
                WidgetsCarousel(
                    savedCount = savedWidgetCount,
                    icons = widgetIcons,
                    onNavigateToWidgets = onNavigateToWidgets,
                    onAddWidget = onFabClick
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// --- SECTIONS ---

@Composable
fun SectionHeader(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(8.dp))
        Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeroSection() {
    val uriHandler = LocalUriHandler.current
    val items = listOf(
        HeroItem(stringResource(R.string.design_hero_customization_title), stringResource(R.string.design_hero_customization_subtitle), Color(0xFF4CAF50),{}),
        HeroItem(stringResource(R.string.design_hero_pro_title), stringResource(R.string.design_hero_pro_subtitle), Color(0xFF2196F3),{ uriHandler.openUri("https://github.com/D4vidDf/HyperBridge/discussions/78") }),
        HeroItem(stringResource(R.string.design_hero_community_title), stringResource(R.string.design_hero_community_subtitle), Color(0xFF9C27B0),
            { uriHandler.openUri("https://github.com/D4vidDf/HyperBridge/discussions") })
    )

    val state = rememberCarouselState { items.size }

    HorizontalCenteredHeroCarousel(
        modifier = Modifier.fillMaxWidth(),
        state = state,
        itemSpacing = 8.dp,
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) { i ->
        val item = items[i]
        HeroCard(item,
            Modifier.maskClip(MaterialTheme.shapes.extraLarge))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemesCarousel(
    themes: List<HyperTheme>,
    activeId: String?,
    onNavigateToThemes: () -> Unit
) {
    val displayThemes = themes.take(5)
    val totalCount = 1 + displayThemes.size + 1

    val state = rememberCarouselState { totalCount }

    HorizontalMultiBrowseCarousel (
        modifier = Modifier.fillMaxWidth(),
        state = state,
        preferredItemWidth = 160.dp,
        itemSpacing = 8.dp,
        contentPadding = PaddingValues(horizontal = 16.dp)
    ){ i ->
        when (i) {
            0 -> {
                ThemePreviewCard(
                    title = stringResource(R.string.theme_system_default_title),
                    subtitle = stringResource(R.string.theme_system_default_desc),
                    color = MaterialTheme.colorScheme.secondary,
                    isActive = activeId == null,
                    icon = Icons.Rounded.PhoneAndroid,
                    onClick = onNavigateToThemes,
                    modifier = Modifier.maskClip(MaterialTheme.shapes.medium)
                )
            }
            in 1..displayThemes.size -> {
                val theme = displayThemes[i - 1]
                val color = try {
                    Color((theme.global.highlightColor ?: "#000000").toColorInt())
                } catch (_: Exception) { MaterialTheme.colorScheme.primary }

                ThemePreviewCard(
                    title = theme.meta.name,
                    subtitle = stringResource(R.string.theme_card_author_format, theme.meta.author),
                    color = color,
                    isActive = theme.id == activeId,
                    icon = Icons.Rounded.Palette,
                    onClick = onNavigateToThemes,
                    modifier = Modifier.maskClip(MaterialTheme.shapes.medium)
                )
            }
            else -> {
                ActionCard(
                    text = stringResource(R.string.design_browse_more),
                    icon = Icons.Rounded.Add,
                    onClick = onNavigateToThemes,
                    modifier = Modifier.maskClip(MaterialTheme.shapes.medium)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetsCarousel(
    savedCount: Int,
    icons: List<Drawable>,
    onNavigateToWidgets: () -> Unit,
    onAddWidget: () -> Unit
) {
    if (savedCount == 0) {
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            Card(
                onClick = onAddWidget,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth().height(160.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Rounded.Widgets, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.design_empty_widget_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    } else {
        val displayCount = icons.size
        val totalCount = displayCount + 1
        val state = rememberCarouselState { totalCount }

        HorizontalMultiBrowseCarousel (
            modifier = Modifier.fillMaxWidth(),
            state = state,
            preferredItemWidth = 140.dp,
            itemSpacing = 8.dp,
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) { i ->
            if (i < displayCount) {
                val icon = icons[i]
                WidgetPreviewCard(
                    label = stringResource(R.string.widget_id_fmt, i + 1),
                    icon = icon,
                    onClick = onNavigateToWidgets,
                    modifier = Modifier.maskClip(MaterialTheme.shapes.medium)
                )
            } else {
                ActionCard(
                    text = stringResource(R.string.design_add_new),
                    icon = Icons.Rounded.Add,
                    onClick = onAddWidget,
                    modifier = Modifier.maskClip(MaterialTheme.shapes.medium)
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

@Composable
fun ThemePreviewCard(
    title: String,
    subtitle: String,
    color: Color,
    isActive: Boolean,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .height(180.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = color
                )
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                            .padding(4.dp)
                    ) {
                        Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.onPrimaryContainer, CircleShape))
                    }
                }
            }

            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

@Composable
fun WidgetPreviewCard(
    label: String,
    icon: Drawable,
    onClick: () -> Unit,
    modifier: Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .height(180.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                bitmap = icon.toBitmap().asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun ActionCard(text: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier
            .height(180.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(icon, null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// --- 3. PREVIEWS ---

@SuppressLint("UseKtx")
@Preview(showBackground = true)
@Composable
private fun DesignScreenPreview() {
    // Generate Fake Data
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

    // Mock Widget Icons (Color Drawables)
    val mockIcons = listOf(
        android.graphics.Color.RED.toDrawable(),
        android.graphics.Color.BLUE.toDrawable()
    )

    MaterialTheme {
        DesignScreenContent(
            activeThemeId = "1",
            availableThemes = mockThemes,
            savedWidgetCount = 2,
            widgetIcons = mockIcons,
            onNavigateToWidgets = {},
            onNavigateToThemes = {},
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
            widgetIcons = emptyList(),
            onNavigateToWidgets = {},
            onNavigateToThemes = {},
            onFabClick = {},
            onSettingsClick = {}
        )
    }
}