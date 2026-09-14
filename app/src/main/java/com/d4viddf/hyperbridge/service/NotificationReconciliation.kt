package com.d4viddf.hyperbridge.service

data class ReconciliationInput(
    val activeLogicalSources: Map<String, String>,
    val currentSourceKeys: Set<String>,
    val trackedBridgeIds: Set<Int>,
    val postedBridgeIds: Set<Int>,
    val recoverableSourceKeys: Set<String>,
    val mappedSourceKeys: Set<String>
)

data class ReconciliationPlan(
    val staleLogicalIds: Set<String>,
    val orphanBridgeIds: Set<Int>,
    val missingSourceKeys: Set<String>
)

object NotificationReconciliation {
    fun plan(input: ReconciliationInput): ReconciliationPlan = ReconciliationPlan(
        staleLogicalIds = input.activeLogicalSources
            .filterValues { it !in input.currentSourceKeys }
            .keys,
        orphanBridgeIds = input.postedBridgeIds - input.trackedBridgeIds,
        missingSourceKeys = input.recoverableSourceKeys - input.mappedSourceKeys
    )
}
