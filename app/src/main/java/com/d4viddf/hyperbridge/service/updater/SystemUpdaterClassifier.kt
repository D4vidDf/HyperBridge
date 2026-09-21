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
        isFinished: Boolean
    ): Int? = when {
        isSystemUpdate && isFinished -> systemUpdateTimeout
        isSystemUpdate && !isFinished -> null
        else -> configuredTimeout
    }
}
