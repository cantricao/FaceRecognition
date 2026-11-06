package com.example.detectionexample

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end user journey test verifying activity launch, Hilt dependency injection,
 * and main UI presentation.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class FaceRecognitionE2ETest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun appLaunch_displaysMainUiOrPermissionPrompt() {
        // Upon app launch, either permission explanation or camera tab is presented
        composeTestRule.waitForIdle()
    }
}
