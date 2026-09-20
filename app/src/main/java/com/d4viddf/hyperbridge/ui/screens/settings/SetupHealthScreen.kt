@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.d4viddf.hyperbridge.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.ui.screens.theme.ShapeStyle
import com.d4viddf.hyperbridge.ui.screens.theme.getExpressiveShape
import com.d4viddf.hyperbridge.ui.theme.HyperBridgeTheme
import com.d4viddf.hyperbridge.util.DeviceUtils
import com.d4viddf.hyperbridge.util.XiaomiNotificationHelper
import com.d4viddf.hyperbridge.util.isNotificationServiceEnabled
import com.d4viddf.hyperbridge.util.isPostNotificationsEnabled
import com.d4viddf.hyperbridge.util.isRestrictedSettingsAllowed
import com.d4viddf.hyperbridge.util.openAutoStartSettings
import com.d4viddf.hyperbridge.util.openBatterySettings

@Composable
fun SetupHealthScreen(
    onBack: () -> Unit,
    onNavigateToBugReport: () -> Unit = {}
) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    val hasRestricted = true
    val scrollState = rememberScrollState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    // --- PERMISSION & SYSTEM STATES ---
    var isListenerGranted by remember {
        mutableStateOf(if (isPreview) true else isNotificationServiceEnabled(context))
    }
    var isPostGranted by remember {
        mutableStateOf(if (isPreview) true else isPostNotificationsEnabled(context))
    }
    var isOverlayGranted by remember {
        mutableStateOf(if (isPreview) false else try { Settings.canDrawOverlays(context) } catch (_: Throwable) { false })
    }
    var isRestrictedAllowed by remember {
        mutableStateOf(if (isPreview) true else isRestrictedSettingsAllowed(context))
    }
    var isBatteryOptimized by remember {
        mutableStateOf(if (isPreview) true else isIgnoringBatteryOptimizations(context))
    }
    var isFeaturedGranted by remember {
        mutableStateOf(if (isPreview) false else try { XiaomiNotificationHelper.hasFocusPermission(context) } catch (_: Throwable) { false })
    }
    var showAutostartDialog by remember { mutableStateOf(false) }

    if (!isPreview) {
        val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    isListenerGranted = isNotificationServiceEnabled(context)
                    isPostGranted = isPostNotificationsEnabled(context)
                    isOverlayGranted = try { Settings.canDrawOverlays(context) } catch (_: Throwable) { false }
                    isRestrictedAllowed = isRestrictedSettingsAllowed(context)
                    isBatteryOptimized = isIgnoringBatteryOptimizations(context)
                    isFeaturedGranted = try { XiaomiNotificationHelper.hasFocusPermission(context) } catch (_: Throwable) { false }
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
    }

    // --- DEVICE CHECKS ---
    val isXiaomi = if (isPreview) true else DeviceUtils.isXiaomi
    val isCompatibleOS = if (isPreview) true else DeviceUtils.isCompatibleOS()
    val osVersionString = if (isPreview) "HyperOS 2.0" else DeviceUtils.getHyperOSVersion()
    val isCN = if (isPreview) false else DeviceUtils.isCNRom
    val deviceCommercialName = remember { if (isPreview) "Xiaomi 14" else DeviceUtils.getDeviceMarketName() }
    val isSupported = if (isPreview) true else XiaomiNotificationHelper.isSupportIsland()

    val totalRequired = listOfNotNull(
        true, // Notification Listener
        true, // Show Island
        true, // Display over other apps
        if (hasRestricted) true else null, // Restricted settings
        true, // Battery optimization
        if (isXiaomi && isSupported) true else null // Xiaomi Featured
    ).count { it }

    val grantedCount = listOfNotNull(
        isListenerGranted,
        isPostGranted,
        isOverlayGranted,
        if (hasRestricted) isRestrictedAllowed else null,
        isBatteryOptimized,
        if (isXiaomi && isSupported) isFeaturedGranted else null
    ).count { it }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            MediumFlexibleTopAppBar(
                title = { Text(stringResource(R.string.system_setup), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    FilledTonalIconButton(
                        onClick = onBack,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // --- HERO STATUS BANNER ---
            SystemHealthHeroBanner(
                grantedCount = grantedCount,
                totalCount = totalRequired
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- 1. SYSTEM COMPATIBILITY ---
            HealthSectionTitle(stringResource(R.string.setup_health_title))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                StatusOptionCard(
                    title = android.os.Build.MANUFACTURER.uppercase(),
                    subtitle = deviceCommercialName,
                    isSuccess = isXiaomi,
                    icon = Icons.Default.Smartphone,
                    shape = getExpressiveShape(2, 0, ShapeStyle.Large)
                )
                StatusOptionCard(
                    title = stringResource(R.string.system_version),
                    subtitle = osVersionString,
                    isSuccess = isCompatibleOS,
                    icon = if (isCompatibleOS) Icons.Default.CheckCircle else Icons.Default.Warning,
                    shape = getExpressiveShape(2, 1, ShapeStyle.Large)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- 2. REQUIRED PERMISSIONS ---
            HealthSectionTitle(stringResource(R.string.req_permissions))
            val permissionCount = 3 + (if (hasRestricted) 1 else 0) + (if (isXiaomi && isSupported) 1 else 0)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                // 1. Notification Access
                HealthOptionCard(
                    title = stringResource(R.string.notif_access),
                    subtitle = stringResource(R.string.notif_access_desc),
                    icon = Icons.Default.NotificationsActive,
                    isGranted = isListenerGranted,
                    shape = getExpressiveShape(permissionCount, 0, ShapeStyle.Large),
                    onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
                )

                // 2. Show Island (Post Notifications)
                HealthOptionCard(
                    title = stringResource(R.string.show_island),
                    subtitle = stringResource(R.string.perm_post_desc),
                    icon = Icons.Default.Visibility,
                    isGranted = isPostGranted,
                    shape = getExpressiveShape(permissionCount, 1, ShapeStyle.Large),
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = "package:${context.packageName}".toUri()
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) { }
                    }
                )

                // 3. Display Over Other Apps (Overlay)
                HealthOptionCard(
                    title = stringResource(R.string.perm_display_title),
                    subtitle = stringResource(R.string.perm_display_onboard_desc),
                    icon = Icons.Default.Layers,
                    isGranted = isOverlayGranted,
                    shape = getExpressiveShape(permissionCount, 2, ShapeStyle.Large),
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                                data = "package:${context.packageName}".toUri()
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) { }
                    }
                )

                // 4. Restricted Permissions (Android 13+)
                if (hasRestricted) {
                    val restrictedIndex = 3
                    HealthOptionCard(
                        title = stringResource(R.string.restricted_permissions_title),
                        subtitle = stringResource(R.string.restricted_permissions_desc),
                        icon = Icons.Default.Lock,
                        isGranted = isRestrictedAllowed,
                        shape = getExpressiveShape(permissionCount, restrictedIndex, ShapeStyle.Large),
                        onClick = {
                            try {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = "package:${context.packageName}".toUri()
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) { }
                        }
                    )
                }

                // 5. Xiaomi Featured Notifications (Optional)
                if (isXiaomi && isSupported) {
                    val featuredIndex = if (hasRestricted) 4 else 3
                    HealthOptionCard(
                        title = stringResource(R.string.xiaomi_featured_notifications),
                        subtitle = stringResource(R.string.featured_notifications_open_settings),
                        icon = Icons.Default.Smartphone,
                        isGranted = isFeaturedGranted,
                        shape = getExpressiveShape(permissionCount, featuredIndex, ShapeStyle.Large),
                        onClick = {
                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                            context.startActivity(intent)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- 3. DEVICE OPTIMIZATION ---
            HealthSectionTitle(stringResource(R.string.device_optimization))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                // Autostart
                HealthOptionCard(
                    title = stringResource(R.string.xiaomi_autostart),
                    subtitle = null,
                    icon = Icons.Default.RestartAlt,
                    isGranted = false,
                    forceAction = true,
                    actionLabel = stringResource(R.string.setup_action_check),
                    shape = getExpressiveShape(2, 0, ShapeStyle.Large),
                    onClick = { openAutoStartSettings(context) },
                    onInfoClick = { showAutostartDialog = true }
                )

                // Battery Unrestricted
                HealthOptionCard(
                    title = stringResource(R.string.battery_unrestricted),
                    subtitle = stringResource(R.string.battery_desc),
                    icon = Icons.Default.BatteryAlert,
                    isGranted = isBatteryOptimized,
                    actionLabel = stringResource(R.string.setup_action_grant),
                    shape = getExpressiveShape(2, 1, ShapeStyle.Large),
                    onClick = { openBatterySettings(context) }
                )
            }

            // --- 4. CHINA ROM WARNING ---
            if (isCN && isXiaomi) {
                Spacer(Modifier.height(24.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.92f)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.warning_cn_rom_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.warning_cn_rom_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }

            // --- RECENTS NOTE ---
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.recents_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // --- 5. BUG REPORT / NEED HELP ---
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                onClick = onNavigateToBugReport,
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 72.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.bug_report_need_help),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.bug_report_entry_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }

    if (showAutostartDialog) {
        AlertDialog(
            onDismissRequest = { showAutostartDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.xiaomi_autostart),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.autostart_manual_check),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = { showAutostartDialog = false }) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        )
    }
}

// --- EXPRESSIVE HERO BANNER ---

@Composable
fun SystemHealthHeroBanner(
    grantedCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier
) {
    val isAllPassed = grantedCount >= totalCount && totalCount > 0
    val containerColor by animateColorAsState(
        targetValue = if (isAllPassed) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        label = "heroContainerColor"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(28.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = if (isAllPassed) Color(0xFF34C759).copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isAllPassed) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isAllPassed) Color(0xFF34C759) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(
                            if (isAllPassed) R.string.setup_status_ready
                            else R.string.setup_status_action_required
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(
                            if (isAllPassed) R.string.setup_status_ready_desc
                            else R.string.setup_status_action_desc
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress bar and pill count
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val progress = if (totalCount > 0) grantedCount.toFloat() / totalCount.toFloat() else 0f
                val animatedProgress by animateFloatAsState(
                    targetValue = progress,
                    animationSpec = tween(durationMillis = 500),
                    label = "heroProgress"
                )

                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(CircleShape),
                    color = if (isAllPassed) Color(0xFF34C759) else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.width(12.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isAllPassed) Color(0xFF34C759).copy(alpha = 0.12f)
                    else MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = stringResource(R.string.setup_status_progress, grantedCount, totalCount),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isAllPassed) Color(0xFF34C759)
                        else MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

// --- EXPRESSIVE SECTION COMPONENTS ---

@Composable
fun HealthSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, bottom = 8.dp)
            .semantics { heading() }
    )
}

@Composable
fun StatusOptionCard(
    title: String,
    subtitle: String,
    isSuccess: Boolean,
    icon: ImageVector,
    shape: Shape,
    modifier: Modifier = Modifier
) {
    val stateColor = if (isSuccess) Color(0xFF34C759) else MaterialTheme.colorScheme.error

    Card(
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 72.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(stateColor.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = stateColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = stateColor.copy(alpha = 0.12f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isSuccess) Icons.Default.Check else Icons.Default.Warning,
                        contentDescription = null,
                        tint = stateColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isSuccess) stringResource(R.string.setup_compatible)
                        else stringResource(R.string.warning_cn_rom_title),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = stateColor
                    )
                }
            }
        }
    }
}

@Composable
fun HealthOptionCard(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    isGranted: Boolean,
    forceAction: Boolean = false,
    actionLabel: String = stringResource(R.string.setup_action_grant),
    shape: Shape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onInfoClick: (() -> Unit)? = null
) {
    val isActionable = !isGranted || forceAction

    Card(
        onClick = onClick,
        enabled = isActionable,
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 72.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (isGranted && !forceAction) Color(0xFF34C759).copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGranted && !forceAction) Color(0xFF34C759) else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (onInfoClick != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .clickable(
                                    role = Role.Button,
                                    onClick = onInfoClick
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                val desc = if (isGranted && !forceAction) stringResource(R.string.status_active) else subtitle
                if (!desc.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isGranted && !forceAction) Color(0xFF34C759)
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            if (isGranted && !forceAction) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF34C759).copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF34C759),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.status_active),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF34C759)
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = actionLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    return try {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        pm?.isIgnoringBatteryOptimizations(context.packageName) ?: false
    } catch (_: Throwable) {
        false
    }
}

@Preview(showBackground = true, name = "1. Setup Health Screen (Light)")
@Composable
fun SetupHealthScreenPreview() {
    HyperBridgeTheme {
        SetupHealthScreen(
            onBack = {},
            onNavigateToBugReport = {}
        )
    }
}

@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    name = "2. Setup Health Screen (Dark)"
)
@Composable
fun SetupHealthScreenDarkPreview() {
    HyperBridgeTheme {
        SetupHealthScreen(
            onBack = {},
            onNavigateToBugReport = {}
        )
    }
}

@Preview(showBackground = true, name = "Hero Banner Preview")
@Composable
fun SystemHealthHeroBannerPreview() {
    HyperBridgeTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            SystemHealthHeroBanner(grantedCount = 4, totalCount = 4)
            Spacer(modifier = Modifier.height(16.dp))
            SystemHealthHeroBanner(grantedCount = 2, totalCount = 4)
        }
    }
}