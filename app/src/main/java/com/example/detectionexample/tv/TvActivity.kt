package com.example.detectionexample.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.util.UnstableApi
import com.example.detectionexample.tv.model.TvMediaCatalog
import com.example.detectionexample.tv.model.TvVideo
import com.example.detectionexample.tv.screens.BrowseScreen
import com.example.detectionexample.tv.screens.GuidedStepScreen
import com.example.detectionexample.tv.screens.PlaybackScreen
import com.example.detectionexample.tv.screens.SearchScreen
import com.example.detectionexample.tv.screens.TvSettingsScreen
import com.example.detectionexample.tv.theme.TvTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Android TV destination modeled as modern [ComponentActivity] with Jetpack Compose for TV.
 *
 * Registered in AndroidManifest.xml with category.LEANBACK_LAUNCHER.
 */
@AndroidEntryPoint
@OptIn(UnstableApi::class)
class TvActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TvTheme {
                TvAppRoot(
                    onExit = { finish() }
                )
            }
        }
    }
}

/**
 * TV destination routes.
 */
sealed interface TvRoute {
    data object Browse : TvRoute
    data class Playback(val video: TvVideo) : TvRoute
    data object Settings : TvRoute
    data object GuidedStep : TvRoute
    data object Search : TvRoute
}

@Composable
fun TvAppRoot(
    onExit: () -> Unit
) {
    var currentRoute by remember { mutableStateOf<TvRoute>(TvRoute.Browse) }
    val categories = remember { TvMediaCatalog.getCategorizedVideos() }

    BackHandler(enabled = currentRoute != TvRoute.Browse) {
        currentRoute = TvRoute.Browse
    }

    when (val route = currentRoute) {
        is TvRoute.Browse -> {
            BrowseScreen(
                categories = categories,
                onVideoClick = { video -> currentRoute = TvRoute.Playback(video) },
                onSearchClick = { currentRoute = TvRoute.Search },
                onSettingsClick = { currentRoute = TvRoute.Settings },
                onGuidedStepClick = { currentRoute = TvRoute.GuidedStep }
            )
        }
        is TvRoute.Playback -> {
            PlaybackScreen(
                video = route.video,
                onBack = { currentRoute = TvRoute.Browse }
            )
        }
        is TvRoute.Settings -> {
            TvSettingsScreen(
                onBack = { currentRoute = TvRoute.Browse }
            )
        }
        is TvRoute.GuidedStep -> {
            GuidedStepScreen(
                onStepCompleted = { currentRoute = TvRoute.Browse },
                onBack = { currentRoute = TvRoute.Browse }
            )
        }
        is TvRoute.Search -> {
            SearchScreen(
                onVideoClick = { video -> currentRoute = TvRoute.Playback(video) },
                onBack = { currentRoute = TvRoute.Browse }
            )
        }
    }
}
