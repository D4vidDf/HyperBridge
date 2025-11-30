package com.d4viddf.hyperbridge.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.models.ActiveIsland
import com.d4viddf.hyperbridge.models.HyperIslandData
import com.d4viddf.hyperbridge.models.IslandLimitMode
import com.d4viddf.hyperbridge.models.NotificationType
import com.d4viddf.hyperbridge.service.translators.CallTranslator
import com.d4viddf.hyperbridge.service.translators.NavTranslator
import com.d4viddf.hyperbridge.service.translators.ProgressTranslator
import com.d4viddf.hyperbridge.service.translators.StandardTranslator
import com.d4viddf.hyperbridge.service.translators.TimerTranslator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class NotificationReaderService : NotificationListenerService() {

    private val TAG = "HyperBridgeService"
    private val ISLAND_CHANNEL_ID = "hyper_bridge_island_channel"

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    private var allowedPackageSet: Set<String> = emptySet()
    private var currentMode = IslandLimitMode.MOST_RECENT
    private var appPriorityList = emptyList<String>()

    private val activeIslands = ConcurrentHashMap<String, ActiveIsland>()
    private val activeTranslations = ConcurrentHashMap<String, Int>()
    private val lastUpdateMap = ConcurrentHashMap<String, Long>()

    private val UPDATE_INTERVAL_MS = 200L
    private val MAX_ISLANDS = 9

    private lateinit var preferences: AppPreferences
    private lateinit var callTranslator: CallTranslator
    private lateinit var navTranslator: NavTranslator
    private lateinit var timerTranslator: TimerTranslator
    private lateinit var progressTranslator: ProgressTranslator
    private lateinit var standardTranslator: StandardTranslator

    override fun onCreate() {
        super.onCreate()
        createIslandChannel()
        preferences = AppPreferences(this)

        callTranslator = CallTranslator(this)
        navTranslator = NavTranslator(this)
        timerTranslator = TimerTranslator(this)
        progressTranslator = ProgressTranslator(this)
        standardTranslator = StandardTranslator(this)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        serviceScope.launch { preferences.allowedPackagesFlow.collectLatest { allowedPackageSet = it } }
        serviceScope.launch { preferences.limitModeFlow.collectLatest { currentMode = it } }
        serviceScope.launch { preferences.appPriorityListFlow.collectLatest { appPriorityList = it } }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn?.let {
            if (shouldIgnore(it.packageName)) return
            if (isJunkNotification(it)) return
            if (isAppAllowed(it.packageName)) {
                if (shouldSkipUpdate(it)) return
                serviceScope.launch { processAndPost(it) }
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

    private fun shouldSkipUpdate(sbn: StatusBarNotification): Boolean {
        val key = sbn.key
        val now = System.currentTimeMillis()
        val lastTime = lastUpdateMap[key] ?: 0L
        val previousIsland = activeIslands[key]

        val extras = sbn.notification.extras
        val currTitle = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val currText = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val currSub = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""

        if (previousIsland != null) {
            if (currTitle != previousIsland.title || currText != previousIsland.text || currSub != previousIsland.subText) {
                lastUpdateMap[key] = now
                return false
            }
        }

        if (now - lastTime < UPDATE_INTERVAL_MS) return true
        lastUpdateMap[key] = now
        return false
    }

    private fun isJunkNotification(sbn: StatusBarNotification): Boolean {
        val notification = sbn.notification
        val extras = notification.extras
        val pkg = sbn.packageName

        // 1. PRIORITY PASS (Allow Progress/Media/Calls/Nav)
        val hasProgress = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0) > 0 ||
                extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE)
        val isSpecial = notification.category == Notification.CATEGORY_TRANSPORT ||
                notification.category == Notification.CATEGORY_CALL ||
                notification.category == Notification.CATEGORY_NAVIGATION ||
                extras.getString(Notification.EXTRA_TEMPLATE)?.contains("MediaStyle") == true

        if (hasProgress || isSpecial) return false

        // 2. Block Group Summaries
        if ((notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return true

        // 3. Extract Content
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim() ?: ""
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.trim() ?: ""

        // 4. Empty Check
        if (title.isEmpty() && text.isEmpty() && subText.isEmpty()) return true

        // --- FIX FOR ISSUE #7: Block Package Name Leaks ---
        // If the Title or Text matches the Package Name (e.g. "com.whatsapp"), it is a placeholder/junk.
        if (title == pkg || text == pkg) {
            Log.d(TAG, "Ignored Placeholder (Package Name match): $pkg")
            return true
        }
        // --------------------------------------------------

        // 5. App Name Placeholder Check
        // (Title == AppName, Text == Empty) -> Usually "WhatsApp checking for messages"
        val appName = try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
        } catch (e: Exception) { "" }

        if (title == appName && text.isEmpty() && subText.isEmpty()) return true

        // 6. System Noise
        if (title.contains("running in background", true)) return true
        if (text.contains("tap for more info", true)) return true
        if (text.contains("displaying over other apps", true)) return true

        return false
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private suspend fun processAndPost(sbn: StatusBarNotification) {
        try {
            val extras = sbn.notification.extras

            val isCall = sbn.notification.category == Notification.CATEGORY_CALL
            val isNavigation = sbn.notification.category == Notification.CATEGORY_NAVIGATION || sbn.packageName.contains("maps")
            val progressMax = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0)
            val hasProgress = progressMax > 0 || extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE)
            val chronometerBase = sbn.notification.`when`
            val isTimer = (extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER) || sbn.notification.category == Notification.CATEGORY_ALARM) && chronometerBase > 0
            val isMedia = extras.getString(Notification.EXTRA_TEMPLATE)?.contains("MediaStyle") == true

            val type = when {
                isCall -> NotificationType.CALL
                isNavigation -> NotificationType.NAVIGATION
                isTimer -> NotificationType.TIMER
                hasProgress -> NotificationType.PROGRESS
                isMedia -> NotificationType.MEDIA
                else -> NotificationType.STANDARD
            }

            val config = preferences.getAppConfig(sbn.packageName).first()
            if (!config.contains(type.name)) return

            val key = sbn.key
            val isUpdate = activeIslands.containsKey(key)
            val bridgeId = sbn.key.hashCode()

            if (!isUpdate && activeIslands.size >= MAX_ISLANDS) {
                handleLimitReached(type, sbn.packageName)
                if (activeIslands.size >= MAX_ISLANDS) return
            }

            val title = extras.getString(Notification.EXTRA_TITLE) ?: sbn.packageName
            val picKey = "pic_${bridgeId}"

            val appIslandConfig = preferences.getAppIslandConfig(sbn.packageName).first()
            val globalConfig = preferences.globalConfigFlow.first()
            val finalConfig = appIslandConfig.mergeWith(globalConfig)

            val data: HyperIslandData = when (type) {
                NotificationType.CALL -> callTranslator.translate(sbn, picKey, finalConfig)

                // *** NEW: Fetch Custom Layout for Navigation ***
                NotificationType.NAVIGATION -> {
                    val navLayout = preferences.getEffectiveNavLayout(sbn.packageName).first()
                    navTranslator.translate(sbn, picKey, finalConfig, navLayout.first, navLayout.second)
                }

                NotificationType.TIMER -> timerTranslator.translate(sbn, picKey, finalConfig)
                NotificationType.PROGRESS -> progressTranslator.translate(sbn, title, picKey, finalConfig)
                else -> standardTranslator.translate(sbn, picKey, finalConfig)
            }

            val newContentHash = data.jsonParam.hashCode()
            val previousIsland = activeIslands[key]

            if (isUpdate && previousIsland != null) {
                if (previousIsland.lastContentHash == newContentHash) return
            }

            postNotification(sbn, bridgeId, data)

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
            Log.e(TAG, "Error", e)
        }
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

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun postNotification(sbn: StatusBarNotification, bridgeId: Int, data: HyperIslandData) {
        val notificationBuilder = NotificationCompat.Builder(this, ISLAND_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.service_active))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addExtras(data.resources)

        sbn.notification.contentIntent?.let { notificationBuilder.setContentIntent(it) }
        val notification = notificationBuilder.build()
        notification.extras.putString("miui.focus.param", data.jsonParam)

        NotificationManagerCompat.from(this).notify(bridgeId, notification)
        activeTranslations[sbn.key] = bridgeId
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