package com.d4viddf.hyperbridge.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Person
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.d4viddf.hyperbridge.MainActivity
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.data.db.AppDatabase
import com.d4viddf.hyperbridge.data.theme.RulesEngine
import com.d4viddf.hyperbridge.data.theme.ThemeRepository
import com.d4viddf.hyperbridge.service.vpn.VpnIslandController
import com.d4viddf.hyperbridge.data.widget.WidgetManager
import com.d4viddf.hyperbridge.models.ActiveIsland
import com.d4viddf.hyperbridge.models.HyperIslandData
import com.d4viddf.hyperbridge.models.IslandConfig
import com.d4viddf.hyperbridge.models.IslandLimitMode
import com.d4viddf.hyperbridge.models.NavContent
import com.d4viddf.hyperbridge.models.NotificationType
import com.d4viddf.hyperbridge.models.WidgetConfig
import com.d4viddf.hyperbridge.models.WidgetRenderMode
import com.d4viddf.hyperbridge.service.translators.CallTranslator
import com.d4viddf.hyperbridge.service.translators.LiveUpdateTranslator
import com.d4viddf.hyperbridge.service.translators.MediaTranslator
import com.d4viddf.hyperbridge.service.translators.MessageTranslator
import com.d4viddf.hyperbridge.service.translators.NavTranslator
import com.d4viddf.hyperbridge.service.translators.ProgressTranslator
import com.d4viddf.hyperbridge.service.translators.DownloadTranslator
import com.d4viddf.hyperbridge.service.translators.StandardTranslator
import com.d4viddf.hyperbridge.service.translators.TimerTranslator
import com.d4viddf.hyperbridge.service.translators.WidgetTranslator
import com.d4viddf.hyperbridge.service.translators.ScreenRecordingTranslator
import com.d4viddf.hyperbridge.service.translators.ScreenRecordingSavedTranslator
import com.d4viddf.hyperbridge.service.recording.ScreenRecordingClassifier
import com.d4viddf.hyperbridge.service.recording.ScreenRecordingControlBackend
import com.d4viddf.hyperbridge.service.recording.ScreenRecordingSavedIdentity
import com.d4viddf.hyperbridge.service.recording.ScreenRecordingSemanticFingerprint
import com.d4viddf.hyperbridge.service.recording.ScreenRecordingSession
import com.d4viddf.hyperbridge.service.recording.ScreenRecordingSessionInput
import com.d4viddf.hyperbridge.service.recording.ScreenRecordingSessionTracker
import com.d4viddf.hyperbridge.service.recording.ScreenRecordingSignals
import com.d4viddf.hyperbridge.service.recording.ScreenRecordingTimeoutPolicy
import com.d4viddf.hyperbridge.service.recording.XiaomiScreenRecordingControlBackend
import com.d4viddf.hyperbridge.util.ShizukuManager
import com.d4viddf.hyperbridge.models.CallStage
import com.d4viddf.hyperbridge.models.MessageEventFingerprint
import com.d4viddf.hyperbridge.models.MessageEventFingerprintSource
import com.d4viddf.hyperbridge.service.call.CallActionSignal
import com.d4viddf.hyperbridge.service.call.CallClassification
import com.d4viddf.hyperbridge.service.call.CallNotificationClassifier
import com.d4viddf.hyperbridge.service.call.CallNotificationSignals
import com.d4viddf.hyperbridge.service.call.CallReplacementPolicy
import com.d4viddf.hyperbridge.service.call.CallSession
import com.d4viddf.hyperbridge.service.call.CallSessionInput
import com.d4viddf.hyperbridge.service.call.CallSessionTracker
import com.d4viddf.hyperbridge.service.call.CallStageVisibilityPolicy
import com.d4viddf.hyperbridge.service.diagnostics.DiagnosticsStore
import com.d4viddf.hyperbridge.service.message.MessageEventFallbackTracker
import com.d4viddf.hyperbridge.service.message.MessageEventSignals
import com.d4viddf.hyperbridge.service.message.MessageIdentity
import com.d4viddf.hyperbridge.service.message.MessageNotificationResolver
import com.d4viddf.hyperbridge.service.message.MessageNotificationSignals
import com.d4viddf.hyperbridge.service.message.MessagePresentationFamilyTracker
import com.d4viddf.hyperbridge.service.message.MessagePresentationSource
import com.d4viddf.hyperbridge.service.message.MessageSourceQuality
import com.d4viddf.hyperbridge.service.message.MessagingEventSignals
import com.d4viddf.hyperbridge.service.message.isMessagingEvent
import io.github.d4viddf.hyperisland_kit.HyperIslandNotification
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds

class NotificationReaderService : NotificationListenerService() {

    companion object {
        const val ACTION_RELOAD_THEME = "com.d4viddf.hyperbridge.ACTION_RELOAD_THEME"
        const val ACTION_PERFORM_MIGRATION = "com.d4viddf.hyperbridge.ACTION_PERFORM_MIGRATION"
        private val GMAIL_PACKAGES = setOf("com.google.android.gm")

        @Volatile
        var isConnected: Boolean = false
            internal set
    }

    private val TAG = "HyperBridgeDebug"
    private val EXTRA_ORIGINAL_KEY = "hyper_original_key"

    // --- CHANNELS ---
    private val NOTIFICATION_CHANNEL_ID = BridgeNotificationChannels.ACTIVE
    private val WIDGET_CHANNEL_ID = BridgeNotificationChannels.WIDGET
    private val LIVE_UPDATE_CHANNEL_ID = BridgeNotificationChannels.LIVE_UPDATE
    private val WATCH_RELAY_CHANNEL_ID = BridgeNotificationChannels.WATCH_RELAY
    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())

    // --- STATE & CONFIG ---
    private var allowedPackageSet: Set<String> = emptySet()
    private var currentMode = IslandLimitMode.MOST_RECENT
    private var appPriorityList = emptyList<String>()
    private var globalBlockedTerms: Set<String> = emptySet()
    
    private var isDndModeEnabled = false
    private var autoDetectDnd = false

    // --- CACHES ---
    private data class RemovedSource(
        val observedAt: Long,
        val sourcePostTime: Long,
        val reason: Int
    )

    private val recentlyRemovedKeys = ConcurrentHashMap<String, RemovedSource>()
    private val nativeIslands = ConcurrentHashMap.newKeySet<String>()
    private val activeIslands = ConcurrentHashMap<String, ActiveIsland>()
    private val activeTranslations = ConcurrentHashMap<String, Int>()
    private val reverseTranslations = ConcurrentHashMap<Int, String>()
    private val internalBridgeReplacements = InternalBridgeReplacementRegistry()
    private val sourceToLogicalKeys = ConcurrentHashMap<String, String>()
    private val processingJobs = ConcurrentHashMap<Long, Job>()
    private val sourceProcessingGeneration = SourceProcessingGeneration()
    private val messageEventTracker = MessageEventFallbackTracker()
    private val messageResolver = MessageNotificationResolver()
    private val messageFamilyTracker = MessagePresentationFamilyTracker()
    private val expiredIslands = ExpiredIslandRegistry()
    private val timeoutJobs = ConcurrentHashMap<String, Job>()
    private val removalJobs = ConcurrentHashMap<String, Job>()
    private lateinit var permanentIslandManager: PermanentIslandManager
    @Volatile private var vpnIslandActive = false
    private lateinit var vpnIslandController: VpnIslandController
    private val intentionallyRemovedKeys = ConcurrentHashMap<String, Long>()
    private val widgetUpdateDebouncer = ConcurrentHashMap<Int, Long>()
    private val dismissedWidgetIds = ConcurrentHashMap.newKeySet<Int>()
    private val activeWidgets = ConcurrentHashMap.newKeySet<Int>()
    private val appLabelCache = ConcurrentHashMap<String, String>()
    private val notificationLifecycleMutex = Mutex()

    private val MAX_ISLANDS = 9
    private val WIDGET_ID_BASE = 9000
    // Negative so these ids can never hit the >= WIDGET_ID_BASE branch in onNotificationRemoved
    private val WATCH_RELAY_ID_BASE = -20000
    private var watchRelaySlot = 0

    private lateinit var preferences: AppPreferences

    // --- THEME ENGINE ---
    private lateinit var themeRepository: ThemeRepository
    private lateinit var rulesEngine: RulesEngine
    private lateinit var callClassifier: CallNotificationClassifier
    private val callSessionTracker = CallSessionTracker()
    private val screenRecordingSessionTracker = ScreenRecordingSessionTracker()
    private lateinit var screenRecordingControlBackend: XiaomiScreenRecordingControlBackend

    // Translators
    private lateinit var callTranslator: CallTranslator
    private lateinit var navTranslator: NavTranslator
    private lateinit var timerTranslator: TimerTranslator
    private lateinit var progressTranslator: ProgressTranslator
    private lateinit var downloadTranslator: DownloadTranslator
    private lateinit var standardTranslator: StandardTranslator
    private lateinit var messageTranslator: MessageTranslator
    private lateinit var mediaTranslator: MediaTranslator
    private lateinit var widgetTranslator: WidgetTranslator
    private lateinit var liveUpdateTranslator: LiveUpdateTranslator
    private lateinit var screenRecordingTranslator: ScreenRecordingTranslator
    private lateinit var screenRecordingSavedTranslator: ScreenRecordingSavedTranslator

    @Volatile
    private var isScreenOn = true

    private val systemReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_USER_UNLOCKED) {
                WidgetManager.init(this@NotificationReaderService)
                syncNotifications(refresh = true)
            } else if (intent.action == Intent.ACTION_SCREEN_ON) {
                isScreenOn = true
                syncNotifications(refresh = true)
            } else if (intent.action == Intent.ACTION_SCREEN_OFF) {
                isScreenOn = false
            }
        }
    }

    private val islandClickReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "com.d4viddf.hyperbridge.ISLAND_CLICKED") {
                val sbnKey = intent.getStringExtra("sbn_key")
                val bridgeId = intent.getIntExtra("bridge_id", -1)
                @Suppress("DEPRECATION")
                val originalIntent = intent.getParcelableExtra<PendingIntent>("original_intent")

                if (originalIntent != null) {
                    try {
                        originalIntent.send()
                    } catch (e: PendingIntent.CanceledException) {
                        Log.e("HyperBridge", "PendingIntent canceled", e)
                    }
                }

                if (sbnKey != null) {
                    cancelNotification(sbnKey)
                }

                if (bridgeId != -1) {
                    ShizukuManager.cancel(context, bridgeId)
                }
            }
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.ACCESS_NETWORK_STATE])
    override fun onCreate() {
        super.onCreate()
        
        val filter = IntentFilter(Intent.ACTION_USER_UNLOCKED)
        filter.addAction(Intent.ACTION_SCREEN_ON)
        filter.addAction(Intent.ACTION_SCREEN_OFF)
        registerReceiver(systemReceiver, filter)
        
        val clickFilter = IntentFilter("com.d4viddf.hyperbridge.ISLAND_CLICKED")
        androidx.core.content.ContextCompat.registerReceiver(
            this,
            islandClickReceiver,
            clickFilter,
            androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
        )
        
        preferences = AppPreferences(applicationContext)
        createChannels()

        // [INIT] Theme Engine
        themeRepository = ThemeRepository(this)
        rulesEngine = RulesEngine()
        callClassifier = CallNotificationClassifier(
            answerKeywords = resources.getStringArray(R.array.call_keywords_answer).toList(),
            declineKeywords = resources.getStringArray(R.array.call_keywords_hangup).toList(),
            hangUpKeywords = resources.getStringArray(R.array.call_keywords_hangup).toList(),
            muteKeywords = resources.getStringArray(R.array.call_keywords_mute).toList(),
            unmuteKeywords = resources.getStringArray(R.array.call_keywords_unmute).toList(),
            speakerKeywords = resources.getStringArray(R.array.call_keywords_speaker).toList()
        )

        // Pass ThemeRepository to Translators
        callTranslator = CallTranslator(this, themeRepository)
        navTranslator = NavTranslator(this, themeRepository)
        timerTranslator = TimerTranslator(this, themeRepository)
        progressTranslator = ProgressTranslator(this, themeRepository)
        downloadTranslator = DownloadTranslator(this, themeRepository)
        standardTranslator = StandardTranslator(this, themeRepository)
        messageTranslator = MessageTranslator(this, themeRepository)
        liveUpdateTranslator = LiveUpdateTranslator(this, themeRepository)

        mediaTranslator = MediaTranslator(this)
        widgetTranslator = WidgetTranslator(this)
        screenRecordingTranslator = ScreenRecordingTranslator(this)
        screenRecordingSavedTranslator = ScreenRecordingSavedTranslator(this, themeRepository)
        screenRecordingControlBackend = XiaomiScreenRecordingControlBackend(this)

        val userManager = getSystemService(USER_SERVICE) as android.os.UserManager
        if (userManager.isUserUnlocked) {
            WidgetManager.init(this)
        }

        permanentIslandManager = PermanentIslandManager(this, serviceScope, preferences)
        vpnIslandController = VpnIslandController(
            this,
            serviceScope,
            preferences,
            themeRepository,
            initialNotifications = { activeNotifications ?: emptyArray() },
            onIslandActiveChanged = { active ->
                vpnIslandActive = active
                updatePermanentIsland()
            }
        )
        vpnIslandController.start()

        serviceScope.launch { preferences.allowedPackagesFlow.collectLatest { allowedPackageSet = it } }
        serviceScope.launch { preferences.limitModeFlow.collectLatest { currentMode = it } }
        serviceScope.launch { preferences.appPriorityListFlow.collectLatest { appPriorityList = it } }
        serviceScope.launch { preferences.globalBlockedTermsFlow.collectLatest { globalBlockedTerms = it } }
        serviceScope.launch { preferences.isDndModeEnabledFlow.collectLatest { isDndModeEnabled = it } }
        serviceScope.launch { preferences.autoDetectDndFlow.collectLatest { autoDetectDnd = it } }

        // Listen for Theme Changes
        serviceScope.launch {
            preferences.activeThemeIdFlow.collectLatest { themeId ->
                Log.d(TAG, "Service detected theme change: $themeId")
                if (themeId != null) {
                    themeRepository.activateTheme(themeId)
                } else {
                    themeRepository.activateTheme("")
                }
            }
        }

        // --- WIDGET LISTENER ---
        serviceScope.launch {
            WidgetManager.widgetUpdates.collect { updatedId ->
                if (dismissedWidgetIds.contains(updatedId)) return@collect
                val savedIds = preferences.savedWidgetIdsFlow.first()
                if (savedIds.contains(updatedId)) {
                    val config = preferences.getWidgetConfigFlow(updatedId).first()
                    if (shouldProcessWidgetUpdate(updatedId, config)) {
                        launch(Dispatchers.Main) {
                            processSingleWidget(updatedId, config)
                        }
                    }
                }
            }
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "ACTION_TEST_WIDGET") {
            val widgetId = intent.getIntExtra("WIDGET_ID", -1)
            if (widgetId != -1) {
                dismissedWidgetIds.remove(widgetId)
                serviceScope.launch(Dispatchers.Main) {
                    val config = preferences.getWidgetConfigFlow(widgetId).first()
                    processSingleWidget(widgetId, config)
                }
            }
        } else if (intent?.action == ACTION_RELOAD_THEME) {
            serviceScope.launch {
                val themeId = preferences.activeThemeIdFlow.first()
                if (themeId != null) {
                    Log.d(TAG, "Hot-reloading theme: $themeId")
                    themeRepository.activateTheme(themeId)
                }
            }
        } else if (intent?.action == ACTION_PERFORM_MIGRATION) {
            serviceScope.launch(Dispatchers.IO) {
                AppDatabase.performMigration(applicationContext) { progress ->
                    launch(Dispatchers.Main) {
                        showMigrationProgress(progress)
                    }
                }
            }
        }
        return START_STICKY
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showMigrationProgress(progress: Int) {
        val title = getString(R.string.migration_title)
        val message = if (progress >= 100) getString(R.string.migration_complete) else getString(R.string.migration_message)
        val bridgeId = "migration_update".hashCode()

        serviceScope.launch {
            val useNative = getEffectiveEngine(packageName)
            
            if (useNative) {
                val notificationBuilder = liveUpdateTranslator.translateToLiveUpdate(
                    sbn = null,
                    channelId = LIVE_UPDATE_CHANNEL_ID,
                    type = NotificationType.PROGRESS,
                    navRight = null,
                    config = null
                )
                notificationBuilder.setContentTitle(title)
                notificationBuilder.setContentText(message)
                notificationBuilder.setProgress(100, progress, progress < 0)
                notificationBuilder.setOngoing(progress in 0..99)
                notificationBuilder.setSmallIcon(R.drawable.ic_launcher_foreground)

                val notification = notificationBuilder.build()
                ShizukuManager.notify(this@NotificationReaderService, bridgeId, notification)
            } else {
                val builder = HyperIslandNotification.Builder(this@NotificationReaderService, "migration", title)
                builder.setProgressBar(progress, "#007AFF")
                builder.setChatInfo(title, message, "migration_icon", packageName)
                builder.setShowNotification(true)
                builder.setIslandFirstFloat(true)

                val data = HyperIslandData(builder.buildResourceBundle(), builder.buildJsonParam())

                val notificationBuilder = NotificationCompat.Builder(this@NotificationReaderService, NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setOngoing(progress in 0..99)
                    .setProgress(100, progress, progress < 0)
                    .addExtras(data.resources)

                val notification = notificationBuilder.build()
                notification.extras.putString("miui.focus.param", data.jsonParam)

                ShizukuManager.notify(this@NotificationReaderService, bridgeId, notification)
            }

            if (progress >= 100) {
                delay(3000)
                NotificationManagerCompat.from(this@NotificationReaderService).cancel(bridgeId)
            }
        }
    }

    // =========================================================================
    //  EFFECTIVE BEHAVIOR RESOLUTION (Theme > App > Global)
    // =========================================================================

    private fun getEffectiveTypes(pkg: String): Set<String> {
        val themeOverride = themeRepository.activeTheme.value?.apps?.get(pkg)
        val rawTypes = if (themeOverride?.activeNotificationTypes != null) {
            themeOverride.activeNotificationTypes
        } else {
            val localPref = preferences.getAppConfigSync(pkg)
            localPref ?: preferences.getGlobalNotificationTypesSync()
        }

        // Fallback: if PROGRESS is enabled but DOWNLOAD is missing, implicitly enable DOWNLOAD
        return if (rawTypes.contains("PROGRESS") && !rawTypes.contains("DOWNLOAD")) {
            rawTypes + "DOWNLOAD"
        } else {
            rawTypes
        }
    }

    private fun getEffectiveCallStages(pkg: String): Set<CallStage> = preferences.getEffectiveCallStagesSync(pkg)

    private fun getEffectiveEngine(pkg: String): Boolean {
        val activeTheme = themeRepository.activeTheme.value

        // 1. Theme App Override (Creator explicitly configured this app)
        val themeAppOverride = activeTheme?.apps?.get(pkg)?.useNativeLiveUpdates
        if (themeAppOverride != null) return themeAppOverride

        // 2. User App Override (User explicitly configured this app via Home Screen)
        val userAppOverride = preferences.getAppEnginePreferenceSync(pkg)
        if (userAppOverride != null) return userAppOverride

        // 3. Theme Global Override (Creator explicitly forced an engine for the whole theme)
        val themeGlobalOverride = activeTheme?.global?.useNativeLiveUpdates
        if (themeGlobalOverride != null) return themeGlobalOverride

        // 4. User Global Fallback (The main Engine Setting on the Home Screen!)
        return preferences.useNativeLiveUpdatesSync()
    }

    private fun getEffectiveNav(pkg: String): Pair<NavContent, NavContent> {
        return preferences.getEffectiveNavLayoutSync(pkg)
    }

    // =========================================================================
    //  NOTIFICATION REMOVAL LOGIC
    // =========================================================================

    override fun onNotificationRemoved(sbn: StatusBarNotification?, rankingMap: RankingMap?, reason: Int) {
        if (!isConnected) {
            isConnected = true
            DiagnosticsStore.setServiceConnected(true)
        }
        sbn?.let {
            if (::vpnIslandController.isInitialized) vpnIslandController.onSourceNotificationRemoved(it)
            if (nativeIslands.remove(it.key)) {
                updatePermanentIsland()
            }

            val isOurApp = it.packageName == packageName
            val notifId = it.id
            val notifKey = it.key

            if (isOurApp) {
                val replacement = internalBridgeReplacements.consume(notifId, System.currentTimeMillis())
                if (replacement != null) {
                    Log.d(
                        TAG,
                        "MESSAGE REPLACE removal ignored logicalId=${replacement.logicalId.hashCode()} " +
                                "oldBridgeId=$notifId generation=${replacement.generation}"
                    )
                    return
                }
            }

            if (intentionallyRemovedKeys.remove(notifKey) != null) {
                return
            }

            recentlyRemovedKeys[notifKey] = RemovedSource(System.currentTimeMillis(), it.postTime, reason)

            messageFamilyTracker.removeSource(notifKey)?.let { removal ->
                if (!removal.familyEnded) {
                    sourceToLogicalKeys.remove(notifKey, removal.logicalId)
                    removalJobs.remove(removal.logicalId)?.cancel()
                    Log.d(
                        TAG,
                        "MESSAGE ALIAS REMOVED sourceHash=${notifKey.hashCode()} " +
                                "logicalHash=${removal.logicalId.hashCode()} " +
                                "primaryRemoved=${removal.removedPrimary} familySurvives=true"
                    )
                    return
                }
            }

            if (isOurApp) {
                // A content click removes auto-cancel bridge notifications just like a shade
                // dismissal. Programmatic cancels (updates and Shizuku workarounds) are ignored.
                val wasContentClick = reason == REASON_CLICK
                if (!wasContentClick && reason != REASON_CANCEL && reason != REASON_CANCEL_ALL) {
                    return
                }

                if (notifId >= WIDGET_ID_BASE) {
                    val widgetId = notifId - WIDGET_ID_BASE
                    dismissedWidgetIds.add(widgetId)
                    activeWidgets.remove(widgetId)
                    updatePermanentIsland()
                    return
                }

                var originalKey = reverseTranslations[notifId]
                if (originalKey == null) {
                    originalKey = it.notification.extras.getString(EXTRA_ORIGINAL_KEY)
                }

                if (originalKey != null) {
                    Log.d(TAG, "Our notification $notifId removed. Cleaning up cache for $originalKey")
                    val island = activeIslands[originalKey]
                    if (NotificationLifecyclePolicy.shouldDismissSourceAfterBridgeRemoval(
                            dismissSourceOnContentClick = island?.dismissSourceOnContentClick == true,
                            wasContentClick = wasContentClick
                        )
                    ) {
                        // The bridge uses the recorder's content PendingIntent, so opening it does
                        // not make Android auto-cancel the recorder's separate source notification.
                        // Retire that source explicitly after the bridge click.
                        island?.sourceKey?.let(::cancelSourceNotification)
                    } else {
                        // [FIX] We no longer kill the source notification when our Island is
                        // dismissed or timed out; only forward its normal deletion callback.
                        try {
                            island?.deleteIntent?.send()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error sending delete intent for original notification", e)
                        }
                    }
                    cleanupCache(originalKey)
                }
                return
            }

            val logicalKey = sourceToLogicalKeys[notifKey]
                ?: callSessionTracker.logicalIdForSource(notifKey)
                ?: screenRecordingSessionTracker.logicalIdForSource(notifKey)
                ?: notifKey
            val trackedCallLogicalId = callSessionTracker.logicalIdForSource(notifKey)
            val trackedRecordingLogicalId = screenRecordingSessionTracker.logicalIdForSource(notifKey)

            if (activeTranslations.containsKey(logicalKey)) {
                val hyperId = activeTranslations[logicalKey] ?: return
                val islandType = activeIslands[logicalKey]?.type
                if (islandType == NotificationType.CALL) {
                    callSessionTracker.markSourceRemoved(notifKey, System.currentTimeMillis())
                }

                lateinit var job: Job
                job = serviceScope.launch(Dispatchers.IO) {
                    val appConfig = preferences.getAppIslandConfigSync(sbn.packageName)
                    val globalConfig = preferences.getGlobalConfigSync()
                    val finalConfig = appConfig.mergeWith(globalConfig)

                    val forceDismiss = NotificationLifecyclePolicy.dismissesWithSource(islandType)

                    if (finalConfig.dismissWithOriginal == true || forceDismiss) {
                        // Debounce updates if the app canceled it programmatically
                        if (islandType == NotificationType.CALL) {
                            kotlinx.coroutines.delay(CallReplacementPolicy.REMOVAL_DELAY_MS)
                        } else if (reason == REASON_APP_CANCEL && islandType != NotificationType.SCREEN_RECORDING) {
                            kotlinx.coroutines.delay(300)
                        }
                        notificationLifecycleMutex.withLock {
                            val current = activeIslands[logicalKey]
                            val sourceStillActive = isSourceNotificationActive(notifKey)
                            if (sourceStillActive || current == null || !NotificationLifecyclePolicy.isCurrentRemoval(
                                    activeSourceKey = current.sourceKey,
                                    activeSourcePostTime = current.sourcePostTime,
                                    removedSourceKey = notifKey,
                                    removedSourcePostTime = sbn.postTime
                                )
                            ) {
                                Log.d(
                                    TAG,
                                    "${islandType?.name ?: "UNKNOWN"} REMOVE reason=stale " +
                                            "sourceKey=${notifKey.hashCode()} logicalId=${logicalKey.hashCode()}"
                                )
                                return@withLock
                            }
                            timeoutJobs.remove(logicalKey)?.cancel()
                            try {
                                NotificationManagerCompat.from(this@NotificationReaderService).cancel(hyperId)
                            } catch (_: Exception) {}
                            Log.d(
                                TAG,
                                "${islandType?.name ?: "UNKNOWN"} REMOVE reason=$reason " +
                                        "sourceKey=${notifKey.hashCode()} logicalId=${logicalKey.hashCode()}"
                            )
                            cleanupCache(logicalKey)
                        }
                    }
                }
                removalJobs[logicalKey]?.cancel()
                removalJobs[logicalKey] = job
                job.invokeOnCompletion { removalJobs.remove(logicalKey, job) }
            } else if (trackedCallLogicalId != null) {
                callSessionTracker.markSourceRemoved(notifKey, System.currentTimeMillis())
                lateinit var job: Job
                job = serviceScope.launch(Dispatchers.IO) {
                    kotlinx.coroutines.delay(CallReplacementPolicy.REMOVAL_DELAY_MS)
                    notificationLifecycleMutex.withLock {
                        if (isSourceNotificationActive(notifKey) ||
                            callSessionTracker.logicalIdForSource(notifKey) != trackedCallLogicalId
                        ) {
                            return@withLock
                        }
                        callSessionTracker.end(trackedCallLogicalId)
                        sourceToLogicalKeys.entries.removeIf { it.value == trackedCallLogicalId }
                    }
                }
                removalJobs[trackedCallLogicalId]?.cancel()
                removalJobs[trackedCallLogicalId] = job
                job.invokeOnCompletion { removalJobs.remove(trackedCallLogicalId, job) }
            } else if (trackedRecordingLogicalId != null) {
                // A removal that wins the race with presentation must still retire this exact
                // recording generation. A reused source key with a newer postTime is preserved.
                screenRecordingSessionTracker.endSource(notifKey, sbn.postTime)
                sourceToLogicalKeys.remove(notifKey, trackedRecordingLogicalId)
            }
        }
    }

    private fun cancelSourceNotification(targetKey: String) {
        try {
            val currentNotifications = try {
                activeNotifications
            } catch (_: Exception) {
                cancelNotification(targetKey)
                return
            }

            val targetSbn = currentNotifications.find { it.key == targetKey }
            cancelNotification(targetKey)

            if (targetSbn != null) {
                val groupKey = targetSbn.groupKey
                val pkg = targetSbn.packageName
                if (groupKey == null) return

                val remainingGroupMembers = currentNotifications.filter {
                    it.packageName == pkg &&
                            it.groupKey == groupKey &&
                            it.key != targetKey
                }

                if (remainingGroupMembers.size == 1) {
                    val survivor = remainingGroupMembers[0]
                    val isSummary = (survivor.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0
                    if (isSummary) {
                        cancelNotification(survivor.key)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during smart dismissal", e)
        }
    }

    private fun cleanupCache(originalKey: String, preserveCallSession: Boolean = false) {
        val hyperId = activeTranslations[originalKey]
        val island = activeIslands.remove(originalKey)
        activeTranslations.remove(originalKey)
        timeoutJobs.remove(originalKey)?.cancel()
        if (island?.type == NotificationType.CALL && !preserveCallSession) {
            callSessionTracker.end(island.logicalId)
        }
        if (island?.type == NotificationType.SCREEN_RECORDING) {
            screenRecordingSessionTracker.end(island.logicalId)
        }
        messageFamilyTracker.end(originalKey)

        if (hyperId != null) {
            reverseTranslations.remove(hyperId)
        }
        sourceToLogicalKeys.entries.removeIf { it.value == originalKey }
        updatePermanentIsland()
    }

    private fun handlePostNotificationSideEffects(
        originalKey: String,
        bridgeId: Int,
        generation: Long,
        config: IslandConfig,
        type: NotificationType,
        isLiveUpdate: Boolean,
        sbn: StatusBarNotification? = null,
        title: String = "",
        text: String = "",
        forceLifecycleTimeout: Boolean = false
    ) {
        // 1. Remove original if enabled (EXCEPT for Media and Call)
        if (config.removeOriginalNotification == true && type != NotificationType.MEDIA && type != NotificationType.CALL) {
            if (sbn != null && !isLiveUpdate && (type == NotificationType.MESSAGE || type == NotificationType.STANDARD)) {
                postWatchRelayNotification(sbn, title, text)
            }
            intentionallyRemovedKeys[originalKey] = System.currentTimeMillis()
            cancelNotification(originalKey)
        }

        // 2. Lifecycle timeout using user-configured timeout
        val needsLifecycleTimeout = (isLiveUpdate ||
                type == NotificationType.MESSAGE || type == NotificationType.STANDARD ||
                forceLifecycleTimeout) &&
                type != NotificationType.CALL && type != NotificationType.MEDIA && type != NotificationType.NAVIGATION
        if (needsLifecycleTimeout) {
            val timeoutMs = IslandTimeoutPolicy.durationMillis(config.timeout)
            timeoutJobs.remove(originalKey)?.cancel()
            if (timeoutMs == null) return

            lateinit var job: Job
            job = serviceScope.launch {
                delay(timeoutMs.milliseconds)
                notificationLifecycleMutex.withLock {
                    val current = activeIslands[originalKey]
                    if (!IslandTimeoutPolicy.isCurrent(current?.generation, current?.id, generation, bridgeId)) return@withLock
                    current ?: return@withLock
                    Log.d(TAG, "${type.name} TIMEOUT bridgeId=$bridgeId logicalId=${originalKey.hashCode()}")
                    recordExpiredIsland(current)
                    NotificationManagerCompat.from(this@NotificationReaderService).cancel(bridgeId)
                    cleanupCache(originalKey)
                }
            }
            timeoutJobs[originalKey] = job
            job.invokeOnCompletion { timeoutJobs.remove(originalKey, job) }
        }
    }

    private fun recordExpiredIsland(island: ActiveIsland) {
        if (island.type != NotificationType.MESSAGE && island.type != NotificationType.STANDARD) return
        expiredIslands.record(
            ExpiredIslandRecord(
                logicalId = island.logicalId,
                sourceKey = island.sourceKey,
                sourceFingerprint = sourceGenerationFingerprint(island.lastContentHash, island.sourcePostTime),
                expiredAt = System.currentTimeMillis(),
                messageEventFingerprint = island.messageEventFingerprint
            )
        )
        DiagnosticsStore.record(island.type.name, "expired", island.packageName)
    }

    private fun shouldSuppressExpiredSource(
        sbn: StatusBarNotification,
        type: NotificationType,
        logicalKey: String,
        contentHash: Int,
        recovery: Boolean,
        messageEventFingerprint: MessageEventFingerprint?
    ): Boolean {
        if (type != NotificationType.MESSAGE && type != NotificationType.STANDARD) return false
        return when (
            expiredIslands.evaluate(
                sbn.key,
                sourceGenerationFingerprint(contentHash, sbn.postTime),
                System.currentTimeMillis(),
                messageEventFingerprint,
                logicalKey
            )
        ) {
            ExpiredSourceDecision.NOT_EXPIRED -> false
            ExpiredSourceDecision.SUPPRESS_IDENTICAL -> {
                sourceToLogicalKeys.remove(sbn.key, logicalKey)
                DiagnosticsStore.record(
                    type.name,
                    if (recovery) "recovery-skipped-expired" else "expired-generation-ignored",
                    sbn.packageName
                )
                true
            }
            ExpiredSourceDecision.NEW_GENERATION -> false
        }
    }

    private fun postWatchRelayNotification(sbn: StatusBarNotification, title: String, text: String) {
        try {
            val appLabel = getCachedAppLabel(sbn.packageName)
            val relayId = WATCH_RELAY_ID_BASE - (watchRelaySlot++ and 0x0F)
            val notification = NotificationCompat.Builder(this, WATCH_RELAY_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(if (title.isNotBlank()) "$appLabel · $title" else appLabel)
                .setContentText(text)
                .setSilent(true)
                .setAutoCancel(true)
                .setTimeoutAfter(10_000L)
                .build()
            NotificationManagerCompat.from(this).notify(relayId, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Error posting watch relay notification", e)
        }
    }

    private fun logStateChange(isLandscape: Boolean) {
        val orientation = if (isLandscape) "Landscape" else "Portrait"
        val isIslandExhibited = activeIslands.isNotEmpty() || activeWidgets.isNotEmpty() || vpnIslandActive || nativeIslands.isNotEmpty() || permanentIslandManager.isIslandActive()
        val islandState = if (isIslandExhibited) "Showing Island" else "No Island"
        Log.d(TAG, "State: $orientation | $islandState")
    }

    private fun updatePermanentIsland() {
        permanentIslandManager.onActiveNotificationsChanged(activeIslandCount(), nativeIslands.isNotEmpty())
        DiagnosticsStore.setActiveIslands(activeIslands.size + if (vpnIslandActive) 1 else 0)
        val isLandscape = resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        logStateChange(isLandscape)
    }

    private fun activeIslandCount(): Int = activeIslands.size + activeWidgets.size + if (vpnIslandActive) 1 else 0

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        permanentIslandManager.onOrientationChanged()
        logStateChange(newConfig.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE)
    }

    // =========================================================================
    //  STANDARD NOTIFICATION LOGIC
    // =========================================================================

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (!isConnected) {
            isConnected = true
            DiagnosticsStore.setServiceConnected(true)
        }
        sbn?.let {
            if (::vpnIslandController.isInitialized) vpnIslandController.onSourceNotificationPosted(it)
            if (it.packageName != packageName) {
                val extras = it.notification.extras
                var isNative = false
                if (extras != null) {
                    if (extras.containsKey("miui.focus.param") || extras.containsKey("miui.system.focus.param")) {
                        isNative = true
                    }
                    val template = extras.getString(Notification.EXTRA_TEMPLATE)
                    if (template == "androidx.media.app.NotificationCompat\$MediaStyle" ||
                        template == "android.app.Notification\$MediaStyle") {
                        isNative = true
                    }
                }
                if (isNative) {
                    if (nativeIslands.add(it.key)) updatePermanentIsland()
                } else {
                    if (nativeIslands.remove(it.key)) updatePermanentIsland()
                }
            }

            enqueueSourceNotification(it)
        }
    }

    private fun enqueueSourceNotification(sbn: StatusBarNotification, recovery: Boolean = false) {
        if (shouldIgnore(sbn.packageName) || !isAppAllowed(sbn.packageName)) return
        if (!recovery) {
            DiagnosticsStore.record("CALLBACK", "received", sbn.packageName)
        }
        if (!com.d4viddf.hyperbridge.util.isPostNotificationsEnabled(this)) {
            DiagnosticsStore.record("PERMISSION", "ignored", sbn.packageName, "post-notifications-missing")
            return
        }

        val sourceSlot = sourceSlotIdentity(sbn)
        val callbackObservedAt = System.currentTimeMillis()
        val rawQuality = sourceCandidateQuality(sbn)
        val processingGeneration = sourceProcessingGeneration.next(sourceSlot, rawQuality)
        val job = serviceScope.launch {
            val selectedSbn = ensureValidSbn(sbn, processingGeneration)
            val selectedQuality = sourceCandidateQuality(selectedSbn)
            sourceProcessingGeneration.consider(sourceSlot, processingGeneration, selectedQuality)
            if (!sourceProcessingGeneration.isCurrent(sourceSlot, processingGeneration)) {
                return@launch
            }
            notificationLifecycleMutex.withLock {
                if (!sourceProcessingGeneration.isCurrent(sourceSlot, processingGeneration)) {
                    return@withLock
                }
                processStandardNotification(
                    rawSbn = sbn,
                    sbn = selectedSbn,
                    sourceSlot = sourceSlot,
                    recovery = recovery,
                    processingGeneration = processingGeneration,
                    callbackObservedAt = callbackObservedAt
                )
            }
        }
        processingJobs[processingGeneration] = job
        job.invokeOnCompletion { cause ->
            processingJobs.remove(processingGeneration, job)
            sourceProcessingGeneration.finish(sourceSlot, processingGeneration)
        }
    }

    private fun sourceCandidateQuality(sbn: StatusBarNotification): SourceCandidateQuality =
        if (needsSourceRefresh(sbn)) SourceCandidateQuality.SPARSE else SourceCandidateQuality.USABLE

    private fun sourceSlotIdentity(sbn: StatusBarNotification): String =
        buildString {
            append(sbn.packageName.length).append(':').append(sbn.packageName)
            append('|').append(sbn.id)
            append('|').append(sbn.tag?.length ?: -1).append(':').append(sbn.tag.orEmpty())
        }

    private fun sourceCandidate(sbn: StatusBarNotification): SourceNotificationCandidate =
        SourceNotificationCandidate(
            sourceKey = sbn.key,
            packageName = sbn.packageName,
            notificationId = sbn.id,
            notificationTag = sbn.tag,
            postTime = sbn.postTime,
            quality = sourceCandidateQuality(sbn)
        )

    private fun needsSourceRefresh(sbn: StatusBarNotification): Boolean {
        val notification = sbn.notification
        val extras = notification.extras
        val content = resolveNotificationContent(sbn)
        val template = extras.getString(Notification.EXTRA_TEMPLATE).orEmpty()
        val hasProgressOrSpecialState = hasProgressNotification(sbn, content.title, content.text) ||
                notification.category == Notification.CATEGORY_CALL ||
                notification.category == Notification.CATEGORY_TRANSPORT ||
                notification.category == Notification.CATEGORY_NAVIGATION ||
                notification.category == Notification.CATEGORY_ALARM ||
                template.contains("MediaStyle") ||
                extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER, false)
        return NotificationRefreshPolicy.shouldRefresh(
            NotificationRefreshSignals(
                packageName = sbn.packageName,
                title = content.title,
                text = content.text,
                hasMessageContent = content.hasMessageContent,
                hasProgressOrSpecialState = hasProgressOrSpecialState
            )
        )
    }

    private suspend fun ensureValidSbn(
        initialSbn: StatusBarNotification,
        processingGeneration: Long
    ): StatusBarNotification {
        var bestSbn = initialSbn
        val requested = sourceCandidate(initialSbn)
        repeat(NotificationRefreshPolicy.MAX_REFRESH_ATTEMPTS) {
            if (!needsSourceRefresh(bestSbn)) {
                return bestSbn
            }

            delay(NotificationRefreshPolicy.REFRESH_DELAY_MS.milliseconds)
            val active = try {
                activeNotifications?.toList().orEmpty()
            } catch (_: Exception) {
                emptyList()
            }
            val exact = active.filter { it.key == initialSbn.key }
            val replacements = active.filter {
                it.key != initialSbn.key &&
                        SourceNotificationCandidatePolicy.isSameSlot(requested, sourceCandidate(it))
            }
            var refreshedSbn: StatusBarNotification? = null
            for (candidate in exact + replacements) {
                val selected = refreshedSbn
                if (selected == null || SourceNotificationCandidatePolicy.shouldPrefer(
                        current = sourceCandidate(selected),
                        incoming = sourceCandidate(candidate),
                        requestedSourceKey = initialSbn.key
                    )
                ) {
                    refreshedSbn = candidate
                }
            }
            if (refreshedSbn == null) {
                return@repeat
            }
            bestSbn = refreshedSbn
            if (!needsSourceRefresh(bestSbn)) {
                return bestSbn
            }
        }
        return bestSbn
    }

    private fun resolveNotificationContent(sbn: StatusBarNotification): ResolvedNotificationContent {
        val notification = sbn.notification
        val extras = notification.extras
        val template = extras.getString(Notification.EXTRA_TEMPLATE).orEmpty()
        val isMessageStyle = notification.category == Notification.CATEGORY_MESSAGE ||
                template.contains("MessagingStyle")
        val rawTitle = extras.getCharSequence(Notification.EXTRA_TITLE)
            ?.takeUnless { it.toString().trim().equals(sbn.packageName, ignoreCase = true) }
        return NotificationContentResolver.resolve(
            title = rawTitle,
            text = extras.getCharSequence(Notification.EXTRA_TEXT),
            bigTitle = extras.getCharSequence(Notification.EXTRA_TITLE_BIG),
            bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT),
            messages = extractMessageContent(notification),
            isMessageStyle = isMessageStyle,
            textLines = try {
                extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.toList().orEmpty()
            } catch (_: Exception) {
                emptyList()
            }
        )
    }

    private fun extractMessageContent(notification: Notification): List<MessageContentCandidate> {
        return try {
            val style = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)
            style?.messages?.map { message ->
                MessageContentCandidate(
                    sender = message.person?.name?.toString(),
                    text = message.text?.toString(),
                    timestamp = message.timestamp
                )
            }.orEmpty()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun isMessagingLifecycleEvent(
        sbn: StatusBarNotification,
        type: NotificationType,
        content: ResolvedNotificationContent
    ): Boolean {
        if (type != NotificationType.MESSAGE && type != NotificationType.STANDARD) return false

        val notification = sbn.notification
        val extras = notification.extras
        val template = extras.getString(Notification.EXTRA_TEMPLATE).orEmpty()
        val hasMessagePersonMetadata = try {
            extras.getParcelable(Notification.EXTRA_MESSAGING_PERSON, android.app.Person::class.java) != null ||
                    extras.getParcelableArrayList(
                        Notification.EXTRA_PEOPLE_LIST,
                        android.app.Person::class.java
                    )?.isNotEmpty() == true ||
                    extras.containsKey(Notification.EXTRA_MESSAGES)
        } catch (_: Exception) {
            extras.containsKey(Notification.EXTRA_MESSAGES)
        }
        val hasRemoteInputReply = (notification.actions ?: emptyArray()).any { action ->
            action.semanticAction == Notification.Action.SEMANTIC_ACTION_REPLY ||
                    !action.remoteInputs.isNullOrEmpty()
        }

        return isMessagingEvent(
            MessagingEventSignals(
                packageName = sbn.packageName,
                isMessageNotificationType = type == NotificationType.MESSAGE,
                isStandardNotificationType = type == NotificationType.STANDARD,
                hasMessageCategory = notification.category == Notification.CATEGORY_MESSAGE,
                hasMessagingStyleTemplate = template.contains("MessagingStyle"),
                extractedMessageCount = content.messageCount,
                hasConversationShortcut = !notification.shortcutId.isNullOrBlank(),
                hasConversationLocus = !notification.locusId?.id.isNullOrBlank(),
                hasMessagePersonMetadata = hasMessagePersonMetadata,
                hasRemoteInputReply = hasRemoteInputReply,
                hasEmailCategory = notification.category == Notification.CATEGORY_EMAIL,
                hasUsefulContent = content.title.isNotBlank() && content.text.isNotBlank()
            )
        )
    }

    private fun resolveMessageIdentity(sbn: StatusBarNotification): MessageIdentity {
        val extras = sbn.notification.extras
        val conversationTitle = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()
        val isGroupSummary = (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0
        return messageResolver.resolve(
            MessageNotificationSignals(
                packageName = sbn.packageName,
                notificationId = sbn.id,
                notificationTag = sbn.tag,
                shortcutId = sbn.notification.shortcutId,
                locusId = sbn.notification.locusId?.id,
                conversationTitle = conversationTitle,
                isGroupSummary = isGroupSummary
            )
        )
    }

    private fun gmailEmailContentFingerprint(
        sbn: StatusBarNotification,
        content: ResolvedNotificationContent
    ): Int? {
        if (sbn.packageName !in GMAIL_PACKAGES ||
            sbn.notification.category != Notification.CATEGORY_EMAIL
        ) {
            return null
        }
        val notification = sbn.notification
        val actions = (notification.actions ?: emptyArray()).map { action ->
            listOf(
                action.semanticAction,
                action.actionIntent != null,
                !action.remoteInputs.isNullOrEmpty()
            )
        }
        return listOf(
            content.title.hashCode(),
            content.text.hashCode(),
            notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.hashCode(),
            actions
        ).hashCode()
    }

    private fun resolveMessageEventFingerprint(
        sbn: StatusBarNotification,
        eventScope: String,
        content: ResolvedNotificationContent,
        effectiveTitle: String,
        effectiveText: String,
        processingGeneration: Long,
        callbackObservedAt: Long,
        recovery: Boolean
    ): MessageEventFingerprint? {
        val notification = sbn.notification
        return messageEventTracker.resolve(
            sourceKey = eventScope,
            contentHash = listOf(effectiveTitle, effectiveText, content.messageCount).hashCode(),
            signals = MessageEventSignals(
                latestMessageTimestamp = content.latestMessageTimestamp,
                messageCount = content.messageCount,
                notificationWhen = notification.`when`.takeIf { it > 0L },
                notificationWhenIsReliable = notification.`when` > 0L,
                sourcePostTime = sbn.postTime.takeIf { it > 0L }
            ),
            callbackGeneration = processingGeneration,
            observedAt = callbackObservedAt,
            recovery = recovery
        )
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private suspend fun processStandardNotification(
        rawSbn: StatusBarNotification,
        sbn: StatusBarNotification,
        sourceSlot: String,
        recovery: Boolean = false,
        processingGeneration: Long,
        callbackObservedAt: Long
    ) {
        val manager = getSystemService(NotificationManager::class.java)
        val isSystemDndActive = manager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
        val dndActive = preferences.isDndModeEnabledSync() || isDndModeEnabled || 
                ((preferences.autoDetectDndSync() || autoDetectDnd) && isSystemDndActive)

        if (dndActive) {
            Log.d(TAG, "DND active. Skipping notification ${rawSbn.packageName}")
            return
        }

        try {
            val extras = sbn.notification.extras
            val resolvedContent = resolveNotificationContent(sbn)
            val typeBeforeRules = detectNotificationType(sbn)

            if (isJunkNotification(sbn, resolvedContent)) {
                DiagnosticsStore.record(typeBeforeRules.name, "ignored", sbn.packageName, "junk-or-empty")
                return
            }

            var effectiveTitle = resolvedContent.title
            val effectiveText = resolvedContent.text
            if (effectiveTitle.isEmpty()) {
                effectiveTitle = getCachedAppLabel(sbn.packageName)
            }

            val hasProgress = hasProgressNotification(sbn, effectiveTitle, effectiveText)
            if (effectiveTitle.isEmpty() && !hasProgress) return

            if (preferences.isBlockedTermFast(sbn.packageName, effectiveTitle, effectiveText)) return

            val activeTheme = themeRepository.activeTheme.value
            val ruleMatch = rulesEngine.match(sbn, effectiveTitle, effectiveText, activeTheme)

            val detectedType = if (ruleMatch?.targetLayout != null) {
                try { NotificationType.valueOf(ruleMatch.targetLayout) }
                catch (_: Exception) { typeBeforeRules }
            } else {
                typeBeforeRules
            }

            val effectiveTypes = getEffectiveTypes(sbn.packageName)
            val hasDirectMessagingStyle = extras.getString(Notification.EXTRA_TEMPLATE)
                ?.contains("MessagingStyle") == true
            val enabledTypeName = NotificationTypeEnablementPolicy.resolveEnabledType(
                effectiveTypes = effectiveTypes,
                detectedType = detectedType.name,
                hasDirectMessagingStyle = hasDirectMessagingStyle
            )
            if (enabledTypeName == null) {
                Log.d(TAG, " ABORTING: Type $detectedType disabled by user/theme for ${sbn.packageName}")
                DiagnosticsStore.record(detectedType.name, "ignored", sbn.packageName, "type-disabled")
                return
            }
            val type = NotificationType.valueOf(enabledTypeName)
            val isSavedScreenRecording = isSavedScreenRecordingNotification(sbn)
            val isMessagingLifecycle = isMessagingLifecycleEvent(sbn, type, resolvedContent)

            var effectiveKey = sbn.key
            var messageEventFingerprint: MessageEventFingerprint? = null
            var callSession: CallSession? = null
            var screenRecordingSession: ScreenRecordingSession? = null

            if (isSavedScreenRecording) {
                // Xiaomi reuses notification key/ID 111 for every completed recording. postTime is
                // the generation boundary, so a new saved file presents once while duplicate
                // callbacks for that same source generation remain idempotent.
                effectiveKey = ScreenRecordingSavedIdentity.logicalId(sbn.key, sbn.postTime)
            } else if (type == NotificationType.SCREEN_RECORDING) {
                val capabilities = screenRecordingControlBackend.probeCapabilities().also { probed ->
                    DiagnosticsStore.record(
                        classification = type.name,
                        action = "control-probed",
                        packageName = sbn.packageName,
                        reason = if (probed.canStop) "stop-available" else "visual-only"
                    )
                }
                val session = screenRecordingSessionTracker.resolve(
                    ScreenRecordingSessionInput(
                        sourceKey = sbn.key,
                        packageName = sbn.packageName,
                        sourcePostTime = sbn.postTime,
                        capabilities = capabilities
                    )
                )
                effectiveKey = session.logicalId
                screenRecordingSession = session
            } else if (type == NotificationType.CALL) {
                val signals = buildCallSignals(sbn)
                val classification = callClassifier.classify(signals)
                val now = System.currentTimeMillis()
                val session = callSessionTracker.resolve(
                    CallSessionInput(
                        sourceKey = sbn.key,
                        packageName = sbn.packageName,
                        notificationId = sbn.id,
                        notificationTag = sbn.tag,
                        groupKey = sbn.groupKey,
                        participantId = resolveCallParticipantId(sbn),
                        classification = classification,
                        showsChronometer = signals.showsChronometer,
                        chronometerBase = signals.whenTime,
                        observedAt = now,
                        isVideoCall = signals.isVideoCall
                    )
                )
                val participantPresent = !resolveCallParticipantId(sbn).isNullOrBlank()
                Log.d(
                    TAG,
                    "${if (signals.isVideoCall) "VIDEO CALL" else "CALL"} SIGNAL pkg=${sbn.packageName} " +
                            "sourceKeyHash=${sbn.key.hashCode()} id=${sbn.id} tagHash=${sbn.tag?.hashCode()} " +
                            "logicalIdHash=${session.logicalCallId.hashCode()} callType=${signals.callType} " +
                            "showsChronometer=${signals.showsChronometer} chronometerBase=${signals.whenTime} " +
                            "postTime=${sbn.postTime} groupHash=${sbn.groupKey?.hashCode()} " +
                            "ongoingFlag=${signals.isOngoingEvent} foreground=${signals.isForegroundService} " +
                            "video=${signals.isVideoCall} actionRoles=${classification.actionRoles} " +
                            "actionSemantics=${signals.actions.map { it.semanticAction }} " +
                            "remoteInput=${signals.actions.any { it.hasRemoteInput }} " +
                            "callPersonPresent=${if (participantPresent) "yes" else "no"} " +
                            "shortcutHash=${sbn.notification.shortcutId?.hashCode()} " +
                            "locusHash=${sbn.notification.locusId?.id?.hashCode()}"
                )
                Log.d(
                    TAG,
                    "${if (signals.isVideoCall) "VIDEO CALL" else "CALL"} TRANSITION " +
                            "previousState=${session.previousState} candidateState=${session.candidateState} " +
                            "resolvedState=${session.state} activeEvidence=${session.activeEvidence} " +
                            "connectedAtSource=${session.connectedAtSource} " +
                            "sourceReplacement=${if (session.sourceReplacement) "yes" else "no"} " +
                            "reason=${session.transitionReason}"
                )
                DiagnosticsStore.record(
                    classification = "CALL",
                    action = "classified",
                    packageName = sbn.packageName,
                    reason = classification.reason,
                    callState = session.state.name
                )
                effectiveKey = session.logicalCallId
                callSession = session
            }

            if (isMessagingLifecycle) {
                sourceToLogicalKeys[sbn.key]?.let { existingLogicalId ->
                    val existing = activeIslands[existingLogicalId]
                    if (existing?.messageEventFingerprint != null && existing.packageName == sbn.packageName) {
                        effectiveKey = existingLogicalId
                    }
                }
                if (effectiveKey == sbn.key) {
                    val identity = resolveMessageIdentity(sbn)
                    effectiveKey = identity.logicalId
                }

                messageEventFingerprint = resolveMessageEventFingerprint(
                    sbn = sbn,
                    eventScope = effectiveKey,
                    content = resolvedContent,
                    effectiveTitle = effectiveTitle,
                    effectiveText = effectiveText,
                    processingGeneration = processingGeneration,
                    callbackObservedAt = callbackObservedAt,
                    recovery = recovery
                )

                val identity = resolveMessageIdentity(sbn)
                val family = messageFamilyTracker.resolve(
                    MessagePresentationSource(
                        sourceKey = sbn.key,
                        packageName = sbn.packageName,
                        proposedLogicalId = identity.logicalId,
                        identitySource = identity.source,
                        notificationType = type,
                        groupKey = sbn.groupKey,
                        isGroupSummary = (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0,
                        hasMessagingStyle = hasDirectMessagingStyle,
                        eventFingerprint = messageEventFingerprint,
                        notificationTag = sbn.tag,
                        sourcePostTime = sbn.postTime,
                        contentFingerprint = gmailEmailContentFingerprint(sbn, resolvedContent),
                        isEmailCategory = sbn.notification.category == Notification.CATEGORY_EMAIL
                    )
                )

                effectiveKey = family.logicalId
                if (family.shouldPresent) {
                    messageEventFingerprint = family.eventFingerprint
                }

                val existingIsland = activeIslands[effectiveKey]
                if (existingIsland != null && !family.shouldPresent) {
                    sourceToLogicalKeys[sbn.key] = effectiveKey
                    return
                }
            }

            sourceToLogicalKeys[sbn.key] = effectiveKey
            removalJobs[effectiveKey]?.cancel()
            removalJobs.remove(effectiveKey)
            val previous = activeIslands[effectiveKey]

            if (callSession != null && !CallStageVisibilityPolicy.isVisible(
                    getEffectiveCallStages(sbn.packageName),
                    callSession.state
                )
            ) {
                suppressCallStagePresentation(
                    sbn = sbn,
                    logicalKey = effectiveKey,
                    session = callSession,
                    previous = previous
                )
                return
            }

            val isUpdate = previous != null
            var candidateBridgeId = previous?.id ?: effectiveKey.hashCode()

            if (isMessagingLifecycle && previous != null &&
                previous.sourceKey == sbn.key && sbn.postTime < previous.sourcePostTime
            ) {
                Log.d(TAG, "MESSAGE skip stale update logical=${effectiveKey.hashCode()}")
                return
            }

            val isSummary = (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0

            // --- LAYERED ENGINE LOGIC ---
            val useLiveUpdates = type != NotificationType.SCREEN_RECORDING &&
                    !isSavedScreenRecording &&
                    getEffectiveEngine(sbn.packageName)
            val appIslandConfig = preferences.getAppIslandConfigSync(sbn.packageName)
            val globalConfig = preferences.getGlobalConfigSync()
            val finalConfig = appIslandConfig.mergeWith(globalConfig).let { config ->
                config.copy(
                    timeout = ScreenRecordingTimeoutPolicy.resolve(
                        configuredTimeout = config.timeout,
                        systemScreenRecordingTimeout = preferences.getScreenRecordingTimeoutSync(),
                        isActiveRecording = type == NotificationType.SCREEN_RECORDING,
                        isSavedRecording = isSavedScreenRecording
                    )
                )
            }

            if (useLiveUpdates) {
                Log.i(TAG, " POSTING Native Live Update -> ID: $candidateBridgeId, Type: $type")
                val navLayout = if (type == NotificationType.NAVIGATION) getEffectiveNav(sbn.packageName) else null

                val builder = liveUpdateTranslator.translateToLiveUpdate(
                    sbn = sbn,
                    channelId = LIVE_UPDATE_CHANNEL_ID,
                    type = type,
                    navRight = navLayout?.second,
                    config = finalConfig
                )

                builder.extras.putString(EXTRA_ORIGINAL_KEY, sbn.key)

                val actualProgress = extras.getInt(Notification.EXTRA_PROGRESS, 0)
                val actualMax = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0)
                val isIndeterminate = extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false)
                val actionState = sbn.notification.actions?.joinToString { it.title?.toString() ?: "" } ?: ""

                val newContentHash = effectiveTitle.hashCode() * 31 +
                        effectiveText.hashCode() + actualProgress + actualMax +
                        isIndeterminate.hashCode() + actionState.hashCode()

                if (shouldSuppressExpiredSource(sbn, type, effectiveKey, newContentHash, recovery, messageEventFingerprint)) {
                    return
                }

                val decision = IslandUpdateResolver.decide(
                    logicalId = effectiveKey,
                    candidateBridgeId = candidateBridgeId,
                    contentHash = newContentHash,
                    previous = previous?.let { PreviousIslandPresentation(it.logicalId, it.id, it.lastContentHash, it.messageEventFingerprint) },
                    notificationType = type,
                    isMessagingEvent = type == NotificationType.MESSAGE,
                    messageEventFingerprint = messageEventFingerprint
                )

                if (decision.kind == IslandPresentationKind.UNCHANGED) {
                    return
                }

                builder.setOnlyAlertOnce(decision.onlyAlertOnce)

                val hasPermission = com.d4viddf.hyperbridge.util.XiaomiNotificationHelper.hasFocusPermission(this)
                if (!hasPermission && com.d4viddf.hyperbridge.util.XiaomiNotificationHelper.isSupportIsland()) {
                    serviceScope.launch {
                        preferences.setFeaturedPermissionWarning(true)
                    }
                    val intent = Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        putExtra("open_troubleshoot", true)
                    }
                    val pendingIntent = PendingIntent.getActivity(
                        this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    builder.addAction(
                        android.R.drawable.ic_dialog_info,
                        getString(R.string.troubleshoot_featured_notification),
                        pendingIntent
                    )
                }

                val notification = builder.build()

                if (decision.cancelBeforeNotify) {
                    internalBridgeReplacements.mark(decision.bridgeId, effectiveKey, processingGeneration, System.currentTimeMillis())
                    NotificationManagerCompat.from(this).cancel(decision.bridgeId)
                }

                if (!decision.onlyAlertOnce) {
                    ShizukuManager.notify(this, decision.bridgeId, notification)
                } else {
                    NotificationManagerCompat.from(this).notify(decision.bridgeId, notification)
                }

                expiredIslands.acceptNewGeneration(
                    sbn.key,
                    sourceGenerationFingerprint(newContentHash, sbn.postTime),
                    messageEventFingerprint,
                    effectiveKey
                )

                activeTranslations[effectiveKey] = decision.bridgeId
                reverseTranslations[decision.bridgeId] = effectiveKey
                activeIslands[effectiveKey] = ActiveIsland(
                    id = decision.bridgeId,
                    type = type,
                    postTime = System.currentTimeMillis(),
                    sourcePostTime = sbn.postTime,
                    packageName = sbn.packageName,
                    sourceKey = sbn.key,
                    logicalId = effectiveKey,
                    groupKey = sbn.groupKey,
                    isGroupSummary = isSummary,
                    generation = processingGeneration,
                    title = effectiveTitle,
                    text = effectiveText,
                    subText = "LiveUpdate",
                    lastContentHash = newContentHash,
                    messageEventFingerprint = messageEventFingerprint,
                    deleteIntent = sbn.notification.deleteIntent
                )
                updatePermanentIsland()

                handlePostNotificationSideEffects(effectiveKey, decision.bridgeId, processingGeneration, finalConfig, type, true, sbn, effectiveTitle, effectiveText)
                return
            }

            // --- LAYERED CUSTOM ISLAND LOGIC ---
            val picKey = "pic_${candidateBridgeId}"
            val data: HyperIslandData = if (isSavedScreenRecording) {
                screenRecordingSavedTranslator.translate(sbn, picKey, finalConfig, activeTheme)
            } else when (type) {
                NotificationType.CALL -> callTranslator.translate(
                    sbn, picKey, finalConfig, activeTheme,
                    requireNotNull(callSession), isUpdate,
                    resolvedTitle = effectiveTitle
                )
                NotificationType.NAVIGATION -> {
                    val navLayout = getEffectiveNav(sbn.packageName)
                    navTranslator.translate(sbn, picKey, finalConfig, navLayout.first, navLayout.second, activeTheme)
                }
                NotificationType.TIMER -> timerTranslator.translate(sbn, picKey, finalConfig, activeTheme)
                NotificationType.PROGRESS -> progressTranslator.translate(sbn, effectiveTitle, picKey, finalConfig, activeTheme, isUpdate)
                NotificationType.DOWNLOAD -> downloadTranslator.translate(sbn, effectiveTitle, picKey, finalConfig, activeTheme, isUpdate)
                NotificationType.MEDIA -> mediaTranslator.translate(sbn, picKey, finalConfig)
                NotificationType.SCREEN_RECORDING -> screenRecordingTranslator.translate(
                    requireNotNull(screenRecordingSession),
                    design = preferences.getScreenRecordingDesignSync()
                )
                NotificationType.MESSAGE -> messageTranslator.translate(sbn, effectiveTitle, effectiveText, picKey, finalConfig, activeTheme, isUpdate)
                else -> standardTranslator.translate(sbn, effectiveTitle, effectiveText, picKey, finalConfig, activeTheme)
            }

            val newContentHash = if (type == NotificationType.SCREEN_RECORDING && screenRecordingSession != null) {
                ScreenRecordingSemanticFingerprint.compute(
                    screenRecordingSession,
                    preferences.getScreenRecordingDesignSync()
                )
            } else {
                val normalizedJson = RenderedJsonNormalizer.normalize(data.jsonParam)
                normalizedJson?.hashCode() ?: data.jsonParam.hashCode()
            }

            if (shouldSuppressExpiredSource(sbn, type, effectiveKey, newContentHash, recovery, messageEventFingerprint)) {
                return
            }

            val decision = IslandUpdateResolver.decide(
                logicalId = effectiveKey,
                candidateBridgeId = candidateBridgeId,
                contentHash = newContentHash,
                previous = previous?.let { PreviousIslandPresentation(it.logicalId, it.id, it.lastContentHash, it.messageEventFingerprint) },
                notificationType = type,
                isMessagingEvent = type == NotificationType.MESSAGE,
                messageEventFingerprint = messageEventFingerprint
            )

            if (decision.kind == IslandPresentationKind.UNCHANGED) {
                return
            }

            if (decision.cancelBeforeNotify) {
                internalBridgeReplacements.mark(decision.bridgeId, effectiveKey, processingGeneration, System.currentTimeMillis())
                NotificationManagerCompat.from(this).cancel(decision.bridgeId)
            }

            Log.i(TAG, " POSTING Island -> ID: ${decision.bridgeId}, Type: $type, FinalTitle: '$effectiveTitle', FinalText: '$effectiveText'")
            postStandardNotification(
                sbn = sbn,
                bridgeId = decision.bridgeId,
                data = data,
                shouldAlertOnce = decision.onlyAlertOnce,
                suppressContentIntent = isSavedScreenRecording
            )

            expiredIslands.acceptNewGeneration(
                sbn.key,
                sourceGenerationFingerprint(newContentHash, sbn.postTime),
                messageEventFingerprint,
                effectiveKey
            )

            activeTranslations[effectiveKey] = decision.bridgeId
            reverseTranslations[decision.bridgeId] = effectiveKey
            activeIslands[effectiveKey] = ActiveIsland(
                id = decision.bridgeId,
                type = type,
                postTime = System.currentTimeMillis(),
                sourcePostTime = sbn.postTime,
                packageName = sbn.packageName,
                sourceKey = sbn.key,
                logicalId = effectiveKey,
                groupKey = sbn.groupKey,
                isGroupSummary = isSummary,
                generation = processingGeneration,
                title = effectiveTitle,
                text = effectiveText,
                subText = "",
                lastContentHash = newContentHash,
                messageEventFingerprint = messageEventFingerprint,
                callSession = callSession,
                screenRecordingSession = screenRecordingSession,
                deleteIntent = sbn.notification.deleteIntent,
                dismissSourceOnContentClick = false
            )
            updatePermanentIsland()

            handlePostNotificationSideEffects(
                originalKey = effectiveKey,
                bridgeId = decision.bridgeId,
                generation = processingGeneration,
                config = finalConfig,
                type = type,
                isLiveUpdate = false,
                sbn = sbn,
                title = effectiveTitle,
                text = effectiveText,
                forceLifecycleTimeout = isSavedScreenRecording
            )

        } catch (e: Exception) {
            Log.e(TAG, "💥 Error processing standard notification", e)
        }
    }

    private fun isDownloadNotification(sbn: StatusBarNotification, title: String, text: String): Boolean {
        val pkg = sbn.packageName.lowercase()
        val titleLower = title.lowercase()
        val textLower = text.lowercase()
        val channelId = sbn.notification.channelId?.lowercase() ?: ""
        
        val isMatch = if (pkg.contains("download") || pkg.contains("downloader") || pkg.contains("chrome") || 
            pkg.contains("browser") || pkg.contains("firefox") || pkg.contains("market") || 
            pkg.contains("vending") || pkg.contains("play.store") || pkg.contains("playstore") || 
            pkg.contains("store") || pkg.contains("fdroid") || pkg.contains("samsungapps") || 
            pkg.contains("mipicks") || pkg.contains("venezia") || pkg.contains("packageinstaller") || 
            pkg.contains("installer") || pkg.contains("gms") || channelId.contains("download") || 
            channelId.contains("install")) {
            true
        } else {
            val extras = sbn.notification.extras
            val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.lowercase() ?: ""
            val infoText = extras.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString()?.lowercase() ?: ""
            
            val downloadKeywords = listOf(
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
            downloadKeywords.any { 
                titleLower.contains(it) || 
                textLower.contains(it) || 
                subText.contains(it) || 
                infoText.contains(it) 
            }
        }

        Log.d(TAG, "🔍 isDownloadNotification check: pkg=$pkg, channelId='$channelId', title='$title', text='$text', resolved=$isMatch")
        return isMatch
    }

    private fun hasProgressNotification(sbn: StatusBarNotification, title: String, text: String): Boolean {
        val extras = sbn.notification.extras
        val isDownload = isDownloadNotification(sbn, title, text)
        val isOngoing = (sbn.notification.flags and Notification.FLAG_ONGOING_EVENT) != 0
        return extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0) > 0 ||
                extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE) ||
                (isDownload && extractTextPercentage(title, text) != null) ||
                (isDownload && isOngoing)
    }

    private fun extractTextPercentage(title: String?, text: String?): Int? {
        val pattern = Regex("""\b(\d{1,3})\s*%""")
        val textMatch = text?.let { pattern.find(it) }
        val titleMatch = title?.let { pattern.find(it) }
        val match = textMatch ?: titleMatch
        if (match != null) {
            val value = match.groupValues[1].toIntOrNull()
            if (value != null && value in 0..100) {
                return value
            }
        }
        return null
    }

    private fun resolveTitle(sbn: StatusBarNotification): String {
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim() ?: ""
        val bigTitle = extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString()?.trim()
        val pkg = sbn.packageName

        if ((title.isEmpty() || title.equals(pkg, ignoreCase = true)) && !bigTitle.isNullOrEmpty()) {
            return bigTitle
        }
        if (title.equals(pkg, ignoreCase = true)) return ""
        return title
    }

    private fun resolveText(extras: Bundle): String {
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim()

        if (!text.isNullOrEmpty()) return text
        return bigText ?: ""
    }

    private fun buildCallSignals(sbn: StatusBarNotification): CallNotificationSignals {
        val n = sbn.notification
        val extras = n.extras
        val callType = try {
            if (extras.containsKey(Notification.EXTRA_CALL_TYPE)) {
                extras.getInt(Notification.EXTRA_CALL_TYPE, CallNotificationClassifier.CALL_TYPE_UNKNOWN)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }

        return CallNotificationSignals(
            category = n.category,
            template = extras.getString(Notification.EXTRA_TEMPLATE),
            callType = callType,
            showsChronometer = extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER),
            whenTime = n.`when`,
            actions = (n.actions ?: emptyArray()).map {
                CallActionSignal(
                    title = it.title?.toString().orEmpty(),
                    semanticAction = it.semanticAction,
                    hasPendingIntent = it.actionIntent != null,
                    hasRemoteInput = !it.remoteInputs.isNullOrEmpty()
                )
            },
            isOngoingEvent = (n.flags and Notification.FLAG_ONGOING_EVENT) != 0,
            isForegroundService = (n.flags and Notification.FLAG_FOREGROUND_SERVICE) != 0,
            isVideoCall = extras.getBoolean(Notification.EXTRA_CALL_IS_VIDEO, false)
        )
    }

    private fun resolveCallParticipantId(sbn: StatusBarNotification): String? {
        val extras = sbn.notification.extras
        val person = try {
            extras.getParcelable(Notification.EXTRA_CALL_PERSON, Person::class.java)
                ?: extras.getParcelable(Notification.EXTRA_MESSAGING_PERSON, Person::class.java)
                ?: extras.getParcelableArrayList(Notification.EXTRA_PEOPLE_LIST, Person::class.java)?.firstOrNull()
        } catch (_: Exception) {
            null
        }
        val identity = person?.let(::personIdentity)
        return identity?.hashCode()?.toUInt()?.toString(16)
    }

    private fun personIdentity(person: Person): String? {
        return person.key?.takeIf { it.isNotBlank() }
            ?: person.uri?.takeIf { it.isNotBlank() }
            ?: person.name?.toString()?.takeIf { it.isNotBlank() }
    }

    private fun suppressCallStagePresentation(
        sbn: StatusBarNotification,
        logicalKey: String,
        session: CallSession,
        previous: ActiveIsland?
    ) {
        if (previous?.type == NotificationType.CALL) {
            activeTranslations[logicalKey]?.let { bridgeId ->
                try {
                    NotificationManagerCompat.from(this).cancel(bridgeId)
                } catch (_: Exception) {}
            }
            cleanupCache(logicalKey, preserveCallSession = true)
        }
        sourceToLogicalKeys[sbn.key] = logicalKey
        Log.d(
            TAG,
            "CALL STAGE HIDDEN pkg=${sbn.packageName} logicalIdHash=${logicalKey.hashCode()} " +
                    "state=${session.state} stage=${CallStageVisibilityPolicy.stageFor(session.state)}"
        )
        DiagnosticsStore.record(
            classification = "CALL",
            action = "stage-hidden",
            packageName = sbn.packageName,
            reason = CallStageVisibilityPolicy.stageFor(session.state)?.name,
            callState = session.state.name
        )
    }

    private suspend fun ensureValidSbn(sbn: StatusBarNotification): StatusBarNotification {
        val extras = sbn.notification.extras
        val title = resolveTitle(sbn)
        val text = resolveText(extras)
        val hasProgress = hasProgressNotification(sbn, title, text)
        if (hasProgress) return sbn

        val pkg = sbn.packageName

        val isSuspicious = title.isEmpty() || text.equals(pkg, ignoreCase = true)

        if (isSuspicious) {
            delay(150.milliseconds)
            try {
                val activeList = activeNotifications
                val updatedSbn = activeList?.firstOrNull { it.key == sbn.key }
                if (updatedSbn != null) return updatedSbn
            } catch (_: Exception) { }
        }
        return sbn
    }

    private fun detectNotificationType(sbn: StatusBarNotification): NotificationType {
        val n = sbn.notification
        val extras = n.extras
        val template = extras.getString(Notification.EXTRA_TEMPLATE) ?: ""
        val isCall = callClassifier.classify(buildCallSignals(sbn)).isCall
        val isNav = n.category == Notification.CATEGORY_NAVIGATION || sbn.packageName.let { it.contains("maps") || it.contains("waze") }
        val isTimer = (extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER) || n.category == Notification.CATEGORY_ALARM) && n.`when` > 0
        val isMedia = template.contains("MediaStyle") || n.category == Notification.CATEGORY_TRANSPORT
        val isMessage = n.category == Notification.CATEGORY_MESSAGE || template == "android.app.Notification.MessagingStyle"
        
        val title = resolveTitle(sbn)
        val text = resolveText(extras)
        val isDownload = isDownloadNotification(sbn, title, text)
        val hasProgress = hasProgressNotification(sbn, title, text)
        val isScreenRecording = ScreenRecordingClassifier.isScreenRecording(
            ScreenRecordingSignals(
                packageName = sbn.packageName,
                notificationId = sbn.id,
                channelId = n.channelId,
                isOngoing = (n.flags and Notification.FLAG_ONGOING_EVENT) != 0,
                isForegroundService = (n.flags and Notification.FLAG_FOREGROUND_SERVICE) != 0,
                isGroupSummary = (n.flags and Notification.FLAG_GROUP_SUMMARY) != 0
            )
        )

        return when {
            isScreenRecording -> NotificationType.SCREEN_RECORDING
            isCall -> NotificationType.CALL
            isNav -> NotificationType.NAVIGATION
            isTimer -> NotificationType.TIMER
            isMedia -> NotificationType.MEDIA
            isMessage -> NotificationType.MESSAGE
            hasProgress -> {
                if (isDownload) {
                    NotificationType.DOWNLOAD
                } else {
                    NotificationType.PROGRESS
                }
            }
            else -> NotificationType.STANDARD
        }
    }

    private fun isSavedScreenRecordingNotification(sbn: StatusBarNotification): Boolean {
        val notification = sbn.notification
        return ScreenRecordingClassifier.isSavedScreenRecording(
            ScreenRecordingSignals(
                packageName = sbn.packageName,
                notificationId = sbn.id,
                channelId = notification.channelId,
                isOngoing = (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0,
                isForegroundService = (notification.flags and Notification.FLAG_FOREGROUND_SERVICE) != 0,
                isGroupSummary = (notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0
            )
        )
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun postStandardNotification(
        sbn: StatusBarNotification,
        bridgeId: Int,
        data: HyperIslandData,
        shouldAlertOnce: Boolean,
        suppressContentIntent: Boolean = false
    ) {
        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notification_went_wrong))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(shouldAlertOnce)

        val extras = Bundle()
        extras.putString(EXTRA_ORIGINAL_KEY, sbn.key)
        builder.addExtras(extras)
        builder.addExtras(data.resources)

        val hasPermission = com.d4viddf.hyperbridge.util.XiaomiNotificationHelper.hasFocusPermission(this)
        if (!hasPermission) {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("open_troubleshoot", true)
            }
            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.setContentIntent(pendingIntent)
            builder.addAction(
                android.R.drawable.ic_dialog_info,
                getString(R.string.troubleshoot_featured_notification),
                pendingIntent
            )
        } else if (!suppressContentIntent) {
            sbn.notification.contentIntent?.let { originalIntent ->
                if (detectNotificationType(sbn) == NotificationType.MESSAGE) {
                    val clickIntent = Intent("com.d4viddf.hyperbridge.ISLAND_CLICKED").apply {
                        setPackage(packageName)
                        putExtra("sbn_key", sbn.key)
                        putExtra("bridge_id", bridgeId)
                        putExtra("original_intent", originalIntent)
                    }
                    val clickPendingIntent = PendingIntent.getBroadcast(
                        this,
                        bridgeId,
                        clickIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    builder.setContentIntent(clickPendingIntent)
                } else {
                    builder.setContentIntent(originalIntent)
                }
            }
        }

        val notification = builder.build()
        notification.extras.putString("miui.focus.param", data.jsonParam)

        if (!shouldAlertOnce) {
            ShizukuManager.notifyWithCancel(this, bridgeId, notification)
        } else {
            NotificationManagerCompat.from(this).notify(bridgeId, notification)
        }

        activeTranslations[sbn.key] = bridgeId
        reverseTranslations[bridgeId] = sbn.key
    }

    // =========================================================================
    //  HELPERS & SETUP
    // =========================================================================

    private fun createChannels() {
        val manager = getSystemService(NotificationManager::class.java)
        val notifChannel = NotificationChannel(NOTIFICATION_CHANNEL_ID, getString(R.string.channel_active_islands), NotificationManager.IMPORTANCE_HIGH).apply {
            setSound(null, null); enableVibration(false); setShowBadge(false)
        }
        manager.createNotificationChannel(notifChannel)

        val widgetChannel = NotificationChannel(WIDGET_CHANNEL_ID, "Widgets Overlay", NotificationManager.IMPORTANCE_LOW).apply {
            setSound(null, null); enableVibration(false); setShowBadge(false)
        }
        manager.createNotificationChannel(widgetChannel)

        val liveUpdateChannel = NotificationChannel(LIVE_UPDATE_CHANNEL_ID, getString(R.string.channel_live_updates), NotificationManager.IMPORTANCE_DEFAULT).apply {
            setSound(null, null); enableVibration(false); setShowBadge(false)
        }
        manager.createNotificationChannel(liveUpdateChannel)

        val watchRelayChannel = NotificationChannel(WATCH_RELAY_CHANNEL_ID, "Watch Relay", NotificationManager.IMPORTANCE_LOW).apply {
            setSound(null, null); enableVibration(false); setShowBadge(false)
        }
        manager.createNotificationChannel(watchRelayChannel)
    }

    private fun shouldProcessWidgetUpdate(widgetId: Int, config: WidgetConfig): Boolean {
        val now = System.currentTimeMillis()
        val lastTime = widgetUpdateDebouncer[widgetId] ?: 0L
        val throttleTime = if (config.renderMode == WidgetRenderMode.SNAPSHOT) 1500L else 200L
        if (now - lastTime < throttleTime) return false
        widgetUpdateDebouncer[widgetId] = now
        return true
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private suspend fun processSingleWidget(widgetId: Int, config: WidgetConfig) {
        try {
            val data = widgetTranslator.translate(widgetId)
            postWidgetNotification(WIDGET_ID_BASE + widgetId, data)
            activeWidgets.add(widgetId)
            updatePermanentIsland()
        } catch (e: Exception) { Log.e(TAG, "Failed widget $widgetId", e) }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun postWidgetNotification(notificationId: Int, data: HyperIslandData) {
        val builder = NotificationCompat.Builder(this, WIDGET_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Widget Overlay").setContentText(getString(R.string.widget_went_wrong))
            .setPriority(NotificationCompat.PRIORITY_LOW).setOngoing(true)
            .setOnlyAlertOnce(true).addExtras(data.resources)

        val intent = Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        builder.setContentIntent(pendingIntent)

        val notification = builder.build()
        notification.extras.putString("miui.focus.param", data.jsonParam)
        ShizukuManager.notify(this, notificationId, notification)
    }

    private fun handleLimitReached(newType: NotificationType, newPkg: String) {
        val oldest = activeIslands.minByOrNull { it.value.postTime } ?: return

        val mode = preferences.getLimitModeSync()
        when (mode) {
            IslandLimitMode.FIRST_COME -> {
                // Ignore the new notification by removing it immediately (or simply returning, but returning here means the caller won't add it)
                return
            }
            IslandLimitMode.MOST_RECENT -> {
                NotificationManagerCompat.from(this).cancel(oldest.value.id)
                cleanupCache(oldest.key)
            }
            IslandLimitMode.PRIORITY -> {
                // Check if newPkg has higher priority than existing ones.
                // Priority is determined by its index in appPriorityList (lower index = higher priority).
                // If it's not in the list, it has the lowest priority (Int.MAX_VALUE).
                val newPriority = preferences.getAppPriorityFast(newPkg).let {
                    if (it == Int.MAX_VALUE && appPriorityList.isNotEmpty()) {
                        appPriorityList.indexOf(newPkg).let { idx -> if (idx == -1) Int.MAX_VALUE else idx }
                    } else it
                }
                
                // Find the existing active island with the lowest priority (highest index value)
                val lowestPriorityIsland = activeIslands.maxByOrNull {
                    val pkg = it.value.packageName
                    preferences.getAppPriorityFast(pkg).let { p ->
                        if (p == Int.MAX_VALUE && appPriorityList.isNotEmpty()) {
                            appPriorityList.indexOf(pkg).let { idx -> if (idx == -1) Int.MAX_VALUE else idx }
                        } else p
                    }
                }

                if (lowestPriorityIsland != null) {
                    val lowestPkg = lowestPriorityIsland.value.packageName
                    val lowestPriority = preferences.getAppPriorityFast(lowestPkg).let { p ->
                        if (p == Int.MAX_VALUE && appPriorityList.isNotEmpty()) {
                            appPriorityList.indexOf(lowestPkg).let { idx -> if (idx == -1) Int.MAX_VALUE else idx }
                        } else p
                    }
                    if (newPriority <= lowestPriority) {
                        // The new notification has equal or higher priority than the lowest existing one.
                        // Remove the lowest priority existing notification.
                        NotificationManagerCompat.from(this).cancel(lowestPriorityIsland.value.id)
                        cleanupCache(lowestPriorityIsland.key)
                    } else {
                        // The new notification has lower priority than all existing ones. Do nothing, which will ignore it.
                        return
                    }
                }
            }
        }
    }

    private fun isJunkNotification(sbn: StatusBarNotification, resolvedContent: ResolvedNotificationContent? = null): Boolean {
        val notification = sbn.notification
        val extras = notification.extras
        val pkg = sbn.packageName

        val title = (resolvedContent?.title ?: extras.getCharSequence(Notification.EXTRA_TITLE)?.toString())?.trim() ?: ""
        val text = (resolvedContent?.text ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString())?.trim() ?: ""

        val hasProgress = hasProgressNotification(sbn, title, text)
        val isSpecial = notification.category == Notification.CATEGORY_TRANSPORT || callClassifier.classify(buildCallSignals(sbn)).isCall ||
                notification.category == Notification.CATEGORY_NAVIGATION || extras.getString(Notification.EXTRA_TEMPLATE)?.contains("MediaStyle") == true
        if (hasProgress || isSpecial) return false
        if (title.isEmpty() && text.isEmpty()) return true
        if (title.equals(pkg, ignoreCase = true) || text.equals(pkg, ignoreCase = true)) return true
        val blockedTerms = preferences.getGlobalBlockedTermsSync().ifEmpty { globalBlockedTerms }
        if (blockedTerms.any { "$title $text".contains(it, true) }) return true

        if ((notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) {
            val type = detectNotificationType(sbn)
            if (type != NotificationType.MESSAGE) return true
            if (text.isEmpty() || title.isEmpty()) return true
            // A summary with live children is a duplicate: messaging apps post the real
            // per-conversation notification plus an "N new messages" summary. With
            // "remove original notification" disabled the summary survives and would
            // become a second island. Only islandify a summary that stands alone
            // (some apps post only the summary).
            val group = notification.group
            if (group != null) {
                val hasLiveChild = try {
                    activeNotifications?.any {
                        it.packageName == pkg && it.key != sbn.key &&
                            (it.notification.flags and Notification.FLAG_GROUP_SUMMARY) == 0 &&
                            it.notification.group == group
                    } == true
                } catch (_: Exception) { false }
                if (hasLiveChild) return true
            }
        }

        return false
    }

    private fun getCachedAppLabel(pkg: String): String = appLabelCache.getOrPut(pkg) {
        try { packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString() } catch (_: Exception) { "" }
    }

    private fun shouldIgnore(packageName: String): Boolean = packageName == this.packageName || packageName == "android" || packageName.contains("miui.notification")
    private fun isAppAllowed(packageName: String): Boolean =
        preferences.isAppAllowedSync(packageName) || allowedPackageSet.contains(packageName)

    private var syncJob: Job? = null

    override fun onListenerConnected() { 
        Log.i(TAG, "HyperBridge Service Connected")
        isConnected = true
        DiagnosticsStore.setServiceConnected(true)
        syncNotifications(refresh = true)
        syncJob?.cancel()
        syncJob = serviceScope.launch {
            while (true) {
                delay(60_000) // 1 minute periodic sync
                // Screen off: nothing to keep in sync visually, and SCREEN_ON runs a full
                // refresh sync on wake — skip the tick instead of waking up all night.
                if (isScreenOn) {
                    syncNotifications()
                }
            }
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.i(TAG, "HyperBridge Service Disconnected")
        isConnected = false
        DiagnosticsStore.setServiceConnected(false)
    }

    private fun syncNotifications(refresh: Boolean = false) {
        val now = System.currentTimeMillis()
        recentlyRemovedKeys.entries.removeIf { now - it.value.observedAt > 10000 }
        callSessionTracker.pruneStale(now)

        serviceScope.launch(Dispatchers.IO) {
            try {
                val currentNotifications = activeNotifications ?: return@launch
                val systemNotificationKeys = currentNotifications.map { it.key }.toSet()

                var nativeChanged = false
                for (sbn in currentNotifications) {
                    if (sbn.packageName != packageName) {
                        val extras = sbn.notification.extras
                        var isNative = false
                        if (extras != null) {
                            if (extras.containsKey("miui.focus.param") || extras.containsKey("miui.system.focus.param")) {
                                isNative = true
                            }
                            val template = extras.getString(Notification.EXTRA_TEMPLATE)
                            if (template == "androidx.media.app.NotificationCompat\$MediaStyle" ||
                                template == "android.app.Notification\$MediaStyle") {
                                isNative = true
                            }
                        }
                        if (isNative) {
                            if (nativeIslands.add(sbn.key)) nativeChanged = true
                        } else {
                            if (nativeIslands.remove(sbn.key)) nativeChanged = true
                        }
                    }
                }
                val currentNatives = nativeIslands.toList()
                for (key in currentNatives) {
                    if (!systemNotificationKeys.contains(key)) {
                        if (nativeIslands.remove(key)) nativeChanged = true
                    }
                }
                if (nativeChanged) updatePermanentIsland()

                val currentKeys = currentNotifications.map { it.key }.toSet()
                
                val keysToRemove = mutableListOf<String>()
                for ((originalKey, activeIsland) in activeIslands) {
                    if (!currentKeys.contains(originalKey)) {
                        val appConfig = preferences.getAppIslandConfigSync(activeIsland.packageName)
                        val globalConfig = preferences.getGlobalConfigSync()
                        val finalConfig = appConfig.mergeWith(globalConfig)

                        val forceDismiss = activeIsland.type == NotificationType.CALL || 
                                           activeIsland.type == NotificationType.MEDIA || 
                                           activeIsland.type == NotificationType.NAVIGATION

                        // If the app intentionally removes the original notification, it's expected to be missing from currentKeys.
                        if (!forceDismiss && finalConfig.removeOriginalNotification == true) {
                            continue
                        }

                        if (finalConfig.dismissWithOriginal == true || forceDismiss) {
                            keysToRemove.add(originalKey)
                        }
                    }
                }

                for (key in keysToRemove) {
                    Log.d(TAG, "Sync: Found stuck notification $key, removing.")
                    val hyperId = activeTranslations[key]
                    if (hyperId != null) {
                        try {
                            NotificationManagerCompat.from(this@NotificationReaderService).cancel(hyperId)
                        } catch (_: Exception) {}
                    }
                    cleanupCache(key)
                }

                // Bridged notifications we no longer track (e.g. left over from a service restart)
                // keep their island slot occupied forever, since island-swipe never removes them.
                for (sbn in currentNotifications) {
                    if (sbn.packageName != packageName) continue
                    val id = sbn.id
                    if (id == PermanentIslandManager.PERMANENT_BRIDGE_ID) continue
                    // The VPN controller deliberately owns its notification outside the ordinary
                    // source-to-translation maps. Do not mistake it for an orphan during the
                    // reconciliation pass and cancel its backing island.
                    if (id == VpnIslandController.NOTIFICATION_ID) continue
                    if (id >= WIDGET_ID_BASE) continue
                    if (id in (WATCH_RELAY_ID_BASE - 0x0F)..WATCH_RELAY_ID_BASE) continue
                    if ((sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) continue
                    if (reverseTranslations.containsKey(id)) continue
                    if (System.currentTimeMillis() - sbn.postTime < 5000) continue
                    Log.d(TAG, "Sync: Reaping orphan bridge notification $id")
                    try {
                        NotificationManagerCompat.from(this@NotificationReaderService).cancel(id)
                    } catch (_: Exception) {}
                }

                val islandPresent = currentNotifications.any {
                    it.packageName == packageName && it.id == PermanentIslandManager.PERMANENT_BRIDGE_ID
                }
                permanentIslandManager.reconcile(
                    activeIslandCount(),
                    nativeIslands.isNotEmpty(),
                    islandPresent,
                    refresh
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing notifications", e)
            }
        }
    }

    private fun isSourceNotificationActive(sourceKey: String): Boolean {
        return try {
            activeNotifications?.any { it.key == sourceKey } == true
        } catch (_: Exception) {
            false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isConnected = false
        DiagnosticsStore.setServiceConnected(false)
        if (::vpnIslandController.isInitialized) vpnIslandController.stop()
        unregisterReceiver(systemReceiver)
        unregisterReceiver(islandClickReceiver)
        syncJob?.cancel()
        callSessionTracker.clear()
        screenRecordingSessionTracker.clear()
        messageFamilyTracker.clear()
        messageEventTracker.clear()
        serviceScope.cancel() 
    }
}