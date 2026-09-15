package com.d4viddf.hyperbridge.service

data class ReconciliationInput(
    /**
     * Logical island id -> the source notification key it was last posted from. Logical ids are
     * NOT shade keys for messages (`message:pkg:...`) and calls (`call:pkg:N`), so staleness
     * must always be judged on the source side of this map, never on the id itself.
     */
    val activeLogicalSources: Map<String, String>,
    val currentSourceKeys: Set<String>,
    val trackedBridgeIds: Set<Int>,
    val postedBridgeIds: Set<Int>,
    val recoverableSourceKeys: Set<String>,
    val mappedSourceKeys: Set<String>,
    /**
     * Logical island id -> every other source key currently aliased to it (a message family
     * whose conversation notification was replaced, a call whose source was re-posted). An
     * island is only stale when none of its sources is in the shade any more.
     */
    val activeLogicalSourceAliases: Map<String, Set<String>> = emptyMap()
)

data class ReconciliationPlan(
    val staleLogicalIds: Set<String>,
    val orphanBridgeIds: Set<Int>,
    val missingSourceKeys: Set<String>
)

object NotificationReconciliation {
    fun plan(input: ReconciliationInput): ReconciliationPlan = ReconciliationPlan(
        staleLogicalIds = input.activeLogicalSources
            .filter { (logicalId, sourceKey) ->
                sourceKey !in input.currentSourceKeys &&
                        input.activeLogicalSourceAliases[logicalId].orEmpty()
                            .none { it in input.currentSourceKeys }
            }
            .keys,
        orphanBridgeIds = input.postedBridgeIds - input.trackedBridgeIds,
        missingSourceKeys = input.recoverableSourceKeys - input.mappedSourceKeys
    )
}
