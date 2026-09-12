package com.d4viddf.hyperbridge.service.translators

import io.github.d4viddf.hyperisland_kit.HyperIslandNotification

data class IslandFloatingPresentation(
    val enableFloat: Boolean,
    val islandFirstFloat: Boolean
)

/**
 * HyperOS evaluates enableFloat again whenever an updatable Focus notification is posted.
 * Keeping it enabled on an in-place update can therefore re-expand an island that the user
 * already collapsed. A logical event may float once; later payload refreshes may not.
 */
object IslandFloatingPresentationPolicy {
    fun resolve(configuredToFloat: Boolean, isUpdate: Boolean): IslandFloatingPresentation {
        val mayAutoExpand = configuredToFloat && !isUpdate
        return IslandFloatingPresentation(
            enableFloat = mayAutoExpand,
            islandFirstFloat = mayAutoExpand
        )
    }
}

internal fun HyperIslandNotification.applyFloatingPresentation(
    configuredToFloat: Boolean,
    isUpdate: Boolean
) = apply {
    val presentation = IslandFloatingPresentationPolicy.resolve(configuredToFloat, isUpdate)
    setEnableFloat(presentation.enableFloat)
    setIslandFirstFloat(presentation.islandFirstFloat)
}
