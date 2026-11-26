package com.d4viddf.hyperbridge.service.translators

import com.d4viddf.hyperbridge.models.NotificationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IslandLifetimePolicyTest {

    @Test
    fun callsAndNavigationOutliveTheOneHourDefault() {
        assertEquals(86_400_000, IslandLifetimePolicy.sourceOwnedTimeout(NotificationType.CALL))
        assertEquals(86_400_000, IslandLifetimePolicy.sourceOwnedTimeout(NotificationType.NAVIGATION))
    }

    @Test
    fun otherTypesKeepTheirOwnTimeout() {
        NotificationType.entries
            .filter { it != NotificationType.CALL && it != NotificationType.NAVIGATION }
            .forEach { assertNull(it.name, IslandLifetimePolicy.sourceOwnedTimeout(it)) }
    }
}
