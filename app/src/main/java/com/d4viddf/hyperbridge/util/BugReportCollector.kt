package com.d4viddf.hyperbridge.util

import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.data.db.AppDatabase
import com.d4viddf.hyperbridge.data.theme.ThemeRepository
import com.d4viddf.hyperbridge.data.widget.WidgetManager
import com.d4viddf.hyperbridge.models.WidgetConfig
import com.d4viddf.hyperbridge.service.NotificationReaderService
import com.d4viddf.hyperbridge.service.diagnostics.DiagnosticsState
import com.d4viddf.hyperbridge.service.diagnostics.DiagnosticsStore
import kotlinx.coroutines.flow.first
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DeviceDiagnosticInfo(
    val marketingName: String,
    val model: String,
    val manufacturer: String,
    val device: String,
    val androidVersion: String,
    val sdkInt: Int,
    val hyperOSVersion: String,
    val isCNRom: Boolean,
    val isCompatibleOS: Boolean,
    val appVersionName: String,
    val appVersionCode: Int
)

data class PermissionDiagnosticInfo(
    val restrictedSettingsAllowed: Boolean,
    val notificationListenerGranted: Boolean,
    val postNotificationsGranted: Boolean,
    val overlayPermissionGranted: Boolean,
    val batteryOptimizationIgnored: Boolean,
    val xiaomiFocusGranted: Boolean,
    val shizukuRunning: Boolean,
    val shizukuPermissionGranted: Boolean
)

data class ThemeDiagnosticInfo(
    val isDefaultTheme: Boolean,
    val themeId: String?,
    val themeTitle: String?,
    val themeAuthor: String?,
    val themeVersion: String?,
    val useNativeEngine: Boolean
)

data class WidgetDiagnosticInfo(
    val widgetId: Int,
    val providerComponent: String,
    val providerPackage: String,
    val size: String,
    val renderMode: String,
    val showInShade: Boolean,
    val autoUpdate: Boolean,
    val updateIntervalMinutes: Int
)

enum class AppConfigScope {
    NONE,
    SPECIFIC_APP,
    ALL_SETTINGS
}

object BugReportCollector {

    const val GITHUB_ISSUES_URL = "https://github.com/D4vidDf/HyperBridge/issues/new?template=bug_report.yml"
    const val DEVELOPER_EMAIL = "d4viddf@d4viddf.com"
    const val PRIVACY_POLICY_URL = "https://hyper-bridge.app/privacy/"

    fun collectDeviceInfo(context: Context): DeviceDiagnosticInfo {
        val pInfo = try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (_: Exception) {
            null
        }

        return DeviceDiagnosticInfo(
            marketingName = DeviceUtils.getDeviceMarketName(),
            model = Build.MODEL,
            manufacturer = Build.MANUFACTURER,
            device = Build.DEVICE,
            androidVersion = Build.VERSION.RELEASE,
            sdkInt = Build.VERSION.SDK_INT,
            hyperOSVersion = DeviceUtils.getHyperOSVersion(),
            isCNRom = DeviceUtils.isCNRom,
            isCompatibleOS = DeviceUtils.isCompatibleOS(),
            appVersionName = pInfo?.versionName ?: "0.6.0-dev1",
            appVersionCode = pInfo?.longVersionCode?.toInt() ?: 34
        )
    }

    fun collectPermissions(context: Context): PermissionDiagnosticInfo {
        val restrictedAllowed = isRestrictedSettingsAllowed(context)
        val listenerGranted = isNotificationServiceEnabled(context)
        val postGranted = isPostNotificationsEnabled(context)
        val overlayGranted = Settings.canDrawOverlays(context)
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val batteryIgnored = pm?.isIgnoringBatteryOptimizations(context.packageName) == true
        val focusGranted = XiaomiNotificationHelper.hasFocusPermission(context)

        var shizukuRunning = false
        var shizukuGranted = false
        try {
            shizukuRunning = Shizuku.pingBinder()
            if (shizukuRunning) {
                shizukuGranted = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            }
        } catch (_: Throwable) {
            shizukuRunning = false
            shizukuGranted = false
        }

        return PermissionDiagnosticInfo(
            restrictedSettingsAllowed = restrictedAllowed,
            notificationListenerGranted = listenerGranted,
            postNotificationsGranted = postGranted,
            overlayPermissionGranted = overlayGranted,
            batteryOptimizationIgnored = batteryIgnored,
            xiaomiFocusGranted = focusGranted,
            shizukuRunning = shizukuRunning,
            shizukuPermissionGranted = shizukuGranted
        )
    }

    private fun isRestrictedSettingsAllowed(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
            val mode = appOps?.unsafeCheckOpNoThrow(
                "android:access_restricted_settings",
                android.os.Process.myUid(),
                context.packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        } catch (_: Throwable) {
            true
        }
    }

    suspend fun collectThemeInfo(
        themeRepo: ThemeRepository,
        preferences: AppPreferences
    ): ThemeDiagnosticInfo {
        val activeTheme = themeRepo.activeTheme.value
        val nativeEngine = preferences.useNativeLiveUpdates.first()
        return if (activeTheme == null) {
            ThemeDiagnosticInfo(
                isDefaultTheme = true,
                themeId = null,
                themeTitle = "System Default",
                themeAuthor = "HyperBridge",
                themeVersion = "1.0",
                useNativeEngine = nativeEngine
            )
        } else {
            ThemeDiagnosticInfo(
                isDefaultTheme = false,
                themeId = activeTheme.id,
                themeTitle = activeTheme.meta.name,
                themeAuthor = activeTheme.meta.author,
                themeVersion = activeTheme.meta.version.toString(),
                useNativeEngine = activeTheme.global.useNativeLiveUpdates ?: nativeEngine
            )
        }
    }

    suspend fun collectWidgetInfo(
        context: Context,
        preferences: AppPreferences,
        targetWidgetId: Int? = null
    ): List<WidgetDiagnosticInfo> {
        val savedIds = preferences.savedWidgetIdsFlow.first()
        val idsToExport = if (targetWidgetId != null) {
            savedIds.filter { it == targetWidgetId }
        } else {
            savedIds
        }

        return idsToExport.map { id ->
            val info = WidgetManager.getWidgetInfo(context, id)
            val config: WidgetConfig = try {
                preferences.getWidgetConfigFlow(id).first()
            } catch (_: Exception) {
                WidgetConfig()
            }

            WidgetDiagnosticInfo(
                widgetId = id,
                providerComponent = info?.provider?.flattenToString() ?: "Unknown",
                providerPackage = info?.provider?.packageName ?: "Unknown",
                size = config.size.name,
                renderMode = config.renderMode.name,
                showInShade = config.isShowShade,
                autoUpdate = config.autoUpdate,
                updateIntervalMinutes = config.updateIntervalMinutes
            )
        }
    }

    suspend fun collectAppConfig(
        context: Context,
        scope: AppConfigScope,
        targetPackage: String? = null
    ): String {
        if (scope == AppConfigScope.NONE) return ""
        val db = AppDatabase.getDatabase(context)
        val allSettings = db.settingsDao().getAllSync()

        return when (scope) {
            AppConfigScope.NONE -> ""
            AppConfigScope.SPECIFIC_APP -> {
                if (targetPackage.isNullOrEmpty()) return ""
                val appSettings = allSettings.filter { setting ->
                    setting.key.contains(targetPackage)
                }
                if (appSettings.isEmpty()) {
                    "No custom overrides saved for $targetPackage (using global defaults)"
                } else {
                    appSettings.joinToString("\n") { "  ${it.key}: ${it.value}" }
                }
            }
            AppConfigScope.ALL_SETTINGS -> {
                allSettings.joinToString("\n") { "  ${it.key}: ${it.value}" }
            }
        }
    }

    fun collectLogcat(maxLines: Int = 500): String {
        return try {
            val process = ProcessBuilder("logcat", "-d", "-v", "time", "-t", maxLines.toString())
                .redirectErrorStream(true)
                .start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val lines = mutableListOf<String>()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                line?.let { lines.add(it) }
            }
            process.waitFor()
            if (lines.isEmpty()) {
                "No logcat entries captured."
            } else {
                lines.takeLast(maxLines).joinToString("\n")
            }
        } catch (e: Exception) {
            "Failed to capture logs: ${e.message}"
        }
    }

    fun collectDiagnosticsInfo(): DiagnosticsState {
        val baseState = DiagnosticsStore.state.value
        val isConnected = NotificationReaderService.isConnected || baseState.serviceConnected
        return if (baseState.serviceConnected != isConnected) {
            baseState.copy(serviceConnected = isConnected)
        } else {
            baseState
        }
    }

    fun buildMarkdownReport(
        userDescription: String,
        userSteps: String,
        deviceInfo: DeviceDiagnosticInfo?,
        permissions: PermissionDiagnosticInfo?,
        themeInfo: ThemeDiagnosticInfo?,
        widgetList: List<WidgetDiagnosticInfo>?,
        appConfigScope: AppConfigScope,
        targetPackage: String?,
        appConfigText: String?,
        logcatText: String?,
        diagnosticsState: DiagnosticsState? = null
    ): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val timestamp = dateFormat.format(Date())

        val sb = StringBuilder()
        sb.append("### HyperBridge Bug Report & Diagnostics\n")
        if (deviceInfo != null) {
            sb.append("**App Version:** ${deviceInfo.appVersionName} (${deviceInfo.appVersionCode})\n")
        }
        sb.append("**Generated:** $timestamp\n\n")

        // User notes
        sb.append("#### User Report\n")
        sb.append("- **Description:** ${if (userDescription.isNotBlank()) userDescription.trim() else "None provided"}\n")
        sb.append("- **Steps to Reproduce:** ${if (userSteps.isNotBlank()) userSteps.trim() else "None provided"}\n\n")

        // Device
        if (deviceInfo != null) {
            sb.append("#### Device & System Information\n")
            sb.append("- **Device Model:** ${deviceInfo.marketingName} (${deviceInfo.model})\n")
            sb.append("- **Manufacturer / Codename:** ${deviceInfo.manufacturer} / ${deviceInfo.device}\n")
            sb.append("- **HyperOS / MIUI Version:** ${deviceInfo.hyperOSVersion}\n")
            sb.append("- **Android Version:** Android ${deviceInfo.androidVersion} (API ${deviceInfo.sdkInt})\n")
            sb.append("- **ROM Region:** ${if (deviceInfo.isCNRom) "China (CN ROM)" else "Global / Non-CN"}\n")
            sb.append("- **System Compatibility:** ${if (deviceInfo.isCompatibleOS) "Compatible (HyperOS 3+)" else "Untested / Below HyperOS 3"}\n\n")
        }

        // Permissions
        if (permissions != null) {
            sb.append("#### Permissions & System Health\n")
            sb.append("- **Restricted Settings (Android 13+):** ${if (permissions.restrictedSettingsAllowed) "Allowed" else "RESTRICTED (Blocked by Android)"}\n")
            sb.append("- **Notification Listener:** ${if (permissions.notificationListenerGranted) "Granted" else "Denied"}\n")
            sb.append("- **Post Notifications:** ${if (permissions.postNotificationsGranted) "Granted" else "Denied"}\n")
            sb.append("- **Draw Over Other Apps (Overlay):** ${if (permissions.overlayPermissionGranted) "Granted" else "Denied"}\n")
            sb.append("- **Battery Optimization:** ${if (permissions.batteryOptimizationIgnored) "Unrestricted" else "Optimized (Restricted)"}\n")
            sb.append("- **Xiaomi Focus Notifications:** ${if (permissions.xiaomiFocusGranted) "Granted" else "Denied / Unsupported"}\n")
            val shizukuStatus = when {
                !permissions.shizukuRunning -> "Not Running"
                permissions.shizukuPermissionGranted -> "Running (Permission Granted)"
                else -> "Running (Permission Denied)"
            }
            sb.append("- **Shizuku Service:** $shizukuStatus\n\n")
        }

        // Theme
        if (themeInfo != null) {
            sb.append("#### Theme Information\n")
            if (themeInfo.isDefaultTheme) {
                sb.append("- **Theme:** System Default\n")
            } else {
                sb.append("- **Theme:** Custom Theme\n")
                sb.append("- **Theme ID:** ${themeInfo.themeId}\n")
                sb.append("- **Theme Title:** ${themeInfo.themeTitle}\n")
                sb.append("- **Author:** ${themeInfo.themeAuthor} | **Version:** ${themeInfo.themeVersion}\n")
            }
            sb.append("- **Engine Preference:** ${if (themeInfo.useNativeEngine) "Native Live Updates" else "Standard Engine"}\n\n")
        }

        // Widgets
        if (!widgetList.isNullOrEmpty()) {
            sb.append("#### Widget Configuration (${widgetList.size} widgets)\n")
            widgetList.forEach { w ->
                sb.append("- **Widget #${w.widgetId}:** `${w.providerComponent}`\n")
                sb.append("  Mode: ${w.renderMode}, Size: ${w.size}, Shade: ${w.showInShade}, AutoUpdate: ${w.autoUpdate} (${w.updateIntervalMinutes}m)\n")
            }
            sb.append("\n")
        }

        // App config
        if (appConfigScope != AppConfigScope.NONE && !appConfigText.isNullOrEmpty()) {
            val scopeLabel = when (appConfigScope) {
                AppConfigScope.ALL_SETTINGS -> "All Apps"
                AppConfigScope.SPECIFIC_APP -> "Specific App${if (targetPackage != null) ": $targetPackage" else ""}"
                AppConfigScope.NONE -> "None"
            }
            sb.append("#### App Configuration ($scopeLabel)\n")
            sb.append("```yaml\n$appConfigText\n```\n\n")
        }

        // Diagnostics & Sanitized Events
        if (diagnosticsState != null) {
            sb.append("#### Diagnostics & Sanitized Events\n")
            sb.append("- **Service Status:** ${if (diagnosticsState.serviceConnected) "Connected" else "Disconnected"}\n")
            sb.append("- **Active Islands:** ${diagnosticsState.activeIslands}\n")
            if (diagnosticsState.lastClassification != null) {
                sb.append("- **Last Classification:** ${diagnosticsState.lastClassification}\n")
            }
            if (diagnosticsState.lastCallState != null) {
                sb.append("- **Last Call State:** ${diagnosticsState.lastCallState}\n")
            }
            if (diagnosticsState.events.isNotEmpty()) {
                sb.append("- **Recent Events (${diagnosticsState.events.size}):**\n")
                diagnosticsState.events.takeLast(20).forEach { ev ->
                    val time = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(ev.timestamp))
                    val details = listOfNotNull(time, ev.classification, ev.action, ev.packageName, ev.reason).joinToString(" · ")
                    sb.append("  - $details\n")
                }
            }
            sb.append("\n")
        }

        // Logs
        if (!logcatText.isNullOrBlank()) {
            sb.append("#### Application Logs (Logcat)\n")
            sb.append("```text\n$logcatText\n```\n")
        }

        return sb.toString()
    }

    fun buildGitHubIssueUrl(
        deviceInfo: DeviceDiagnosticInfo? = null,
        userDescription: String = "",
        userSteps: String = "",
        permissionsInfo: PermissionDiagnosticInfo? = null,
        themeInfo: ThemeDiagnosticInfo? = null,
        widgetList: List<WidgetDiagnosticInfo>? = null,
        appConfigScope: AppConfigScope = AppConfigScope.NONE,
        selectedAppPackage: String? = null,
        appConfigText: String? = null,
        logcatText: String? = null,
        diagnosticsState: DiagnosticsState? = null
    ): String {
        val androidOption = when {
            deviceInfo == null -> "Other"
            deviceInfo.sdkInt >= 37 || deviceInfo.androidVersion.startsWith("17") -> "Android 17"
            deviceInfo.sdkInt == 36 || deviceInfo.androidVersion.startsWith("16") -> "Android 16"
            deviceInfo.sdkInt == 35 || deviceInfo.androidVersion.startsWith("15") -> "Android 15"
            else -> "Other"
        }

        val deviceStr = if (deviceInfo != null) {
            "${deviceInfo.marketingName} (${deviceInfo.model})"
        } else ""

        val osVersionStr = deviceInfo?.hyperOSVersion ?: ""

        val params = mutableListOf<Pair<String, String>>()

        val shortTitle = if (userDescription.isNotBlank()) {
            userDescription.trim().take(60)
        } else {
            "Bug report"
        }
        params.add("title" to "[BUG] $shortTitle")

        if (deviceStr.isNotBlank()) {
            params.add("device" to deviceStr)
        }
        if (osVersionStr.isNotBlank()) {
            params.add("os_version" to osVersionStr)
        }
        params.add("android_version" to androidOption)
        params.add("app_version" to "v0.6.x")

        // Build description field containing user description and diagnostics summary
        val descBuilder = StringBuilder()
        if (userDescription.isNotBlank()) {
            descBuilder.append(userDescription.trim())
        }

        val diagSummary = StringBuilder()
        if (permissionsInfo != null) {
            diagSummary.append("\n\n**Permissions & System Health:**\n")
            diagSummary.append("- Restricted Settings (Android 13+): ${if (permissionsInfo.restrictedSettingsAllowed) "Allowed" else "RESTRICTED (Blocked)"}\n")
            diagSummary.append("- Notification Listener: ${if (permissionsInfo.notificationListenerGranted) "Granted" else "Denied"}\n")
            diagSummary.append("- Overlay: ${if (permissionsInfo.overlayPermissionGranted) "Granted" else "Denied"}\n")
            diagSummary.append("- Battery Unrestricted: ${if (permissionsInfo.batteryOptimizationIgnored) "Yes" else "No"}\n")
            diagSummary.append("- Notification Focus: ${if (permissionsInfo.xiaomiFocusGranted) "Granted" else "Denied"}\n")
            if (permissionsInfo.shizukuRunning) {
                diagSummary.append("- Shizuku: Running (${if (permissionsInfo.shizukuPermissionGranted) "Granted" else "Denied"})\n")
            }
        }

        if (themeInfo != null) {
            diagSummary.append("\n**Theme & Engine:**\n")
            val themeName = if (themeInfo.isDefaultTheme) "Default" else (themeInfo.themeTitle ?: "Custom")
            val engine = if (themeInfo.useNativeEngine) "HyperIsland" else "TransparentOverlay"
            diagSummary.append("- Theme: $themeName | Engine: $engine\n")
        }

        if (!widgetList.isNullOrEmpty()) {
            diagSummary.append("\n**Active Widgets (${widgetList.size}):**\n")
            val widgetSummary = widgetList.take(3).joinToString("; ") { "#${it.widgetId} ${it.providerPackage} (${it.size}, ${it.renderMode})" }
            diagSummary.append("- $widgetSummary\n")
        }

        if (appConfigScope != AppConfigScope.NONE && !appConfigText.isNullOrBlank()) {
            val scopeLabel = if (appConfigScope == AppConfigScope.SPECIFIC_APP) "Specific App: $selectedAppPackage" else "All Apps"
            diagSummary.append("\n**App Config ($scopeLabel):**\n```yaml\n${appConfigText.take(400)}\n```\n")
        }

        if (diagnosticsState != null) {
            diagSummary.append("\n**Diagnostics & Island State:**\n")
            diagSummary.append("- Service: ${if (diagnosticsState.serviceConnected) "Connected" else "Disconnected"} | Active Islands: ${diagnosticsState.activeIslands}\n")
            if (diagnosticsState.lastClassification != null) {
                diagSummary.append("- Last Classification: ${diagnosticsState.lastClassification}\n")
            }
            if (diagnosticsState.events.isNotEmpty()) {
                val recentEventsStr = diagnosticsState.events.takeLast(5).joinToString("\n") { ev ->
                    val time = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(ev.timestamp))
                    "  - " + listOfNotNull(time, ev.classification, ev.action, ev.packageName, ev.reason).joinToString(" · ")
                }
                diagSummary.append("- Recent Events:\n$recentEventsStr\n")
            }
        }

        val finalDesc = (descBuilder.toString() + diagSummary.toString()).trim()
        if (finalDesc.isNotBlank()) {
            params.add("description" to finalDesc.take(1500))
        }

        // Steps to reproduce
        val stepsStr = if (userSteps.isNotBlank()) {
            userSteps.trim()
        } else {
            "1. Open Hyper Bridge\n2. \n3. "
        }
        params.add("steps" to stepsStr)

        // Logs excerpt
        if (!logcatText.isNullOrBlank()) {
            params.add("logs" to logcatText.takeLast(1200))
        }

        val queryString = params.joinToString("&") { (k, v) ->
            java.net.URLEncoder.encode(k, "UTF-8").replace("+", "%20") + "=" +
                java.net.URLEncoder.encode(v, "UTF-8").replace("+", "%20")
        }

        return if (queryString.isNotBlank()) {
            "$GITHUB_ISSUES_URL&$queryString"
        } else {
            GITHUB_ISSUES_URL
        }
    }
}
