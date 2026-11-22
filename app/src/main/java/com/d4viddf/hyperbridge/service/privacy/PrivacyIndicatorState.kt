package com.d4viddf.hyperbridge.service.privacy

/** What is currently using a privacy-sensitive sensor. At least one of them is true. */
data class PrivacyIndicatorState(
    val microphone: Boolean,
    val camera: Boolean
) {
    val kind: PrivacyIndicatorKind
        get() = when {
            microphone && camera -> PrivacyIndicatorKind.MICROPHONE_AND_CAMERA
            camera -> PrivacyIndicatorKind.CAMERA
            else -> PrivacyIndicatorKind.MICROPHONE
        }
}

enum class PrivacyIndicatorKind(val colorHex: String) {
    MICROPHONE(PrivacyIndicatorResolver.MICROPHONE_COLOR),
    CAMERA(PrivacyIndicatorResolver.CAMERA_COLOR),
    /** Highlight follows the camera (green), the icon shows both sensors. */
    MICROPHONE_AND_CAMERA(PrivacyIndicatorResolver.CAMERA_COLOR)
}

/**
 * Pure mapping from the two signals to what the island shows, so it can be unit tested.
 *
 * Microphone: AudioManager recording callbacks (any app recording; the package is not visible to
 * third-party apps). Camera: CameraManager availability callbacks, where an "unavailable" camera
 * id means some client has it open.
 */
object PrivacyIndicatorResolver {
    const val MICROPHONE_COLOR = "#FF9500"
    const val CAMERA_COLOR = "#34C759"
    /** Both signals flap (apps reopening the camera, recordings restarting): settle before reposting. */
    const val DEBOUNCE_MILLIS = 300L

    /** A recording shows almost at once; nothing in the system grabs the mic for a blink. */
    const val MIC_SHOW_DELAY_MILLIS = 150L

    /**
     * Camera-only waits a bit longer: face unlock opens the front camera for a fraction of a
     * second on every wake, a real camera session lasts longer.
     */
    const val CAMERA_SHOW_DELAY_MILLIS = 800L

    fun resolve(microphoneActive: Boolean, cameraActive: Boolean): PrivacyIndicatorState? =
        if (microphoneActive || cameraActive) PrivacyIndicatorState(microphoneActive, cameraActive) else null

    fun settleDelayMillis(isShowing: Boolean, microphone: Boolean, camera: Boolean): Long = when {
        isShowing -> DEBOUNCE_MILLIS
        microphone -> MIC_SHOW_DELAY_MILLIS
        camera -> CAMERA_SHOW_DELAY_MILLIS
        else -> DEBOUNCE_MILLIS
    }
}

/**
 * Tracks which camera ids are reported unavailable. The first availability registration
 * reports every camera, so the set is seeded by the callbacks themselves.
 */
class CameraUsageTracker {
    private val unavailable = HashSet<String>()

    @Synchronized
    fun onUnavailable(cameraId: String) {
        unavailable.add(cameraId)
    }

    @Synchronized
    fun onAvailable(cameraId: String) {
        unavailable.remove(cameraId)
    }

    @Synchronized
    fun clear() = unavailable.clear()

    @get:Synchronized
    val inUse: Boolean
        get() = unavailable.isNotEmpty()
}
