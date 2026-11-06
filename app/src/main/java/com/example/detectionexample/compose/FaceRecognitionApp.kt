package com.example.detectionexample.compose

import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import androidx.xr.projected.ProjectedContext
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import com.example.detectionexample.R
import com.example.detectionexample.chat.ChatScreen
import com.example.detectionexample.compose.adaptive.FormFactorPreviews
import com.example.detectionexample.compose.camera.CameraPreview
import com.example.detectionexample.compose.video.VideoPlayer
import com.example.detectionexample.glasses.ProjectedRecognitionActivity
import com.example.detectionexample.navigation.Navigator
import com.example.detectionexample.navigation.Screen
import com.example.detectionexample.navigation.rememberNavigationState
import com.example.detectionexample.navigation.toEntries
import com.example.detectionexample.ui.theme.DetectionExampleTheme
import kotlinx.coroutines.flow.flowOf

val items = listOf(
    Screen.Camera,
    Screen.Video,
    Screen.Chat,
)

@Composable
fun FaceRecognitionApp(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    @OptIn(ExperimentalProjectedApi::class)
    val isGlassesConnected by remember(context) {
        if (Build.VERSION.SDK_INT >= 35) {
            ProjectedContext.isProjectedDeviceConnected(context, coroutineScope.coroutineContext)
        } else {
            flowOf(false)
        }
    }.collectAsStateWithLifecycle(initialValue = false)

    val navigationState = rememberNavigationState(
        startRoute = Screen.Camera,
        topLevelRoutes = setOf(Screen.Camera, Screen.Video, Screen.Chat)
    )
    val navigator = remember { Navigator(navigationState) }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val entryProvider = entryProvider<NavKey> {
        entry<Screen.Camera> {
            CameraPreview()
        }
        entry<Screen.Video> {
            VideoPlayer(onBack = { navigator.goBack() })
        }
        entry<Screen.Chat> {
            ChatScreen()
        }
    }

    NavigationSuiteScaffold(
        modifier = modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.isCtrlPressed) {
                    when (event.key) {
                        Key.One -> {
                            navigator.navigate(Screen.Camera)
                            true
                        }
                        Key.Two -> {
                            navigator.navigate(Screen.Video)
                            true
                        }
                        Key.Three -> {
                            navigator.navigate(Screen.Chat)
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            },
        navigationSuiteItems = {
            items.forEach { screen ->
                val selected = screen == navigationState.topLevelRoute
                item(
                    selected = selected,
                    onClick = { navigator.navigate(screen) },
                    icon = {
                        Icon(
                            painter = painterResource(
                                id = when (screen) {
                                    Screen.Camera -> R.drawable.ic_baseline_camera_24
                                    Screen.Video -> R.drawable.ic_baseline_play_arrow_24
                                    Screen.Chat -> R.drawable.ic_twotone_model_training_24
                                }
                            ),
                            contentDescription = stringResource(screen.resourceId)
                        )
                    },
                    label = { Text(stringResource(screen.resourceId)) }
                )
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            NavDisplay(
                entries = navigationState.toEntries(entryProvider),
                onBack = { navigator.goBack() }
            )

            // Actions row: Android TV experience launcher & Intelligent Eyewear Projection launcher
            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(end = 16.dp, top = 8.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val intent = Intent(context, com.example.detectionexample.tv.TvActivity::class.java)
                        context.startActivity(intent)
                    }
                ) {
                    Text("TV Mode")
                }

                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= 35) {
                            try {
                                @OptIn(ExperimentalProjectedApi::class)
                                val options = ProjectedContext.createProjectedActivityOptions(context)
                                val intent = Intent(context, ProjectedRecognitionActivity::class.java)
                                context.startActivity(intent, options.toBundle())
                            } catch (e: Exception) {
                                Log.e("FaceRecognitionApp", "Failed to launch projected activity", e)
                            }
                        }
                    },
                    enabled = isGlassesConnected
                ) {
                    Text("Launch on Glasses")
                }
            }
        }
    }
}

@FormFactorPreviews
@Composable
private fun FaceRecognitionAppAdaptivePreview() {
    DetectionExampleTheme {
        FaceRecognitionApp()
    }
}
