package com.example.detectionexample.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.wear.compose.foundation.LocalAmbientModeManager
import androidx.wear.compose.foundation.rememberAmbientModeManager
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.navigation3.rememberSwipeDismissableSceneStrategy
import com.example.detectionexample.wear.navigation.WearNavKey
import com.example.detectionexample.wear.screens.WearDetailScreen
import com.example.detectionexample.wear.screens.WearHomeScreen
import com.example.detectionexample.wear.screens.WearIdentitiesScreen
import com.example.detectionexample.wear.theme.WearTheme

/**
 * Main activity for the Face Recognition Wear OS companion app.
 * Follows Wear Compose Material 3 standards:
 * - AmbientModeManager instantiated and provided at root via CompositionLocalProvider
 * - A single AppScaffold wrapper
 * - Navigation 3 coordinated by NavDisplay with SwipeDismissableSceneStrategy
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val ambientModeManager = rememberAmbientModeManager()
            CompositionLocalProvider(LocalAmbientModeManager provides ambientModeManager) {
                WearTheme {
                    WearFaceRecognitionApp()
                }
            }
        }
    }
}

@Composable
fun WearFaceRecognitionApp() {
    val backStack = rememberNavBackStack(WearNavKey.Home)

    AppScaffold {
        NavDisplay(
            backStack = backStack,
            onBack = {
                if (backStack.size > 1) {
                    backStack.removeLastOrNull()
                }
            },
            sceneStrategies = listOf(rememberSwipeDismissableSceneStrategy()),
            entryProvider = entryProvider {
                entry<WearNavKey.Home> {
                    WearHomeScreen(
                        onNavigateToIdentities = {
                            backStack.add(WearNavKey.Identities)
                        }
                    )
                }

                entry<WearNavKey.Identities> {
                    WearIdentitiesScreen(
                        onIdentityClick = { identity ->
                            backStack.add(
                                WearNavKey.Detail(
                                    id = identity.id,
                                    name = identity.name,
                                    confidence = identity.confidence,
                                )
                            )
                        }
                    )
                }

                entry<WearNavKey.Detail> { key ->
                    WearDetailScreen(
                        id = key.id,
                        name = key.name,
                        confidence = key.confidence,
                        onBack = {
                            backStack.removeLastOrNull()
                        }
                    )
                }
            }
        )
    }
}
