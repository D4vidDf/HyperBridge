package com.d4viddf.hyperbridge

import com.d4viddf.hyperbridge.util.AppConfigScope
import com.d4viddf.hyperbridge.util.BugReportCollector
import com.d4viddf.hyperbridge.util.DeviceDiagnosticInfo
import com.d4viddf.hyperbridge.util.PermissionDiagnosticInfo
import com.d4viddf.hyperbridge.util.ThemeDiagnosticInfo
import com.d4viddf.hyperbridge.util.WidgetDiagnosticInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URLDecoder

class BugReportCollectorTest {

    @Test
    fun testBuildGitHubIssueUrlWithFullInfo() {
        val deviceInfo = DeviceDiagnosticInfo(
            marketingName = "Xiaomi 14 Ultra",
            model = "24030PN60G",
            manufacturer = "Xiaomi",
            device = "aurora",
            androidVersion = "15",
            sdkInt = 35,
            hyperOSVersion = "OS2.0.1.0.VNAMIXM",
            isCNRom = false,
            isCompatibleOS = true,
            appVersionName = "0.6.0-dev1",
            appVersionCode = 34
        )

        val permissions = PermissionDiagnosticInfo(
            restrictedSettingsAllowed = true,
            notificationListenerGranted = true,
            postNotificationsGranted = true,
            overlayPermissionGranted = true,
            batteryOptimizationIgnored = true,
            xiaomiFocusGranted = true,
            shizukuRunning = false,
            shizukuPermissionGranted = false
        )

        val url = BugReportCollector.buildGitHubIssueUrl(
            deviceInfo = deviceInfo,
            userDescription = "Notification island does not show when receiving a message.",
            userSteps = "1. Receive message\n2. Look at top of screen",
            permissionsInfo = permissions,
            logcatText = "E/HyperBridge: crash log line"
        )

        assertTrue(url.startsWith(BugReportCollector.GITHUB_ISSUES_URL))
        assertTrue(url.contains("template=bug_report.yml"))
        assertTrue(url.contains("android_version=Android%2015") || url.contains("android_version=Android+15"))
        assertTrue(url.contains("app_version=v0.6.x"))

        // Decode query and verify contents
        val decoded = URLDecoder.decode(url, "UTF-8")
        assertTrue(decoded.contains("[BUG] Notification island does not show when receiving a message."))
        assertTrue(decoded.contains("Xiaomi 14 Ultra (24030PN60G)"))
        assertTrue(decoded.contains("OS2.0.1.0.VNAMIXM"))
        assertTrue(decoded.contains("Notification island does not show when receiving a message."))
        assertTrue(decoded.contains("1. Receive message"))
        assertTrue(decoded.contains("Restricted Settings (Android 13+): Allowed"))
        assertTrue(decoded.contains("E/HyperBridge: crash log line"))
    }

    @Test
    fun testBuildGitHubIssueUrlWithMinimalInfo() {
        val url = BugReportCollector.buildGitHubIssueUrl(
            deviceInfo = null,
            userDescription = ""
        )

        assertTrue(url.startsWith(BugReportCollector.GITHUB_ISSUES_URL))
        assertTrue(url.contains("template=bug_report.yml"))
        assertTrue(url.contains("android_version=Other"))
        assertTrue(url.contains("app_version=v0.6.x"))
        assertTrue(url.contains("title=%5BBUG%5D%20Bug%20report") || url.contains("title=%5BBUG%5D+Bug+report"))
    }

    @Test
    fun testTemplateDefinedOnBaseUrls() {
        assertEquals("https://github.com/D4vidDf/HyperBridge/issues/new?template=bug_report.yml", BugReportCollector.GITHUB_ISSUES_URL)
        assertEquals("https://github.com/D4vidDf/HyperBridge/issues/new?template=bug_report.yml", com.d4viddf.hyperbridge.util.DocumentationUrls.GITHUB_BUG_REPORT)
    }

    @Test
    fun testBuildMarkdownReportFull() {
        val deviceInfo = DeviceDiagnosticInfo(
            marketingName = "Redmi Note 13 Pro",
            model = "2312DRA50G",
            manufacturer = "Xiaomi",
            device = "garnet",
            androidVersion = "14",
            sdkInt = 34,
            hyperOSVersion = "OS1.0.9.0.UNRMIXM",
            isCNRom = false,
            isCompatibleOS = true,
            appVersionName = "0.6.0-dev1",
            appVersionCode = 34
        )

        val permissions = PermissionDiagnosticInfo(
            restrictedSettingsAllowed = true,
            notificationListenerGranted = true,
            postNotificationsGranted = true,
            overlayPermissionGranted = true,
            batteryOptimizationIgnored = true,
            xiaomiFocusGranted = true,
            shizukuRunning = false,
            shizukuPermissionGranted = false
        )

        val themeInfo = ThemeDiagnosticInfo(
            isDefaultTheme = false,
            themeId = "neon_island_v2",
            themeTitle = "Neon Island",
            themeAuthor = "CommunityArtist",
            themeVersion = "2",
            useNativeEngine = true
        )

        val widgets = listOf(
            WidgetDiagnosticInfo(
                widgetId = 42,
                providerComponent = "com.spotify.music/.WidgetProvider",
                providerPackage = "com.spotify.music",
                size = "MEDIUM",
                renderMode = "HYBRID",
                showInShade = false,
                autoUpdate = true,
                updateIntervalMinutes = 15
            )
        )

        val report = BugReportCollector.buildMarkdownReport(
            userDescription = "Island freezes on screen lock",
            userSteps = "1. Play music\n2. Lock phone",
            deviceInfo = deviceInfo,
            permissions = permissions,
            themeInfo = themeInfo,
            widgetList = widgets,
            appConfigScope = AppConfigScope.SPECIFIC_APP,
            targetPackage = "com.spotify.music",
            appConfigText = "spotify_enabled: true\nspotify_theme: custom",
            logcatText = "2026-09-11 12:00:00.000 D/HyperBridge: Island created"
        )

        assertTrue(report.contains("### HyperBridge Bug Report & Diagnostics"))
        assertTrue(report.contains("**App Version:** 0.6.0-dev1 (34)"))
        assertTrue(report.contains("#### User Report"))
        assertTrue(report.contains("Island freezes on screen lock"))
        assertTrue(report.contains("1. Play music"))
        assertTrue(report.contains("#### Device & System Information"))
        assertTrue(report.contains("Redmi Note 13 Pro (2312DRA50G)"))
        assertTrue(report.contains("OS1.0.9.0.UNRMIXM"))
        assertTrue(report.contains("#### Permissions & System Health"))
        assertTrue(report.contains("**Restricted Settings (Android 13+):** Allowed"))
        assertTrue(report.contains("**Notification Listener:** Granted"))
        assertTrue(report.contains("#### Theme Information"))
        assertTrue(report.contains("Neon Island"))
        assertTrue(report.contains("CommunityArtist"))
        assertTrue(report.contains("Native Live Updates"))
        assertTrue(report.contains("#### Widget Configuration (1 widgets)"))
        assertTrue(report.contains("Widget #42"))
        assertTrue(report.contains("com.spotify.music/.WidgetProvider"))
        assertTrue(report.contains("#### App Configuration (Specific App: com.spotify.music)"))
        assertTrue(report.contains("spotify_enabled: true"))
        assertTrue(report.contains("#### Application Logs (Logcat)"))
        assertTrue(report.contains("D/HyperBridge: Island created"))
    }

    @Test
    fun testBuildMarkdownReportAllAppsScope() {
        val report = BugReportCollector.buildMarkdownReport(
            userDescription = "Test all apps",
            userSteps = "",
            deviceInfo = null,
            permissions = null,
            themeInfo = null,
            widgetList = null,
            appConfigScope = AppConfigScope.ALL_SETTINGS,
            targetPackage = null,
            appConfigText = "global_mode: true",
            logcatText = null
        )

        assertTrue(report.contains("#### App Configuration (All Apps)"))
        assertTrue(report.contains("global_mode: true"))
    }

    @Test
    fun testBuildMarkdownReportRestrictedSettingsBlocked() {
        val permissions = PermissionDiagnosticInfo(
            restrictedSettingsAllowed = false,
            notificationListenerGranted = false,
            postNotificationsGranted = true,
            overlayPermissionGranted = false,
            batteryOptimizationIgnored = false,
            xiaomiFocusGranted = false,
            shizukuRunning = true,
            shizukuPermissionGranted = true
        )

        val report = BugReportCollector.buildMarkdownReport(
            userDescription = "",
            userSteps = "",
            deviceInfo = null,
            permissions = permissions,
            themeInfo = null,
            widgetList = null,
            appConfigScope = AppConfigScope.NONE,
            targetPackage = null,
            appConfigText = null,
            logcatText = null
        )

        assertTrue(report.contains("**Restricted Settings (Android 13+):** RESTRICTED (Blocked by Android)"))
        assertTrue(report.contains("**Notification Listener:** Denied"))
        assertTrue(report.contains("**Shizuku Service:** Running (Permission Granted)"))
        assertFalse(report.contains("#### Device & System Information"))
        assertFalse(report.contains("#### Theme Information"))
        assertFalse(report.contains("#### Widget Configuration"))
        assertFalse(report.contains("#### Application Logs (Logcat)"))
    }

    @Test
    fun testBuildMarkdownReportAndGitHubUrlWithDiagnosticsState() {
        val diagnostics = com.d4viddf.hyperbridge.service.diagnostics.DiagnosticsState(
            serviceConnected = true,
            activeIslands = 2,
            lastClassification = "MESSAGE",
            lastCallState = "RINGING",
            events = listOf(
                com.d4viddf.hyperbridge.service.diagnostics.DiagnosticEvent(
                    timestamp = 1700000000000L,
                    packageName = "com.whatsapp",
                    classification = "MESSAGE",
                    action = "updated",
                    reason = "active"
                )
            )
        )

        val report = BugReportCollector.buildMarkdownReport(
            userDescription = "Test issue",
            userSteps = "Steps",
            deviceInfo = null,
            permissions = null,
            themeInfo = null,
            widgetList = null,
            appConfigScope = AppConfigScope.NONE,
            targetPackage = null,
            appConfigText = null,
            logcatText = null,
            diagnosticsState = diagnostics
        )

        assertTrue(report.contains("#### Diagnostics & Sanitized Events"))
        assertTrue(report.contains("**Service Status:** Connected"))
        assertTrue(report.contains("**Active Islands:** 2"))
        assertTrue(report.contains("**Last Classification:** MESSAGE"))
        assertTrue(report.contains("**Last Call State:** RINGING"))
        assertTrue(report.contains("com.whatsapp"))

        val url = BugReportCollector.buildGitHubIssueUrl(
            userDescription = "Test issue",
            diagnosticsState = diagnostics
        )

        val decoded = java.net.URLDecoder.decode(url, "UTF-8")
        assertTrue(decoded.contains("Service: Connected"))
        assertTrue(decoded.contains("Active Islands: 2"))
        assertTrue(decoded.contains("Last Classification: MESSAGE"))
        assertTrue(decoded.contains("com.whatsapp"))
    }
}
