package com.example.detectionexample.compose.dialog

import android.graphics.Bitmap
import com.example.detectionexample.models.TrackedRecognition

data class FaceAndTrackedObject(
    val face: Bitmap,
    val trackedRecognition: TrackedRecognition
)
