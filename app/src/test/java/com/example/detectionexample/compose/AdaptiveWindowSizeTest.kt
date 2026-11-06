package com.example.detectionexample.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Verifies adaptive navigation UI layout across canonical window size classes:
 * Compact (400dp width), Medium (610dp width), and Expanded (900dp width).
 */
@RunWith(RobolectricTestRunner::class)
class AdaptiveWindowSizeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun compactWidth_rendersNavigationSuccessfully() {
        composeTestRule.setContent {
            Box(modifier = Modifier.size(400.dp, 800.dp)) {
                NavigationSuiteScaffold(
                    navigationSuiteItems = {
                        item(
                            selected = true,
                            onClick = {},
                            icon = {},
                            label = { Text("Camera") }
                        )
                    }
                ) {
                    Text("Compact Screen Content")
                }
            }
        }
        composeTestRule.onNodeWithText("Camera").assertIsDisplayed()
        composeTestRule.onNodeWithText("Compact Screen Content").assertIsDisplayed()
    }

    @Test
    fun mediumWidth_rendersNavigationSuccessfully() {
        composeTestRule.setContent {
            Box(modifier = Modifier.size(610.dp, 800.dp)) {
                NavigationSuiteScaffold(
                    navigationSuiteItems = {
                        item(
                            selected = true,
                            onClick = {},
                            icon = {},
                            label = { Text("Camera") }
                        )
                    }
                ) {
                    Text("Medium Screen Content")
                }
            }
        }
        composeTestRule.onNodeWithText("Camera").assertIsDisplayed()
        composeTestRule.onNodeWithText("Medium Screen Content").assertIsDisplayed()
    }

    @Test
    fun expandedWidth_rendersNavigationSuccessfully() {
        composeTestRule.setContent {
            Box(modifier = Modifier.size(900.dp, 1000.dp)) {
                NavigationSuiteScaffold(
                    navigationSuiteItems = {
                        item(
                            selected = true,
                            onClick = {},
                            icon = {},
                            label = { Text("Camera") }
                        )
                    }
                ) {
                    Text("Expanded Screen Content")
                }
            }
        }
        composeTestRule.onNodeWithText("Camera").assertIsDisplayed()
        composeTestRule.onNodeWithText("Expanded Screen Content").assertIsDisplayed()
    }
}
