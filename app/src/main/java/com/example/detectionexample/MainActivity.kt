package com.example.detectionexample


import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.core.view.WindowCompat
import com.example.detectionexample.compose.*
import com.example.detectionexample.ui.theme.DetectionExampleTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class MainActivity : ComponentActivity() {

    override fun  onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Displaying edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val listPermission = listOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            val explanation = "Use camera for detection and store image"
            WindowCompat.setDecorFitsSystemWindows(window, false)
            DetectionExampleTheme {
                Surface {
                    Permission(listPermission, explanation, {
                        FaceRecognitionApp()
                    },
                    {
                        PermissionNotAvailableContent()
                    })
                }
            }
        }
    }
}

