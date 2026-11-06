package com.example.detectionexample.compose

import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NavigationSuiteComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun navigationSuiteScaffold_switchesContentOnTabClick() {
        composeTestRule.setContent {
            var currentTab by remember { mutableStateOf("Camera") }
            NavigationSuiteScaffold(
                navigationSuiteItems = {
                    item(
                        selected = currentTab == "Camera",
                        onClick = { currentTab = "Camera" },
                        icon = {},
                        label = { Text("Camera") }
                    )
                    item(
                        selected = currentTab == "Video",
                        onClick = { currentTab = "Video" },
                        icon = {},
                        label = { Text("Video") }
                    )
                }
            ) {
                Text(text = "Current Screen: $currentTab")
            }
        }

        // Verify initial state
        composeTestRule.onNodeWithText("Current Screen: Camera").assertIsDisplayed()

        // Click Video tab
        composeTestRule.onNodeWithText("Video").performClick()

        // Verify state update
        composeTestRule.onNodeWithText("Current Screen: Video").assertIsDisplayed()

        // Click Camera tab
        composeTestRule.onNodeWithText("Camera").performClick()
        composeTestRule.onNodeWithText("Current Screen: Camera").assertIsDisplayed()
    }
}
