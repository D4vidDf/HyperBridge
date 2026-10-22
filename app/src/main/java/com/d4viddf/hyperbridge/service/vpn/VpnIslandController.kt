package com.d4viddf.hyperbridge.service.vpn

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.service.notification.StatusBarNotification
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.data.theme.ThemeRepository
import com.d4viddf.hyperbridge.receiver.VpnActionReceiver
import com.d4viddf.hyperbridge.service.BridgeNotificationChannels
import com.d4viddf.hyperbridge.service.translators.VpnTranslator
import com.d4viddf.hyperbridge.util.ShizukuManager
import com.d4viddf.hyperbridge.util.sendAllowingBackgroundLaunch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

class VpnIslandController(
    private val context: Context,
    private val scope: CoroutineScope,
    private val preferences: AppPreferences,
    private val themeRepository: ThemeRepository,
    private val initialNotifications: () -> Array<StatusBarNotification> = { emptyArray() },
    private val onIslandActiveChanged: (Boolean) -> Unit = {}
) : VpnSystemObserver.Listener {
    private data class SourceEvidence(
        val evidence: VpnNotificationEvidence,
        val disconnectIntent: PendingIntent?,
        val contentIntent: PendingIntent?,
        val countryFlagBitmap: Bitmap?,
        val observedAtMillis: Long
    )

    private val reducer = VpnSessionReducer()
    private val observer = VpnSystemObserver(context, this)
    private val providerRegistry = VpnProviderRegistry(context)
    private val systemInspector = VpnSystemInspector(context, providerRegistry)
    private val notificationAnalyzer = VpnNotificationAnalyzer()
    private val controlResolver = VpnControlResolver(context)
    private val translator = VpnTranslator(context, themeRepository)
    private val timerOriginTracker = VpnTimerOriginTracker()
    private val mutex = Mutex()
    private val sourceEvidence = ConcurrentHashMap<String, SourceEvidence>()
    private val controlHandler: (String) -> Unit = { logicalId -> scope.launch { disconnect(logicalId) } }

    @Volatile private var enabled = false
    private var session: VpnSession? = null
    private var postedGeneration: Long? = null
    private var notificationPosted = false
    private var reportedActive = false
    private var handoffJob: Job? = null
    private var disconnectTimeoutJob: Job? = null
    private var minorRenderJob: Job? = null
    private var lastRenderAtMillis = 0L

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    fun start() {
        VpnControlRegistry.register(controlHandler)
        scope.launch {
            preferences.vpnIslandEnabledFlow.collectLatest { shouldEnable ->
                enabled = shouldEnable
                if (shouldEnable) {
                    providerRegistry.providerPackages(refresh = true)
                    // The listener may not be bound yet; the service re-seeds via onListenerConnected.
                    runCatching { initialNotifications() }
                        .getOrDefault(emptyArray())
                        .forEach(::onSourceNotificationPosted)
                    observer.start()
                } else {
                    observer.stop()
                    mutex.withLock {
                        session = null
                        postedGeneration = null
                        notificationPosted = false
                        timerOriginTracker.clear()
                        sourceEvidence.clear()
                        cancelJobs()
                        cancelIsland()
                    }
                }
            }
        }
        scope.launch {
            combine(
                preferences.vpnDetectionModeFlow,
                preferences.vpnDisabledProvidersFlow,
                preferences.vpnProviderPriorityFlow
            ) { mode, disabled, priority -> Triple(mode, disabled, priority) }
                .collectLatest {
                    if (enabled) {
                        applyBestEvidence()
                    }
                }
        }
    }

    /**
     * Re-scan the shade once the listener is actually bound. [start] runs from the service's
     * onCreate, which can precede onListenerConnected, so its initial scan may legitimately
     * have seen nothing. Feeding the same source twice is idempotent (keyed by sbn.key).
     */
    fun onListenerConnected() {
        if (!enabled) return
        scope.launch {
            runCatching { initialNotifications() }
                .getOrDefault(emptyArray())
                .forEach(::onSourceNotificationPosted)
        }
    }

    fun stop() {
        enabled = false
        observer.stop()
        VpnControlRegistry.unregister(controlHandler)
        handoffJob?.cancel(); disconnectTimeoutJob?.cancel(); minorRenderJob?.cancel()
        session = null
        postedGeneration = null
        notificationPosted = false
        timerOriginTracker.clear()
        cancelIsland()
    }

    fun onSourceNotificationPosted(sbn: StatusBarNotification) {
        if (!enabled || sbn.packageName == context.packageName) return
        val providers = providerRegistry.providerPackages()
        if (sbn.packageName !in providers) return
        val notification = sbn.notification
        val largeIcon = notification.getLargeIcon()
        val largeIconResourceName = largeIcon
            ?.takeIf { it.type == Icon.TYPE_RESOURCE }
            ?.resId
            ?.let { providerRegistry.resourceEntryName(sbn.packageName, it) }
        val now = System.currentTimeMillis()
        val hints = providerRegistry.notificationHints(sbn.packageName)
        val signals = VpnNotificationSignals(
            packageName = sbn.packageName,
            appLabel = providerRegistry.appLabel(sbn.packageName),
            title = notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty(),
            text = notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty(),
            subText = notification.extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString().orEmpty(),
            isOngoing = notification.flags and Notification.FLAG_ONGOING_EVENT != 0,
            isForegroundService = notification.flags and Notification.FLAG_FOREGROUND_SERVICE != 0,
            usesChronometer = notification.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER, false),
            whenMillis = notification.`when`,
            actions = notification.actions.orEmpty().mapIndexed { index, action ->
                val resourceId = action.getIcon()?.takeIf { it.type == android.graphics.drawable.Icon.TYPE_RESOURCE }?.resId
                VpnNotificationActionSignal(
                    index,
                    action.title?.toString().orEmpty(),
                    action.semanticAction,
                    action.actionIntent != null,
                    resourceId?.let { providerRegistry.resourceEntryName(sbn.packageName, it) }
                )
            },
            category = notification.category,
            hasNativeXiaomiPayload = VpnNativeCapabilityGuard.hasNativeIsland(notification),
            localizedStateLabels = hints.stateLabels,
            localizedDestinationTemplates = hints.destinationTemplates,
            hasLargeIcon = largeIcon != null,
            largeIconResourceName = largeIconResourceName
        )
        val evidence = notificationAnalyzer.analyze(signals, providers, now) ?: return
        val pendingIntent = evidence.disconnectActionIndex?.let { notification.actions?.getOrNull(it)?.actionIntent }
        val countryFlagBitmap = if (evidence.useLargeIconAsCountryFlag) {
            runCatching { largeIcon?.loadDrawable(context)?.toBitmap(width = 96, height = 96) }.getOrNull()
        } else null
        sourceEvidence[sbn.key] = SourceEvidence(
            evidence,
            pendingIntent,
            notification.contentIntent,
            countryFlagBitmap,
            now
        )
        scope.launch { applyBestEvidence() }
    }

    fun onSourceNotificationRemoved(sbn: StatusBarNotification) {
        if (sourceEvidence.remove(sbn.key) != null) scope.launch { applyBestEvidence() }
    }

    override fun onVpnNetworkAvailable(networkHandle: Long, observedAtMillis: Long, wasAlreadyPresent: Boolean) {
        if (!enabled) return
        scope.launch {
            handoffJob?.cancel()
            applyEvent(
                VpnSessionEvent.NetworkAvailable(
                    networkHandle = networkHandle,
                    observedAtMillis = observedAtMillis,
                    connectionStartObserved = !wasAlreadyPresent
                ),
                minor = false
            )
            applyBestEvidence()
        }
    }

    override fun onVpnNetworkLost(networkHandle: Long, observedAtMillis: Long) {
        if (!enabled) return
        scope.launch {
            val generation = mutex.withLock {
                session = reducer.reduce(session, VpnSessionEvent.NetworkLost(networkHandle, observedAtMillis))
                session?.generation
            } ?: return@launch
            renderNow()
            handoffJob?.cancel()
            handoffJob = scope.launch {
                delay(HANDOFF_GRACE_MS)
                applyEvent(VpnSessionEvent.HandoffGraceExpired(generation, System.currentTimeMillis()), minor = false)
            }
        }
    }

    override fun onVpnNetworkCapabilitiesChanged(metadata: VpnNetworkMetadata, observedAtMillis: Long) {
        if (!enabled) return
        scope.launch {
            applyEvent(VpnSessionEvent.NetworkCapabilitiesChanged(metadata, observedAtMillis), minor = true)
        }
    }

    override fun onVpnLinkPropertiesChanged(networkHandle: Long, observedAtMillis: Long) {
        // LinkProperties changes are intentionally observed but currently contain no stable,
        // user-facing VPN metadata. Capabilities/network existence remain the source of truth.
    }

    private fun providerPriorityRank(packageName: String, priorityList: List<String>): Int {
        val idx = priorityList.indexOf(packageName)
        return if (idx == -1) Int.MAX_VALUE else idx
    }

    private suspend fun applyBestEvidence() {
        val mode = preferences.getVpnDetectionModeSync()
        val disabled = preferences.getVpnDisabledProvidersSync()
        val priorityList = preferences.getVpnProviderPrioritySync()
        val allProviders = providerRegistry.providerPackages()
        val allowedProviders = allProviders - disabled

        val systemProvider = if (mode != VpnDetectionMode.NOTIFICATIONS_ONLY) {
            systemInspector.resolveSystemProvider(allowedProviders)
        } else {
            null
        }

        val bestNotification = if (mode != VpnDetectionMode.SYSTEM_ONLY) {
            sourceEvidence.values
                .filter { it.evidence.provider.packageName in allowedProviders }
                .filter { systemProvider == null || it.evidence.provider.packageName == systemProvider.packageName }
                .maxWithOrNull(
                    compareBy<SourceEvidence> { it.evidence.provider.confidence }
                        .thenByDescending { providerPriorityRank(it.evidence.provider.packageName, priorityList) }
                        .thenBy { it.observedAtMillis }
                )
        } else {
            null
        }

        val resolvedProvider = systemProvider ?: bestNotification?.evidence?.provider
        val nativePresent = bestNotification?.evidence?.hasNativeXiaomiPayload == true
        val event = VpnSessionEvent.Enriched(
            provider = resolvedProvider,
            destination = bestNotification?.evidence?.destination,
            traffic = bestNotification?.evidence?.traffic,
            connectedAtMillis = bestNotification?.evidence?.connectedAtMillis,
            timestampSource = bestNotification?.evidence?.timestampSource ?: VpnTimestampSource.UNKNOWN,
            providerState = bestNotification?.evidence?.providerState,
            controlProvenance = if (bestNotification?.disconnectIntent != null) {
                VpnControlProvenance.SOURCE_PENDING_INTENT
            } else {
                VpnControlProvenance.UNSUPPORTED
            },
            nativeProviderIslandPresent = nativePresent,
            authoritativeProviderOverride = true,
            observedAtMillis = System.currentTimeMillis()
        )
        val isMinor = mutex.withLock {
            val before = session
            val after = reducer.reduce(before, event)
            session = after
            before != null && after != null &&
                before.copy(traffic = after.traffic, lastUpdatedAtMillis = after.lastUpdatedAtMillis) == after
        }
        scheduleRender(isMinor)
    }

    private suspend fun applyEvent(event: VpnSessionEvent, minor: Boolean) {
        mutex.withLock { session = reducer.reduce(session, event) }
        scheduleRender(minor)
    }

    private fun scheduleRender(minor: Boolean) {
        if (!minor) {
            minorRenderJob?.cancel()
            scope.launch { renderNow() }
            return
        }
        val wait = (lastRenderAtMillis + MINOR_UPDATE_INTERVAL_MS - System.currentTimeMillis()).coerceAtLeast(0L)
        if (wait == 0L) {
            scope.launch { renderNow() }
        } else if (minorRenderJob?.isActive != true) {
            minorRenderJob = scope.launch { delay(wait); renderNow() }
        }
    }

    private suspend fun renderNow() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPosted = false
            setReportedActive(false)
            return
        }
        val snapshot = mutex.withLock { session }
        if (snapshot == null) {
            postedGeneration = null
            notificationPosted = false
            timerOriginTracker.clear()
            disconnectTimeoutJob?.cancel()
            cancelIsland()
            return
        }
        if (snapshot.nativeProviderIslandPresent) {
            postedGeneration = snapshot.generation
            notificationPosted = false
            cancelIsland()
            return
        }

        val isUpdate = postedGeneration == snapshot.generation
        val mode = preferences.getVpnDetectionModeSync()
        val disabled = preferences.getVpnDisabledProvidersSync()
        val priorityList = preferences.getVpnProviderPrioritySync()
        val visualSource = if (mode != VpnDetectionMode.SYSTEM_ONLY) {
            bestSourceFor(snapshot, disabled, priorityList)
        } else {
            null
        }
        val notificationBacked = visualSource != null || snapshot.provider != null
        val stableTimerStartMillis = timerOriginTracker.resolve(
            sessionGeneration = snapshot.generation,
            authoritativeStartMillis = snapshot.connectedAtMillis,
            observedAtMillis = System.currentTimeMillis()
        )
        val visualControl = if (notificationBacked) {
            controlResolver.resolveDisconnect(snapshot, bestDisconnectSource(snapshot, disabled)?.disconnectIntent)
                ?: controlResolver.manage(snapshot, visualSource?.contentIntent)
        } else null
        val actionIntent = when (visualControl?.kind) {
            VpnControlKind.DISCONNECT -> createDisconnectIntent(snapshot.logicalId)
            VpnControlKind.MANAGE -> visualControl.pendingIntent
            null -> null
        }
        val config = preferences.getGlobalConfigSync()
        val data = translator.translate(
            snapshot,
            config,
            themeRepository.activeTheme.value,
            visualControl?.kind ?: VpnControlKind.MANAGE,
            actionIntent,
            notificationBacked,
            timerStartedAtMillis = if (visualSource != null) snapshot.connectedAtMillis else stableTimerStartMillis,
            countryFlagBitmap = visualSource?.countryFlagBitmap
        )
        val notification = NotificationCompat.Builder(context, BridgeNotificationChannels.ACTIVE)
            .setSmallIcon(R.drawable.ic_vpn)
            .setContentTitle(context.getString(R.string.vpn_title))
            .setContentText(context.getString(R.string.vpn_is_connected))
            .setOngoing(true)
            .setOnlyAlertOnce(isUpdate)
            .setDefaults(0)
            .setSound(null)
            .setVibrate(null)
            .addExtras(data.resources)
            .apply {
                val contentIntent = visualSource?.contentIntent
                if (contentIntent != null) {
                    setContentIntent(contentIntent)
                } else {
                    snapshot.provider?.packageName?.let { pkg ->
                        context.packageManager.getLaunchIntentForPackage(pkg)?.let { launch ->
                            setContentIntent(
                                PendingIntent.getActivity(
                                    context,
                                    pkg.hashCode(),
                                    launch,
                                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                )
                            )
                        }
                    }
                }
            }
            .build()
        notification.extras.putString("miui.focus.param", data.jsonParam)
        if (notificationPosted) NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        else ShizukuManager.notifyInPlace(context, NOTIFICATION_ID, notification)
        postedGeneration = snapshot.generation
        notificationPosted = true
        setReportedActive(true)
        lastRenderAtMillis = System.currentTimeMillis()
    }

    private fun bestDisconnectSource(
        snapshot: VpnSession,
        disabled: Set<String> = preferences.getVpnDisabledProvidersSync()
    ): SourceEvidence? = sourceEvidence.values
        .filter { it.disconnectIntent != null }
        .filter { it.evidence.provider.packageName !in disabled }
        .filter { snapshot.provider == null || it.evidence.provider.packageName == snapshot.provider.packageName }
        .maxByOrNull { it.observedAtMillis }

    private fun bestSourceFor(
        snapshot: VpnSession,
        disabled: Set<String>,
        priorityList: List<String>
    ): SourceEvidence? = sourceEvidence.values
        .filter { it.evidence.provider.packageName !in disabled }
        .filter { snapshot.provider == null || it.evidence.provider.packageName == snapshot.provider.packageName }
        .maxWithOrNull(
            compareBy<SourceEvidence> { it.evidence.provider.confidence }
                .thenByDescending { providerPriorityRank(it.evidence.provider.packageName, priorityList) }
                .thenBy { it.observedAtMillis }
        )

    private fun createDisconnectIntent(logicalId: String): PendingIntent {
        val intent = Intent(context, VpnActionReceiver::class.java).apply {
            action = VpnActionReceiver.ACTION_DISCONNECT
            putExtra(VpnActionReceiver.EXTRA_LOGICAL_ID, logicalId)
        }
        return PendingIntent.getBroadcast(
            context, logicalId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private suspend fun disconnect(logicalId: String) {
        val control = mutex.withLock {
            val current = session?.takeIf { it.logicalId == logicalId && it.networkHandles.isNotEmpty() } ?: return
            val resolved = controlResolver.resolveDisconnect(current, bestDisconnectSource(current)?.disconnectIntent) ?: return
            session = reducer.reduce(
                current.copy(controlProvenance = resolved.provenance),
                VpnSessionEvent.DisconnectRequested(System.currentTimeMillis())
            )
            resolved
        }
        renderNow()
        // Sent from our receiver, so an activity-type disconnect (OpenVPN for Android's DisconnectVPN)
        // needs the same background-launch opt-in as the message island tap (#359).
        val sent = runCatching { control.pendingIntent.sendAllowingBackgroundLaunch(); true }.getOrDefault(false)
        if (!sent) {
            applyEvent(VpnSessionEvent.DisconnectFailed(System.currentTimeMillis()), minor = false)
            return
        }
        disconnectTimeoutJob?.cancel()
        disconnectTimeoutJob = scope.launch {
            delay(DISCONNECT_VERIFY_TIMEOUT_MS)
            val stillConnected = mutex.withLock { session?.networkHandles?.isNotEmpty() == true }
            if (stillConnected) applyEvent(VpnSessionEvent.DisconnectFailed(System.currentTimeMillis()), minor = false)
        }
    }

    private fun cancelJobs() {
        handoffJob?.cancel(); disconnectTimeoutJob?.cancel(); minorRenderJob?.cancel()
        handoffJob = null; disconnectTimeoutJob = null; minorRenderJob = null
    }

    private fun cancelIsland() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        setReportedActive(false)
    }

    private fun setReportedActive(active: Boolean) {
        if (reportedActive == active) return
        reportedActive = active
        onIslandActiveChanged(active)
    }

    companion object {
        /** Reserved controller-owned ID; periodic orphan cleanup must leave this notification alone. */
        val NOTIFICATION_ID = VpnTranslator.BUSINESS.hashCode()
        private const val HANDOFF_GRACE_MS = 1_500L
        private const val DISCONNECT_VERIFY_TIMEOUT_MS = 10_000L
        private const val MINOR_UPDATE_INTERVAL_MS = 2_000L
    }
}

