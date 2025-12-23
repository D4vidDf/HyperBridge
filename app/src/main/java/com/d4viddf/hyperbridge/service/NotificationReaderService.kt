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
import com.d4viddf.hyperbridge.models.WidgetConfig
import com.d4viddf.hyperbridge.models.WidgetRenderMode
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

    // --- CHANNELS ---
    private val NOTIFICATION_CHANNEL_ID = "hyper_bridge_notification_channel"
    private val WIDGET_CHANNEL_ID = "hyper_bridge_widget_channel"

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    // --- STATE & CONFIG ---
    private var allowedPackageSet: Set<String> = emptySet()
    private var currentMode = IslandLimitMode.MOST_RECENT
    private var appPriorityList = emptyList<String>()
    private var globalBlockedTerms: Set<String> = emptySet()

    // --- CACHES ---
    private val activeIslands = ConcurrentHashMap<String, ActiveIsland>()
    private val activeTranslations = ConcurrentHashMap<String, Int>()
    private val lastUpdateMap = ConcurrentHashMap<String, Long>()

    // Widget Specific Caches
    private val widgetUpdateDebouncer = ConcurrentHashMap<Int, Long>()

    // [NEW] Track dismissed widgets to prevent them from reappearing automatically
    private val dismissedWidgetIds = ConcurrentHashMap.newKeySet<Int>()

    private val appLabelCache = ConcurrentHashMap<String, String>()

    private val NOTIF_UPDATE_INTERVAL_MS = 200L
    private val MAX_ISLANDS = 9
    private val WIDGET_ID_BASE = 9000

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
        createChannels()

        callTranslator = CallTranslator(this)
        navTranslator = NavTranslator(this)
        timerTranslator = TimerTranslator(this)
        progressTranslator = ProgressTranslator(this)
        standardTranslator = StandardTranslator(this)
        mediaTranslator = MediaTranslator(this)
        widgetTranslator = WidgetTranslator(this)

        WidgetManager.init(this)

        serviceScope.launch { preferences.allowedPackagesFlow.collectLatest { allowedPackageSet = it } }
        serviceScope.launch { preferences.limitModeFlow.collectLatest { currentMode = it } }
        serviceScope.launch { preferences.appPriorityListFlow.collectLatest { appPriorityList = it } }
        serviceScope.launch { preferences.globalBlockedTermsFlow.collectLatest { globalBlockedTerms = it } }

        // --- WIDGET LISTENER ---
        serviceScope.launch {
            WidgetManager.widgetUpdates.collect { updatedId ->
                // 1. Check if user explicitly dismissed this widget
                if (dismissedWidgetIds.contains(updatedId)) {
                    Log.d(TAG, "Skipping update for dismissed widget: $updatedId")
                    return@collect
                }

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
                // [NEW] User manually requested this widget -> Un-dismiss it
                dismissedWidgetIds.remove(widgetId)

                serviceScope.launch(Dispatchers.Main) {
                    val config = preferences.getWidgetConfigFlow(widgetId).first()
                    processSingleWidget(widgetId, config)
                }
            }
        }
        return START_STICKY
    }

    // =========================================================================
    //  WIDGET LOGIC
    // =========================================================================

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
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process widget $widgetId", e)
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun postWidgetNotification(notificationId: Int, data: HyperIslandData) {
        val builder = NotificationCompat.Builder(this, WIDGET_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Widget Overlay")
            .setContentText("Active")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addExtras(data.resources)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        builder.setContentIntent(pendingIntent)

        val notification = builder.build()
        notification.extras.putString("miui.focus.param", data.jsonParam)

        NotificationManagerCompat.from(this).notify(notificationId, notification)
    }

    // =========================================================================
    //  NOTIFICATION REMOVAL LOGIC
    // =========================================================================

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        sbn?.let {
            // [NEW] Check if the removed notification was a Widget
            if (it.id >= WIDGET_ID_BASE) {
                val widgetId = it.id - WIDGET_ID_BASE
                Log.d(TAG, "Widget Dismissed: $widgetId")
                dismissedWidgetIds.add(widgetId)
                return
            }

            // Standard Notification Removal Logic
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
    //  STANDARD NOTIFICATION LOGIC (Unchanged)
    // =========================================================================

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn?.let {
            if (shouldIgnore(it.packageName)) return

            serviceScope.launch {
                if (isAppAllowed(it.packageName)) {
                    if (isJunkNotification(it)) return@launch
                    processStandardNotification(it)
                }
            }
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private suspend fun processStandardNotification(sbn: StatusBarNotification) {
        if (shouldSkipUpdate(sbn)) return

        try {
            val extras = sbn.notification.extras
            val title = extras.getString(Notification.EXTRA_TITLE) ?: sbn.packageName
            val text = extras.getString(Notification.EXTRA_TEXT) ?: ""

            val appBlockedTerms = preferences.getAppBlockedTerms(sbn.packageName).first()
            if (appBlockedTerms.isNotEmpty()) {
                val content = "$title $text"
                if (appBlockedTerms.any { term -> content.contains(term, ignoreCase = true) }) return
            }

            val type = detectNotificationType(sbn)

            val config = preferences.getAppConfig(sbn.packageName).first()
            if (!config.contains(type.name)) return

            val key = sbn.key
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

            val newContentHash = data.jsonParam.hashCode()
            val previousIsland = activeIslands[key]
            if (isUpdate && previousIsland != null && previousIsland.lastContentHash == newContentHash) {
                return
            }

            postStandardNotification(sbn, bridgeId, data)

            activeIslands[key] = ActiveIsland(
                id = bridgeId, type = type, postTime = System.currentTimeMillis(),
                packageName = sbn.packageName, title = title, text = text, subText = "",
                lastContentHash = newContentHash
            )

        } catch (e: Exception) {
            Log.e(TAG, "💥 Error processing standard notification", e)
        }
    }

    private fun detectNotificationType(sbn: StatusBarNotification): NotificationType {
        val n = sbn.notification
        val extras = n.extras
        val template = extras.getString(Notification.EXTRA_TEMPLATE) ?: ""
        val isCall = n.category == Notification.CATEGORY_CALL || template == "android.app.Notification\$CallStyle"
        val isNav = n.category == Notification.CATEGORY_NAVIGATION || sbn.packageName.let { it.contains("maps") || it.contains("waze") }
        val isTimer = (extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER) || n.category == Notification.CATEGORY_ALARM) && n.`when` > 0
        val isMedia = template.contains("MediaStyle") || n.category == Notification.CATEGORY_TRANSPORT
        val hasProgress = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0) > 0

        return when {
            isCall -> NotificationType.CALL
            isNav -> NotificationType.NAVIGATION
            isTimer -> NotificationType.TIMER
            isMedia -> NotificationType.MEDIA
            hasProgress -> NotificationType.PROGRESS
            else -> NotificationType.STANDARD
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun postStandardNotification(sbn: StatusBarNotification, bridgeId: Int, data: HyperIslandData) {
        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Active Island")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addExtras(data.resources)

        sbn.notification.contentIntent?.let { builder.setContentIntent(it) }

        val notification = builder.build()
        notification.extras.putString("miui.focus.param", data.jsonParam)

        NotificationManagerCompat.from(this).notify(bridgeId, notification)
        activeTranslations[sbn.key] = bridgeId
    }

    // =========================================================================
    //  HELPERS & SETUP
    // =========================================================================

    private fun createChannels() {
        val manager = getSystemService(NotificationManager::class.java)

        val notifChannel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            getString(R.string.channel_active_islands),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
        }
        manager.createNotificationChannel(notifChannel)

        val widgetChannel = NotificationChannel(
            WIDGET_CHANNEL_ID,
            "Widgets Overlay",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
        }
        manager.createNotificationChannel(widgetChannel)
    }

    private fun shouldSkipUpdate(sbn: StatusBarNotification): Boolean {
        val key = sbn.key
        val now = System.currentTimeMillis()
        val lastTime = lastUpdateMap[key] ?: 0L
        if (now - lastTime < NOTIF_UPDATE_INTERVAL_MS) return true
        lastUpdateMap[key] = now
        return false
    }

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
            IslandLimitMode.PRIORITY -> {}
        }
    }

    private fun isJunkNotification(sbn: StatusBarNotification): Boolean {
        val title = sbn.notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val appName = getCachedAppLabel(sbn.packageName)
        if (title == appName && sbn.notification.extras.getCharSequence(Notification.EXTRA_TEXT).isNullOrEmpty()) return true
        return false
    }

    private fun getCachedAppLabel(pkg: String): String {
        return appLabelCache.getOrPut(pkg) {
            try {
                val info = packageManager.getApplicationInfo(pkg, 0)
                packageManager.getApplicationLabel(info).toString()
            } catch (e: Exception) { "" }
        }
    }

    private fun shouldIgnore(packageName: String): Boolean {
        return packageName == this.packageName || packageName == "android" || packageName.contains("miui.notification")
    }

    private fun isAppAllowed(packageName: String): Boolean = allowedPackageSet.contains(packageName)

    override fun onListenerConnected() {
        Log.i(TAG, "HyperBridge Service Connected")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}