package com.example.detectionexample.cameraeffect

import androidx.camera.core.CameraEffect
import androidx.compose.runtime.State
import com.example.detectionexample.models.TrackedRecognition

/**
 * A tone mapping effect for Preview/VideoCapture UseCase.
 */
internal class ToneMappingSurfaceEffect(
    trackedObjectsState: State<List<TrackedRecognition>>,
    targets: Int = PREVIEW or VIDEO_CAPTURE,
    private val processor: ToneMappingSurfaceProcessor = ToneMappingSurfaceProcessor(trackedObjectsState)
) :
    CameraEffect(
        targets, processor.getGlExecutor(), processor, {}
    ) {

    fun release() {
        processor.release()
    }
}