package com.example.detectionexample


import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.example.detectionexample.compose.*
import com.example.detectionexample.ui.theme.DetectionExampleTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val listPermission = listOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            val explanation = "Use camera for detection and store image"

            DetectionExampleTheme {
                Surface {
                    Permission(listPermission, explanation, {
                        CameraPreview()
                        VideoPlayer()
//                        BottomSheet()
                    },
                    {
                        PermissionNotAvailableContent()
                    })
                }
            }
        }
    }
}

