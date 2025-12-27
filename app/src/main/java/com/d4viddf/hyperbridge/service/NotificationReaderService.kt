package com.d4viddf.hyperbridge.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class NotificationReaderService : NotificationListenerService() {

    private val TAG = "HyperBridgeDebug"
    private val EXTRA_ORIGINAL_KEY = "hyper_original_key"

    // --- CHANNELS ---
    private val NOTIFICATION_CHANNEL_ID = "hyper_bridge_notification_channel"
    private val WIDGET_CHANNEL_ID = "hyper_bridge_widget_channel"

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())

    // --- STATE & CONFIG ---
    private var allowedPackageSet: Set<String> = emptySet()
    private var currentMode = IslandLimitMode.MOST_RECENT
    private var appPriorityList = emptyList<String>()
    private var globalBlockedTerms: Set<String> = emptySet()

    // --- CACHES ---
    private val activeIslands = ConcurrentHashMap<String, ActiveIsland>()

    // Forward Map: Original Key -> HyperBridge ID
    private val activeTranslations = ConcurrentHashMap<String, Int>()

    // Reverse Map: HyperBridge ID -> Original Key
    private val reverseTranslations = ConcurrentHashMap<Int, String>()

    // Job Tracker
    private val processingJobs = ConcurrentHashMap<String, Job>()

    private val widgetUpdateDebouncer = ConcurrentHashMap<Int, Long>()
    private val dismissedWidgetIds = ConcurrentHashMap.newKeySet<Int>()

    private val appLabelCache = ConcurrentHashMap<String, String>()

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
        }
        return START_STICKY
    }

    // =========================================================================
    //  NOTIFICATION REMOVAL LOGIC
    // =========================================================================

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        sbn?.let {
            val isOurApp = it.packageName == packageName
            val notifId = it.id
            val notifKey = it.key

            // Cancel any pending processing for this key
            processingJobs[notifKey]?.cancel()
            processingJobs.remove(notifKey)

            // --- CASE A: User Dismissed OUR Notification (HyperBridge) ---
            if (isOurApp) {
                if (notifId >= WIDGET_ID_BASE) {
                    val widgetId = notifId - WIDGET_ID_BASE
                    dismissedWidgetIds.add(widgetId)
                    return
                }

                var originalKey = reverseTranslations[notifId]
                if (originalKey == null) {
                    originalKey = it.notification.extras.getString(EXTRA_ORIGINAL_KEY)
                }

                if (originalKey != null) {
                    Log.d(TAG, "Dismissing source notification for ID $notifId -> Key: $originalKey")
                    serviceScope.launch {
                        cancelSourceNotification(originalKey)
                    }
                    cleanupCache(originalKey)
                }
                return
            }

            // --- CASE B: User Dismissed SOURCE Notification (Other App) ---
            if (activeTranslations.containsKey(notifKey)) {
                val hyperId = activeTranslations[notifKey] ?: return

                try {
                    NotificationManagerCompat.from(this).cancel(hyperId)
                } catch (e: Exception) {}

                cleanupCache(notifKey)
            }
        }
    }

    private fun cancelSourceNotification(targetKey: String) {
        try {
            val currentNotifications = try {
                activeNotifications
            } catch (e: Exception) {
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

        if (hyperId != null) {
            reverseTranslations.remove(hyperId)
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
        val sbn = ensureValidSbn(rawSbn)

        try {
            // [UPDATED] Resolve Title and Text intelligently
            val title = resolveTitle(sbn)
            val text = resolveText(sbn.notification.extras)

            // App Blocked Terms Check
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

            // Deduplication via Content Hash
            val newContentHash = data.jsonParam.hashCode()
            val previousIsland = activeIslands[key]

            if (isUpdate && previousIsland != null && previousIsland.lastContentHash == newContentHash) {
                return
            }

            // Final safety check
            try {
                val currentNotifs = activeNotifications
                val exists = currentNotifs.any { it.key == key }
                if (!exists) return
            } catch (e: Exception) { }

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

    /**
     * [UPDATED] Helper to get the best Title (Handles BigPicture AND BigText styles)
     * If standard title is garbage (package name), use EXTRA_TITLE_BIG.
     */
    private fun resolveTitle(sbn: StatusBarNotification): String {
        val extras = sbn.notification.extras
        var title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim() ?: ""
        val bigTitle = extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString()?.trim()
        val pkg = sbn.packageName

        // If title is package name (bug), or empty... AND we have an expanded title
        // Use the Expanded Title (works for BigText and BigPicture)
        if ((title.isEmpty() || title.equals(pkg, ignoreCase = true)) && !bigTitle.isNullOrEmpty()) {
            return bigTitle
        }

        if (title.isEmpty()) return pkg
        return title
    }

    /**
     * Helper to get the best Text.
     * BigText notifications sometimes leave EXTRA_TEXT empty and only fill EXTRA_BIG_TEXT.
     */
    private fun resolveText(extras: Bundle): String {
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim()

        // If standard text is present, use it (usually a good summary).
        // If standard text is empty, fall back to BigText (the full body).
        if (!text.isNullOrEmpty()) return text

        return bigText ?: ""
    }

    private suspend fun ensureValidSbn(sbn: StatusBarNotification): StatusBarNotification {
        val extras = sbn.notification.extras
        val hasProgress = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0) > 0 ||
                extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE)
        if (hasProgress) return sbn

        // [UPDATED] Use our smart resolver to check the title
        val title = resolveTitle(sbn)
        val text = resolveText(extras) // Use smart text resolver too
        val pkg = sbn.packageName

        // If even the "smart resolved" title still looks like the package name...
        val isSuspicious = title.equals(pkg, ignoreCase = true) || text.equals(pkg, ignoreCase = true)

        if (isSuspicious) {
            delay(150)
            try {
                val activeList = activeNotifications
                val updatedSbn = activeList?.firstOrNull { it.key == sbn.key }
                if (updatedSbn != null) {
                    return updatedSbn
                }
            } catch (e: Exception) { }
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

        val extras = Bundle()
        extras.putString(EXTRA_ORIGINAL_KEY, sbn.key)
        builder.addExtras(extras)
        builder.addExtras(data.resources)

        sbn.notification.contentIntent?.let { builder.setContentIntent(it) }

        val notification = builder.build()
        notification.extras.putString("miui.focus.param", data.jsonParam)

        NotificationManagerCompat.from(this).notify(bridgeId, notification)

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
        } catch (e: Exception) { Log.e(TAG, "Failed widget $widgetId", e) }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun postWidgetNotification(notificationId: Int, data: HyperIslandData) {
        val builder = NotificationCompat.Builder(this, WIDGET_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Widget Overlay").setContentText("Active")
            .setPriority(NotificationCompat.PRIORITY_LOW).setOngoing(true)
            .setOnlyAlertOnce(true).addExtras(data.resources)

        val intent = Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        builder.setContentIntent(pendingIntent)

        val notification = builder.build()
        notification.extras.putString("miui.focus.param", data.jsonParam)
        NotificationManagerCompat.from(this).notify(notificationId, notification)
    }

    private fun handleLimitReached(newType: NotificationType, newPkg: String) {
        if (currentMode == IslandLimitMode.MOST_RECENT) {
            val oldest = activeIslands.minByOrNull { it.value.postTime }
            oldest?.let {
                NotificationManagerCompat.from(this).cancel(it.value.id)
                cleanupCache(it.key)
            }
        }
    }

    private fun isJunkNotification(sbn: StatusBarNotification): Boolean {
        val notification = sbn.notification
        val extras = notification.extras
        val pkg = sbn.packageName

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim() ?: ""

        val hasProgress = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0) > 0 || extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE)
        val isSpecial = notification.category == Notification.CATEGORY_TRANSPORT || notification.category == Notification.CATEGORY_CALL ||
                notification.category == Notification.CATEGORY_NAVIGATION || extras.getString(Notification.EXTRA_TEMPLATE)?.contains("MediaStyle") == true
        if (hasProgress || isSpecial) return false

        if (title.isEmpty() && text.isEmpty()) return true

        if (title.equals(pkg, ignoreCase = true) || text.equals(pkg, ignoreCase = true)) return true

        if (globalBlockedTerms.any { "$title $text".contains(it, true) }) return true
        if ((notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return true

        return false
    }

    private fun shouldIgnore(packageName: String): Boolean = packageName == this.packageName || packageName == "android" || packageName.contains("miui.notification")
    private fun isAppAllowed(packageName: String): Boolean = allowedPackageSet.contains(packageName)

    override fun onListenerConnected() { Log.i(TAG, "HyperBridge Service Connected") }
    override fun onDestroy() { super.onDestroy(); serviceScope.cancel() }
}