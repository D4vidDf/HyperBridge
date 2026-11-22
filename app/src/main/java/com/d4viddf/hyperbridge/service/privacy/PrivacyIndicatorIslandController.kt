package com.d4viddf.hyperbridge.service.privacy

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.AudioRecordingConfiguration
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.data.theme.ThemeRepository
import com.d4viddf.hyperbridge.service.BridgeNotificationChannels
import com.d4viddf.hyperbridge.service.translators.PrivacyIndicatorTranslator
import com.d4viddf.hyperbridge.util.ShizukuManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Privacy indicators island: orange for the microphone, green for the camera. Like the VPN and
 * debugging islands it owns its notification. The signals need no permission: AudioManager
 * recording callbacks and CameraManager availability callbacks, registered only while the
 * toggle is on and the listener service is running.
 */
class PrivacyIndicatorIslandController(
    private val context: Context,
    private val scope: CoroutineScope,
    private val preferences: AppPreferences,
    private val themeRepository: ThemeRepository,
    private val onIslandActiveChanged: (Boolean) -> Unit = {}
) {
    private val translator = PrivacyIndicatorTranslator(context, themeRepository)
    private val mutex = Mutex()
    private val handler = Handler(Looper.getMainLooper())
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val cameraManager = context.getSystemService(CameraManager::class.java)
    private val cameraTracker = CameraUsageTracker()

    @Volatile private var enabled = false
    @Volatile private var microphoneActive = false
    private var callbacksRegistered = false
    private var refreshJob: Job? = null
    private var shownState: PrivacyIndicatorState? = null
    private var reportedActive = false

    private val audioCallback = object : AudioManager.AudioRecordingCallback() {
        override fun onRecordingConfigChanged(configs: MutableList<AudioRecordingConfiguration>?) {
            microphoneActive = configs.isRecordingAudio()
            scheduleRefresh()
        }
    }

    private val cameraCallback = object : CameraManager.AvailabilityCallback() {
        override fun onCameraAvailable(cameraId: String) {
            cameraTracker.onAvailable(cameraId)
            scheduleRefresh()
        }

        override fun onCameraUnavailable(cameraId: String) {
            cameraTracker.onUnavailable(cameraId)
            scheduleRefresh()
        }
    }

    fun start() {
        scope.launch {
            preferences.privacyIndicatorIslandEnabledFlow.collectLatest { shouldEnable ->
                enabled = shouldEnable
                if (shouldEnable) {
                    registerCallbacks()
                    scheduleRefresh()
                } else {
                    unregisterCallbacks()
                    refreshJob?.cancel()
                    mutex.withLock { clear() }
                }
            }
        }
    }

    fun stop() {
        enabled = false
        unregisterCallbacks()
        refreshJob?.cancel()
        shownState = null
        cancelIsland()
    }

    private fun registerCallbacks() {
        if (callbacksRegistered) return
        callbacksRegistered = true
        runCatching {
            audioManager?.let {
                microphoneActive = it.activeRecordingConfigurations.isRecordingAudio()
                it.registerAudioRecordingCallback(audioCallback, handler)
            }
        }
        // The first registration immediately reports the current status of every camera.
        runCatching { cameraManager?.registerAvailabilityCallback(cameraCallback, handler) }
    }

    private fun unregisterCallbacks() {
        if (!callbacksRegistered) return
        callbacksRegistered = false
        runCatching { audioManager?.unregisterAudioRecordingCallback(audioCallback) }
        runCatching { cameraManager?.unregisterAvailabilityCallback(cameraCallback) }
        microphoneActive = false
        cameraTracker.clear()
    }

    /**
     * Both signals flap, so settle for a moment before touching the notification. Showing waits
     * longer than updating or hiding, so a short system use (face unlock opening the front
     * camera on wake) does not flash the island.
     */
    private fun scheduleRefresh() {
        if (!enabled) return
        refreshJob?.cancel()
        val delayMillis = PrivacyIndicatorResolver.settleDelayMillis(
            isShowing = shownState != null,
            microphone = microphoneActive,
            camera = cameraTracker.inUse
        )
        refreshJob = scope.launch {
            delay(delayMillis)
            refresh()
        }
    }

    /** A recording the system has silenced (another app owns the mic) is not live audio. */
    private fun List<AudioRecordingConfiguration>?.isRecordingAudio(): Boolean =
        this?.any { !it.isClientSilenced } == true

    private suspend fun refresh() = mutex.withLock {
        if (!enabled) return@withLock
        val state = PrivacyIndicatorResolver.resolve(microphoneActive, cameraTracker.inUse)
        if (state == null) {
            clear()
            return@withLock
        }
        if (state != shownState) render(state)
    }

    private suspend fun render(state: PrivacyIndicatorState) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            setReportedActive(false)
            return
        }
        val data = translator.translate(
            state = state,
            config = preferences.getGlobalConfigSync()
        )
        val builder = NotificationCompat.Builder(context, BridgeNotificationChannels.ACTIVE)
            .setSmallIcon(R.drawable.ic_privacy_indicator)
            .setContentTitle(translator.title())
            .setContentText(translator.content(state))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setDefaults(0)
            .setSound(null)
            .setVibrate(null)
            .addExtras(data.resources)
        builder.setContentIntent(privacySettingsIntent())
        val notification = builder.build()
        notification.extras.putString("miui.focus.param", data.jsonParam)
        if (shownState != null) NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        else ShizukuManager.notifyInPlace(context, NOTIFICATION_ID, notification)
        shownState = state
        setReportedActive(true)
    }

    /**
     * The system privacy settings, where the permission manager and usage history live.
     * ACTION_REVIEW_PERMISSION_USAGE would land on the dashboard directly, but it is a system API
     * that needs GRANT_RUNTIME_PERMISSIONS to start.
     */
    private fun privacySettingsIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        NOTIFICATION_ID,
        Intent(Settings.ACTION_PRIVACY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun clear() {
        if (shownState == null && !reportedActive) return
        shownState = null
        cancelIsland()
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
        val NOTIFICATION_ID = PrivacyIndicatorTranslator.BUSINESS.hashCode()
    }
}
