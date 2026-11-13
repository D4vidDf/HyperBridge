package com.d4viddf.hyperbridge.ui.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigatorTest {

    @Test
    fun navigator_pushesAndPops_onCurrentTopLevelStack() {
        val topLevelState = mutableStateOf<Screen>(Screen.Home)
        val homeStack = NavBackStack<Screen>(Screen.Home)
        val onboardingStack = NavBackStack<Screen>(Screen.Onboarding)
        val state = NavigationState(
            startRoute = Screen.Home,
            topLevelRoute = topLevelState,
            backStacks = mapOf(
                Screen.Home to homeStack,
                Screen.Onboarding to onboardingStack
            )
        )
        val navigator = Navigator(state)

        assertEquals(listOf(Screen.Home), homeStack.toList())

        navigator.navigate(Screen.Info)
        assertEquals(listOf(Screen.Home, Screen.Info), homeStack.toList())

        navigator.navigate(Screen.GlobalSettings)
        assertEquals(listOf(Screen.Home, Screen.Info, Screen.GlobalSettings), homeStack.toList())

        assertTrue(navigator.goBack())
        assertEquals(listOf(Screen.Home, Screen.Info), homeStack.toList())

        assertTrue(navigator.goBack())
        assertEquals(listOf(Screen.Home), homeStack.toList())

        assertFalse(navigator.goBack())
    }

    @Test
    fun navigator_switchesTopLevelRoute() {
        val topLevelState = mutableStateOf<Screen>(Screen.Onboarding)
        val homeStack = NavBackStack<Screen>(Screen.Home)
        val onboardingStack = NavBackStack<Screen>(Screen.Onboarding)
        val state = NavigationState(
            startRoute = Screen.Onboarding,
            topLevelRoute = topLevelState,
            backStacks = mapOf(
                Screen.Onboarding to onboardingStack,
                Screen.Home to homeStack
            )
        )
        val navigator = Navigator(state)

        assertEquals(Screen.Onboarding, state.topLevelRoute)
        navigator.finishOnboarding(Screen.Home)

        assertEquals(Screen.Home, state.topLevelRoute)
        assertEquals(listOf(Screen.Home), homeStack.toList())
    }

    @Test
    fun screenRoutes_serializeAndDeserializeCorrectly() {
        val json = Json { ignoreUnknownKeys = true }

        val homeJson = json.encodeToString<Screen>(Screen.Home)
        val restoredHome = json.decodeFromString<Screen>(homeJson)
        assertEquals(Screen.Home, restoredHome)

        val navCustomization = Screen.NavCustomization(packageName = "com.example.app")
        val navJson = json.encodeToString<Screen>(navCustomization)
        val restoredNav = json.decodeFromString<Screen>(navJson)
        assertEquals(navCustomization, restoredNav)

        val appConfig = Screen.AppConfig(packageName = "com.whatsapp")
        val appConfigJson = json.encodeToString<Screen>(appConfig)
        val restoredAppConfig = json.decodeFromString<Screen>(appConfigJson)
        assertEquals(appConfig, restoredAppConfig)
    }
}
