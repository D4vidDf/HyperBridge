// ... imports remain the same ...
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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.util.*
import kotlinx.coroutines.launch

// ... (OnboardingScreen, WelcomePage, OnboardingPageLayout, ExplanationPage remain the same) ...
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { 6 })
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    BackHandler(enabled = pagerState.currentPage > 0) {
        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
    }

    // Permissions
    var isListenerGranted by remember { mutableStateOf(isNotificationServiceEnabled(context)) }
    var isPostGranted by remember { mutableStateOf(isPostNotificationsEnabled(context)) }

    // Compatibility
    val isXiaomi = remember { DeviceUtils.isXiaomi }
    val isCompatibleOS = remember { DeviceUtils.isCompatibleOS() }
    val canProceedCompat = isXiaomi && isCompatibleOS

    val postPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> isPostGranted = isGranted }
    )

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
                    modifier = Modifier.fillMaxWidth().padding(24.dp).navigationBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(5) { iteration ->
                            val active = (pagerState.currentPage - 1) == iteration
                            val width = if (active) 24.dp else 10.dp
                            val color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            Box(modifier = Modifier.height(10.dp).width(width).clip(CircleShape).background(color))
                        }
                    }

                    val canProceed = when (pagerState.currentPage) {
                        2 -> canProceedCompat
                        3 -> isPostGranted
                        4 -> isListenerGranted
                        else -> true
                    }
                    val isLastPage = pagerState.currentPage == 5

                    Button(
                        onClick = {
                            if (isLastPage) onFinish()
                            else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        },
                        enabled = canProceed,
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(text = stringResource(if (isLastPage) R.string.finish else R.string.next), fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(18.dp))
                    }
                }
            }
        }
    ) { padding ->
        HorizontalPager(state = pagerState, modifier = Modifier.padding(padding).fillMaxSize(), userScrollEnabled = false) { page ->
            when (page) {
                0 -> WelcomePage(onStartClick = { scope.launch { pagerState.animateScrollToPage(1) } })
                1 -> ExplanationPage()
                2 -> CompatibilityPage() // Updated
                3 -> PostPermissionPage(isGranted = isPostGranted, onRequest = { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) postPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) })
                4 -> ListenerPermissionPage(context, isListenerGranted)
                5 -> OptimizationPage(context)
            }
        }
    }
}

@Composable
fun WelcomePage(onStartClick: () -> Unit) {
    val context = LocalContext.current
    val appIconBitmap = remember(context) {
        try { context.packageManager.getApplicationIcon(context.packageName).toBitmap().asImageBitmap() } catch (e: Exception) { null }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(140.dp)) {
            if (appIconBitmap != null) {
                Image(bitmap = appIconBitmap, contentDescription = stringResource(R.string.logo_desc), modifier = Modifier.fillMaxSize())
            } else {
                Icon(imageVector = Icons.Default.Bolt, contentDescription = stringResource(R.string.logo_desc), modifier = Modifier.size(100.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(modifier = Modifier.height(48.dp))
        Text(text = stringResource(R.string.welcome_title), style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = stringResource(R.string.welcome_subtitle), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.weight(1f))
        Button(onClick = onStartClick, modifier = Modifier.fillMaxWidth().height(58.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
            Text(stringResource(R.string.get_started), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun OnboardingPageLayout(title: String, description: String, icon: ImageVector, iconColor: Color = MaterialTheme.colorScheme.primary, actionContent: @Composable BoxScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.weight(0.8f))
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(80.dp), tint = iconColor)
        Spacer(modifier = Modifier.height(40.dp))
        Text(text = title, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = description, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 24.sp)
        Spacer(modifier = Modifier.height(32.dp))
        Box(modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp), contentAlignment = Alignment.Center) { actionContent() }
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun ExplanationPage() {
    OnboardingPageLayout(title = stringResource(R.string.how_it_works), description = stringResource(R.string.how_it_works_desc), icon = Icons.Default.ToggleOn) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer), shape = RoundedCornerShape(16.dp)) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Construction, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                Spacer(modifier = Modifier.width(16.dp))
                Text(stringResource(R.string.beta_warning), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
            }
        }
    }
}

// --- UPDATED COMPATIBILITY PAGE ---
@Composable
fun CompatibilityPage() {
    val isXiaomi = DeviceUtils.isXiaomi
    val isCompatibleOS = DeviceUtils.isCompatibleOS()
    val isCN = DeviceUtils.isCNRom
    val osVersion = DeviceUtils.getHyperOSVersion()
    val deviceName = DeviceUtils.getDeviceMarketName() // e.g. "Xiaomi 14"

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
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            // Information Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Device Row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Smartphone, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(text = android.os.Build.MANUFACTURER.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Text(text = deviceName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                    Spacer(Modifier.height(12.dp))

                    // OS Row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(text = stringResource(R.string.system_version), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Text(text = osVersion, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // CN Warning
            if (isCN && isXiaomi) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.warning_cn_rom_title), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Helper
data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

// ... (PostPermissionPage, ListenerPermissionPage, OptimizationPage remain the same) ...

@Composable
fun PostPermissionPage(isGranted: Boolean, onRequest: () -> Unit) {
    OnboardingPageLayout(title = stringResource(R.string.show_island), description = stringResource(R.string.perm_post_desc), icon = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Notifications) {
        if (isGranted) {
            Text(stringResource(R.string.perm_granted), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        } else {
            FilledTonalButton(onClick = onRequest, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                Text(stringResource(R.string.allow_notifications))
            }
        }
    }
}

@Composable
fun ListenerPermissionPage(context: Context, isGranted: Boolean) {
    OnboardingPageLayout(title = stringResource(R.string.read_data), description = stringResource(R.string.perm_listener_desc), icon = if (isGranted) Icons.Default.CheckCircle else Icons.Default.NotificationsActive) {
        if (isGranted) {
            Text(stringResource(R.string.perm_granted), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        } else {
            FilledTonalButton(onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                Text(stringResource(R.string.open_settings))
            }
        }
    }
}

@Composable
fun OptimizationPage(context: Context) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.weight(0.8f))
        Icon(Icons.Default.BatteryAlert, null, Modifier.size(80.dp), tint = Color(0xFFFF9800))
        Spacer(modifier = Modifier.height(40.dp))
        Text(stringResource(R.string.keep_alive), style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Text(stringResource(R.string.optimization_desc), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedButton(onClick = { openAutoStartSettings(context) }, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) { Text(stringResource(R.string.enable_autostart)) }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(onClick = { openBatterySettings(context) }, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) { Text(stringResource(R.string.set_battery_no_restrictions)) }
        Spacer(modifier = Modifier.weight(1f))
    }
}