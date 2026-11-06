package com.example.detectionexample.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class NavigatorTest {

    private sealed interface TestScreen : NavKey {
        data object Home : TestScreen
        data object Camera : TestScreen
        data object Video : TestScreen
        data object Gallery : TestScreen
        data object Details : TestScreen
    }

    private val homeRoute: NavKey = TestScreen.Home
    private val cameraRoute: NavKey = TestScreen.Camera
    private val videoRoute: NavKey = TestScreen.Video
    private val galleryRoute: NavKey = TestScreen.Gallery

    private lateinit var state: NavigationState
    private lateinit var navigator: Navigator

    @Before
    fun setUp() {
        val topLevelRoutes = setOf(homeRoute, cameraRoute, videoRoute, galleryRoute)
        val backStacks = topLevelRoutes.associateWith { key ->
            NavBackStack(key)
        }
        val topLevelRouteState = mutableStateOf(homeRoute)

        state = NavigationState(
            startRoute = homeRoute,
            topLevelRoute = topLevelRouteState,
            backStacks = backStacks
        )
        navigator = Navigator(state)
    }

    @Test
    fun initialState_isHomeRoute() {
        assertEquals(homeRoute, state.topLevelRoute)
        assertEquals(listOf(homeRoute), state.stacksInUse)
    }

    @Test
    fun navigateToTopLevelRoute_switchesActiveTab() {
        navigator.navigate(cameraRoute)
        assertEquals(cameraRoute, state.topLevelRoute)
        assertEquals(listOf(homeRoute, cameraRoute), state.stacksInUse)

        navigator.navigate(galleryRoute)
        assertEquals(galleryRoute, state.topLevelRoute)
        assertEquals(listOf(homeRoute, galleryRoute), state.stacksInUse)
    }

    @Test
    fun goBackFromSecondaryTopLevelRoute_exitsThroughHome() {
        // Switch to Video tab
        navigator.navigate(videoRoute)
        assertEquals(videoRoute, state.topLevelRoute)

        // Tapping back at the root of Video tab should navigate back to startRoute (Home)
        navigator.goBack()
        assertEquals(homeRoute, state.topLevelRoute)
        assertEquals(listOf(homeRoute), state.stacksInUse)
    }

    @Test
    fun goBackFromPushedChildRoute_popsTopRouteFirst() {
        // On Camera tab, simulate pushing a sub-route
        navigator.navigate(cameraRoute)
        state.backStacks[cameraRoute]?.add(TestScreen.Details)

        assertEquals(2, state.backStacks[cameraRoute]?.size)

        // goBack should pop the child route and remain on Camera tab
        navigator.goBack()
        assertEquals(cameraRoute, state.topLevelRoute)
        assertEquals(1, state.backStacks[cameraRoute]?.size)
        assertEquals(cameraRoute, state.backStacks[cameraRoute]?.last())
    }

    @Test
    fun multipleBackstacks_preserveIndependentState() {
        navigator.navigate(cameraRoute)
        state.backStacks[cameraRoute]?.add(TestScreen.Details)

        // Switch to Gallery and back
        navigator.navigate(galleryRoute)
        assertEquals(galleryRoute, state.topLevelRoute)

        navigator.navigate(cameraRoute)
        assertEquals(cameraRoute, state.topLevelRoute)
        // Camera backstack preserved
        assertEquals(2, state.backStacks[cameraRoute]?.size)
    }
}
