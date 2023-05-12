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

package com.example.detectionexample.viewmodels

import android.annotation.SuppressLint
import android.app.Application
import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraSelector.LensFacing
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.video.OnVideoSavedCallback
import androidx.camera.view.video.OutputFileOptions
import androidx.camera.view.video.OutputFileResults
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.example.detectionexample.MainApplication
import com.example.detectionexample.config.CameraConfig
import com.example.detectionexample.config.CameraConfig.TAG
import com.example.detectionexample.custom.BitmapEncoding
import com.example.detectionexample.record.BitmapToVideoEncoder
import com.example.detectionexample.uistate.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * View model for camera extensions. This manages all the operations on the camera.
 * This includes opening and closing the camera, showing the camera preview, capturing a photo,
 * checking which extensions are available, and selecting an extension.
 *
 * Camera UI state is communicated via the cameraUiState flow.
 * Capture UI state is communicated via the captureUiState flow.
 *
 * Rebinding to the UI state flows will always emit the last UI state.
 */

@HiltViewModel
class CameraXViewModel @Inject constructor(private val workManager: WorkManager, application: Application) : AndroidViewModel(application) {

     val cameraController = LifecycleCameraController(getApplication()).apply {
         isTapToFocusEnabled = true
     }
    private lateinit var extensionsManager: ExtensionsManager


    val bitmapEncoding: BitmapEncoding = object : BitmapEncoding {
        override fun queueFrame(bitmap: Bitmap) {
            encoder.queueFrame(Bitmap.createBitmap(bitmap))
        }

    }

    private val encoderCallback = object : BitmapToVideoEncoder.IBitmapToVideoEncoderCallback{
        override fun onEncodingComplete(outputFile: File?) {
            viewModelScope.launch {
                _recordState.emit(RecordState.FINALIZED)
                Toast.makeText(getApplication(), "File recording completed\n${outputFile?.absolutePath}\n__" , Toast.LENGTH_LONG).show()
            }
            Log.d("BitmapToVideoEncoder", "File recording completed\n${outputFile?.absolutePath}\n__")
        }
    }

    private val encoder = BitmapToVideoEncoder(encoderCallback)

    private val _cameraUiState: MutableStateFlow<CameraUiState> = MutableStateFlow(CameraUiState())
    private val _captureUiState: MutableStateFlow<CaptureState> =
        MutableStateFlow(CaptureState.CaptureNotReady)


    private val _recordState: MutableStateFlow<RecordState> = MutableStateFlow(RecordState.IDLE)

    private var _action: MutableSharedFlow<CameraUiAction> = MutableSharedFlow()


    val cameraUiState: Flow<CameraUiState> = _cameraUiState
    val captureUiState: Flow<CaptureState> = _captureUiState
    val action: Flow<CameraUiAction> = _action
    val recordState: Flow<RecordState> = _recordState


    /**
     * Initializes the camera and checks which extensions are available for the selected camera lens
     * face. If no extensions are available then the selected extension will be set to None and the
     * available extensions list will also contain None.
     * Because this operation is async, clients should wait for cameraUiState to emit
     * MediaState.READY. Once the camera is ready the client can start the preview.
     */
    fun initializeCamera() {
        viewModelScope.launch {
            val currentCameraUiState = _cameraUiState.value

            // wait for the camera provider instance and extensions manager instance
            cameraController.initializationFuture.await()

            val availableCameraLens =
                listOf(
                    CameraSelector.LENS_FACING_BACK,
                    CameraSelector.LENS_FACING_FRONT
                ).filter { lensFacing ->
                    cameraController.hasCamera(cameraLensToSelector(lensFacing))
                }

            // get the supported extensions for the selected camera lens by filtering the full list
            // of extensions and checking each one if it's available
            val availableExtensions = listOf(
                ExtensionMode.AUTO,
                ExtensionMode.BOKEH,
                ExtensionMode.HDR,
                ExtensionMode.NIGHT,
                ExtensionMode.FACE_RETOUCH,
            )
//                .filter { extensionMode ->
//                extensionsManager.isExtensionAvailable(cameraSelector, extensionMode)
//            }

            // prepare the new camera UI state which is now in the READY state and contains the list
            // of available extensions, available lens faces.
            val newCameraUiState = currentCameraUiState.copy(
                cameraState = MediaState.READY,
                analysisState = AnalysisState.NOT_READY,
                availableExtensions = listOf(ExtensionMode.NONE), // + availableExtensions,
                availableCameraLens = availableCameraLens,
                extensionMode = if (true) ExtensionMode.NONE else currentCameraUiState.extensionMode,
            )
            _cameraUiState.emit(newCameraUiState)
        }
    }

    fun setAction(cameraUiAction: CameraUiAction) {
        viewModelScope.launch {
            _action.emit(cameraUiAction)
        }
    }


    /**
     * Starts the preview stream. The camera state should be in the READY or PREVIEW_STOPPED state
     * when calling this operation.
     * This process will bind the preview and image capture uses cases to the camera provider.
     */
    fun startPreview(
        lifecycleOwner: LifecycleOwner
    ) {
        val currentCameraUiState = _cameraUiState.value
        val cameraSelector = if (currentCameraUiState.extensionMode == ExtensionMode.NONE) {
            cameraLensToSelector(currentCameraUiState.cameraLens)
        } else {
            extensionsManager.getExtensionEnabledCameraSelector(
                cameraLensToSelector(currentCameraUiState.cameraLens),
                currentCameraUiState.extensionMode
            )
        }

        cameraController.cameraSelector = cameraSelector
        cameraController.bindToLifecycle(lifecycleOwner)



        viewModelScope.launch {
            _cameraUiState.emit(_cameraUiState.value.copy(cameraState = MediaState.READY))
            _captureUiState.emit(CaptureState.CaptureReady)
        }
    }

    val width = 480
    val height = 640


    /**
     * Kick start the video recording
     *   - config Recorder to capture to MediaStoreOutput
     *   - register RecordEvent Listener
     *   - apply audio request from user
     *   - start recording!
     * After this function, user could start/pause/resume/stop recording and application listens
     * to VideoRecordEvent for the current recording status.
     */
    @SuppressLint("MissingPermission")
    @androidx.annotation.OptIn(androidx.camera.view.video.ExperimentalVideo::class)
    fun startRecording() {
//         create MediaStoreOutputOptions for our recorder: resulting our recording!
        cameraController.setEnabledUseCases(CameraController.VIDEO_CAPTURE)

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "${CameraConfig.FILENAME}.mp4")
            put(MediaStore.MediaColumns.MIME_TYPE, CameraConfig.VIDEO_MIMETYPE)
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Video.Media.RELATIVE_PATH, CameraConfig.VIDEO_FOLDER)
                put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis())
            }

        }

        val outputFileOptions = OutputFileOptions
            .builder(
                getApplication<MainApplication>().contentResolver,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            .build()

        cameraController.startRecording(outputFileOptions, Dispatchers.Main.asExecutor(),
            object : OnVideoSavedCallback {
            override fun onVideoSaved(outputFileResults: OutputFileResults) {
                val msg = "Video record succeeded: " + outputFileResults.savedUri
                Toast.makeText(getApplication(), msg, Toast.LENGTH_LONG).show()
                viewModelScope.launch {
                    _recordState.emit(RecordState.FINALIZED)
                }
            }

            override fun onError(
                videoCaptureError: Int,
                message: String,
                cause: Throwable?
            ) {
                Log.e(TAG, "Video saving failed: $message")
            }
        })

        // Prevents screen rotation during the video recording


        viewModelScope.launch {
            _recordState.emit(RecordState.RECORDING)
        }



        Log.i(TAG, "Recording started")
    }


    fun setAnalysis(analyzer: ImageAnalysis.Analyzer){
        if(!cameraController.isImageAnalysisEnabled) {
            cameraController.setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
        }
        cameraController.setImageAnalysisAnalyzer(Dispatchers.Default.asExecutor(), analyzer)
        viewModelScope.launch {
            val currentCameraUiState = _cameraUiState.value
            _cameraUiState.emit(currentCameraUiState.copy(analysisState = AnalysisState.READY))
        }
    }

    fun clearAnalysis(){
        cameraController.clearImageAnalysisAnalyzer()
        Log.d("CameraX", "clearAnalysis")
    }

    /**
     * Stops the preview stream. This should be invoked when the captured image is displayed.
     */
    fun stopPreview() {
        viewModelScope.launch {
            _cameraUiState.emit(_cameraUiState.value.copy(cameraState = MediaState.PREVIEW_STOPPED, analysisState = AnalysisState.ANALYSIS_STOPPED))
        }
    }

    @androidx.annotation.OptIn(androidx.camera.view.video.ExperimentalVideo::class)
    fun stopRecording() {
        cameraController.stopRecording()
    }


    /**
     * Toggle the camera lens face. This has no effect if there is only one available camera lens.
     */
    /**
     * Toggle the camera lens face. This has no effect if there is only one available camera lens.
     */
    fun switchCamera() {
        val currentCameraUiState = _cameraUiState.value
        if (currentCameraUiState.cameraState == MediaState.READY) {
            // To switch the camera lens, there has to be at least 2 camera lenses
            if (currentCameraUiState.availableCameraLens.size == 1) return

            val camLensFacing = currentCameraUiState.cameraLens
            // Toggle the lens facing
            val newCameraUiState = if (camLensFacing == CameraSelector.LENS_FACING_BACK) {
                currentCameraUiState.copy(cameraLens = CameraSelector.LENS_FACING_FRONT)
            } else {
                currentCameraUiState.copy(cameraLens = CameraSelector.LENS_FACING_BACK)
            }

            viewModelScope.launch {
                _cameraUiState.emit(
                    newCameraUiState.copy(
                        cameraState = MediaState.NOT_READY
                    )
                )
                _captureUiState.emit(CaptureState.CaptureNotReady)
            }
        }
    }

    /**
     * Captures the photo and saves it to the pictures directory that's inside the app-specific
     * directory on external storage.
     * Upon successful capture, the captureUiState flow will emit CaptureFinished with the URI to
     * the captured photo.
     * If the capture operation failed then captureUiState flow will emit CaptureFailed with the
     * exception containing more details on the reason for failure.
     */
    /**
     * Captures the photo and saves it to the pictures directory that's inside the app-specific
     * directory on external storage.
     * Upon successful capture, the captureUiState flow will emit CaptureFinished with the URI to
     * the captured photo.
     * If the capture operation failed then captureUiState flow will emit CaptureFailed with the
     * exception containing more details on the reason for failure.
     */
    fun capturePhoto() {

        if(!cameraController.isImageCaptureEnabled){
            cameraController.setEnabledUseCases(CameraController.IMAGE_CAPTURE)
        }
        viewModelScope.launch {
            _captureUiState.emit(CaptureState.CaptureStarted)
        }
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, CameraConfig.FILENAME)
            put(MediaStore.MediaColumns.MIME_TYPE, CameraConfig.IMAGE_MIMETYPE)
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, CameraConfig.IMAGE_FOLDER)
                put(MediaStore.Images.Media.IS_PENDING, 1)
                put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
            }
        }

        val metadata = ImageCapture.Metadata().apply {
            // Mirror image when using the front camera
            isReversedHorizontal =
                _cameraUiState.value.cameraLens == CameraSelector.LENS_FACING_FRONT
        }
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(
                getApplication<MainApplication>().contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                .setMetadata(metadata)
                .build()

        cameraController.takePicture(
            outputFileOptions,
            Dispatchers.Default.asExecutor(),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    viewModelScope.launch {
                        _captureUiState.emit(CaptureState.CaptureFinished(outputFileResults))
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    viewModelScope.launch {
                        _captureUiState.emit(CaptureState.CaptureFailed(exception))
                    }
                }
            })
    }

    /**
     * Sets the current extension mode. This will force the camera to rebind the use cases.
     */
    /**
     * Sets the current extension mode. This will force the camera to rebind the use cases.
     */
    fun setExtensionMode(@ExtensionMode.Mode extensionMode: Int) {
        viewModelScope.launch {
            _cameraUiState.emit(
                _cameraUiState.value.copy(
                    cameraState = MediaState.NOT_READY,
                    extensionMode = extensionMode,
                )
            )
            _captureUiState.emit(CaptureState.CaptureNotReady)
        }
    }

    private fun cameraLensToSelector(@LensFacing lensFacing: Int): CameraSelector =
        when (lensFacing) {
            CameraSelector.LENS_FACING_FRONT -> CameraSelector.DEFAULT_FRONT_CAMERA
            CameraSelector.LENS_FACING_BACK -> CameraSelector.DEFAULT_BACK_CAMERA
            else -> throw IllegalArgumentException("Invalid lens facing type: $lensFacing")
        }

}
