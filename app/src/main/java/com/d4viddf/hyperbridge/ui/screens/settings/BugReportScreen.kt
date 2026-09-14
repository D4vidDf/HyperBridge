package com.d4viddf.hyperbridge.ui.screens.settings

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.data.theme.ThemeRepository
import com.d4viddf.hyperbridge.service.diagnostics.DiagnosticsStore
import com.d4viddf.hyperbridge.ui.components.ExpressiveGroupCard
import com.d4viddf.hyperbridge.ui.components.ExpressiveSectionTitle
import com.d4viddf.hyperbridge.ui.theme.HyperBridgeTheme
import com.d4viddf.hyperbridge.util.AppConfigScope
import com.d4viddf.hyperbridge.util.BugReportCollector
import com.d4viddf.hyperbridge.util.DeviceDiagnosticInfo
import com.d4viddf.hyperbridge.util.DocumentationUrls
import com.d4viddf.hyperbridge.util.PermissionDiagnosticInfo
import com.d4viddf.hyperbridge.util.ThemeDiagnosticInfo
import com.d4viddf.hyperbridge.util.WidgetDiagnosticInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

data class AppEntry(
    val packageName: String,
    val label: String,
    val icon: Bitmap?,
    val isBridged: Boolean = false
)

@SuppressLint("UseKtx")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BugReportScreen(
    onBack: () -> Unit,
    onNavigateToDiagnostics: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val preferences = remember { AppPreferences(context) }
    val themeRepo = remember { ThemeRepository(context) }

    // Form inputs
    var userDescription by remember { mutableStateOf("") }
    var userSteps by remember { mutableStateOf("") }

    // Inclusion toggles
    var includeDevice by remember { mutableStateOf(true) }
    var includePermissions by remember { mutableStateOf(true) }
    var includeTheme by remember { mutableStateOf(true) }
    var includeWidgets by remember { mutableStateOf(true) }
    var selectedWidgetId by remember { mutableStateOf<Int?>(null) } // null = all widgets
    var appConfigScope by remember { mutableStateOf(AppConfigScope.NONE) }
    var selectedAppPackage by remember { mutableStateOf<String?>(null) }
    var includeDiagnostics by remember { mutableStateOf(true) }
    var includeLogs by remember { mutableStateOf(true) }

    // Live diagnostics store state
    val diagnosticsState by DiagnosticsStore.state.collectAsState()

    // Collected diagnostic state
    var deviceInfo by remember { mutableStateOf<DeviceDiagnosticInfo?>(null) }
    var permissionsInfo by remember { mutableStateOf<PermissionDiagnosticInfo?>(null) }
    var themeInfo by remember { mutableStateOf<ThemeDiagnosticInfo?>(null) }
    var widgetList by remember { mutableStateOf<List<WidgetDiagnosticInfo>?>(null) }
    var appConfigText by remember { mutableStateOf<String?>(null) }
    var logcatText by remember { mutableStateOf<String?>(null) }

    val bridgedPackages by preferences.allowedPackagesFlow.collectAsState(initial = emptySet())
    val savedWidgetIds by preferences.savedWidgetIdsFlow.collectAsState(initial = emptyList())

    // Load launchable & bridged apps with icons and labels
    var allAppEntries by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
    var isLoadingApps by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            isLoadingApps = true
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
            val resolveInfos = pm.queryIntentActivities(intent, 0)
            val seenPkgs = mutableSetOf<String>()
            val list = mutableListOf<AppEntry>()

            val bridged = preferences.allowedPackagesFlow.first()

            resolveInfos.forEach { r ->
                val pkg = r.activityInfo.packageName
                if (pkg != context.packageName && seenPkgs.add(pkg)) {
                    val label = r.loadLabel(pm).toString()
                    val icon = try { r.loadIcon(pm).toBitmap() } catch (_: Exception) { null }
                    list.add(AppEntry(pkg, label, icon, isBridged = bridged.contains(pkg)))
                }
            }

            // Also include any bridged packages not having a launcher activity
            bridged.forEach { pkg ->
                if (seenPkgs.add(pkg)) {
                    try {
                        val appInfo = pm.getApplicationInfo(pkg, 0)
                        val label = pm.getApplicationLabel(appInfo).toString()
                        val icon = try { pm.getApplicationIcon(appInfo).toBitmap() } catch (_: Exception) { null }
                        list.add(AppEntry(pkg, label, icon, isBridged = true))
                    } catch (_: Exception) {
                        list.add(AppEntry(pkg, pkg, null, isBridged = true))
                    }
                }
            }

            val sorted = list.sortedWith(
                compareByDescending<AppEntry> { it.isBridged }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.label }
            )

            withContext(Dispatchers.Main) {
                allAppEntries = sorted
                isLoadingApps = false
            }
        }
    }

    // Initial diagnostic collection
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val dev = BugReportCollector.collectDeviceInfo(context)
            val perm = BugReportCollector.collectPermissions(context)
            val thm = BugReportCollector.collectThemeInfo(themeRepo, preferences)
            val wgt = BugReportCollector.collectWidgetInfo(context, preferences, selectedWidgetId)
            val logs = BugReportCollector.collectLogcat(500)

            withContext(Dispatchers.Main) {
                deviceInfo = dev
                permissionsInfo = perm
                themeInfo = thm
                widgetList = wgt
                logcatText = logs
            }
        }
    }

    // Refresh app config when scope or package changes
    LaunchedEffect(appConfigScope, selectedAppPackage) {
        if (appConfigScope != AppConfigScope.NONE) {
            withContext(Dispatchers.IO) {
                val cfg = BugReportCollector.collectAppConfig(context, appConfigScope, selectedAppPackage)
                withContext(Dispatchers.Main) {
                    appConfigText = cfg
                }
            }
        } else {
            appConfigText = null
        }
    }

    // Refresh widget info when selected widget changes
    LaunchedEffect(selectedWidgetId) {
        withContext(Dispatchers.IO) {
            val wgt = BugReportCollector.collectWidgetInfo(context, preferences, selectedWidgetId)
            withContext(Dispatchers.Main) {
                widgetList = wgt
            }
        }
    }

    // Compute active markdown report
    val markdownReport = remember(
        userDescription,
        userSteps,
        includeDevice,
        deviceInfo,
        includePermissions,
        permissionsInfo,
        includeTheme,
        themeInfo,
        includeWidgets,
        widgetList,
        appConfigScope,
        selectedAppPackage,
        appConfigText,
        includeDiagnostics,
        diagnosticsState,
        includeLogs,
        logcatText
    ) {
        BugReportCollector.buildMarkdownReport(
            userDescription = userDescription,
            userSteps = userSteps,
            deviceInfo = if (includeDevice) deviceInfo else null,
            permissions = if (includePermissions) permissionsInfo else null,
            themeInfo = if (includeTheme) themeInfo else null,
            widgetList = if (includeWidgets) widgetList else null,
            appConfigScope = appConfigScope,
            targetPackage = selectedAppPackage,
            appConfigText = if (appConfigScope != AppConfigScope.NONE) appConfigText else null,
            logcatText = if (includeLogs) logcatText else null,
            diagnosticsState = if (includeDiagnostics) diagnosticsState else null
        )
    }

    val onSubmitGitHub = {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Hyper Bridge Diagnostics", markdownReport)
        clipboard.setPrimaryClip(clip)

        Toast.makeText(
            context,
            context.getString(R.string.bug_report_copied_github_toast),
            Toast.LENGTH_LONG
        ).show()

        val gitHubUrl = BugReportCollector.buildGitHubIssueUrl(
            deviceInfo = if (includeDevice) deviceInfo else null,
            userDescription = userDescription,
            userSteps = userSteps,
            permissionsInfo = if (includePermissions) permissionsInfo else null,
            themeInfo = if (includeTheme) themeInfo else null,
            widgetList = if (includeWidgets) widgetList else null,
            appConfigScope = appConfigScope,
            selectedAppPackage = selectedAppPackage,
            appConfigText = if (appConfigScope != AppConfigScope.NONE) appConfigText else null,
            logcatText = if (includeLogs) logcatText else null,
            diagnosticsState = if (includeDiagnostics) diagnosticsState else null
        )
        uriHandler.openUri(gitHubUrl)
    }

    val onSendEmail = {
        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = "mailto:".toUri()
            putExtra(Intent.EXTRA_EMAIL, arrayOf(BugReportCollector.DEVELOPER_EMAIL))
            val subjectDesc = if (userDescription.isNotBlank()) {
                userDescription.trim().take(50)
            } else {
                "Diagnostics"
            }
            putExtra(Intent.EXTRA_SUBJECT, "[Hyper Bridge Bug Report] $subjectDesc")
            putExtra(Intent.EXTRA_TEXT, markdownReport)
        }
        try {
            context.startActivity(Intent.createChooser(emailIntent, context.getString(R.string.bug_report_send_email)))
        } catch (_: Exception) {
            Toast.makeText(context, "No email client available", Toast.LENGTH_SHORT).show()
        }
    }

    val onCopyClipboard = {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Hyper Bridge Diagnostics", markdownReport)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, context.getString(R.string.bug_report_copied_toast), Toast.LENGTH_SHORT).show()
    }

    val onShare = {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.bug_report_email_subject))
            putExtra(Intent.EXTRA_TEXT, markdownReport)
        }
        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.bug_report_share)))
    }

    BugReportContent(
        userDescription = userDescription,
        onUserDescriptionChange = { userDescription = it },
        userSteps = userSteps,
        onUserStepsChange = { userSteps = it },
        includeDevice = includeDevice,
        onIncludeDeviceChange = { includeDevice = it },
        includePermissions = includePermissions,
        onIncludePermissionsChange = { includePermissions = it },
        includeTheme = includeTheme,
        onIncludeThemeChange = { includeTheme = it },
        includeWidgets = includeWidgets,
        onIncludeWidgetsChange = { includeWidgets = it },
        selectedWidgetId = selectedWidgetId,
        onSelectedWidgetIdChange = { selectedWidgetId = it },
        savedWidgetIds = savedWidgetIds,
        appConfigScope = appConfigScope,
        onAppConfigScopeChange = { appConfigScope = it },
        selectedAppPackage = selectedAppPackage,
        onSelectedAppPackageChange = { selectedAppPackage = it },
        allAppEntries = allAppEntries,
        isLoadingApps = isLoadingApps,
        includeDiagnostics = includeDiagnostics,
        onIncludeDiagnosticsChange = { includeDiagnostics = it },
        includeLogs = includeLogs,
        onIncludeLogsChange = { includeLogs = it },
        markdownReport = markdownReport,
        onSubmitGitHub = onSubmitGitHub,
        onSendEmail = onSendEmail,
        onCopyClipboard = onCopyClipboard,
        onShare = onShare,
        onBack = onBack,
        onNavigateToDiagnostics = onNavigateToDiagnostics
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BugReportContent(
    userDescription: String,
    onUserDescriptionChange: (String) -> Unit,
    userSteps: String,
    onUserStepsChange: (String) -> Unit,
    includeDevice: Boolean,
    onIncludeDeviceChange: (Boolean) -> Unit,
    includePermissions: Boolean,
    onIncludePermissionsChange: (Boolean) -> Unit,
    includeTheme: Boolean,
    onIncludeThemeChange: (Boolean) -> Unit,
    includeWidgets: Boolean,
    onIncludeWidgetsChange: (Boolean) -> Unit,
    selectedWidgetId: Int?,
    onSelectedWidgetIdChange: (Int?) -> Unit,
    savedWidgetIds: List<Int>,
    appConfigScope: AppConfigScope,
    onAppConfigScopeChange: (AppConfigScope) -> Unit,
    selectedAppPackage: String?,
    onSelectedAppPackageChange: (String?) -> Unit,
    allAppEntries: List<AppEntry>,
    isLoadingApps: Boolean,
    includeDiagnostics: Boolean,
    onIncludeDiagnosticsChange: (Boolean) -> Unit,
    includeLogs: Boolean,
    onIncludeLogsChange: (Boolean) -> Unit,
    markdownReport: String,
    onSubmitGitHub: () -> Unit,
    onSendEmail: () -> Unit,
    onCopyClipboard: () -> Unit,
    onShare: () -> Unit,
    onBack: () -> Unit,
    onNavigateToDiagnostics: (() -> Unit)? = null
) {
    val uriHandler = LocalUriHandler.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    // UI BottomSheet state
    var isPreviewExpanded by remember { mutableStateOf(false) }
    var showScopeBottomSheet by remember { mutableStateOf(false) }
    var showAppPickerBottomSheet by remember { mutableStateOf(false) }
    var showWidgetPickerDialog by remember { mutableStateOf(false) }
    var showShareWarningDialog by remember { mutableStateOf(false) }
    var pendingShareAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.bug_report_title)) },
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // --- SCREEN DESCRIPTION AT TOP ---
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
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
                    Text(
                        text = stringResource(R.string.bug_report_screen_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // --- USER REPORT INPUTS ---
            ExpressiveSectionTitle(stringResource(R.string.bug_report_details_title))
            OutlinedTextField(
                value = userDescription,
                onValueChange = onUserDescriptionChange,
                label = { Text(stringResource(R.string.bug_report_what_happened)) },
                placeholder = { Text(stringResource(R.string.bug_report_what_happened_hint)) },
                minLines = 2,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = userSteps,
                onValueChange = onUserStepsChange,
                label = { Text(stringResource(R.string.bug_report_steps)) },
                placeholder = { Text(stringResource(R.string.bug_report_steps_hint)) },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            // --- INFORMATION TO INCLUDE ---
            ExpressiveSectionTitle(stringResource(R.string.bug_report_options_title))

            ExpressiveGroupCard {
                // Device Info Toggle
                ToggleSettingRow(
                    icon = Icons.Default.Smartphone,
                    title = stringResource(R.string.bug_report_include_device),
                    subtitle = stringResource(R.string.bug_report_include_device_desc),
                    checked = includeDevice,
                    onCheckedChange = onIncludeDeviceChange
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Permissions Toggle
                ToggleSettingRow(
                    icon = Icons.Default.Security,
                    title = stringResource(R.string.bug_report_include_permissions),
                    subtitle = stringResource(R.string.bug_report_include_permissions_desc),
                    checked = includePermissions,
                    onCheckedChange = onIncludePermissionsChange
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Theme Toggle
                ToggleSettingRow(
                    icon = Icons.Default.Palette,
                    title = stringResource(R.string.bug_report_include_theme),
                    subtitle = stringResource(R.string.bug_report_include_theme_desc),
                    checked = includeTheme,
                    onCheckedChange = onIncludeThemeChange
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Widget Configuration Toggle
                ToggleSettingRow(
                    icon = Icons.Default.Widgets,
                    title = stringResource(R.string.bug_report_include_widgets),
                    subtitle = stringResource(R.string.bug_report_include_widgets_desc),
                    checked = includeWidgets,
                    onCheckedChange = onIncludeWidgetsChange
                )

                if (includeWidgets && savedWidgetIds.isNotEmpty()) {
                    Surface(
                        onClick = { showWidgetPickerDialog = true },
                        color = Color.Transparent,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (selectedWidgetId == null) {
                                    stringResource(R.string.bug_report_widget_scope_all)
                                } else {
                                    "Widget #$selectedWidgetId"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.configure),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // App Configuration Scope: Dropdown opening bottom sheet with 3 options
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.bug_report_include_app_config),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.bug_report_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Dropdown Selector Card that opens the 3-options bottom sheet
                    Card(
                        onClick = { showScopeBottomSheet = true },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = when (appConfigScope) {
                                        AppConfigScope.NONE -> stringResource(R.string.bug_report_app_scope_none)
                                        AppConfigScope.ALL_SETTINGS -> stringResource(R.string.bug_report_app_scope_all)
                                        AppConfigScope.SPECIFIC_APP -> stringResource(R.string.bug_report_app_scope_specific)
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = when (appConfigScope) {
                                        AppConfigScope.NONE -> stringResource(R.string.bug_report_app_scope_none_desc)
                                        AppConfigScope.ALL_SETTINGS -> stringResource(R.string.bug_report_app_scope_all_desc)
                                        AppConfigScope.SPECIFIC_APP -> stringResource(R.string.bug_report_app_scope_specific_desc)
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // When Specific App is selected, show card with "Select App" or the selected app instead of dropdown arrow
                    AnimatedVisibility(
                        visible = appConfigScope == AppConfigScope.SPECIFIC_APP,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        val selectedEntry = allAppEntries.find { it.packageName == selectedAppPackage }

                        Column {
                            Spacer(modifier = Modifier.height(10.dp))

                            Card(
                                onClick = { showAppPickerBottomSheet = true },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedEntry != null) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHighest
                                    }
                                ),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (selectedEntry != null) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    }
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (selectedEntry?.icon != null) {
                                        Image(
                                            bitmap = selectedEntry.icon.asImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                        )
                                        Spacer(modifier = Modifier.width(14.dp))
                                    } else if (selectedEntry != null) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                    RoundedCornerShape(10.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Android,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(14.dp))
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Apps,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(14.dp))
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        if (selectedEntry != null) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = selectedEntry.label,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (selectedEntry.isBridged) {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                    ) {
                                                        Text(
                                                            text = stringResource(R.string.bug_report_bridged),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = selectedEntry.packageName,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        } else if (selectedAppPackage != null) {
                                            Text(
                                                text = selectedAppPackage,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        } else {
                                            Text(
                                                text = stringResource(R.string.bug_report_select_app),
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = stringResource(R.string.bug_report_app_scope_specific_desc),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Diagnostics & Events Toggle
                ToggleSettingRow(
                    icon = Icons.Default.Code,
                    title = stringResource(R.string.bug_report_include_diagnostics),
                    subtitle = stringResource(R.string.bug_report_include_diagnostics_desc),
                    checked = includeDiagnostics,
                    onCheckedChange = onIncludeDiagnosticsChange
                )

                if (includeDiagnostics && onNavigateToDiagnostics != null) {
                    Surface(
                        onClick = onNavigateToDiagnostics,
                        color = Color.Transparent,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.diagnostics_title),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = stringResource(R.string.view_diagnostics),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Logcat Logs Toggle
                ToggleSettingRow(
                    icon = Icons.AutoMirrored.Filled.Article,
                    title = stringResource(R.string.bug_report_include_logs),
                    subtitle = stringResource(R.string.bug_report_include_logs_desc),
                    checked = includeLogs,
                    onCheckedChange = onIncludeLogsChange
                )
            }

            // --- REPORT PREVIEW CARD ---
            ExpressiveSectionTitle(stringResource(R.string.bug_report_preview_title))

            Surface(
                onClick = { isPreviewExpanded = !isPreviewExpanded },
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.bug_report_preview_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.bug_report_preview_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = if (isPreviewExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    AnimatedVisibility(visible = isPreviewExpanded) {
                        Column {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(12.dp))
                            SelectionContainer {
                                Text(
                                    text = markdownReport,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 350.dp)
                                        .verticalScroll(rememberScrollState())
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- ACTIONS ---
            // 1. Primary: Submit on GitHub
            Button(
                onClick = {
                    pendingShareAction = onSubmitGitHub
                    showShareWarningDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.OpenInBrowser, contentDescription = null)
                Spacer(modifier = Modifier.width(12.dp))
                Text(stringResource(R.string.bug_report_submit_github), fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Secondary: Send via Email
            OutlinedButton(
                onClick = onSendEmail,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Email, contentDescription = null)
                Spacer(modifier = Modifier.width(12.dp))
                Text(stringResource(R.string.bug_report_send_email), fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Utility buttons: Copy & Share
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilledTonalButton(
                    onClick = onCopyClipboard,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.bug_report_copy_clipboard))
                }

                FilledTonalButton(
                    onClick = {
                        pendingShareAction = onShare
                        showShareWarningDialog = true
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.bug_report_share))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // --- BOTTOM SHEET WITH THE 3 OPTIONS (None, All Apps, Specific App) ---
    if (showScopeBottomSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { showScopeBottomSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = stringResource(R.string.bug_report_include_app_config),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.bug_report_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Option 1: None
                Surface(
                    onClick = {
                        onAppConfigScopeChange(AppConfigScope.NONE)
                        showScopeBottomSheet = false
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = if (appConfigScope == AppConfigScope.NONE)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    else MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = appConfigScope == AppConfigScope.NONE,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.bug_report_app_scope_none),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.bug_report_app_scope_none_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Option 2: All Apps
                Surface(
                    onClick = {
                        onAppConfigScopeChange(AppConfigScope.ALL_SETTINGS)
                        showScopeBottomSheet = false
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = if (appConfigScope == AppConfigScope.ALL_SETTINGS)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    else MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = appConfigScope == AppConfigScope.ALL_SETTINGS,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.bug_report_app_scope_all),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.bug_report_app_scope_all_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Option 3: Specific App
                Surface(
                    onClick = {
                        onAppConfigScopeChange(AppConfigScope.SPECIFIC_APP)
                        showScopeBottomSheet = false
                        if (selectedAppPackage == null) {
                            showAppPickerBottomSheet = true
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = if (appConfigScope == AppConfigScope.SPECIFIC_APP)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    else MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = appConfigScope == AppConfigScope.SPECIFIC_APP,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.bug_report_app_scope_specific),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.bug_report_app_scope_specific_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
        }
    }

    // --- APP PICKER BOTTOM SHEET (WITH SEARCH BAR, ICONS, LABELS, PACKAGES) ---
    if (showAppPickerBottomSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var searchQuery by remember { mutableStateOf("") }

        val filteredApps = remember(allAppEntries, searchQuery) {
            if (searchQuery.isBlank()) {
                allAppEntries
            } else {
                val query = searchQuery.trim()
                allAppEntries.filter {
                    it.label.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true)
                }
            }
        }

        ModalBottomSheet(
            onDismissRequest = { showAppPickerBottomSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = stringResource(R.string.bug_report_select_app),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.search_hint)) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = null)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (isLoadingApps) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (filteredApps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_apps_found),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                    ) {
                        items(filteredApps, key = { it.packageName }) { app ->
                            val isSelected = selectedAppPackage == app.packageName && appConfigScope == AppConfigScope.SPECIFIC_APP
                            Surface(
                                onClick = {
                                    onSelectedAppPackageChange(app.packageName)
                                    onAppConfigScopeChange(AppConfigScope.SPECIFIC_APP)
                                    showAppPickerBottomSheet = false
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                else Color.Transparent,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (app.icon != null) {
                                        Image(
                                            bitmap = app.icon.asImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(10.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Smartphone, null, modifier = Modifier.size(22.dp))
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = app.label,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                            )
                                            if (app.isBridged) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = stringResource(R.string.bug_report_bridged),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        Text(
                                            text = app.packageName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    RadioButton(
                                        selected = isSelected,
                                        onClick = null
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- WIDGET SELECTION DIALOG ---
    if (showWidgetPickerDialog) {
        AlertDialog(
            onDismissRequest = { showWidgetPickerDialog = false },
            title = { Text(stringResource(R.string.bug_report_include_widgets)) },
            text = {
                Column {
                    Surface(
                        onClick = {
                            onSelectedWidgetIdChange(null)
                            showWidgetPickerDialog = false
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedWidgetId == null) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        else Color.Transparent,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedWidgetId == null,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.bug_report_widget_scope_all))
                        }
                    }

                    savedWidgetIds.forEach { id ->
                        Surface(
                            onClick = {
                                onSelectedWidgetIdChange(id)
                                showWidgetPickerDialog = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedWidgetId == id) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            else Color.Transparent,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedWidgetId == id,
                                    onClick = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Widget #$id")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showWidgetPickerDialog = false }) {
                    Text(stringResource(R.string.done))
                }
            }
        )
    }

    // --- SHARING WARNING MODAL ---
    if (showShareWarningDialog) {
        AlertDialog(
            onDismissRequest = {
                showShareWarningDialog = false
                pendingShareAction = null
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.bug_report_privacy_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.bug_report_privacy_desc, BugReportCollector.DEVELOPER_EMAIL),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = { uriHandler.openUri(DocumentationUrls.PRIVACY_POLICY) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.bug_report_view_privacy))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val action = pendingShareAction
                        showShareWarningDialog = false
                        pendingShareAction = null
                        action?.invoke()
                    }
                ) {
                    Text(stringResource(R.string.continue_action))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showShareWarningDialog = false
                        pendingShareAction = null
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun ToggleSettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        onClick = { onCheckedChange(!checked) },
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
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
                    modifier = Modifier.size(24.dp)
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
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Switch(
                checked = checked,
                onCheckedChange = null
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BugReportScreenPreview() {
    HyperBridgeTheme {
        BugReportContent(
            userDescription = "Island does not show up when playing Spotify music.",
            onUserDescriptionChange = {},
            userSteps = "1. Play music on Spotify\n2. Lock and unlock device\n3. Notice island missing",
            onUserStepsChange = {},
            includeDevice = true,
            onIncludeDeviceChange = {},
            includePermissions = true,
            onIncludePermissionsChange = {},
            includeTheme = true,
            onIncludeThemeChange = {},
            includeWidgets = true,
            onIncludeWidgetsChange = {},
            selectedWidgetId = null,
            onSelectedWidgetIdChange = {},
            savedWidgetIds = listOf(1, 2),
            appConfigScope = AppConfigScope.ALL_SETTINGS,
            onAppConfigScopeChange = {},
            selectedAppPackage = null,
            onSelectedAppPackageChange = {},
            allAppEntries = listOf(
                AppEntry("com.spotify.music", "Spotify", null, isBridged = true),
                AppEntry("org.telegram.messenger", "Telegram", null, isBridged = true),
                AppEntry("com.whatsapp", "WhatsApp", null, isBridged = false)
            ),
            isLoadingApps = false,
            includeDiagnostics = true,
            onIncludeDiagnosticsChange = {},
            includeLogs = true,
            onIncludeLogsChange = {},
            markdownReport = """
                ### Device Information
                - Device: Xiaomi 13 Pro (nuwa)
                - Android: 14 (API 34)
                - HyperOS: 1.0.8.0.UMBMIXM

                ### Diagnostics
                - Service Connected: Yes
                - Active Islands: 1
            """.trimIndent(),
            onSubmitGitHub = {},
            onSendEmail = {},
            onCopyClipboard = {},
            onShare = {},
            onBack = {},
            onNavigateToDiagnostics = {}
        )
    }
}
