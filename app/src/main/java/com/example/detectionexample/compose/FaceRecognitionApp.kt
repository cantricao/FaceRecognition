package com.example.detectionexample.compose

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.detectionexample.R
import com.example.detectionexample.compose.camera.CameraPreview
import com.example.detectionexample.compose.video.VideoPlayer


sealed class Screen(val route: String, @StringRes val resourceId: Int) {
    object Camera : Screen("camera", R.string.camera)
    object Video : Screen("video", R.string.video)
}

val items = listOf(
    Screen.Camera,
    Screen.Video,
)
@Composable
fun FaceRecognitionApp(
) {
    val navController = rememberNavController()
    Scaffold (
        bottomBar = {
            BottomNavigation {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { screen ->
                    BottomNavigationItem(
                        icon = {
                            Icon(
                                Icons.Filled.Favorite,
                                contentDescription = null
                            )
                        },
                        label = { Text(stringResource(screen.resourceId)) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                // Pop up to the start destination of the graph to
                                // avoid building up a large stack of destinations
                                // on the back stack as users select items
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                // Avoid multiple copies of the same destination when
                                // re-selecting the same item
                                launchSingleTop = true
                                // Restore state when re-selecting a previously selected item
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) {innerPadding-> FaceRecognitionNavHost(navController, innerPadding)
    }
}

@Composable
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
fun  FaceRecognitionNavHost(navController: NavHostController, innerPadding: PaddingValues ){
    NavHost(navController = navController, startDestination = items[0].route, Modifier.padding(innerPadding)){
        composable(Screen.Camera.route){
            CameraPreview()
        }
        composable(Screen.Video.route){
            VideoPlayer()
        }
    }
}