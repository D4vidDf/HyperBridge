package com.d4viddf.hyperbridge.service.systemtoggles

/**
 * The system switches the island reports on. Status only: Android does not let a third-party
 * app flip any of them, so the island just says what the user changed.
 *
 * [settingsAction] is the Settings screen opened when the island is tapped (null opens the
 * Settings root). [settleMillis] is how long a change must hold still before it is shown.
 */
enum class SystemToggle(
    val settingsAction: String?,
    val settleMillis: Long = SystemToggleTracker.DEFAULT_SETTLE_MILLIS
) {
    BATTERY_SAVER("android.settings.BATTERY_SAVER_SETTINGS"),
    AIRPLANE_MODE("android.settings.AIRPLANE_MODE_SETTINGS"),
    DO_NOT_DISTURB("android.settings.SOUND_SETTINGS"),
    // Ringer changes often arrive together with Do Not Disturb ones, so this waits a little longer.
    RINGER("android.settings.SOUND_SETTINGS", SystemToggleTracker.RINGER_SETTLE_MILLIS),
    BLUETOOTH("android.settings.BLUETOOTH_SETTINGS"),
    WIFI("android.settings.WIFI_SETTINGS"),
    MOBILE_DATA("android.settings.DATA_ROAMING_SETTINGS"),
    HOTSPOT("android.settings.WIRELESS_SETTINGS"),
    NFC("android.settings.NFC_SETTINGS"),
    LOCATION("android.settings.LOCATION_SOURCE_SETTINGS"),
    AUTO_BRIGHTNESS("android.settings.DISPLAY_SETTINGS");

    companion object {
        /** Reads the stored list. Null (never saved) means the caller's default; unknown names are dropped. */
        fun parseSet(serialized: String?): Set<SystemToggle>? {
            if (serialized == null) return null
            return serialized.split(",")
                .mapNotNull { name -> entries.firstOrNull { it.name == name.trim() } }
                .toSet()
        }

        fun serialize(toggles: Set<SystemToggle>): String = toggles.joinToString(",") { it.name }
    }
}

/** ON/OFF for the switches, SILENT/VIBRATE/RING for the ringer. */
enum class ToggleValue(val colorHex: String) {
    ON(SystemToggleSignals.ON_COLOR),
    OFF(SystemToggleSignals.OFF_COLOR),
    SILENT(SystemToggleSignals.ATTENTION_COLOR),
    VIBRATE(SystemToggleSignals.ATTENTION_COLOR),
    RING(SystemToggleSignals.ON_COLOR)
}

/** What the island shows: [toggle] changed to [value]. */
data class SystemToggleEvent(val toggle: SystemToggle, val value: ToggleValue)

/**
 * Pure mapping from the raw values the framework reports to [ToggleValue]. A null result means
 * the value is a transitional state (TURNING_ON, ENABLING...) or unknown, and is ignored: the
 * final state follows in another broadcast.
 *
 * The integers are the constants of WifiManager, NfcAdapter, AudioManager and NotificationManager,
 * copied here so the logic runs on the JVM without the framework.
 */
object SystemToggleSignals {
    const val ON_COLOR = "#34C759"
    const val OFF_COLOR = "#8E8E93"
    const val ATTENTION_COLOR = "#FF9500"

    // WifiManager.WIFI_STATE_*
    private const val WIFI_STATE_DISABLED = 1
    private const val WIFI_STATE_ENABLED = 3

    // NfcAdapter.STATE_*
    private const val NFC_STATE_OFF = 1
    private const val NFC_STATE_ON = 3

    // WifiManager.WIFI_AP_STATE_* (hidden)
    private const val AP_STATE_DISABLED = 11
    private const val AP_STATE_ENABLED = 13

    // AudioManager.RINGER_MODE_*
    private const val RINGER_SILENT = 0
    private const val RINGER_VIBRATE = 1
    private const val RINGER_NORMAL = 2

    // NotificationManager.INTERRUPTION_FILTER_*
    private const val FILTER_ALL = 1
    private const val FILTER_PRIORITY = 2
    private const val FILTER_NONE = 3
    private const val FILTER_ALARMS = 4

    fun fromBoolean(on: Boolean): ToggleValue = if (on) ToggleValue.ON else ToggleValue.OFF

    fun fromWifiState(state: Int): ToggleValue? = when (state) {
        WIFI_STATE_DISABLED -> ToggleValue.OFF
        WIFI_STATE_ENABLED -> ToggleValue.ON
        else -> null
    }

    /**
     * Settings.Global.wifi_on: 1 on, 2 on (airplane override), 0 off, 3 off (airplane mode
     * switched it off). Used to read the state at registration time, since
     * WifiManager.getWifiState needs ACCESS_WIFI_STATE.
     */
    fun fromWifiSetting(value: Int): ToggleValue = fromBoolean(value == 1 || value == 2)

    fun fromNfcState(state: Int): ToggleValue? = when (state) {
        NFC_STATE_OFF -> ToggleValue.OFF
        NFC_STATE_ON -> ToggleValue.ON
        else -> null
    }

    fun fromHotspotState(state: Int): ToggleValue? = when (state) {
        AP_STATE_DISABLED -> ToggleValue.OFF
        AP_STATE_ENABLED -> ToggleValue.ON
        else -> null
    }

    fun fromRingerMode(mode: Int): ToggleValue? = when (mode) {
        RINGER_SILENT -> ToggleValue.SILENT
        RINGER_VIBRATE -> ToggleValue.VIBRATE
        RINGER_NORMAL -> ToggleValue.RING
        else -> null
    }

    /** Priority only, alarms only and total silence all mean Do Not Disturb is on. */
    fun fromInterruptionFilter(filter: Int): ToggleValue? = when (filter) {
        FILTER_ALL -> ToggleValue.OFF
        FILTER_PRIORITY, FILTER_NONE, FILTER_ALARMS -> ToggleValue.ON
        else -> null
    }
}

/**
 * Decides which changes become an island. Pure and driven by explicit timestamps so it runs on
 * the JVM.
 *
 * Flow: the controller [seed]s the current state of every toggle when it starts, then calls
 * [submit] for each broadcast or setting change and, if that returns true, [settle] for the same
 * toggle after [SystemToggle.settleMillis]. [settle] returns the event to show, or null when:
 * - the value is the one already known (repeated broadcast, or a quick flip back and forth that
 *   ended where it began);
 * - the toggle is switched off in the settings screen;
 * - another switch caused it (Airplane mode turns the radios off, Do Not Disturb moves the
 *   ringer, the hotspot takes Wi-Fi down). That change is still remembered, just not shown.
 */
class SystemToggleTracker(
    enabledToggles: Set<SystemToggle> = SystemToggle.entries.toSet()
) {
    private val shown = HashMap<SystemToggle, ToggleValue>()
    private val current = HashMap<SystemToggle, ToggleValue>()
    private val changedAt = HashMap<SystemToggle, Long>()
    private var enabled: Set<SystemToggle> = enabledToggles

    @Synchronized
    fun setEnabledToggles(toggles: Set<SystemToggle>) {
        enabled = toggles
    }

    /** Records the state at registration time. Never produces an event. */
    @Synchronized
    fun seed(toggle: SystemToggle, value: ToggleValue) {
        shown[toggle] = value
        current[toggle] = value
        changedAt.remove(toggle)
    }

    /**
     * Records a reported value. Returns true when a [settle] call should follow. A null value
     * (transitional state) is ignored. A toggle that was never seeded takes its first report as
     * the starting state, so an initial or sticky broadcast cannot show an island.
     */
    @Synchronized
    fun submit(toggle: SystemToggle, value: ToggleValue?, nowMillis: Long): Boolean {
        if (value == null) return false
        if (toggle !in shown) {
            seed(toggle, value)
            return false
        }
        if (current[toggle] != value) {
            current[toggle] = value
            changedAt[toggle] = nowMillis
        }
        return value != shown[toggle]
    }

    @Synchronized
    fun settle(toggle: SystemToggle): SystemToggleEvent? {
        val value = current[toggle] ?: return null
        if (value == shown[toggle]) return null
        shown[toggle] = value
        if (toggle !in enabled) return null
        if (isCausedByAnotherToggle(toggle)) return null
        return SystemToggleEvent(toggle, value)
    }

    @Synchronized
    fun clear() {
        shown.clear()
        current.clear()
        changedAt.clear()
    }

    private fun isCausedByAnotherToggle(toggle: SystemToggle): Boolean {
        val at = changedAt[toggle] ?: return false
        return CAUSES[toggle].orEmpty().any { (cause, windowMillis) ->
            val causeAt = changedAt[cause] ?: return@any false
            kotlin.math.abs(at - causeAt) <= windowMillis
        }
    }

    companion object {
        /** A change must hold this long before it shows, so a quick flip back and forth is dropped. */
        const val DEFAULT_SETTLE_MILLIS = 400L
        const val RINGER_SETTLE_MILLIS = 700L

        /** How long the island stays up. */
        const val DISPLAY_MILLIS = 3_000L

        /** Seconds the island declares to the system, as a backstop in case the app is killed while it is up. */
        const val ISLAND_TIMEOUT_SECONDS = 5

        private const val RADIO_WINDOW_MILLIS = 5_000L
        private const val SOUND_WINDOW_MILLIS = 1_500L

        /**
         * toggle -> switches whose change explains it, with how close in time they must be.
         * One direction only: Do Not Disturb moves the ringer, never the other way round.
         */
        private val CAUSES: Map<SystemToggle, List<Pair<SystemToggle, Long>>> = mapOf(
            SystemToggle.WIFI to listOf(
                SystemToggle.AIRPLANE_MODE to RADIO_WINDOW_MILLIS,
                SystemToggle.HOTSPOT to RADIO_WINDOW_MILLIS
            ),
            SystemToggle.BLUETOOTH to listOf(SystemToggle.AIRPLANE_MODE to RADIO_WINDOW_MILLIS),
            SystemToggle.NFC to listOf(SystemToggle.AIRPLANE_MODE to RADIO_WINDOW_MILLIS),
            SystemToggle.HOTSPOT to listOf(SystemToggle.AIRPLANE_MODE to RADIO_WINDOW_MILLIS),
            SystemToggle.RINGER to listOf(SystemToggle.DO_NOT_DISTURB to SOUND_WINDOW_MILLIS)
        )
    }
}
