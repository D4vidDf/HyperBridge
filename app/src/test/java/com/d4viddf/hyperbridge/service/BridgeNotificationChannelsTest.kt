package com.d4viddf.hyperbridge.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BridgeNotificationChannelsTest {
    @Test
    fun existingStableIdsRemainUniqueAndSourceOwnsAlerts() {
        val contracts = BridgeNotificationChannels.contracts
        assertEquals(contracts.size, contracts.map { it.id }.distinct().size)
        assertTrue(contracts.all { it.sourceOwnsAudibleAlert && !it.showsBadge })
        assertEquals(1, BridgeNotificationChannels.SCHEMA_VERSION)
    }

    @Test
    fun widgetOwnershipUsesChannelInsteadOfOverlappingNumericRange() {
        assertTrue(BridgeNotificationChannels.isWidget(BridgeNotificationChannels.WIDGET))
        assertTrue(!BridgeNotificationChannels.isWidget(BridgeNotificationChannels.ACTIVE))
        assertTrue(!BridgeNotificationChannels.isWidget(null))
    }
}
