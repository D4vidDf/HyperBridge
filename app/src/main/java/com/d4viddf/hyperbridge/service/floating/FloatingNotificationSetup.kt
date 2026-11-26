package com.d4viddf.hyperbridge.service.floating

enum class FloatingSetupStatus {
    USER_CONFIRMED,
    NEEDS_REVIEW,
    NOT_REQUIRED
}

object FloatingNotificationSetup {
    fun status(
        isSelected: Boolean,
        userConfirmedDisabled: Boolean,
        requiresManualSetup: Boolean
    ): FloatingSetupStatus = when {
        !isSelected || !requiresManualSetup -> FloatingSetupStatus.NOT_REQUIRED
        userConfirmedDisabled -> FloatingSetupStatus.USER_CONFIRMED
        else -> FloatingSetupStatus.NEEDS_REVIEW
    }
}
