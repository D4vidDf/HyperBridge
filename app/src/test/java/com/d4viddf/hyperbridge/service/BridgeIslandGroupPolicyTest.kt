package com.d4viddf.hyperbridge.service

import com.d4viddf.hyperbridge.service.BridgeIslandGroupPolicy.OwnNotification
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BridgeIslandGroupPolicyTest {
    private val summary = OwnNotification(BridgeIslandGroupPolicy.SUMMARY_ID, BridgeIslandGroupPolicy.GROUP_KEY)
    private val island = OwnNotification(123456, BridgeIslandGroupPolicy.GROUP_KEY)
    private val widget = OwnNotification(9003, null)
    private val relay = OwnNotification(-15, "something_else")

    @Test
    fun summaryIdIsOutsideEveryOtherIdRange() {
        assertTrue(BridgeIslandGroupPolicy.SUMMARY_ID < 0)
        assertTrue(BridgeIslandGroupPolicy.SUMMARY_ID != -20301) // ServiceHealthNotifier (#330)
    }

    @Test
    fun groupOnlyAppliesFromAndroid16() {
        // Force grouping at 2 is Android 16+; below that the summary only adds a shade row (#358).
        assertFalse(BridgeIslandGroupPolicy.appliesTo(34))
        assertFalse(BridgeIslandGroupPolicy.appliesTo(35))
        assertTrue(BridgeIslandGroupPolicy.appliesTo(36))
        assertTrue(BridgeIslandGroupPolicy.appliesTo(37))
    }

    @Test
    fun childrenAreOnlyGroupedNonSummaryNotifications() {
        assertTrue(BridgeIslandGroupPolicy.isChild(island))
        assertFalse(BridgeIslandGroupPolicy.isChild(summary))
        assertFalse(BridgeIslandGroupPolicy.isChild(widget))
        assertFalse(BridgeIslandGroupPolicy.isChild(relay))
    }

    @Test
    fun summaryIsNeededUntilItExists() {
        assertTrue(BridgeIslandGroupPolicy.needsSummary(emptyList()))
        assertTrue(BridgeIslandGroupPolicy.needsSummary(listOf(island, widget)))
        assertFalse(BridgeIslandGroupPolicy.needsSummary(listOf(summary)))
        assertFalse(BridgeIslandGroupPolicy.needsSummary(listOf(summary, island)))
    }

    @Test
    fun summaryIsReleasedOnlyWhenPresentAndChildless() {
        assertTrue(BridgeIslandGroupPolicy.shouldReleaseSummary(listOf(summary)))
        assertTrue(BridgeIslandGroupPolicy.shouldReleaseSummary(listOf(summary, widget, relay)))
        assertFalse(BridgeIslandGroupPolicy.shouldReleaseSummary(listOf(summary, island)))
        assertFalse(BridgeIslandGroupPolicy.shouldReleaseSummary(listOf(island)))
        assertFalse(BridgeIslandGroupPolicy.shouldReleaseSummary(emptyList()))
    }

    @Test
    fun summaryIsRestoredOnlyWhenChildrenOutliveIt() {
        // The user swiped the summary row away, or the process died between the two posts (#372).
        assertTrue(BridgeIslandGroupPolicy.shouldRestoreSummary(listOf(island)))
        assertTrue(BridgeIslandGroupPolicy.shouldRestoreSummary(listOf(island, widget, relay)))
        assertFalse(BridgeIslandGroupPolicy.shouldRestoreSummary(listOf(summary, island)))
        assertFalse(BridgeIslandGroupPolicy.shouldRestoreSummary(listOf(widget, relay)))
        assertFalse(BridgeIslandGroupPolicy.shouldRestoreSummary(emptyList()))
    }

    @Test
    fun reconcileNeverBothReleasesAndRestores() {
        val worlds = listOf(
            emptyList(), listOf(summary), listOf(island), listOf(summary, island),
            listOf(widget, relay), listOf(summary, widget), listOf(island, relay)
        )
        for (active in worlds) {
            assertFalse(
                "ambiguous for $active",
                BridgeIslandGroupPolicy.shouldReleaseSummary(active) &&
                        BridgeIslandGroupPolicy.shouldRestoreSummary(active)
            )
        }
    }

    @Test
    fun releaseWaitsLongerThanAShizukuCancelAndRepost() {
        // ShizukuManager.notifyWithCancel cancels, waits 20 ms, re-posts; plus binder latency.
        assertTrue(BridgeIslandGroupPolicy.RELEASE_DELAY_MS >= 1_000L)
    }
}
