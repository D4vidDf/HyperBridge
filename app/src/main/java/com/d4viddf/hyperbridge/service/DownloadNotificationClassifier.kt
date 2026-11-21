package com.d4viddf.hyperbridge.service

import com.d4viddf.hyperbridge.service.updater.SystemUpdaterClassifier

/**
 * Decides whether a notification is a download and whether it carries progress.
 *
 * There are two kinds of download evidence, and they are not worth the same:
 *  - the **package** looks like an app that downloads things (a browser, a store, an installer).
 *    That is only a hint: those apps post plenty of notifications that are not downloads.
 *  - the **notification itself** says so: a download/install channel, or a download word in its
 *    title/text.
 *
 * A notification with no progress extras and no percentage in its text only counts as progress
 * when it is ongoing *and* the notification itself says it is a download. The package hint alone
 * used to be enough, so every ongoing notification from a browser became a 0 % progress circle:
 * Chrome's "Close all Incognito tabs", and the same one in every Chromium browser (#231).
 *
 * Pure Kotlin so it can be unit-tested without an Android runtime.
 */
object DownloadNotificationClassifier {

    data class Signals(
        val packageName: String,
        val channelId: String? = null,
        val title: String = "",
        val text: String = "",
        val subText: String = "",
        val infoText: String = "",
        val progressMax: Int = 0,
        val isIndeterminate: Boolean = false,
        val isOngoing: Boolean = false,
        val textPercent: Int? = null
    )

    private val DOWNLOAD_PACKAGE_HINTS = listOf(
        "download", "downloader", "chrome", "browser", "firefox", "market", "vending",
        "play.store", "playstore", "store", "fdroid", "samsungapps", "mipicks", "venezia",
        "packageinstaller", "installer", "gms"
    )

    private val DOWNLOAD_CHANNEL_HINTS = listOf("download", "install")

    private val DOWNLOAD_KEYWORDS = listOf(
        // English
        "download", "install", "update", "updat", "upload", "transfer",
        // Spanish / Portuguese / Italian / French
        "descarg", "baix", "telecharg", "instal", "actuali", "carg", "subi", "transf",
        // German
        "laden", "gelad", "aktualis",
        // Polish
        "pobier", "pobran", "aktual",
        // Russian / Ukrainian
        "скач", "загруз", "устан", "обнов"
    )

    /** The app is one that usually downloads or installs things. A hint, not proof. */
    fun isDownloadPackage(packageName: String): Boolean {
        val pkg = packageName.lowercase()
        return pkg == SystemUpdaterClassifier.PACKAGE_NAME || DOWNLOAD_PACKAGE_HINTS.any { pkg.contains(it) }
    }

    /** The notification itself reads as a download: its channel, or a word in its content. */
    fun hasDownloadContent(signals: Signals): Boolean {
        val channelId = signals.channelId?.lowercase().orEmpty()
        if (DOWNLOAD_CHANNEL_HINTS.any { channelId.contains(it) }) return true
        val fields = listOf(signals.title, signals.text, signals.subText, signals.infoText).map { it.lowercase() }
        return DOWNLOAD_KEYWORDS.any { keyword -> fields.any { it.contains(keyword) } }
    }

    /** Picks the DOWNLOAD layout over the generic PROGRESS one. */
    fun isDownload(signals: Signals): Boolean =
        isDownloadPackage(signals.packageName) || hasDownloadContent(signals)

    fun hasProgress(signals: Signals): Boolean =
        signals.progressMax > 0 ||
            signals.isIndeterminate ||
            (signals.textPercent != null && isDownload(signals)) ||
            (signals.isOngoing && isOngoingDownload(signals))

    /**
     * An ongoing notification without any progress value. The system updater keeps its old
     * behaviour (its package is matched exactly, so it is not a guess).
     */
    private fun isOngoingDownload(signals: Signals): Boolean =
        signals.packageName.lowercase() == SystemUpdaterClassifier.PACKAGE_NAME || hasDownloadContent(signals)
}
