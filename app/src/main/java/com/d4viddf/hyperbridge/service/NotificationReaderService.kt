package com.d4viddf.hyperbridge.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
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
import com.d4viddf.hyperbridge.data.theme.RulesEngine
import com.d4viddf.hyperbridge.data.theme.ThemeRepository
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
import com.d4viddf.hyperbridge.service.translators.NavTranslator
import com.d4viddf.hyperbridge.service.translators.ProgressTranslator
import com.d4viddf.hyperbridge.service.translators.DownloadTranslator
import com.d4viddf.hyperbridge.service.translators.StandardTranslator
import com.d4viddf.hyperbridge.service.translators.TimerTranslator
import com.d4viddf.hyperbridge.service.translators.WidgetTranslator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class NotificationReaderService : NotificationListenerService() {

    companion object {
        const val ACTION_RELOAD_THEME = "com.d4viddf.hyperbridge.ACTION_RELOAD_THEME"
    }

    private val TAG = "HyperBridgeDebug"
    private val EXTRA_ORIGINAL_KEY = "hyper_original_key"

    // --- CHANNELS ---
    private val NOTIFICATION_CHANNEL_ID = "hyper_bridge_notification_channel"
    private val WIDGET_CHANNEL_ID = "hyper_bridge_widget_channel"
    private val LIVE_UPDATE_CHANNEL_ID = "hyper_bridge_live_update_channel"
    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())

    // --- STATE & CONFIG ---
    private var allowedPackageSet: Set<String> = emptySet()
    private var currentMode = IslandLimitMode.MOST_RECENT
    private var appPriorityList = emptyList<String>()
    private var globalBlockedTerms: Set<String> = emptySet()
    
    private var isDndModeEnabled = false
    private var autoDetectDnd = true

    // --- CACHES ---
    private val activeIslands = ConcurrentHashMap<String, ActiveIsland>()
    private val activeTranslations = ConcurrentHashMap<String, Int>()
    private val reverseTranslations = ConcurrentHashMap<Int, String>()
    private val processingJobs = ConcurrentHashMap<String, Job>()
    private val timeoutJobs = ConcurrentHashMap<String, Job>()
    private lateinit var permanentIslandManager: PermanentIslandManager
    private val intentionallyRemovedKeys = ConcurrentHashMap.newKeySet<String>()
    private val widgetUpdateDebouncer = ConcurrentHashMap<Int, Long>()
    private val dismissedWidgetIds = ConcurrentHashMap.newKeySet<Int>()
    private val activeWidgets = ConcurrentHashMap.newKeySet<Int>()
    private val appLabelCache = ConcurrentHashMap<String, String>()

    private val MAX_ISLANDS = 9
    private val WIDGET_ID_BASE = 9000

    private lateinit var preferences: AppPreferences

    // --- THEME ENGINE ---
    private lateinit var themeRepository: ThemeRepository
    private lateinit var rulesEngine: RulesEngine

    // Translators
    private lateinit var callTranslator: CallTranslator
    private lateinit var navTranslator: NavTranslator
    private lateinit var timerTranslator: TimerTranslator
    private lateinit var progressTranslator: ProgressTranslator
    private lateinit var downloadTranslator: DownloadTranslator
    private lateinit var standardTranslator: StandardTranslator
    private lateinit var mediaTranslator: MediaTranslator
    private lateinit var widgetTranslator: WidgetTranslator
    private lateinit var liveUpdateTranslator: LiveUpdateTranslator

    private val userUnlockedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_USER_UNLOCKED) {
                WidgetManager.init(this@NotificationReaderService)
            }
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onCreate() {
        super.onCreate()
        
        val filter = IntentFilter(Intent.ACTION_USER_UNLOCKED)
        registerReceiver(userUnlockedReceiver, filter)
        
        preferences = AppPreferences(applicationContext)
        createChannels()

        // [INIT] Theme Engine
        themeRepository = ThemeRepository(this)
        rulesEngine = RulesEngine()

        // Pass ThemeRepository to Translators
        callTranslator = CallTranslator(this, themeRepository)
        navTranslator = NavTranslator(this, themeRepository)
        timerTranslator = TimerTranslator(this, themeRepository)
        progressTranslator = ProgressTranslator(this, themeRepository)
        downloadTranslator = DownloadTranslator(this, themeRepository)
        standardTranslator = StandardTranslator(this, themeRepository)
        liveUpdateTranslator = LiveUpdateTranslator(this, themeRepository)

        mediaTranslator = MediaTranslator(this)
        widgetTranslator = WidgetTranslator(this)

        val userManager = getSystemService(USER_SERVICE) as android.os.UserManager
        if (userManager.isUserUnlocked) {
            WidgetManager.init(this)
        }

        permanentIslandManager = PermanentIslandManager(this, serviceScope, preferences)

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
        }
        return START_STICKY
    }

    // =========================================================================
    //  EFFECTIVE BEHAVIOR RESOLUTION (Theme > App > Global)
    // =========================================================================

    private suspend fun getEffectiveTypes(pkg: String): Set<String> {
        val themeOverride = themeRepository.activeTheme.value?.apps?.get(pkg)
        val rawTypes = if (themeOverride?.activeNotificationTypes != null) {
            themeOverride.activeNotificationTypes
        } else {
            val localPref = preferences.getAppConfigFlow(pkg).first()
            localPref ?: preferences.globalNotificationTypesFlow.first()
        }

        // Fallback: if PROGRESS is enabled but DOWNLOAD is missing, implicitly enable DOWNLOAD
        return if (rawTypes.contains("PROGRESS") && !rawTypes.contains("DOWNLOAD")) {
            rawTypes + "DOWNLOAD"
        } else {
            rawTypes
        }
    }

    private suspend fun getEffectiveEngine(pkg: String): Boolean {
        val activeTheme = themeRepository.activeTheme.value

        // 1. Theme App Override (Creator explicitly configured this app)
        val themeAppOverride = activeTheme?.apps?.get(pkg)?.useNativeLiveUpdates
        if (themeAppOverride != null) return themeAppOverride

        // 2. User App Override (User explicitly configured this app via Home Screen)
        val userAppOverride = preferences.getAppEnginePreferenceFlow(pkg).first()
        if (userAppOverride != null) return userAppOverride

        // 3. Theme Global Override (Creator explicitly forced an engine for the whole theme)
        val themeGlobalOverride = activeTheme?.global?.useNativeLiveUpdates
        if (themeGlobalOverride != null) return themeGlobalOverride

        // 4. User Global Fallback (The main Engine Setting on the Home Screen!)
        return preferences.useNativeLiveUpdates.first()
    }

    private suspend fun getEffectiveNav(pkg: String): Pair<NavContent, NavContent> {
        return preferences.getEffectiveNavLayout(pkg).first()
    }

    // =========================================================================
    //  NOTIFICATION REMOVAL LOGIC
    // =========================================================================

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        sbn?.let {
            val isOurApp = it.packageName == packageName
            val notifId = it.id
            val notifKey = it.key

            if (intentionallyRemovedKeys.remove(notifKey)) {
                return
            }

            processingJobs[notifKey]?.cancel()
            processingJobs.remove(notifKey)

            timeoutJobs[notifKey]?.cancel()
            timeoutJobs.remove(notifKey)

            if (isOurApp) {
                if (notifId >= WIDGET_ID_BASE) {
                    val widgetId = notifId - WIDGET_ID_BASE
                    dismissedWidgetIds.add(widgetId)
                    activeWidgets.remove(widgetId)
                    permanentIslandManager.onActiveNotificationsChanged(activeIslands.size + activeWidgets.size)
                    return
                }

                var originalKey = reverseTranslations[notifId]
                if (originalKey == null) {
                    originalKey = it.notification.extras.getString(EXTRA_ORIGINAL_KEY)
                }

                if (originalKey != null) {
                    Log.d(TAG, "Our notification $notifId removed. Cleaning up cache for $originalKey")
                    // [FIX] We no longer kill the source notification when our Island is dismissed or timed out
                    cleanupCache(originalKey)
                }
                return
            }

            if (activeTranslations.containsKey(notifKey)) {
                val hyperId = activeTranslations[notifKey] ?: return

                serviceScope.launch(Dispatchers.IO) {
                    val appConfig = preferences.getAppIslandConfig(sbn.packageName).first()
                    val globalConfig = preferences.globalConfigFlow.first()
                    val finalConfig = appConfig.mergeWith(globalConfig)

                    if (finalConfig.dismissWithOriginal == true) {
                        try {
                            NotificationManagerCompat.from(this@NotificationReaderService).cancel(hyperId)
                        } catch (_: Exception) {}

                        cleanupCache(notifKey)
                    }
                }
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

    private fun cleanupCache(originalKey: String) {
        val hyperId = activeTranslations[originalKey]
        activeIslands.remove(originalKey)
        activeTranslations.remove(originalKey)
        timeoutJobs[originalKey]?.cancel()
        timeoutJobs.remove(originalKey)

        if (hyperId != null) {
            reverseTranslations.remove(hyperId)
        }
        permanentIslandManager.onActiveNotificationsChanged(activeIslands.size + activeWidgets.size)
    }

    private fun handlePostNotificationSideEffects(originalKey: String, bridgeId: Int, config: IslandConfig, type: NotificationType, isLiveUpdate: Boolean) {
        // 1. Remove original if enabled (EXCEPT for Media)
        if (config.removeOriginalNotification == true && type != NotificationType.MEDIA && type != NotificationType.CALL) {
            intentionallyRemovedKeys.add(originalKey)
            cancelNotification(originalKey)
        }

        // 2. Schedule timeout ONLY for Live Update notifications
        if (isLiveUpdate) {
            val timeoutSeconds = config.timeout ?: 0
            timeoutJobs[originalKey]?.cancel()
            if (timeoutSeconds > 0) {
                timeoutJobs[originalKey] = serviceScope.launch {
                    delay(timeoutSeconds * 1000L)
                    Log.d(TAG, "Timeout reached for $originalKey, removing translated notification $bridgeId")
                    NotificationManagerCompat.from(this@NotificationReaderService).cancel(bridgeId)
                    cleanupCache(originalKey)
                    timeoutJobs.remove(originalKey)
                }
            }
        }
    }

    // =========================================================================
    //  STANDARD NOTIFICATION LOGIC
    // =========================================================================

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn?.let {
            if (shouldIgnore(it.packageName)) return

            processingJobs[it.key]?.cancel()

            val job = serviceScope.launch {
                if (isAppAllowed(it.packageName)) {
                    if (isJunkNotification(it)) return@launch
                    processStandardNotification(it)
                }
            }
            processingJobs[it.key] = job
            job.invokeOnCompletion { processingJobs.remove(sbn.key) }
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private suspend fun processStandardNotification(rawSbn: StatusBarNotification) {
        val manager = getSystemService(NotificationManager::class.java)
        val isSystemDndActive = manager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
        val dndActive = isDndModeEnabled || (autoDetectDnd && isSystemDndActive)

        if (dndActive) {
            Log.d(TAG, "DND active. Skipping notification ${rawSbn.packageName}")
            return
        }

        val sbn = ensureValidSbn(rawSbn)

        try {
            val extras = sbn.notification.extras

            // [LOGIC] 1. Resolve Info intelligently
            var effectiveTitle = resolveTitle(sbn)
            val effectiveText = resolveText(sbn.notification.extras)

            // [LOGIC] 2. State Preservation
            val key = sbn.key
            val previous = activeIslands[key]

            if (effectiveTitle.isEmpty()) {
                if (previous != null && previous.title.isNotEmpty() && previous.title != sbn.packageName) {
                    effectiveTitle = previous.title
                } else {
                    effectiveTitle = getCachedAppLabel(sbn.packageName)
                }
            }

            // [LOGIC] 3. Hard Stop
            val hasProgress = hasProgressNotification(sbn, effectiveTitle, effectiveText)

            if (effectiveTitle.isEmpty() && !hasProgress) return

            val appBlockedTerms = preferences.getAppBlockedTerms(sbn.packageName).first()
            if (appBlockedTerms.isNotEmpty()) {
                val content = "$effectiveTitle $effectiveText"
                if (appBlockedTerms.any { term -> content.contains(term, ignoreCase = true) }) return
            }

            // [LOGIC] 4. Theme & Rules Interception
            val activeTheme = themeRepository.activeTheme.value
            val ruleMatch = rulesEngine.match(sbn, effectiveTitle, effectiveText, activeTheme)

            val type = if (ruleMatch?.targetLayout != null) {
                try { NotificationType.valueOf(ruleMatch.targetLayout) }
                catch (_: Exception) { detectNotificationType(sbn) }
            } else {
                detectNotificationType(sbn)
            }

            // --- LAYERED TRIGGERS LOGIC ---
            val effectiveTypes = getEffectiveTypes(sbn.packageName)
            if (!effectiveTypes.contains(type.name)) {
                Log.d(TAG, " ABORTING: Type $type disabled by user/theme for ${sbn.packageName}")
                return
            }

            val isUpdate = activeIslands.containsKey(key)

            if (!isUpdate && activeIslands.size >= MAX_ISLANDS) {
                handleLimitReached(type, sbn.packageName)
                if (activeIslands.size >= MAX_ISLANDS) return
            }

            val appIslandConfig = preferences.getAppIslandConfig(sbn.packageName).first()
            val globalConfig = preferences.globalConfigFlow.first()
            val finalConfig = appIslandConfig.mergeWith(globalConfig)

            val bridgeId = sbn.key.hashCode()
            val picKey = "pic_${bridgeId}"

            // --- LAYERED ENGINE LOGIC ---
            val useLiveUpdates = getEffectiveEngine(sbn.packageName)

            if (useLiveUpdates) {
                Log.i(TAG, " POSTING Native Live Update -> ID: $bridgeId, Type: $type")

                // [FIX] Fetch the user's custom layout so the Live Update can use it!
                val navLayout = if (type == NotificationType.NAVIGATION) getEffectiveNav(sbn.packageName) else null

                // [FIX] Pass the type and the right layout to the translator
                val builder = liveUpdateTranslator.translateToLiveUpdate(
                    sbn = sbn,
                    channelId = LIVE_UPDATE_CHANNEL_ID,
                    type = type,
                    navRight = navLayout?.second
                )

                builder.extras.putString(EXTRA_ORIGINAL_KEY, sbn.key)

                val shouldAlertOnce = isUpdate && (type == NotificationType.PROGRESS || type == NotificationType.DOWNLOAD || type == NotificationType.MEDIA)
                builder.setOnlyAlertOnce(shouldAlertOnce)

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

                val actualProgress = extras.getInt(Notification.EXTRA_PROGRESS, 0)
                val actualMax = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0)
                val isIndeterminate = extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false)
                val actionState = sbn.notification.actions?.joinToString { it.title?.toString() ?: "" } ?: ""

                val newContentHash = effectiveTitle.hashCode() * 31 +
                        effectiveText.hashCode() + actualProgress + actualMax +
                        isIndeterminate.hashCode() + actionState.hashCode()

                if (isUpdate && previous != null && previous.lastContentHash == newContentHash) return

                com.d4viddf.hyperbridge.util.ShizukuManager.notify(this, bridgeId, notification)

                activeTranslations[sbn.key] = bridgeId
                reverseTranslations[bridgeId] = sbn.key
                activeIslands[key] = ActiveIsland(
                    id = bridgeId, type = type, postTime = System.currentTimeMillis(),
                    packageName = sbn.packageName, title = effectiveTitle, text = effectiveText,
                    subText = "LiveUpdate", lastContentHash = newContentHash
                )
                permanentIslandManager.onActiveNotificationsChanged(activeIslands.size + activeWidgets.size)

                handlePostNotificationSideEffects(key, bridgeId, finalConfig, type, true)
                return
            }

            // --- LAYERED CUSTOM ISLAND LOGIC ---
            val data: HyperIslandData = when (type) {
                NotificationType.CALL -> callTranslator.translate(sbn, picKey, finalConfig, activeTheme)
                NotificationType.NAVIGATION -> {
                    // --- LAYERED NAVIGATION LOGIC ---
                    val navLayout = getEffectiveNav(sbn.packageName)
                    navTranslator.translate(sbn, picKey, finalConfig, navLayout.first, navLayout.second, activeTheme)
                }
                NotificationType.TIMER -> timerTranslator.translate(sbn, picKey, finalConfig, activeTheme)
                NotificationType.PROGRESS -> progressTranslator.translate(sbn, effectiveTitle, picKey, finalConfig, activeTheme, isUpdate)
                NotificationType.DOWNLOAD -> downloadTranslator.translate(sbn, effectiveTitle, picKey, finalConfig, activeTheme, isUpdate)
                NotificationType.MEDIA -> mediaTranslator.translate(sbn, picKey, finalConfig)
                else -> standardTranslator.translate(sbn, effectiveTitle, effectiveText, picKey, finalConfig, activeTheme)
            }

            val newContentHash = data.jsonParam.hashCode()
            if (isUpdate && previous != null && previous.lastContentHash == newContentHash) return

            val shouldAlertOnce = isUpdate && (type == NotificationType.PROGRESS || type == NotificationType.DOWNLOAD || type == NotificationType.MEDIA)

            Log.i(TAG, " POSTING Island -> ID: $bridgeId, Type: $type, FinalTitle: '$effectiveTitle', FinalText: '$effectiveText'")
            postStandardNotification(sbn, bridgeId, data, shouldAlertOnce)

            activeIslands[key] = ActiveIsland(
                id = bridgeId, type = type, postTime = System.currentTimeMillis(),
                packageName = sbn.packageName, title = effectiveTitle, text = effectiveText,
                subText = "", lastContentHash = newContentHash
            )
            permanentIslandManager.onActiveNotificationsChanged(activeIslands.size + activeWidgets.size)

            handlePostNotificationSideEffects(key, bridgeId, finalConfig, type, false)

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
        return extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0) > 0 ||
                extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE) ||
                (isDownload && extractTextPercentage(title, text) != null)
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

    private suspend fun ensureValidSbn(sbn: StatusBarNotification): StatusBarNotification {
        val extras = sbn.notification.extras
        val title = resolveTitle(sbn)
        val text = resolveText(extras)
        val hasProgress = hasProgressNotification(sbn, title, text)
        if (hasProgress) return sbn

        val pkg = sbn.packageName

        val isSuspicious = title.isEmpty() || text.equals(pkg, ignoreCase = true)

        if (isSuspicious) {
            delay(150)
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
        val isCall = n.category == Notification.CATEGORY_CALL || template == "android.app.Notification\$CallStyle"
        val isNav = n.category == Notification.CATEGORY_NAVIGATION || sbn.packageName.let { it.contains("maps") || it.contains("waze") }
        val isTimer = (extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER) || n.category == Notification.CATEGORY_ALARM) && n.`when` > 0
        val isMedia = template.contains("MediaStyle") || n.category == Notification.CATEGORY_TRANSPORT
        
        val title = resolveTitle(sbn)
        val text = resolveText(extras)
        val isDownload = isDownloadNotification(sbn, title, text)
        val hasProgress = hasProgressNotification(sbn, title, text)

        return when {
            isCall -> NotificationType.CALL
            isNav -> NotificationType.NAVIGATION
            isTimer -> NotificationType.TIMER
            isMedia -> NotificationType.MEDIA
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

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun postStandardNotification(sbn: StatusBarNotification, bridgeId: Int, data: HyperIslandData, shouldAlertOnce: Boolean) {
        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notification_went_wrong))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
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
        } else {
            sbn.notification.contentIntent?.let { builder.setContentIntent(it) }
        }

        val notification = builder.build()
        notification.extras.putString("miui.focus.param", data.jsonParam)

        if (!shouldAlertOnce) {
            com.d4viddf.hyperbridge.util.ShizukuManager.notify(this, bridgeId, notification)
        } else {
            com.d4viddf.hyperbridge.util.ShizukuManager.notify(this, bridgeId, notification)
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
            permanentIslandManager.onActiveNotificationsChanged(activeIslands.size + activeWidgets.size)
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
        com.d4viddf.hyperbridge.util.ShizukuManager.notify(this, notificationId, notification)
    }

    private fun handleLimitReached(newType: NotificationType, newPkg: String) {
        val oldest = activeIslands.minByOrNull { it.value.postTime } ?: return

        when (currentMode) {
            IslandLimitMode.FIRST_COME -> {
                // Ignore the new notification by removing it immediately (or simply returning, but returning here means the caller won't add it)
                // The logic in the caller says:
                // if (!isUpdate && activeIslands.size >= MAX_ISLANDS) {
                //    handleLimitReached(type, sbn.packageName)
                //    if (activeIslands.size >= MAX_ISLANDS) return
                // }
                // So if we do nothing here, the size remains >= MAX_ISLANDS, and the caller will return.
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
                val newPriority = appPriorityList.indexOf(newPkg).let { if (it == -1) Int.MAX_VALUE else it }
                
                // Find the existing active island with the lowest priority (highest index value)
                val lowestPriorityIsland = activeIslands.maxByOrNull {
                    appPriorityList.indexOf(it.value.packageName).let { idx -> if (idx == -1) Int.MAX_VALUE else idx }
                }

                if (lowestPriorityIsland != null) {
                    val lowestPriority = appPriorityList.indexOf(lowestPriorityIsland.value.packageName).let { if (it == -1) Int.MAX_VALUE else it }
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

    private fun isJunkNotification(sbn: StatusBarNotification): Boolean {
        val notification = sbn.notification
        val extras = notification.extras
        val pkg = sbn.packageName

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim() ?: ""

        val hasProgress = hasProgressNotification(sbn, title, text)
        val isSpecial = notification.category == Notification.CATEGORY_TRANSPORT || notification.category == Notification.CATEGORY_CALL ||
                notification.category == Notification.CATEGORY_NAVIGATION || extras.getString(Notification.EXTRA_TEMPLATE)?.contains("MediaStyle") == true
        if (hasProgress || isSpecial) return false
        if (title.isEmpty() && text.isEmpty()) return true
        if (title.equals(pkg, ignoreCase = true) || text.equals(pkg, ignoreCase = true)) return true
        if (globalBlockedTerms.any { "$title $text".contains(it, true) }) return true

        if ((notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) {
            // We previously blocked GROUP_ALERT_CHILDREN, but some apps like Telegram 
            // use it while silencing their actual children, leading to no alerts at all.
            // Let's just allow group summaries if they have actual text.
            if (pkg.contains("whatsapp", ignoreCase = true)) return true
            if (text.isEmpty() || title.isEmpty()) return true
        }

        return false
    }

    private fun getCachedAppLabel(pkg: String): String = appLabelCache.getOrPut(pkg) {
        try { packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString() } catch (_: Exception) { "" }
    }

    private fun shouldIgnore(packageName: String): Boolean = packageName == this.packageName || packageName == "android" || packageName.contains("miui.notification")
    private fun isAppAllowed(packageName: String): Boolean = allowedPackageSet.contains(packageName)

    override fun onListenerConnected() { Log.i(TAG, "HyperBridge Service Connected") }
    override fun onDestroy() { 
        super.onDestroy()
        unregisterReceiver(userUnlockedReceiver)
        serviceScope.cancel() 
    }
}