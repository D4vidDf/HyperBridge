package com.d4viddf.hyperbridge.service.vpn

import java.util.Locale
import kotlin.math.roundToLong
import com.d4viddf.hyperbridge.service.vpn.providers.KnownVpnProviderAdapters

data class VpnNotificationActionSignal(
    val index: Int,
    val title: String,
    val semanticAction: Int,
    val hasPendingIntent: Boolean,
    val iconResourceName: String? = null
)
data class VpnNotificationSignals(
    val packageName: String, val appLabel: String, val title: String, val text: String, val subText: String,
    val isOngoing: Boolean, val isForegroundService: Boolean, val usesChronometer: Boolean,
    val whenMillis: Long, val actions: List<VpnNotificationActionSignal>, val category: String?,
    val hasNativeXiaomiPayload: Boolean,
    val localizedStateLabels: Map<String, VpnConnectionState> = emptyMap(),
    val localizedDestinationTemplates: List<String> = emptyList(),
    val hasLargeIcon: Boolean = false,
    val largeIconResourceName: String? = null
)
data class VpnNotificationEvidence(
    val provider: VpnProviderIdentity, val destination: VpnDestination?, val traffic: VpnTrafficSnapshot?,
    val connectedAtMillis: Long?, val timestampSource: VpnTimestampSource,
    val providerState: VpnConnectionState?, val disconnectActionIndex: Int?, val hasNativeXiaomiPayload: Boolean,
    val useLargeIconAsCountryFlag: Boolean
)

class VpnNotificationAnalyzer {
    fun analyze(signals: VpnNotificationSignals, vpnProviderPackages: Set<String>, observedAtMillis: Long): VpnNotificationEvidence? {
        if (signals.packageName !in vpnProviderPackages || (!signals.isOngoing && !signals.isForegroundService)) return null
        val lines = listOf(signals.title, signals.text, signals.subText).filter(String::isNotBlank)
        val adapter = KnownVpnProviderAdapters.forPackage(signals.packageName)
        val adapterResult = adapter?.analyzeNotification(signals)
        val timestampTrusted = signals.usesChronometer || adapter?.capabilities?.notificationTimestamp == true
        val providerState = adapterResult?.state ?: resolveState(lines, signals.localizedStateLabels)
        val elapsedStart = resolveElapsedStart(lines, providerState, observedAtMillis)
        val connectedAt = elapsedStart
            ?: signals.whenMillis.takeIf { timestampTrusted && it in 1 until observedAtMillis }
        val destination = resolveDestination(lines, signals.localizedDestinationTemplates)
            ?: signals.text.takeIf {
                adapter?.trustsContentAsConnectedDestination == true &&
                    providerState == VpnConnectionState.CONNECTED && isHumanReadableDestination(it)
            }?.let { destination(it.trim(), 90, VpnEvidenceSource.PROVIDER_ADAPTER) }
        val traffic = resolveTraffic(
            lines.joinToString(" "),
            observedAtMillis,
            adapter?.capabilities?.notificationTraffic == true
        )
        return VpnNotificationEvidence(
            provider = VpnProviderIdentity(
                signals.packageName,
                signals.appLabel,
                if (adapter != null) 95 else 80,
                if (adapter != null) VpnEvidenceSource.PROVIDER_ADAPTER else VpnEvidenceSource.STRUCTURED_NOTIFICATION
            ),
            destination = destination,
            traffic = traffic,
            connectedAtMillis = connectedAt,
            timestampSource = when {
                connectedAt == null -> VpnTimestampSource.UNKNOWN
                elapsedStart != null -> VpnTimestampSource.PROVIDER_NOTIFICATION_ELAPSED
                signals.usesChronometer -> VpnTimestampSource.NOTIFICATION_CHRONOMETER
                else -> VpnTimestampSource.PROVIDER_NOTIFICATION_TIMESTAMP
            },
            providerState = providerState,
            disconnectActionIndex = adapterResult?.disconnectActionIndex,
            hasNativeXiaomiPayload = signals.hasNativeXiaomiPayload,
            useLargeIconAsCountryFlag = shouldUseLargeIconAsCountryFlag(signals, destination)
        )
    }

    private fun resolveState(lines: List<String>, localizedLabels: Map<String, VpnConnectionState>): VpnConnectionState? {
        lines.forEach { line ->
            localizedLabels.entries.firstOrNull { it.key.equals(line.trim(), ignoreCase = true) }?.let { return it.value }
        }
        val text = lines.joinToString(" ").lowercase(Locale.ROOT)
        return when {
            "disconnecting" in text -> VpnConnectionState.DISCONNECTING
            "reconnecting" in text || "re-connecting" in text -> VpnConnectionState.RECONNECTING
            "waiting for network" in text || "device offline" in text -> VpnConnectionState.WAITING_FOR_NETWORK
            "paused" in text -> VpnConnectionState.PAUSED
            "blocking" in text || "internet blocked" in text -> VpnConnectionState.BLOCKING
            "critical error" in text || "vpn error" in text -> VpnConnectionState.ERROR
            "connecting" in text -> VpnConnectionState.CONNECTING
            "connected" in text || "vpn active" in text -> VpnConnectionState.CONNECTED
            else -> null
        }
    }

    private fun resolveDestination(lines: List<String>, localizedTemplates: List<String>): VpnDestination? {
        lines.forEach { line ->
            localizedTemplates.firstNotNullOfOrNull { template -> extractFormatArgument(template, line.trim()) }
                ?.takeIf(::isHumanReadableDestination)
                ?.let { return destination(it, 90, VpnEvidenceSource.PROVIDER_ADAPTER) }
        }
        lines.forEach { line ->
            val match = DESTINATION_PATTERN.matchEntire(line.trim()) ?: return@forEach
            val value = match.groupValues[1].trim().trimEnd('.', '!', ',')
            if (isHumanReadableDestination(value)) return destination(value, 80, VpnEvidenceSource.NOTIFICATION_TEXT)
        }
        lines.forEach { line ->
            val withoutDuration = DURATION_AT_END.replace(line.trim(), "").trim()
            val match = CONNECTED_SUFFIX_PATTERN.matchEntire(withoutDuration) ?: return@forEach
            val value = match.groupValues[1].trim().trimEnd('.', '!', ',')
            if (isHumanReadableDestination(value)) return destination(value, 80, VpnEvidenceSource.NOTIFICATION_TEXT)
        }
        return null
    }

    private fun resolveElapsedStart(
        lines: List<String>,
        providerState: VpnConnectionState?,
        observedAtMillis: Long
    ): Long? {
        if (providerState != VpnConnectionState.CONNECTED && providerState != VpnConnectionState.RECONNECTING) return null
        val match = lines.firstNotNullOfOrNull { DURATION_AT_END.find(it.trim()) } ?: return null
        val first = match.groupValues[1].toLongOrNull() ?: return null
        val second = match.groupValues[2].toLongOrNull() ?: return null
        val third = match.groupValues[3].toLongOrNull()
        val elapsedSeconds = if (third == null) {
            if (second >= 60) return null
            first * 60L + second
        } else {
            if (second >= 60 || third >= 60) return null
            first * 3_600L + second * 60L + third
        }
        val elapsedMillis = elapsedSeconds * 1_000L
        return (observedAtMillis - elapsedMillis).takeIf { elapsedSeconds > 0 && it > 0 }
    }

    private fun destination(label: String, confidence: Int, source: VpnEvidenceSource) = VpnDestination(
        label = label,
        confidence = confidence,
        source = source,
        countryCode = VpnCountryResolver.resolve(label)?.isoCode
    )

    private fun shouldUseLargeIconAsCountryFlag(
        signals: VpnNotificationSignals,
        destination: VpnDestination?
    ): Boolean {
        if (!signals.hasLargeIcon || destination?.countryCode == null) return false
        val resourceName = signals.largeIconResourceName?.lowercase(Locale.ROOT) ?: return true
        if (NON_FLAG_ICON_TOKENS.any(resourceName::contains)) return false
        val countryCode = destination.countryCode.lowercase(Locale.ROOT)
        return FLAG_ICON_TOKENS.any(resourceName::contains) || countryCode in resourceName.split('_', '-')
    }

    private fun extractFormatArgument(template: String, rendered: String): String? {
        val marker = listOf("%1\$s", "%s").firstOrNull { it in template } ?: return null
        val prefix = template.substringBefore(marker)
        val suffix = template.substringAfter(marker)
        if (!rendered.startsWith(prefix, ignoreCase = true) || !rendered.endsWith(suffix, ignoreCase = true)) return null
        return rendered.substring(prefix.length, rendered.length - suffix.length).trim().takeIf(String::isNotBlank)
    }

    private fun isHumanReadableDestination(value: String): Boolean = value.length in 2..48 &&
        value.count { it == '.' } < 2 && !IP_ADDRESS.matches(value) &&
        !value.equals("vpn", ignoreCase = true) && !value.contains("http", ignoreCase = true) &&
        value.none { it == '\n' || it == '\r' } && value.any(Char::isLetter)

    private fun resolveTraffic(text: String, now: Long, hasVerifiedProviderLayout: Boolean): VpnTrafficSnapshot? {
        val down = (if (hasVerifiedProviderLayout) {
            DOWN_TOTAL_AND_RATE_PATTERN.find(text)?.let(::toBytesPerSecond)
        } else null) ?: DOWN_PATTERN.find(text)?.let(::toBytesPerSecond)
        val up = (if (hasVerifiedProviderLayout) {
            UP_TOTAL_AND_RATE_PATTERN.find(text)?.let(::toBytesPerSecond)
        } else null) ?: UP_PATTERN.find(text)?.let(::toBytesPerSecond)
        if ((down ?: 0L) <= 0L && (up ?: 0L) <= 0L) return null
        return VpnTrafficSnapshot(down, up, now, VpnEvidenceSource.NOTIFICATION_TEXT)
    }

    private fun toBytesPerSecond(match: MatchResult): Long? {
        val value = match.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
        val multiplier = when (match.groupValues[2].uppercase(Locale.ROOT)) {
            "B" -> 1L; "KB" -> 1_000L; "MB" -> 1_000_000L; "GB" -> 1_000_000_000L; else -> return null
        }
        return (value * multiplier).roundToLong().coerceAtLeast(0L)
    }

    companion object {
        private val DESTINATION_PATTERN = Regex("^(?:connected\\s+to|server|location)\\s*[:\\-]?\\s+(.+)$", RegexOption.IGNORE_CASE)
        private val CONNECTED_SUFFIX_PATTERN = Regex("^(.+?)\\s+(?:is\\s+)?connected$", RegexOption.IGNORE_CASE)
        private val DURATION_AT_END = Regex("(?:^|\\s)(\\d{1,3}):(\\d{2})(?::(\\d{2}))?\\s*$")
        private val IP_ADDRESS = Regex("^\\d{1,3}(?:\\.\\d{1,3}){3}(?::\\d+)?$")
        private val DOWN_PATTERN = Regex("(?:↓|download|down)\\s*[:\\-]?\\s*(\\d+(?:[.,]\\d+)?)\\s*(B|KB|MB|GB)/s", RegexOption.IGNORE_CASE)
        private val UP_PATTERN = Regex("(?:↑|upload|up)\\s*[:\\-]?\\s*(\\d+(?:[.,]\\d+)?)\\s*(B|KB|MB|GB)/s", RegexOption.IGNORE_CASE)
        private val DOWN_TOTAL_AND_RATE_PATTERN = Regex("(?:\\u2193|download|down)\\s*[:\\-]?\\s*\\d+(?:[.,]\\d+)?\\s*(?:B|KB|MB|GB)\\s*\\|\\s*(\\d+(?:[.,]\\d+)?)\\s*(B|KB|MB|GB)/s", RegexOption.IGNORE_CASE)
        private val UP_TOTAL_AND_RATE_PATTERN = Regex("(?:\\u2191|upload|up)\\s*[:\\-]?\\s*\\d+(?:[.,]\\d+)?\\s*(?:B|KB|MB|GB)\\s*\\|\\s*(\\d+(?:[.,]\\d+)?)\\s*(B|KB|MB|GB)/s", RegexOption.IGNORE_CASE)
        private val FLAG_ICON_TOKENS = setOf("flag", "country", "location", "server")
        private val NON_FLAG_ICON_TOKENS = setOf("launcher", "app_icon", "appicon", "logo", "notification_icon")
    }
}

object VpnTrafficFormatter {
    fun format(snapshot: VpnTrafficSnapshot): String? {
        if (!snapshot.hasUsefulData) return null
        return buildList {
            snapshot.downloadBytesPerSecond?.takeIf { it > 0 }?.let { add("↓ ${formatRate(it)}") }
            snapshot.uploadBytesPerSecond?.takeIf { it > 0 }?.let { add("↑ ${formatRate(it)}") }
        }.takeIf(List<String>::isNotEmpty)?.joinToString(" · ")
    }

    private fun formatRate(bytes: Long): String {
        val (value, suffix) = when {
            bytes >= 1_000_000_000L -> bytes / 1_000_000_000.0 to "GB/s"
            bytes >= 1_000_000L -> bytes / 1_000_000.0 to "MB/s"
            bytes >= 1_000L -> bytes / 1_000.0 to "KB/s"
            else -> bytes.toDouble() to "B/s"
        }
        val rendered = if (value >= 100 || value % 1.0 == 0.0) String.format(Locale.ROOT, "%.0f", value)
        else String.format(Locale.ROOT, "%.1f", value)
        return "$rendered $suffix"
    }
}

