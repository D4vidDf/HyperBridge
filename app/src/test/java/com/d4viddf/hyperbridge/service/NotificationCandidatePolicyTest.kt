package com.d4viddf.hyperbridge.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationCandidatePolicyTest {
    private fun candidate(
        key: String,
        quality: SourceCandidateQuality,
        postTime: Long = 1L,
        pkg: String = "com.whatsapp",
        id: Int = 7,
        tag: String? = "conversation"
    ) = SourceNotificationCandidate(key, pkg, id, tag, postTime, quality)

    @Test
    fun rawUpstreamTitleAndTextWithoutMessagingStyleAreAcceptedWithoutRefresh() {
        val shouldRefresh = NotificationRefreshPolicy.shouldRefresh(
            NotificationRefreshSignals("com.whatsapp", "Kişi", "İleti", false, false)
        )
        val junk = NotificationAcceptancePolicy.isJunk(
            NotificationAcceptanceSignals(
                packageName = "com.whatsapp",
                title = "Kişi",
                text = "İleti",
                hasMessageContent = false,
                hasProgressOrSpecialState = false,
                containsBlockedTerm = false
            )
        )

        assertFalse(shouldRefresh)
        assertFalse(junk)
    }

    @Test
    fun sparseCallbackRequiresRefreshBeforeEmptyAcceptanceDecision() {
        assertTrue(
            NotificationRefreshPolicy.shouldRefresh(
                NotificationRefreshSignals("com.whatsapp", "", "", false, false)
            )
        )
    }

    @Test
    fun usefulRefreshedContentStopsRetrying() {
        assertFalse(
            NotificationRefreshPolicy.shouldRefresh(
                NotificationRefreshSignals("com.whatsapp", "Alice", "Hello", false, false)
            )
        )
    }

    @Test
    fun refreshIsShortAndBounded() {
        assertTrue(NotificationRefreshPolicy.REFRESH_DELAY_MS in 100L..150L)
        assertTrue(NotificationRefreshPolicy.MAX_REFRESH_ATTEMPTS == 2)
    }

    @Test
    fun persistentStateDoesNotWaitForContent() {
        assertFalse(
            NotificationRefreshPolicy.shouldRefresh(
                NotificationRefreshSignals("com.example", "", "", false, true)
            )
        )
    }

    @Test
    fun usefulStandardContentIsAcceptedWithoutGroupSummaryGate() {
        assertFalse(
            NotificationAcceptancePolicy.isJunk(
                NotificationAcceptanceSignals("com.whatsapp", "Sender", "Content", false, false, false)
            )
        )
    }

    @Test
    fun genuinelyEmptyContentIsJunkAfterRefreshAttempts() {
        assertTrue(
            NotificationAcceptancePolicy.isJunk(
                NotificationAcceptanceSignals("com.whatsapp", "", "", false, false, false)
            )
        )
    }

    @Test
    fun periodicSyncSkipsClassificationButDiscreteRecoveryRunsIt() {
        assertFalse(OngoingRecoveryPolicy.shouldClassifyShadeNotifications(refresh = false))
        assertTrue(OngoingRecoveryPolicy.shouldClassifyShadeNotifications(refresh = true))
    }

    @Test
    fun usefulRawCandidateWinsOverWorseRefresh() {
        val raw = candidate("raw", SourceCandidateQuality.USABLE)
        val refreshed = candidate("raw", SourceCandidateQuality.SPARSE, postTime = 2L)

        assertFalse(SourceNotificationCandidatePolicy.shouldPrefer(raw, refreshed, "raw"))
    }

    @Test
    fun betterRefreshWinsOverSparseRawCandidate() {
        val raw = candidate("raw", SourceCandidateQuality.SPARSE)
        val refreshed = candidate("raw", SourceCandidateQuality.USABLE, postTime = 2L)

        assertTrue(SourceNotificationCandidatePolicy.shouldPrefer(raw, refreshed, "raw"))
    }

    @Test
    fun replacementWithSamePackageIdAndTagCanWin() {
        val raw = candidate("old-key", SourceCandidateQuality.SPARSE)
        val replacement = candidate("new-key", SourceCandidateQuality.USABLE, postTime = 2L)

        assertTrue(SourceNotificationCandidatePolicy.isSameSlot(raw, replacement))
        assertTrue(SourceNotificationCandidatePolicy.shouldPrefer(raw, replacement, "old-key"))
    }

    @Test
    fun fallbackDoesNotMergeDifferentNotificationSlots() {
        val raw = candidate("old-key", SourceCandidateQuality.SPARSE)
        val unrelated = candidate("new-key", SourceCandidateQuality.USABLE, id = 8)

        assertFalse(SourceNotificationCandidatePolicy.isSameSlot(raw, unrelated))
        assertFalse(SourceNotificationCandidatePolicy.shouldPrefer(raw, unrelated, "old-key"))
    }

    @Test
    fun fallbackDoesNotMergeDifferentTags() {
        val raw = candidate("old-key", SourceCandidateQuality.SPARSE, tag = "first")
        val unrelated = candidate("new-key", SourceCandidateQuality.USABLE, tag = "second")

        assertFalse(SourceNotificationCandidatePolicy.isSameSlot(raw, unrelated))
    }

    @Test
    fun appRemovalDoesNotSuppressPendingUsefulEvent() {
        assertFalse(
            PendingRemovalPolicy.shouldSuppress(
                PendingRemovalSignals(true, 2L, 1L, explicitUserDismissal = false, activeExactOrReplacement = false)
            )
        )
    }

    @Test
    fun explicitDismissalSuppressesOnlyWhenNoReplacementIsActive() {
        assertTrue(
            PendingRemovalPolicy.shouldSuppress(
                PendingRemovalSignals(true, 2L, 1L, explicitUserDismissal = true, activeExactOrReplacement = false)
            )
        )
        assertFalse(
            PendingRemovalPolicy.shouldSuppress(
                PendingRemovalSignals(true, 2L, 1L, explicitUserDismissal = true, activeExactOrReplacement = true)
            )
        )
    }

    @Test
    fun messageAndStandardRemainIndependentlyEnabled() {
        val enabled = setOf("MESSAGE", "STANDARD")

        assertTrue(NotificationTypeEnablementPolicy.isEnabled(enabled, "MESSAGE"))
        assertTrue(NotificationTypeEnablementPolicy.isEnabled(enabled, "STANDARD"))
        assertEquals(
            "MESSAGE",
            NotificationTypeEnablementPolicy.resolveEnabledType(enabled, "MESSAGE", hasDirectMessagingStyle = true)
        )
    }

    @Test
    fun directMessagingStyleFallsBackToEnabledStandardType() {
        assertEquals(
            "STANDARD",
            NotificationTypeEnablementPolicy.resolveEnabledType(
                effectiveTypes = setOf("STANDARD"),
                detectedType = "MESSAGE",
                hasDirectMessagingStyle = true
            )
        )
    }

    @Test
    fun inboxAggregateDoesNotUseDirectMessageStandardFallback() {
        assertEquals(
            null,
            NotificationTypeEnablementPolicy.resolveEnabledType(
                effectiveTypes = setOf("STANDARD"),
                detectedType = "MESSAGE",
                hasDirectMessagingStyle = false
            )
        )
    }

    @Test
    fun whatsappStandardAcceptanceNeedsNoAggregateCategorySetting() {
        assertFalse(
            NotificationAcceptancePolicy.isJunk(
                NotificationAcceptanceSignals("com.whatsapp", "Sender", "Message", false, false, false)
            )
        )
    }

    @Test
    fun genericUsefulNotificationsRemainAccepted() {
        for (packageName in listOf("com.instagram.android", "com.zhiliaoapp.musically", "com.google.android.gm")) {
            assertFalse(
                NotificationAcceptancePolicy.isJunk(
                    NotificationAcceptanceSignals(packageName, "Title", "Text", false, false, false)
                )
            )
        }
    }
}
