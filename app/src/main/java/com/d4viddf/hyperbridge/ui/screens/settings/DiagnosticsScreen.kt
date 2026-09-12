package com.d4viddf.hyperbridge.ui.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.util.Log
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.service.NotificationReaderService
import com.d4viddf.hyperbridge.service.diagnostics.DiagnosticEvent
import com.d4viddf.hyperbridge.service.diagnostics.DiagnosticsStore
import com.d4viddf.hyperbridge.ui.components.ExpressiveGroupCard
import com.d4viddf.hyperbridge.ui.components.ExpressiveSectionTitle
import com.d4viddf.hyperbridge.ui.theme.HyperBridgeTheme
import com.d4viddf.hyperbridge.util.XiaomiNotificationHelper
import com.d4viddf.hyperbridge.util.isNotificationServiceEnabled
import com.d4viddf.hyperbridge.util.isPostNotificationsEnabled
import java.text.DateFormat
import java.util.Date

data class DiagnosticsData(
    val notificationAccess: Boolean,
    val postPermission: Boolean,
    val focusSupported: Boolean,
    val focusPermission: Boolean,
    val selectedAppsCount: Int,
    val floatingReviewCount: Int,
    val activeIslands: Int,
    val lastClassification: String?,
    val lastCallState: String?,
    val serviceConnected: Boolean,
    val events: List<DiagnosticEvent>
)

@Composable
fun DiagnosticsScreen(
    onBack: () -> Unit,
    onReportError: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val preferences = remember { AppPreferences(context.applicationContext) }
    val state by DiagnosticsStore.state.collectAsState()
    val selectedApps by preferences.allowedPackagesFlow.collectAsState(initial = emptySet())
    val confirmedApps by preferences.floatingSetupConfirmedPackagesFlow.collectAsState(initial = emptySet())
    var notificationAccess by remember { mutableStateOf(isNotificationServiceEnabled(context)) }
    var postPermission by remember { mutableStateOf(isPostNotificationsEnabled(context)) }
    var focusPermission by remember { mutableStateOf(XiaomiNotificationHelper.hasFocusPermission(context)) }
    val focusSupported = remember { XiaomiNotificationHelper.isSupportIsland() }
    val diagnosticsTitle = stringResource(R.string.diagnostics_title)
    val exportHeader = stringResource(R.string.diagnostic_export_header)
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(notificationAccess) {
        if (notificationAccess && !NotificationReaderService.isConnected && !state.serviceConnected) {
            try {
                NotificationListenerService.requestRebind(
                    ComponentName(context, NotificationReaderService::class.java)
                )
            } catch (e: Exception) {
                Log.w("DiagnosticsScreen", "Failed to requestRebind", e)
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationAccess = isNotificationServiceEnabled(context)
                postPermission = isPostNotificationsEnabled(context)
                focusPermission = XiaomiNotificationHelper.hasFocusPermission(context)
                if (notificationAccess && !NotificationReaderService.isConnected && !state.serviceConnected) {
                    try {
                        NotificationListenerService.requestRebind(
                            ComponentName(context, NotificationReaderService::class.java)
                        )
                    } catch (e: Exception) {
                        Log.w("DiagnosticsScreen", "Failed to requestRebind", e)
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val isConnected = notificationAccess && (NotificationReaderService.isConnected || state.serviceConnected)

    val data = DiagnosticsData(
        notificationAccess = notificationAccess,
        postPermission = postPermission,
        focusSupported = focusSupported,
        focusPermission = focusPermission,
        selectedAppsCount = selectedApps.size,
        floatingReviewCount = (selectedApps - confirmedApps).size,
        activeIslands = state.activeIslands,
        lastClassification = state.lastClassification,
        lastCallState = state.lastCallState,
        serviceConnected = isConnected,
        events = state.events
    )

    DiagnosticsContent(
        data = data,
        onBack = onBack,
        onReportError = onReportError,
        onCopyDiagnostics = {
            val text = buildDiagnosticExport(exportHeader, state.events)
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText(diagnosticsTitle, text))
            Toast.makeText(context, context.getString(R.string.diagnostic_copied_toast), Toast.LENGTH_SHORT).show()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsContent(
    data: DiagnosticsData,
    onBack: () -> Unit,
    onReportError: (() -> Unit)? = null,
    onCopyDiagnostics: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.diagnostics_title)) },
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
                actions = {
                    if (onReportError != null) {
                        IconButton(onClick = onReportError) {
                            Icon(
                                imageVector = Icons.Default.BugReport,
                                contentDescription = stringResource(R.string.bug_report),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- 1. OVERVIEW HERO CARD ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
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
                                        color = if (data.serviceConnected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                        else MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (data.serviceConnected) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (data.serviceConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.diagnostic_service),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = stringResource(if (data.serviceConnected) R.string.connected else R.string.disconnected),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (data.serviceConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (data.serviceConnected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                            ) {
                                Text(
                                    text = stringResource(if (data.serviceConnected) R.string.connected else R.string.disconnected),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (data.serviceConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(16.dp))

                        // Quick metric pills
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DiagnosticMetricPill(
                                modifier = Modifier.weight(1f),
                                label = stringResource(R.string.diagnostic_active_islands),
                                value = data.activeIslands.toString(),
                                icon = Icons.Default.Widgets
                            )
                            DiagnosticMetricPill(
                                modifier = Modifier.weight(1f),
                                label = stringResource(R.string.diagnostic_selected_apps),
                                value = data.selectedAppsCount.toString(),
                                icon = Icons.Default.Apps
                            )
                            DiagnosticMetricPill(
                                modifier = Modifier.weight(1f),
                                label = stringResource(R.string.diagnostic_metric_review),
                                value = if (data.floatingReviewCount > 0) data.floatingReviewCount.toString() else "0",
                                icon = Icons.Default.Warning,
                                isWarning = data.floatingReviewCount > 0
                            )
                        }
                    }
                }
            }

            // --- 2. ACTION BUTTONS ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilledTonalButton(
                        onClick = onCopyDiagnostics,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.copy),
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (onReportError != null) {
                        Button(
                            onClick = onReportError,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.bug_report),
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // --- 3. SERVICE & BRIDGE STATE ---
            item {
                ExpressiveSectionTitle(stringResource(R.string.diagnostic_section_service))
                ExpressiveGroupCard {
                    ExpressiveDiagnosticRow(
                        icon = Icons.Default.CheckCircle,
                        title = stringResource(R.string.diagnostic_service),
                        subtitle = if (data.serviceConnected) stringResource(R.string.diagnostic_service_active_desc) else stringResource(R.string.diagnostic_service_disconnected_desc),
                        trailingBadge = {
                            StatusBadge(
                                text = stringResource(if (data.serviceConnected) R.string.connected else R.string.disconnected),
                                isSuccess = data.serviceConnected,
                                isWarning = !data.serviceConnected
                            )
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                    ExpressiveDiagnosticRow(
                        icon = Icons.Default.Widgets,
                        title = stringResource(R.string.diagnostic_active_islands),
                        subtitle = stringResource(R.string.diagnostic_active_islands_desc),
                        trailingBadge = {
                            ValueBadge(text = stringResource(R.string.diagnostic_active_count, data.activeIslands))
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                    ExpressiveDiagnosticRow(
                        icon = Icons.Default.Code,
                        title = stringResource(R.string.diagnostic_last_classification),
                        subtitle = stringResource(R.string.diagnostic_last_classification_desc),
                        trailingBadge = {
                            ValueBadge(text = data.lastClassification ?: "—")
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                    ExpressiveDiagnosticRow(
                        icon = Icons.Default.Phone,
                        title = stringResource(R.string.diagnostic_last_call_state),
                        subtitle = stringResource(R.string.diagnostic_last_call_state_desc),
                        trailingBadge = {
                            ValueBadge(text = data.lastCallState ?: "—")
                        }
                    )
                }
            }

            // --- 4. PERMISSIONS & HYPEROS INTEGRATION ---
            item {
                ExpressiveSectionTitle(stringResource(R.string.diagnostic_section_permissions))
                ExpressiveGroupCard {
                    ExpressiveDiagnosticRow(
                        icon = Icons.Default.NotificationsActive,
                        title = stringResource(R.string.diagnostic_notification_access),
                        subtitle = stringResource(R.string.diagnostic_notification_access_desc),
                        trailingBadge = {
                            StatusBadge(
                                text = yesNo(data.notificationAccess),
                                isSuccess = data.notificationAccess,
                                isWarning = !data.notificationAccess
                            )
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                    ExpressiveDiagnosticRow(
                        icon = Icons.Default.Notifications,
                        title = stringResource(R.string.diagnostic_post_notifications),
                        subtitle = stringResource(R.string.diagnostic_post_notifications_desc),
                        trailingBadge = {
                            StatusBadge(
                                text = yesNo(data.postPermission),
                                isSuccess = data.postPermission,
                                isWarning = !data.postPermission
                            )
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                    ExpressiveDiagnosticRow(
                        icon = Icons.Default.Smartphone,
                        title = stringResource(R.string.diagnostic_focus_support),
                        subtitle = stringResource(R.string.diagnostic_focus_support_desc),
                        trailingBadge = {
                            StatusBadge(
                                text = stringResource(if (data.focusSupported) R.string.diagnostic_supported else R.string.diagnostic_unsupported),
                                isSuccess = data.focusSupported,
                                isWarning = !data.focusSupported
                            )
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                    ExpressiveDiagnosticRow(
                        icon = Icons.Default.Security,
                        title = stringResource(R.string.diagnostic_featured_permission),
                        subtitle = stringResource(R.string.diagnostic_featured_permission_desc),
                        trailingBadge = {
                            StatusBadge(
                                text = yesNo(data.focusPermission),
                                isSuccess = data.focusPermission,
                                isWarning = !data.focusPermission
                            )
                        }
                    )
                }
            }

            // --- 5. APPS & FLOATING SETUP ---
            item {
                ExpressiveSectionTitle(stringResource(R.string.diagnostic_section_apps))
                ExpressiveGroupCard {
                    ExpressiveDiagnosticRow(
                        icon = Icons.Default.Apps,
                        title = stringResource(R.string.diagnostic_selected_apps),
                        subtitle = stringResource(R.string.diagnostic_selected_apps_desc),
                        trailingBadge = {
                            ValueBadge(text = stringResource(R.string.diagnostic_apps_count, data.selectedAppsCount))
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                    ExpressiveDiagnosticRow(
                        icon = Icons.Default.Warning,
                        title = stringResource(R.string.diagnostic_floating_review),
                        subtitle = stringResource(R.string.diagnostic_floating_review_desc),
                        trailingBadge = {
                            if (data.floatingReviewCount > 0) {
                                StatusBadge(
                                    text = stringResource(R.string.diagnostic_pending_count, data.floatingReviewCount),
                                    isSuccess = false,
                                    isWarning = true
                                )
                            } else {
                                StatusBadge(
                                    text = stringResource(R.string.diagnostic_all_clear),
                                    isSuccess = true
                                )
                            }
                        }
                    )
                }
            }

            // --- 6. RECENT EVENTS SECTION ---
            item {
                ExpressiveSectionTitle("${stringResource(R.string.diagnostic_recent_events)} (${data.events.size})")

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.diagnostic_privacy_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (data.events.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.diagnostic_no_events),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.diagnostic_no_events_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(data.events.asReversed()) { event ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Top Row: Chips (Classification & Action) + Timestamp
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val classification = event.classification
                                    val (chipBg, chipFg) = when (classification) {
                                        "MESSAGE" -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.primary
                                        "CALL" -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
                                        else -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = chipBg
                                    ) {
                                        Text(
                                            text = classification,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = chipFg,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }

                                    val action = event.action
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHighest
                                    ) {
                                        Text(
                                            text = action,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = formatEventTime(event.timestamp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Middle Row: Android package name
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Android,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = event.packageName ?: "System",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Reason (if present)
                            if (!event.reason.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = stringResource(R.string.diagnostic_reason_prefix, event.reason),
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

@Composable
private fun ExpressiveDiagnosticRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    trailingBadge: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
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
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        trailingBadge()
    }
}

@Composable
private fun StatusBadge(
    text: String,
    isSuccess: Boolean,
    isWarning: Boolean = false
) {
    val bgColor = when {
        isSuccess -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        isWarning -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
        else -> MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val textColor = when {
        isSuccess -> MaterialTheme.colorScheme.primary
        isWarning -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isSuccess) Icons.Default.CheckCircle else if (isWarning) Icons.Default.Warning else Icons.Default.Close,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

@Composable
private fun ValueBadge(
    text: String,
    color: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun DiagnosticMetricPill(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: ImageVector,
    isWarning: Boolean = false
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (isWarning) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
        else MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun yesNo(value: Boolean): String = stringResource(if (value) R.string.granted else R.string.not_granted)

private fun formatEventTime(timestamp: Long): String {
    return DateFormat.getTimeInstance(DateFormat.MEDIUM).format(Date(timestamp))
}

private fun formatEvent(event: DiagnosticEvent): String {
    val time = DateFormat.getTimeInstance(DateFormat.MEDIUM).format(Date(event.timestamp))
    return listOfNotNull(time, event.classification, event.action, event.packageName, event.reason).joinToString(" · ")
}

private fun buildDiagnosticExport(header: String, events: List<DiagnosticEvent>): String {
    return (listOf(header) + events.map(::formatEvent)).joinToString("\n")
}

@Preview(showBackground = true)
@Composable
fun DiagnosticsScreenPreview() {
    HyperBridgeTheme {
        DiagnosticsContent(
            data = DiagnosticsData(
                notificationAccess = true,
                postPermission = true,
                focusSupported = true,
                focusPermission = true,
                selectedAppsCount = 5,
                floatingReviewCount = 2,
                activeIslands = 1,
                lastClassification = "MESSAGE",
                lastCallState = "RINGING",
                serviceConnected = true,
                events = listOf(
                    DiagnosticEvent(
                        timestamp = 1700000000000L,
                        packageName = "com.whatsapp",
                        classification = "MESSAGE",
                        action = "updated",
                        reason = "active"
                    ),
                    DiagnosticEvent(
                        timestamp = 1699999900000L,
                        packageName = "org.telegram.messenger",
                        classification = "CALL",
                        action = "started",
                        reason = "incoming"
                    )
                )
            ),
            onBack = {},
            onReportError = {},
            onCopyDiagnostics = {}
        )
    }
}
