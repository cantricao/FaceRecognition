/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.detectionexample.uistate

import androidx.camera.core.CameraSelector.LENS_FACING_BACK
import androidx.camera.core.CameraSelector.LensFacing
import androidx.camera.core.ImageCapture
import androidx.camera.extensions.ExtensionMode
import androidx.compose.runtime.Immutable

/**
 * Capture format choices.
 */
enum class CaptureFormat {
    JPEG,
    RAW,
    RAW_JPEG
}

/**
 * Camera manual settings for Camera2Interop control.
 */
@Immutable
data class CameraManualSettings(
    val isManualMode: Boolean = false,
    val iso: Int = 200,
    val exposureTimeNs: Long = 10_000_000L, // 1/100s
    val focusDistance: Float = 0.0f, // 0.0 = infinity
    val exposureCompensation: Int = 0,
    val zoomRatio: Float = 1.0f
)

/**
 * Defines the current UI state of the camera during pre-capture.
 */
@Immutable
data class CameraUiState(
    val cameraState: MediaState = MediaState.NOT_READY,
    val analysisState: AnalysisState = AnalysisState.NOT_READY,
    val captureState: CaptureState = CaptureState.CaptureNotReady,
    val availableExtensions: List<Int> = emptyList(),
    val availableCameraLens: List<Int> = listOf(LENS_FACING_BACK),
    @param:LensFacing val cameraLens: Int = LENS_FACING_BACK,
    @param:ExtensionMode.Mode val extensionMode: Int = ExtensionMode.NONE,
    val manualSettings: CameraManualSettings = CameraManualSettings(),
    val captureFormat: CaptureFormat = CaptureFormat.JPEG,
    val isRawSupported: Boolean = false,
    val hasAudioPermission: Boolean = false,
    val isManualControlsVisible: Boolean = false
)

/**
 * Defines the current state of the camera.
 */
enum class MediaState {
    NOT_READY,
    READY,
    PREVIEW_STOPPED
}

enum class AnalysisState {
    NOT_READY,
    READY,
    ANALYSIS_STOPPED
}

// Camera UI video recording states
enum class RecordState {
    IDLE, // Not recording
    RECORDING, // Actively recording
    PAUSED, // Recording is paused
    FINALIZED, // Recording finalized
    RECOVERY
}

/**
 * Defines the various states during post-capture.
 */
sealed class CaptureState {
    object CaptureNotReady : CaptureState()
    object CaptureReady : CaptureState()
    object CaptureStarted : CaptureState()
    data class CaptureFinished(val outputResults: ImageCapture.OutputFileResults) : CaptureState()
    data class CaptureFailed(val exception: Exception) : CaptureState()
}
