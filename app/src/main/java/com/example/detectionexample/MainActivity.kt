package com.example.detectionexample


import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import com.example.detectionexample.compose.*
import com.example.detectionexample.ui.theme.DetectionExampleTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val listPermission = listOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            val explanation = "Use camera for detection and store image"
            DetectionExampleTheme {
                Surface {
                    Permission(listPermission, explanation, {
                            CameraPreview()
                            VideoPlayer()
                            OverlayView()
                            BottomSheet()
                        },
                        {
                            PermissionNotAvailableContent()
                        })
                }
            }
        }
    }
}

