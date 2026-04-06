package com.d4viddf.hyperbridge.ui.screens.onboarding

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LowPriority
import androidx.compose.material.icons.filled.NotificationAdd
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.d4viddf.hyperbridge.BuildConfig
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.models.IslandConfig
import com.d4viddf.hyperbridge.models.IslandLimitMode
import com.d4viddf.hyperbridge.models.NotificationType
import com.d4viddf.hyperbridge.ui.components.EngineOptionCard
import com.d4viddf.hyperbridge.ui.components.EnginePreview
import com.d4viddf.hyperbridge.ui.components.formatSeconds
import com.d4viddf.hyperbridge.ui.components.timeoutSteps
import com.d4viddf.hyperbridge.ui.theme.HyperBridgeTheme
import com.d4viddf.hyperbridge.util.DeviceUtils
import com.d4viddf.hyperbridge.util.isNotificationServiceEnabled
import com.d4viddf.hyperbridge.util.isPostNotificationsEnabled
import com.d4viddf.hyperbridge.util.toBitmap
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    // 11 Pages
    val pagerState = rememberPagerState(pageCount = { 13 })
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }

    // Handle Hardware Back Button
    BackHandler(enabled = pagerState.currentPage > 1) {
        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
    }

    // --- Permissions State ---
    var isListenerGranted by remember { mutableStateOf(isNotificationServiceEnabled(context)) }
    var isPostGranted by remember { mutableStateOf(isPostNotificationsEnabled(context)) }

    // --- Compatibility Logic ---
    val isXiaomi = remember { DeviceUtils.isXiaomi }
    val isCompatibleOS = remember { DeviceUtils.isCompatibleOS() }
    val canProceedCompat = isXiaomi && isCompatibleOS

    // --- Permission Launcher ---
    val postPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> isPostGranted = isGranted }
    )

    // --- Lifecycle Observer ---
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isListenerGranted = isNotificationServiceEnabled(context)
                isPostGranted = isPostNotificationsEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val currentPage = pagerState.currentPage
                val isLastPage = currentPage == 13

                if (currentPage == 0) {
                    Button(
                        onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(
                            stringResource(R.string.get_started),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    val canProceed = when (currentPage) {
                        1 -> canProceedCompat || BuildConfig.DEBUG
                        2 -> isPostGranted
                        3 -> isListenerGranted
                        else -> true
                    }

                    if (currentPage > 1) {
                        OutlinedButton(
                            onClick = {
                                scope.launch { pagerState.animateScrollToPage(currentPage - 1) }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(0.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.back),
                                style = MaterialTheme.typography.labelLarge,
                                fontSize = 16.sp
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    Button(
                        onClick = {
                            if (isLastPage) onFinish()
                            else scope.launch { pagerState.animateScrollToPage(currentPage + 1) }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        enabled = canProceed,
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(
                            text = stringResource(if (isLastPage) R.string.finish else R.string.next),
                            style = MaterialTheme.typography.labelLarge,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            userScrollEnabled = false
        ) { page ->
            when (page) {
                0 -> WelcomePage()
                1 -> CompatibilityPage()
                2 -> PostPermissionPage(
                    isGranted = isPostGranted,
                    onRequest = {
                        postPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                )
                3 -> ListenerPermissionPage(context, isListenerGranted)
                4 -> OptimizationPage(context)
                5 -> ExplanationPage()
                6 -> PrivacyPage()
                7 -> CustomizationPage()
                8 -> TriggersConfigPage(prefs)
                9 -> EngineConfigPage(prefs)
                10 -> PriorityEducationPage(prefs)
                11 -> BehaviorConfigPage(prefs)
                12 -> AutoHideConfigPage(prefs)
            }
        }
    }
}

// ==========================================
//              PAGE COMPOSABLES
// ==========================================

@Composable
fun WelcomePage() {
    val context = LocalContext.current
    val appIconBitmap = remember(context) {
        try { context.packageManager.getApplicationIcon(context.packageName).toBitmap().asImageBitmap() } catch (_: Exception) { null }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))

        if (appIconBitmap != null) {
            Image(
                bitmap = appIconBitmap,
                contentDescription = stringResource(R.string.logo_desc),
                modifier = Modifier.size(140.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Default.Bolt,
                contentDescription = stringResource(R.string.logo_desc),
                modifier = Modifier.size(140.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = stringResource(R.string.welcome_title),
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.welcome_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun ExplanationPage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.5f))

        Text(
            text = stringResource(R.string.how_it_works),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(24.dp))

        EnginePreview(isNative = false)

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.how_it_works_desc),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            shape = RoundedCornerShape(24.dp)
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Construction, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    stringResource(R.string.beta_warning),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TriggersConfigPage(prefs: AppPreferences) {
    val activeTypes by prefs.globalNotificationTypesFlow.collectAsState(initial = emptySet())
    val scope = rememberCoroutineScope()
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    OnboardingPageLayout(
        title = stringResource(R.string.active_triggers),
        description = stringResource(R.string.active_triggers_global_desc),
        icon = Icons.Default.NotificationAdd,
        iconColor = MaterialTheme.colorScheme.primary
    ) {
        Button(
            onClick = { showSheet = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Settings, null)
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.configure), style = MaterialTheme.typography.bodyLarge)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.onboarding_can_be_changed),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        if (showSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSheet = false },
                sheetState = sheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        stringResource(R.string.active_triggers),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    NotificationType.entries.forEach { type ->
                        val isEnabled = activeTypes.contains(type.name)
                        Card(
                            onClick = {
                                scope.launch { prefs.updateGlobalNotificationType(type, !isEnabled) }
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isEnabled) MaterialTheme.colorScheme.primaryContainer.copy(0.3f) else MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = isEnabled, onCheckedChange = null)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(stringResource(type.labelRes), fontWeight = FontWeight.Bold)
                                    val descRes = when (type) {
                                        NotificationType.STANDARD -> R.string.type_standard_desc
                                        NotificationType.PROGRESS -> R.string.type_progress_desc
                                        NotificationType.MEDIA -> R.string.type_media_desc
                                        NotificationType.NAVIGATION -> R.string.type_nav_desc
                                        NotificationType.CALL -> R.string.type_call_desc
                                        NotificationType.TIMER -> R.string.type_timer_desc
                                    }
                                    Text(
                                        stringResource(descRes),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriorityEducationPage(prefs: AppPreferences) {
    val currentMode by prefs.limitModeFlow.collectAsState(initial = IslandLimitMode.MOST_RECENT)
    val scope = rememberCoroutineScope()
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    OnboardingPageLayout(
        title = stringResource(R.string.priority_edu_title),
        description = stringResource(R.string.priority_edu_desc),
        icon = Icons.Default.LowPriority,
        iconColor = MaterialTheme.colorScheme.primary
    ) {
        Button(
            onClick = { showSheet = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Settings, null)
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.configure), style = MaterialTheme.typography.bodyLarge)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.onboarding_can_be_changed),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        if (showSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSheet = false },
                sheetState = sheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        stringResource(R.string.island_behavior),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    IslandLimitMode.entries.forEach { mode ->
                        BehaviorOptionCard(
                            mode = mode,
                            isSelected = currentMode == mode,
                            onClick = {
                                scope.launch {
                                    prefs.setLimitMode(mode)
                                    prefs.setPriorityEduShown(true)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BehaviorOptionCard(
    mode: IslandLimitMode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, label = "border"
    )
    val containerColor = if (isSelected)
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    else
        MaterialTheme.colorScheme.surfaceContainerLow

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = if (isSelected) BorderStroke(2.dp, borderColor) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = isSelected, onClick = null)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    stringResource(mode.titleRes),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    stringResource(mode.descRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun BehaviorConfigPage(prefs: AppPreferences) {
    val config by prefs.globalConfigFlow.collectAsState(initial = IslandConfig())
    val scope = rememberCoroutineScope()

    OnboardingPageLayout(
        title = stringResource(R.string.island_behavior),
        description = stringResource(R.string.behavior_desc_glob_config),
        icon = Icons.Default.Settings,
        iconColor = MaterialTheme.colorScheme.primary
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            ListOptionCard(
                title = stringResource(R.string.remove_original_notification),
                subtitle = stringResource(R.string.remove_original_notification_desc),
                icon = Icons.Default.Notifications,
                shape = RoundedCornerShape(24.dp, 24.dp, 4.dp, 4.dp),
                onClick = {
                    scope.launch {
                        prefs.updateGlobalConfig(config.copy(removeOriginalNotification = !(config.removeOriginalNotification ?: false)))
                    }
                },
                trailingContent = {
                    Checkbox(checked = config.removeOriginalNotification ?: false, onCheckedChange = null)
                }
            )

            ListOptionCard(
                title = stringResource(R.string.config_shade_title),
                subtitle = stringResource(R.string.config_shade_desc),
                icon = Icons.Default.Info,
                shape = RoundedCornerShape(4.dp, 4.dp, 24.dp, 24.dp),
                onClick = {
                    scope.launch {
                        prefs.updateGlobalConfig(config.copy(isShowShade = !(config.isShowShade ?: true)))
                    }
                },
                trailingContent = {
                    Checkbox(checked = config.isShowShade ?: true, onCheckedChange = null)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.onboarding_can_be_changed),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
fun AutoHideConfigPage(prefs: AppPreferences) {
    val config by prefs.globalConfigFlow.collectAsState(initial = IslandConfig())
    val scope = rememberCoroutineScope()
    val isOverridden = config.isFloat != null

    // Timeout is "Enabled" if it's > 0
    val currentTimeout = config.timeout ?: 10
    val isTimeoutEnabled = currentTimeout > 0

    OnboardingPageLayout(
        title = stringResource(R.string.island_behavior),
        description = stringResource(R.string.behavior_desc_hide_long),
        icon = Icons.Rounded.Timer,
        iconColor = MaterialTheme.colorScheme.primary
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                // Header with Switch
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccessTime, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(20.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.auto_hide_island), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                        Text(
                            text = if (isTimeoutEnabled) stringResource(R.string.hides_after_a_set_time) else stringResource(
                                R.string.behavior_hide_desc
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isTimeoutEnabled,
                        onCheckedChange = { enabled ->
                            // When changing this, we also ensure isFloat is set to track the override
                            val newTimeout = if (enabled) 5 else 0
                            val currentIsFloat = config.isFloat ?:  true
                            scope.launch { prefs.updateGlobalConfig(config.copy(timeout = newTimeout, isFloat = currentIsFloat)) }
                        }
                    )
                }

                // Expandable Slider Section
                AnimatedVisibility(
                    visible = isTimeoutEnabled,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column {
                        Spacer(Modifier.height(16.dp))

                        // Time Display Label
                        Text(
                            text = formatSeconds(currentTimeout),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )

                        // Slider mapping to our steps list
                        val currentIndex = timeoutSteps.indexOf(currentTimeout).coerceAtLeast(0).toFloat()

                        Slider(
                            value = currentIndex,
                            onValueChange = { index ->
                                val selectedSeconds = timeoutSteps[index.toInt()]
                                val currentIsFloat = config.isFloat ?: true
                                scope.launch { prefs.updateGlobalConfig(config.copy(timeout = selectedSeconds, isFloat = currentIsFloat))}
                            },
                            valueRange = 0f..(timeoutSteps.size - 1).toFloat(),
                            steps = timeoutSteps.size - 2
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CustomizationPage() {
    OnboardingPageLayout(
        title = stringResource(R.string.design),
        description = stringResource(R.string.onboard_design_desc),
        icon = Icons.Default.Palette,
        iconColor = MaterialTheme.colorScheme.secondary
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            ListOptionCard(
                title = stringResource(R.string.design_section_themes),
                subtitle = stringResource(R.string.onboard_theme_desc),
                icon = Icons.Default.Palette,
                shape = RoundedCornerShape(24.dp, 24.dp, 4.dp, 4.dp),
                onClick = {},
                trailingContent = {}
            )
            ListOptionCard(
                title = stringResource(R.string.design_section_widgets),
                subtitle = stringResource(R.string.onboarding_widget_desc),
                icon = Icons.Default.Architecture,
                shape = RoundedCornerShape(4.dp, 4.dp, 24.dp, 24.dp),
                onClick = {},
                trailingContent = {}
            )
        }
    }
}

// Shared Layout
@Composable
fun OnboardingPageLayout(
    title: String,
    description: String,
    icon: ImageVector? = null,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.5f))

        // Expressive Icon Container
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(iconColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = iconColor
                )
            }
            Spacer(modifier = Modifier.height(40.dp))
        }

        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Content Area
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun EngineConfigPage(prefs: AppPreferences) {
    val useNative by prefs.useNativeLiveUpdates.collectAsState(initial = false)
    val scope = rememberCoroutineScope()

    OnboardingPageLayout(
        title = stringResource(R.string.engine_config_title),
        description = stringResource(R.string.engine_desc)
    ) {
        EnginePreview(isNative = useNative)

        Spacer(modifier = Modifier.height(24.dp))

        EngineOptionCard(
            title = stringResource(R.string.engine_xiaomi_title),
            description = stringResource(R.string.engine_xiaomi_desc),
            isSelected = !useNative,
            onClick = { scope.launch { prefs.setUseNativeLiveUpdates(false) } }
        )

        Spacer(modifier = Modifier.height(12.dp))

        EngineOptionCard(
            title = stringResource(R.string.engine_native_title),
            description = stringResource(R.string.engine_native_desc),
            isSelected = useNative,
            onClick = { scope.launch { prefs.setUseNativeLiveUpdates(true) } }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.onboarding_can_be_changed),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
fun PrivacyPage() {
    OnboardingPageLayout(
        title = stringResource(R.string.privacy_title),
        description = stringResource(R.string.privacy_desc),
        icon = Icons.Default.Security,
        iconColor = MaterialTheme.colorScheme.primary
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.WifiOff, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(stringResource(R.string.privacy_card_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.privacy_card_desc), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun CompatibilityPage() {
    val isXiaomi = DeviceUtils.isXiaomi
    val isCompatibleOS = DeviceUtils.isCompatibleOS()
    val isCN = DeviceUtils.isCNRom
    val osVersion = DeviceUtils.getHyperOSVersion()
    val deviceName = DeviceUtils.getDeviceMarketName()

    val (icon, color, titleRes, descRes) = when {
        !isXiaomi -> Quad(Icons.Rounded.Devices, MaterialTheme.colorScheme.error, R.string.unsupported_device, R.string.req_xiaomi)
        !isCompatibleOS -> Quad(Icons.Rounded.Devices, MaterialTheme.colorScheme.error, R.string.unsupported_device, R.string.req_hyperos)
        else -> Quad(Icons.Rounded.Devices, Color(0xFF34C759), R.string.device_compatible, R.string.compatible_msg)
    }

    OnboardingPageLayout(
        title = stringResource(titleRes),
        description = stringResource(descRes),
        icon = icon,
        iconColor = color
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            ListOptionCard(
                title = Build.MANUFACTURER.uppercase(),
                subtitle = deviceName,
                icon = Icons.Default.Smartphone,
                shape = RoundedCornerShape(24.dp, 24.dp, 4.dp, 4.dp),
                onClick = {},
                trailingContent = {}
            )
            ListOptionCard(
                title = stringResource(R.string.system_version),
                subtitle = osVersion,
                icon = Icons.Default.Info,
                shape = RoundedCornerShape(4.dp, 4.dp, 24.dp, 24.dp),
                onClick = {},
                trailingContent = {}
            )

            if (isCN && isXiaomi) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            stringResource(R.string.warning_cn_rom_title),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ListOptionCard(
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
            .heightIn(min = 88.dp)
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
            }
        }
    }
}

// Helper
data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

// --- 5. POST PERMISSION PAGE ---
@Composable
fun PostPermissionPage(isGranted: Boolean, onRequest: () -> Unit) {
    OnboardingPageLayout(
        title = stringResource(R.string.show_island),
        description = stringResource(R.string.perm_post_desc),
        icon = Icons.Rounded.Notifications,
        iconColor = MaterialTheme.colorScheme.primary
    ) {
        // FIX: Standard Button, Disabled when granted, Text changes, No Icon
        Button(
            onClick = { if (!isGranted) onRequest() },
            enabled = !isGranted,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
            )
        ) {
            Text(
                stringResource(if (isGranted) R.string.perm_granted else R.string.allow_notifications),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

// --- 6. LISTENER PERMISSION PAGE ---
@Composable
fun ListenerPermissionPage(context: Context, isGranted: Boolean) {
    OnboardingPageLayout(
        title = stringResource(R.string.read_data),
        description = stringResource(R.string.perm_listener_desc),
        icon = Icons.Default.NotificationsActive,
        iconColor = MaterialTheme.colorScheme.secondary
    ) {
        // FIX: Standard Button, Disabled when granted, Text changes, No Icon
        Button(
            onClick = {
                if (!isGranted) {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }
            },
            enabled = !isGranted,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
            )
        ) {
            Text(
                stringResource(if (isGranted) R.string.perm_granted else R.string.open_settings),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

// --- 7. OPTIMIZATION PAGE ---
@SuppressLint("BatteryLife")
@Composable
fun OptimizationPage(context: Context) {
    OnboardingPageLayout(
        title = stringResource(R.string.optimization_title),
        description = stringResource(R.string.optimization_desc),
        icon = Icons.Default.BatteryStd,
        iconColor = MaterialTheme.colorScheme.secondary
    ) {
        Button(
            onClick = {
                try {
                    val intent = Intent()
                    intent.component = android.content.ComponentName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.autostart.AutoStartManagementActivity"
                    )
                    context.startActivity(intent)
                } catch (_: Exception) { }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(stringResource(R.string.enable_autostart), style = MaterialTheme.typography.bodyLarge)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = "package:${context.packageName}".toUri()
                    }
                    context.startActivity(intent)
                } catch (_: Exception) { }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(stringResource(R.string.set_battery_no_restrictions), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OnboardingScreenPreview() {
    HyperBridgeTheme {
        OnboardingScreen(onFinish = {})
    }
}

@Preview(showBackground = true)
@Composable
fun WelcomePagePreview() {
    HyperBridgeTheme {
        WelcomePage()
    }
}

@Preview(showBackground = true)
@Composable
fun ExplanationPagePreview() {
    HyperBridgeTheme {
        ExplanationPage()
    }
}

@Preview(showBackground = true)
@Composable
fun PrivacyPagePreview() {
    HyperBridgeTheme {
        PrivacyPage()
    }
}

@Preview(showBackground = true)
@Composable
fun CompatibilityPagePreview() {
    HyperBridgeTheme {
        CompatibilityPage()
    }
}

@Preview(showBackground = true)
@Composable
fun PostPermissionPagePreview() {
    HyperBridgeTheme {
        PostPermissionPage(isGranted = false, onRequest = {})
    }
}

@Preview(showBackground = true)
@Composable
fun ListenerPermissionPagePreview() {
    HyperBridgeTheme {
        ListenerPermissionPage(context = LocalContext.current, isGranted = false)
    }
}

@Preview(showBackground = true)
@Composable
fun TriggersConfigPagePreview() {
    HyperBridgeTheme {
        TriggersConfigPage(prefs = AppPreferences(LocalContext.current))
    }
}

@Preview(showBackground = true)
@Composable
fun PriorityEducationPagePreview() {
    HyperBridgeTheme {
        PriorityEducationPage(prefs = AppPreferences(LocalContext.current))
    }
}

@Preview(showBackground = true)
@Composable
fun BehaviorConfigPagePreview() {
    HyperBridgeTheme {
        BehaviorConfigPage(prefs = AppPreferences(LocalContext.current))
    }
}

@Preview(showBackground = true)
@Composable
fun CustomizationPagePreview() {
    HyperBridgeTheme {
        CustomizationPage()
    }
}

@Preview(showBackground = true)
@Composable
fun EngineConfigPagePreview() {
    HyperBridgeTheme {
        EngineConfigPage(prefs = AppPreferences(LocalContext.current))
    }
}

@Preview(showBackground = true)
@Composable
fun OptimizationPagePreview() {
    HyperBridgeTheme {
        OptimizationPage(context = LocalContext.current)
    }
}

@Preview(showBackground = true)
@Composable
fun AutoHidePagePreview(){
    HyperBridgeTheme {
        AutoHideConfigPage(prefs = AppPreferences(LocalContext.current))
    }
}