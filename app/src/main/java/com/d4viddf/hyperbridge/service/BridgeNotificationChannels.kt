package com.d4viddf.hyperbridge.service

data class BridgeChannelContract(
    val id: String,
    val sourceOwnsAudibleAlert: Boolean,
    val showsBadge: Boolean
)

/**
 * These IDs intentionally remain compatible with upstream and Pass 1/2. Their shipped defaults
 * were already silent, so a versioned replacement would only discard user channel choices.
 */
object BridgeNotificationChannels {
    const val SCHEMA_VERSION = 1
    const val ACTIVE = "hyper_bridge_notification_channel"
    const val WIDGET = "hyper_bridge_widget_channel"
    const val LIVE_UPDATE = "hyper_bridge_live_update_channel"
    const val WATCH_RELAY = "hyper_bridge_watch_relay_channel"

    val contracts = listOf(
        BridgeChannelContract(ACTIVE, sourceOwnsAudibleAlert = true, showsBadge = false),
        BridgeChannelContract(WIDGET, sourceOwnsAudibleAlert = true, showsBadge = false),
        BridgeChannelContract(LIVE_UPDATE, sourceOwnsAudibleAlert = true, showsBadge = false),
        BridgeChannelContract(WATCH_RELAY, sourceOwnsAudibleAlert = true, showsBadge = false)
    )

    fun isWidget(channelId: String?): Boolean = channelId == WIDGET
    fun isWatchRelay(channelId: String?): Boolean = channelId == WATCH_RELAY
}
