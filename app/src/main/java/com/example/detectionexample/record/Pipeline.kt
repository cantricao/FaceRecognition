/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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

package com.example.detectionexample.record

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.util.Size
import android.view.Surface
import android.view.SurfaceView

abstract class Pipeline(
    protected val width: Int, protected val height: Int, protected val fps: Int,
    protected val filterOn: Boolean,
    protected val characteristics: CameraCharacteristics, protected val encoder: EncoderWrapper,
    protected val surface: Surface
) {

    open fun createPreviewRequest(session: CameraCaptureSession,
                                  previewStabilization: Boolean): CaptureRequest? {
        return null
    }

    abstract fun createRecordRequest(session: CameraCaptureSession,
            previewStabilization: Boolean): CaptureRequest

    open fun destroyWindowSurface() { }

    open fun setPreviewSize(previewSize: Size) { }

    open fun createResources(surface: Surface) { }

    abstract fun getTargets(): List<Surface>

    open fun actionDown(encoderSurface: Surface) { }

    open fun clearFrameListener() { }

    open fun cleanup() { }

    open fun startRecording() { }

    open fun stopRecording() { }
}