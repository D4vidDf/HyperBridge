package com.d4viddf.hyperbridge.data

import android.content.Context
import com.d4viddf.hyperbridge.data.theme.ThemeRepository
import com.d4viddf.hyperbridge.models.NavContent
import com.d4viddf.hyperbridge.models.theme.HyperTheme
import com.d4viddf.hyperbridge.models.theme.NavigationModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * A fast, in-memory resolver for Translators to look up effective
 * configurations without blocking the main thread.
 */
class ConfigResolver(context: Context) {

    private val preferences = AppPreferences(context)
    private val themeRepo = ThemeRepository(context)

    // Keep a hot flow of the active theme in memory
    private val activeTheme: StateFlow<HyperTheme?> = themeRepo.activeTheme

    /**
     * Data class holding the exact UI parameters a Translator needs to render the Island
     */
    data class ResolvedNavConfig(
        val useNativeEngine: Boolean,
        val leftContent: NavContent,
        val rightContent: NavContent,
        val themeVisuals: NavigationModule?
    )

    /**
     * Call this from your NavTranslator when a notification arrives.
     * It instantly calculates the result based on in-memory state.
     */
    suspend fun resolveNavConfig(packageName: String): ResolvedNavConfig {
        val theme = activeTheme.value
        val themeAppOverride = theme?.apps?.get(packageName)

        // 1. Resolve Engine
        val effectiveEngine = themeAppOverride?.useNativeLiveUpdates
            ?: preferences.useNativeLiveUpdates.stateIn(CoroutineScope(Dispatchers.IO), SharingStarted.Eagerly, true).value

        // 2. Resolve Visuals
        val effectiveVisuals = themeAppOverride?.navigation

        // 3. Resolve Content (A bit tricky as it requires reading DB, but we do it fast)
        // Note: For absolute zero-latency, you might want to cache globalNavLayoutFlow in memory too!
        var leftContent = NavContent.DISTANCE_ETA
        var rightContent = NavContent.INSTRUCTION

        preferences.getEffectiveNavLayout(packageName).collect { (l, r) ->
            leftContent = l
            rightContent = r
        }

        return ResolvedNavConfig(
            useNativeEngine = effectiveEngine,
            leftContent = leftContent,
            rightContent = rightContent,
            themeVisuals = effectiveVisuals
        )
    }
}