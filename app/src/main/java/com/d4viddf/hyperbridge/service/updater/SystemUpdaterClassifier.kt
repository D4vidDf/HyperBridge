package com.d4viddf.hyperbridge.service.updater

object SystemUpdaterClassifier {
    const val PACKAGE_NAME = "com.android.updater"

    fun isSystemUpdater(packageName: String): Boolean =
        packageName == PACKAGE_NAME
}

object SystemUpdateTimeoutPolicy {
    fun resolve(
        configuredTimeout: Int?,
        systemUpdateTimeout: Int,
        isSystemUpdate: Boolean,
        isFinished: Boolean,
        hasProgress: Boolean = true
    ): Int? = when {
        isSystemUpdate && isFinished -> systemUpdateTimeout
        isSystemUpdate && hasProgress -> null
        isSystemUpdate && !hasProgress -> systemUpdateTimeout
        else -> configuredTimeout
    }
}
