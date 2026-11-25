package com.d4viddf.hyperbridge.service.translators

import com.d4viddf.hyperbridge.models.NotificationType

/**
 * HyperOS closes a focus Island that does not declare `islandTimeout` after its default lifetime,
 * one hour since the SystemUI update Xiaomi shipped in January (#168). That is what made the call
 * Island vanish an hour into a long WhatsApp video call.
 *
 * Calls and navigation are already excluded from Hyper Bridge's own auto-hide and end when their
 * source notification goes away, so they declare the same one-day value the permanent and VPN
 * Islands use ([VpnTranslator.PERSISTENT_ISLAND_TIMEOUT_MILLIS]). Every other type keeps whatever
 * its translator passes today.
 */
object IslandLifetimePolicy {
    const val SOURCE_OWNED_ISLAND_TIMEOUT = VpnTranslator.PERSISTENT_ISLAND_TIMEOUT_MILLIS

    fun sourceOwnedTimeout(type: NotificationType): Int? = when (type) {
        NotificationType.CALL,
        NotificationType.NAVIGATION -> SOURCE_OWNED_ISLAND_TIMEOUT
        else -> null
    }
}
