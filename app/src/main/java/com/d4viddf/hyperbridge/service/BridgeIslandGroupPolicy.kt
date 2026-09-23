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
 *
 * The group is an Android 16+ workaround only. Below that the system never force-groups at 2, and
 * the summary has a visible cost: HyperOS hides Focus children from the shade but not an ordinary
 * summary, so a "Hyper Bridge / Active Islands" row stays in the shade for as long as any island
 * (the permanent one included) exists (#358).
 */
object BridgeIslandGroupPolicy {
    const val GROUP_KEY = "hyperbridge_islands"

    /** First SDK where force grouping at 2 exists (`Build.VERSION_CODES.BAKLAVA`, Android 16). */
    const val MIN_SDK = 36

    /** True when the app group and its summary are needed at all on this SDK. */
    fun appliesTo(sdkInt: Int): Boolean = sdkInt >= MIN_SDK

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
