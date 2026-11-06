package com.example.detectionexample.compose.camera

import androidx.compose.runtime.Immutable
import com.example.detectionexample.models.TrackedRecognition

@Immutable
data class DetectionInspectorState(
    val trackedRecognitions: List<TrackedRecognition> = emptyList(),
    val isExpanded: Boolean = true
)
