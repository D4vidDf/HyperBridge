package com.d4viddf.hyperbridge.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.d4viddf.hyperbridge.MainActivity
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.data.widget.WidgetManager
import com.d4viddf.hyperbridge.models.ActiveIsland
import com.d4viddf.hyperbridge.models.HyperIslandData
import com.d4viddf.hyperbridge.models.IslandLimitMode
import com.d4viddf.hyperbridge.models.NotificationType
import com.d4viddf.hyperbridge.service.translators.CallTranslator
import com.d4viddf.hyperbridge.service.translators.MediaTranslator
import com.d4viddf.hyperbridge.service.translators.NavTranslator
import com.d4viddf.hyperbridge.service.translators.ProgressTranslator
import com.d4viddf.hyperbridge.service.translators.StandardTranslator
import com.d4viddf.hyperbridge.service.translators.TimerTranslator
import com.d4viddf.hyperbridge.service.translators.WidgetTranslator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class NotificationReaderService : NotificationListenerService() {

    private val TAG = "HyperBridgeDebug"
    private val ISLAND_CHANNEL_ID = "hyper_bridge_island_channel"

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    // --- STATE ---
    private var allowedPackageSet: Set<String> = emptySet()
    private var currentMode = IslandLimitMode.MOST_RECENT
    private var appPriorityList = emptyList<String>()
    private var globalBlockedTerms: Set<String> = emptySet()

    // --- CACHES ---
    private val activeIslands = ConcurrentHashMap<String, ActiveIsland>()
    private val activeTranslations = ConcurrentHashMap<String, Int>()
    private val lastUpdateMap = ConcurrentHashMap<String, Long>()

    // Cache app labels to avoid heavy PackageManager calls
    private val appLabelCache = ConcurrentHashMap<String, String>()

    private val UPDATE_INTERVAL_MS = 200L
    private val MAX_ISLANDS = 9

    private lateinit var preferences: AppPreferences

    // Translators
    private lateinit var callTranslator: CallTranslator
    private lateinit var navTranslator: NavTranslator
    private lateinit var timerTranslator: TimerTranslator
    private lateinit var progressTranslator: ProgressTranslator
    private lateinit var standardTranslator: StandardTranslator
    private lateinit var mediaTranslator: MediaTranslator
    private lateinit var widgetTranslator: WidgetTranslator

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onCreate() {
        super.onCreate()
        preferences = AppPreferences(applicationContext)
        createIslandChannel()

        // [FIX 1] Initialize Translators BEFORE WidgetManager to prevent lateinit crash
        callTranslator = CallTranslator(this)
        navTranslator = NavTranslator(this)
        timerTranslator = TimerTranslator(this)
        progressTranslator = ProgressTranslator(this)
        standardTranslator = StandardTranslator(this)
        mediaTranslator = MediaTranslator(this)
        widgetTranslator = WidgetTranslator(this)

        // [FIX 1] Initialize Widget Host Manager AFTER translators are ready
        WidgetManager.init(this)

        // Observe settings changes
        serviceScope.launch { preferences.allowedPackagesFlow.collectLatest { allowedPackageSet = it } }
        serviceScope.launch { preferences.limitModeFlow.collectLatest { currentMode = it } }
        serviceScope.launch { preferences.appPriorityListFlow.collectLatest { appPriorityList = it } }
        serviceScope.launch { preferences.globalBlockedTermsFlow.collectLatest { globalBlockedTerms = it } }

        // [WIDGET LISTENER]
        serviceScope.launch {
            WidgetManager.widgetUpdates.collect { updatedId ->
                // Only react if this widget is actually saved in our config
                val savedIds = preferences.savedWidgetIdsFlow.first()
                if (savedIds.contains(updatedId)) {
                    // Launch on Main for View rendering if needed
                    launch(Dispatchers.Main) {
                        try {
                            val data = widgetTranslator.translate(updatedId)
                            postNotification(null, 9000 + updatedId, data)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to update widget", e)
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
                serviceScope.launch(Dispatchers.Main) {
                    try {
                        val data = widgetTranslator.translate(widgetId)
                        postNotification(null, 9000 + widgetId, data)
                    } catch (e: Exception) {
                        Log.e(TAG, "💥 Failed to post widget notification", e)
                    }
                }
            }
        }
        return START_STICKY // Recommended for services
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "HyperBridge Service Connected & Listening")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn?.let {
            if (shouldIgnore(it.packageName)) return

            // [FIX 2] Move heavy logic (Junk check / App Labels) inside IO scope
            serviceScope.launch {
                if (isAppAllowed(it.packageName)) {
                    if (isJunkNotification(it)) return@launch
                    processAndPost(it)
                }
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        sbn?.let {
            val key = it.key
            if (activeTranslations.containsKey(key)) {
                val hyperId = activeTranslations[key] ?: return
                try { NotificationManagerCompat.from(this).cancel(hyperId) } catch (e: Exception) {}

                activeIslands.remove(key)
                activeTranslations.remove(key)
                lastUpdateMap.remove(key)
            }
        }
    }

    // =========================================================================
    //  CORE LOGIC (Standard Notifications)
    // =========================================================================

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private suspend fun processAndPost(sbn: StatusBarNotification) {
        try {
            val extras = sbn.notification.extras
            val title = extras.getString(Notification.EXTRA_TITLE) ?: sbn.packageName
            val text = extras.getString(Notification.EXTRA_TEXT) ?: ""

            // --- App-Specific Blocklist Check ---
            val appBlockedTerms = preferences.getAppBlockedTerms(sbn.packageName).first()
            if (appBlockedTerms.isNotEmpty()) {
                val content = "$title $text"
                if (appBlockedTerms.any { term -> content.contains(term, ignoreCase = true) }) {
                    return
                }
            }

            // --- Type Detection ---
            val template = extras.getString(Notification.EXTRA_TEMPLATE) ?: ""

            val isCall = sbn.notification.category == Notification.CATEGORY_CALL ||
                    template == "android.app.Notification\$CallStyle"
            val isNavigation = sbn.notification.category == Notification.CATEGORY_NAVIGATION ||
                    sbn.packageName.contains("maps") || sbn.packageName.contains("waze")
            val progressMax = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0)
            val hasProgress = progressMax > 0 || extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE)
            val chronometerBase = sbn.notification.`when`
            val isTimer = (extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER) ||
                    sbn.notification.category == Notification.CATEGORY_ALARM ||
                    sbn.notification.category == Notification.CATEGORY_STOPWATCH) && chronometerBase > 0
            val isMedia = template.contains("MediaStyle") == true ||
                    sbn.notification.category == Notification.CATEGORY_TRANSPORT

            val type = when {
                isCall -> NotificationType.CALL
                isNavigation -> NotificationType.NAVIGATION
                isTimer -> NotificationType.TIMER
                isMedia -> NotificationType.MEDIA
                hasProgress -> NotificationType.PROGRESS
                else -> NotificationType.STANDARD
            }

            val config = preferences.getAppConfig(sbn.packageName).first()
            if (!config.contains(type.name)) return

            // --- Island Management ---
            val key = sbn.key
            val isUpdate = activeIslands.containsKey(key)
            val bridgeId = sbn.key.hashCode()

            if (!isUpdate && activeIslands.size >= MAX_ISLANDS) {
                handleLimitReached(type, sbn.packageName)
                if (activeIslands.size >= MAX_ISLANDS) return
            }

            // --- Configuration Merge ---
            val appIslandConfig = preferences.getAppIslandConfig(sbn.packageName).first()
            val globalConfig = preferences.globalConfigFlow.first()
            val finalConfig = appIslandConfig.mergeWith(globalConfig)

            // --- Translation ---
            val picKey = "pic_${bridgeId}"
            val data: HyperIslandData = when (type) {
                NotificationType.CALL -> callTranslator.translate(sbn, picKey, finalConfig)
                NotificationType.NAVIGATION -> {
                    val navLayout = preferences.getEffectiveNavLayout(sbn.packageName).first()
                    navTranslator.translate(sbn, picKey, finalConfig, navLayout.first, navLayout.second)
                }
                NotificationType.TIMER -> timerTranslator.translate(sbn, picKey, finalConfig)
                NotificationType.PROGRESS -> progressTranslator.translate(sbn, title, picKey, finalConfig)
                NotificationType.MEDIA -> mediaTranslator.translate(sbn, picKey, finalConfig)
                else -> standardTranslator.translate(sbn, picKey, finalConfig)
            }

            // --- Deduplication ---
            val newContentHash = data.jsonParam.hashCode()
            val previousIsland = activeIslands[key]
            if (isUpdate && previousIsland != null) {
                if (previousIsland.lastContentHash == newContentHash) {
                    return
                }
            }

            // --- Post to System ---
            postNotification(sbn, bridgeId, data)

            // --- Cache State ---
            val currTitle = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
            val currText = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            val currSub = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""

            activeIslands[key] = ActiveIsland(
                id = bridgeId,
                type = type,
                postTime = System.currentTimeMillis(),
                packageName = sbn.packageName,
                title = currTitle,
                text = currText,
                subText = currSub,
                lastContentHash = newContentHash
            )

        } catch (e: Exception) {
            Log.e(TAG, "💥 Error processing notification", e)
        }
    }

    // =========================================================================
    //  HELPERS
    // =========================================================================

    private fun handleLimitReached(newType: NotificationType, newPkg: String) {
        when (currentMode) {
            IslandLimitMode.FIRST_COME -> return
            IslandLimitMode.MOST_RECENT -> {
                val oldest = activeIslands.minByOrNull { it.value.postTime }
                oldest?.let {
                    NotificationManagerCompat.from(this).cancel(it.value.id)
                    activeIslands.remove(it.key)
                }
            }
            IslandLimitMode.PRIORITY -> {
                val newPriority = appPriorityList.indexOf(newPkg).takeIf { it != -1 } ?: 9999
                val lowestActiveEntry = activeIslands.maxByOrNull { entry ->
                    appPriorityList.indexOf(entry.value.packageName).takeIf { it != -1 } ?: 9999
                }
                if (lowestActiveEntry != null) {
                    val lowestPriority = appPriorityList.indexOf(lowestActiveEntry.value.packageName).takeIf { it != -1 } ?: 9999
                    if (newPriority < lowestPriority) {
                        NotificationManagerCompat.from(this).cancel(lowestActiveEntry.value.id)
                        activeIslands.remove(lowestActiveEntry.key)
                    }
                }
            }
        }
    }

    /**
     * Posts the notification to the System.
     * @param sbn Nullable because Widgets don't have a StatusBarNotification.
     */
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun postNotification(sbn: StatusBarNotification?, bridgeId: Int, data: HyperIslandData) {
        val notificationBuilder = NotificationCompat.Builder(this, ISLAND_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Ensure this icon exists
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.service_active))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addExtras(data.resources)

        // [FIX 3] Handle Click Intent
        if (sbn != null) {
            // Standard Notification: Use the original intent
            sbn.notification.contentIntent?.let { notificationBuilder.setContentIntent(it) }
        } else {
            // Widget: Create a generic intent to open the app or config
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            notificationBuilder.setContentIntent(pendingIntent)
        }

        val notification = notificationBuilder.build()
        notification.extras.putString("miui.focus.param", data.jsonParam)

        NotificationManagerCompat.from(this).notify(bridgeId, notification)

        // Track ID for removal later (Only for actual notifications)
        if (sbn != null) {
            activeTranslations[sbn.key] = bridgeId
        }
    }

    private fun isJunkNotification(sbn: StatusBarNotification): Boolean {
        val notification = sbn.notification
        val extras = notification.extras
        val pkg = sbn.packageName

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim() ?: ""
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.trim() ?: ""

        if (title.isEmpty() && text.isEmpty() && subText.isEmpty()) return true
        if (title.contains(pkg, ignoreCase = true) || text.contains(pkg, ignoreCase = true)) return true
        if (title.contains("com.google.android", ignoreCase = true)) return true

        if (globalBlockedTerms.isNotEmpty()) {
            val content = "$title $text"
            if (globalBlockedTerms.any { term -> content.contains(term, ignoreCase = true) }) {
                return true
            }
        }

        // [FIX 2] Optimized App Label check using Cache
        val appName = getCachedAppLabel(pkg)
        if (title == appName && text.isEmpty()) return true

        if (title.contains("running in background", true)) return true
        if (text.contains("tap for more info", true)) return true
        if (text.contains("displaying over other apps", true)) return true

        if ((notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return true

        val hasProgress = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0) > 0 ||
                extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE)
        val isSpecial = notification.category == Notification.CATEGORY_TRANSPORT ||
                notification.category == Notification.CATEGORY_CALL ||
                notification.category == Notification.CATEGORY_NAVIGATION ||
                extras.getString(Notification.EXTRA_TEMPLATE)?.contains("MediaStyle") == true

        if (hasProgress || isSpecial) return false

        return false
    }

    private fun getCachedAppLabel(pkg: String): String {
        return appLabelCache.getOrPut(pkg) {
            try {
                val info = packageManager.getApplicationInfo(pkg, 0)
                packageManager.getApplicationLabel(info).toString()
            } catch (e: Exception) {
                ""
            }
        }
    }

    private fun shouldIgnore(packageName: String): Boolean {
        return packageName == this.packageName ||
                packageName == "android" ||
                packageName == "com.android.systemui" ||
                packageName.contains("miui.notification")
    }

    private fun createIslandChannel() {
        val name = getString(R.string.channel_active_islands)
        val channel = NotificationChannel(
            ISLAND_CHANNEL_ID,
            name,
            NotificationManager.IMPORTANCE_HIGH).apply {
            setSound(null, null)
            enableVibration(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun isAppAllowed(packageName: String): Boolean {
        return allowedPackageSet.contains(packageName)
    }
}