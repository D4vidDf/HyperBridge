package com.d4viddf.hyperbridge.ui.screens.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.util.DeviceUtils
import com.d4viddf.hyperbridge.util.isNotificationServiceEnabled
import com.d4viddf.hyperbridge.util.isPostNotificationsEnabled
import com.d4viddf.hyperbridge.util.toBitmap
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    // 7 Pages
    val pagerState = rememberPagerState(pageCount = { 7 })
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Handle Hardware Back Button
    BackHandler(enabled = pagerState.currentPage > 0) {
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
            if (pagerState.currentPage > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pagination Dots
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(6) { iteration ->
                            val active = (pagerState.currentPage - 1) == iteration
                            val width = if (active) 32.dp else 10.dp
                            val color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            Box(
                                modifier = Modifier
                                    .height(10.dp)
                                    .width(width)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                        }
                    }

                    // Blocking Logic
                    val canProceed = when (pagerState.currentPage) {
                        3 -> canProceedCompat
                        4 -> isPostGranted
                        5 -> isListenerGranted
                        else -> true
                    }
                    val isLastPage = pagerState.currentPage == 6

                    Button(
                        onClick = {
                            if (isLastPage) onFinish()
                            else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        },
                        enabled = canProceed,
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(
                            text = stringResource(if (isLastPage) R.string.finish else R.string.next),
                            style = MaterialTheme.typography.labelLarge,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(18.dp))
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
                0 -> WelcomePage(onStartClick = { scope.launch { pagerState.animateScrollToPage(1) } })
                1 -> ExplanationPage()
                2 -> PrivacyPage()
                3 -> CompatibilityPage()
                4 -> PostPermissionPage(
                    isGranted = isPostGranted,
                    onRequest = {
                        postPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                )
                5 -> ListenerPermissionPage(context, isListenerGranted)
                6 -> OptimizationPage(context)
            }
        }
    }
}

// ==========================================
//              PAGE COMPOSABLES
// ==========================================

@Composable
fun WelcomePage(onStartClick: () -> Unit) {
    val context = LocalContext.current
    val appIconBitmap = remember(context) {
        try { context.packageManager.getApplicationIcon(context.packageName).toBitmap().asImageBitmap() } catch (e: Exception) { null }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))

        // Icon (No Box Container)
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

        Button(
            onClick = onStartClick,
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
        Spacer(modifier = Modifier.height(16.dp))
    }
}

// Shared Layout
@Composable
fun OnboardingPageLayout(
    title: String,
    description: String,
    icon: ImageVector,
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
fun ExplanationPage() {
    OnboardingPageLayout(
        title = stringResource(R.string.how_it_works),
        description = stringResource(R.string.how_it_works_desc),
        icon = Icons.Default.Architecture,
        iconColor = MaterialTheme.colorScheme.tertiary
    ) {
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
        !isXiaomi -> Quad(Icons.Default.Cancel, MaterialTheme.colorScheme.error, R.string.unsupported_device, R.string.req_xiaomi)
        !isCompatibleOS -> Quad(Icons.Default.Cancel, MaterialTheme.colorScheme.error, R.string.unsupported_device, R.string.req_hyperos)
        else -> Quad(Icons.Default.CheckCircle, Color(0xFF34C759), R.string.device_compatible, R.string.compatible_msg)
    }

    OnboardingPageLayout(
        title = stringResource(titleRes),
        description = stringResource(descRes),
        icon = icon,
        iconColor = color
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Smartphone, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(text = Build.MANUFACTURER.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text(text = deviceName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(text = stringResource(R.string.system_version), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text(text = osVersion, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (isCN && isXiaomi) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), shape = RoundedCornerShape(24.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.warning_cn_rom_title), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
                }
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
        icon = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Notifications,
        iconColor = if (isGranted) Color(0xFF34C759) else MaterialTheme.colorScheme.primary
    ) {
        // FIX: Standard Button, Disabled when granted, Text changes, No Icon
        Button(
            onClick = { if (!isGranted) onRequest() },
            enabled = !isGranted,
            modifier = Modifier.fillMaxWidth().height(56.dp),
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
        icon = if (isGranted) Icons.Default.CheckCircle else Icons.Default.NotificationsActive,
        iconColor = if (isGranted) Color(0xFF34C759) else MaterialTheme.colorScheme.secondary
    ) {
        // FIX: Standard Button, Disabled when granted, Text changes, No Icon
        Button(
            onClick = {
                if (!isGranted) {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }
            },
            enabled = !isGranted,
            modifier = Modifier.fillMaxWidth().height(56.dp),
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
@Composable
fun OptimizationPage(context: Context) {
    OnboardingPageLayout(
        title = stringResource(R.string.optimization_title),
        description = stringResource(R.string.optimization_desc),
        icon = Icons.Default.BatteryStd,
        iconColor = Color(0xFFFF9800)
    ) {
        OutlinedButton(
            onClick = {
                try {
                    val intent = Intent()
                    intent.component = android.content.ComponentName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.autostart.AutoStartManagementActivity"
                    )
                    context.startActivity(intent)
                } catch (e: Exception) { }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(stringResource(R.string.enable_autostart), style = MaterialTheme.typography.bodyLarge)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = "package:${context.packageName}".toUri()
                    }
                    context.startActivity(intent)
                } catch (e: Exception) { }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(stringResource(R.string.no_restrictions), style = MaterialTheme.typography.bodyLarge)
        }
    }
}