package com.d4viddf.hyperbridge.service

import com.d4viddf.hyperbridge.service.DownloadNotificationClassifier.Signals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadNotificationClassifierTest {

    @Test
    fun chromeIncognitoNotificationIsNotProgress() {
        // #231: ongoing, no progress extras, browser package.
        val incognito = Signals(
            packageName = "com.android.chrome",
            channelId = "incognito",
            title = "Close all Incognito tabs",
            isOngoing = true
        )
        assertFalse(DownloadNotificationClassifier.hasProgress(incognito))
    }

    @Test
    fun otherChromiumBrowsersAreNotProgressEither() {
        val quetta = Signals(
            packageName = "net.quetta.browser",
            channelId = "incognito",
            title = "Close all Incognito tabs",
            isOngoing = true
        )
        assertFalse(DownloadNotificationClassifier.hasProgress(quetta))
    }

    @Test
    fun ongoingNotificationFromAStoreOrGmsIsNotProgressByPackageAlone() {
        assertFalse(
            DownloadNotificationClassifier.hasProgress(
                Signals(packageName = "com.google.android.gms", channelId = "status", title = "Connected to car", isOngoing = true)
            )
        )
    }

    @Test
    fun ongoingBrowserDownloadWithoutProgressExtrasIsStillProgress() {
        val byChannel = Signals(
            packageName = "com.android.chrome",
            channelId = "downloads",
            title = "file.zip",
            text = "Pending",
            isOngoing = true
        )
        val byText = Signals(
            packageName = "com.android.chrome",
            channelId = "browser",
            title = "Downloading file.zip",
            isOngoing = true
        )
        assertTrue(DownloadNotificationClassifier.hasProgress(byChannel))
        assertTrue(DownloadNotificationClassifier.hasProgress(byText))
    }

    @Test
    fun progressExtrasAlwaysCount() {
        assertTrue(DownloadNotificationClassifier.hasProgress(Signals(packageName = "com.example.app", progressMax = 100)))
        assertTrue(DownloadNotificationClassifier.hasProgress(Signals(packageName = "com.example.app", isIndeterminate = true)))
    }

    @Test
    fun textPercentageCountsForDownloadAppsOnly() {
        assertTrue(
            DownloadNotificationClassifier.hasProgress(
                Signals(packageName = "com.android.vending", title = "Some game", text = "42%", textPercent = 42)
            )
        )
        assertFalse(
            DownloadNotificationClassifier.hasProgress(
                Signals(packageName = "com.example.battery", title = "Battery", text = "42%", textPercent = 42)
            )
        )
    }

    @Test
    fun ongoingSystemUpdaterKeepsItsProgress() {
        assertTrue(
            DownloadNotificationClassifier.hasProgress(
                Signals(packageName = "com.android.updater", channelId = "ota", title = "HyperOS", isOngoing = true)
            )
        )
    }

    @Test
    fun plainOngoingNotificationIsNotProgress() {
        assertFalse(
            DownloadNotificationClassifier.hasProgress(
                Signals(packageName = "com.example.vpn", title = "Connected", isOngoing = true)
            )
        )
    }

    @Test
    fun downloadLayoutStillFollowsPackageOrContent() {
        assertTrue(DownloadNotificationClassifier.isDownload(Signals(packageName = "com.android.chrome")))
        assertTrue(DownloadNotificationClassifier.isDownload(Signals(packageName = "com.example.app", title = "Descargando mapa")))
        assertTrue(DownloadNotificationClassifier.isDownload(Signals(packageName = "com.example.app", channelId = "app_install")))
        assertFalse(DownloadNotificationClassifier.isDownload(Signals(packageName = "com.example.app", title = "Hello")))
    }
}
