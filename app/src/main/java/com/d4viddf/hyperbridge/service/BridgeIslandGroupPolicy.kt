package com.d4viddf.hyperbridge.service

/**
 * Pure decisions behind [BridgeIslandGroup] (#331).
 *
 * Android 16+ force-groups notifications from one app as soon as two of them have no app group
 * (`config_autoGroupAtCount = 2`), and every notification it groups gets the hidden
 * `FLAG_SILENT`, which SystemUI (and HyperOS's island engine) treats as "never alert". A child
 * whose summary was never posted counts as ungrouped too, so the only safe shape is one app
 * group with a real summary that exists whenever a child exists and goes away when the last
 * child does.
 */
object BridgeIslandGroupPolicy {
    const val GROUP_KEY = "hyperbridge_islands"

    /** Negative: can never collide with hash-derived bridge ids or the widget id range. */
    const val SUMMARY_ID = -20302

    /** How long after a removal we wait before deciding the group is empty (covers cancel+repost). */
    const val RELEASE_DELAY_MS = 2_000L

    /** A notification we posted, reduced to what the group decisions need. */
    data class OwnNotification(val id: Int, val group: String?)

    fun isSummary(n: OwnNotification): Boolean = n.id == SUMMARY_ID

    fun isChild(n: OwnNotification): Boolean = n.group == GROUP_KEY && n.id != SUMMARY_ID

    /** True when a child is about to be posted and no summary is active. */
    fun needsSummary(active: List<OwnNotification>): Boolean = active.none(::isSummary)

    /** True when the summary is active but no child is left. */
    fun shouldReleaseSummary(active: List<OwnNotification>): Boolean =
        active.any(::isSummary) && active.none(::isChild)
}
