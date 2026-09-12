package com.d4viddf.hyperbridge.ui.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.service.diagnostics.DiagnosticEvent
import com.d4viddf.hyperbridge.service.diagnostics.DiagnosticsStore
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

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationAccess = isNotificationServiceEnabled(context)
                postPermission = isPostNotificationsEnabled(context)
                focusPermission = XiaomiNotificationHelper.hasFocusPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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
        serviceConnected = state.serviceConnected,
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
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.diagnostics_title)) },
                navigationIcon = {
                    FilledTonalIconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    if (onReportError != null) {
                        IconButton(onClick = onReportError) {
                            Icon(
                                imageVector = Icons.Default.BugReport,
                                contentDescription = stringResource(R.string.bug_report_entry_title)
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DiagnosticRow(stringResource(R.string.diagnostic_notification_access), yesNo(data.notificationAccess))
                        DiagnosticRow(stringResource(R.string.diagnostic_post_notifications), yesNo(data.postPermission))
                        DiagnosticRow(stringResource(R.string.diagnostic_focus_support), yesNo(data.focusSupported))
                        DiagnosticRow(stringResource(R.string.diagnostic_featured_permission), yesNo(data.focusPermission))
                        DiagnosticRow(stringResource(R.string.diagnostic_selected_apps), data.selectedAppsCount.toString())
                        DiagnosticRow(
                            stringResource(R.string.diagnostic_floating_review),
                            data.floatingReviewCount.toString()
                        )
                        DiagnosticRow(stringResource(R.string.diagnostic_active_islands), data.activeIslands.toString())
                        DiagnosticRow(stringResource(R.string.diagnostic_last_classification), data.lastClassification ?: "—")
                        DiagnosticRow(stringResource(R.string.diagnostic_last_call_state), data.lastCallState ?: "—")
                        DiagnosticRow(
                            stringResource(R.string.diagnostic_service),
                            stringResource(if (data.serviceConnected) R.string.connected else R.string.disconnected)
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = onCopyDiagnostics,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.copy_sanitized_diagnostics))
                }
            }

            if (onReportError != null) {
                item {
                    OutlinedButton(
                        onClick = onReportError,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.BugReport, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.bug_report_entry_title))
                    }
                }
            }

            item {
                Text(stringResource(R.string.diagnostic_recent_events), fontWeight = FontWeight.Bold)
                Text(
                    stringResource(R.string.diagnostic_privacy_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(data.events.asReversed()) { event ->
                Card(Modifier.fillMaxWidth()) {
                    Text(
                        formatEvent(event),
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun DiagnosticRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun yesNo(value: Boolean): String = stringResource(if (value) R.string.granted else R.string.not_granted)

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
