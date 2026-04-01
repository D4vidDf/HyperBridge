package com.d4viddf.hyperbridge.ui.navigation

import androidx.navigation3.runtime.NavKey

/**
 * Handles navigation events (forward and back) by updating the navigation state.
 */
class Navigator<T : NavKey>(val state: NavigationState<T>) {
    fun navigate(route: T) {
        if (route in state.backStacks.keys) {
            // This is a top level route, just switch to it.
            state.topLevelRoute = route
        } else {
            state.backStacks[state.topLevelRoute]?.add(route)
        }
    }

    fun goBack() {
        val currentStack = state.backStacks[state.topLevelRoute]
            ?: error("Stack for ${state.topLevelRoute} not found")
        val currentRoute = currentStack.last()

        // If we're at the base of the current route, go back to the start route stack.
        if (currentRoute == state.topLevelRoute) {
            if (state.topLevelRoute != state.startRoute) {
                state.topLevelRoute = state.startRoute
            } else {
                // Already at start route, maybe exit app or do nothing
                // NavDisplay handles the back gesture, so we might not need to do much here
                // unless we want to exit the app.
            }
        } else {
            currentStack.removeLastOrNull()
        }
    }
}
