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

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.ContentValues
import android.content.pm.PackageManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraEffect
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraSelector.LensFacing
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.video.VideoRecordEvent.Finalize
import androidx.camera.video.VideoRecordEvent.Finalize.ERROR_NONE
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.video.AudioConfig
import androidx.compose.runtime.State
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.example.detectionexample.cameraeffect.ToneMappingSurfaceEffect
import com.example.detectionexample.config.CameraConfig
import com.example.detectionexample.config.CameraConfig.TAG
import com.example.detectionexample.models.TrackedRecognition
import com.example.detectionexample.uistate.AnalysisState
import com.example.detectionexample.uistate.CameraManualSettings
import com.example.detectionexample.uistate.CameraUiAction
import com.example.detectionexample.uistate.CameraUiState
import com.example.detectionexample.uistate.CaptureFormat
import com.example.detectionexample.uistate.CaptureState
import com.example.detectionexample.uistate.MediaState
import com.example.detectionexample.uistate.RecordState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * View model for camera operations and extensions.
 * Manages preview, capture, analysis, video recording, manual Camera2 controls, and extensions.
 */
@HiltViewModel
class CameraXViewModel @Inject constructor(application: Application) : AndroidViewModel(application) {

    val cameraController = LifecycleCameraController(getApplication()).apply {
        isTapToFocusEnabled = true
        isPinchToZoomEnabled = true
    }

    private var cameraProvider: ProcessCameraProvider? = null
    private var extensionsManager: ExtensionsManager? = null
    private var mActiveRecording: Recording? = null

    private val _cameraUiState: MutableStateFlow<CameraUiState> = MutableStateFlow(CameraUiState())
    private val _captureUiState: MutableStateFlow<CaptureState> = MutableStateFlow(CaptureState.CaptureNotReady)
    private val _recordState: MutableStateFlow<RecordState> = MutableStateFlow(RecordState.IDLE)
    private val _action: MutableSharedFlow<CameraUiAction> = MutableSharedFlow()

    val cameraUiState: Flow<CameraUiState> = _cameraUiState
    val captureUiState: Flow<CaptureState> = _captureUiState
    val action: Flow<CameraUiAction> = _action
    val recordState: Flow<RecordState> = _recordState

    /**
     * Initializes the camera, binds CameraProvider, and initializes ExtensionsManager asynchronously.
     */
    fun initializeCamera() {
        viewModelScope.launch {
            try {
                val currentCameraUiState = _cameraUiState.value

                cameraController.initializationFuture.await()
                val provider = ProcessCameraProvider.getInstance(getApplication()).await()
                cameraProvider = provider
                val extManager = ExtensionsManager.getInstanceAsync(getApplication(), provider).await()
                extensionsManager = extManager

                val availableCameraLens = listOf(
                    CameraSelector.LENS_FACING_BACK,
                    CameraSelector.LENS_FACING_FRONT
                ).filter { lensFacing ->
                    cameraController.hasCamera(cameraLensToSelector(lensFacing))
                }

                // Query supported extension modes for the active lens
                val baseSelector = cameraLensToSelector(currentCameraUiState.cameraLens)
                val candidateModes = listOf(
                    ExtensionMode.AUTO,
                    ExtensionMode.NIGHT,
                    ExtensionMode.HDR,
                    ExtensionMode.FACE_RETOUCH,
                    ExtensionMode.BOKEH
                )
                val supportedExtensions = candidateModes.filter { mode ->
                    extManager.isExtensionAvailable(baseSelector, mode)
                }

                val hasAudioPermission = ContextCompat.checkSelfPermission(
                    getApplication(),
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED

                val isRawSupported = cameraController.cameraInfo?.let { info ->
                    try {
                        val capabilities = ImageCapture.getImageCaptureCapabilities(info)
                        capabilities.supportedOutputFormats.contains(ImageCapture.OUTPUT_FORMAT_RAW)
                    } catch (ise: IllegalStateException) {
                        Log.d(TAG, "RAW capabilities check failed: ${ise.message}", ise)
                        false
                    } catch (iae: IllegalArgumentException) {
                        Log.d(TAG, "RAW capabilities check failed: ${iae.message}", iae)
                        false
                    }
                } ?: false

                val newCameraUiState = currentCameraUiState.copy(
                    cameraState = MediaState.READY,
                    analysisState = AnalysisState.NOT_READY,
                    availableExtensions = listOf(ExtensionMode.NONE) + supportedExtensions,
                    availableCameraLens = availableCameraLens,
                    extensionMode = ExtensionMode.NONE,
                    isRawSupported = isRawSupported,
                    hasAudioPermission = hasAudioPermission
                )
                _cameraUiState.emit(newCameraUiState)
            } catch (ise: IllegalStateException) {
                Log.e(TAG, "Camera initialization failed: ${ise.message}", ise)
            } catch (iae: IllegalArgumentException) {
                Log.e(TAG, "Camera initialization failed: ${iae.message}", iae)
            }
        }
    }

    fun setAction(cameraUiAction: CameraUiAction) {
        viewModelScope.launch {
            _action.emit(cameraUiAction)
        }
    }

    /**
     * Starts the preview stream and binds use cases.
     */
    fun startPreview(
        lifecycleOwner: LifecycleOwner,
        trackedObjectsState: State<List<TrackedRecognition>>
    ) {
        val currentCameraUiState = _cameraUiState.value
        val extManager = extensionsManager
        val cameraSelector = if (currentCameraUiState.extensionMode == ExtensionMode.NONE || extManager == null) {
            cameraLensToSelector(currentCameraUiState.cameraLens)
        } else {
            try {
                extManager.getExtensionEnabledCameraSelector(
                    cameraLensToSelector(currentCameraUiState.cameraLens),
                    currentCameraUiState.extensionMode
                )
            } catch (iae: IllegalArgumentException) {
                Log.w(
                    TAG,
                    "Extension mode ${currentCameraUiState.extensionMode} fallback to default selector: ${iae.message}",
                    iae
                )
                cameraLensToSelector(currentCameraUiState.cameraLens)
            } catch (ise: IllegalStateException) {
                Log.w(
                    TAG,
                    "Extension mode ${currentCameraUiState.extensionMode} fallback to default selector: ${ise.message}",
                    ise
                )
                cameraLensToSelector(currentCameraUiState.cameraLens)
            }
        }

        cameraController.cameraSelector = cameraSelector
        cameraController.bindToLifecycle(lifecycleOwner)

        val effects = mutableSetOf<CameraEffect>()
        effects.add(ToneMappingSurfaceEffect(trackedObjectsState))
        cameraController.setEffects(effects)

        applyManualSettings(currentCameraUiState.manualSettings)

        viewModelScope.launch {
            _cameraUiState.emit(_cameraUiState.value.copy(cameraState = MediaState.READY))
            _captureUiState.emit(CaptureState.CaptureReady)
        }
    }

    /**
     * Updates manual Camera2Interop settings such as ISO, exposure time, and focus distance.
     */
    fun updateManualSettings(settings: CameraManualSettings) {
        _cameraUiState.value = _cameraUiState.value.copy(manualSettings = settings)
        applyManualSettings(settings)
    }

    fun toggleManualControls() {
        val current = _cameraUiState.value.isManualControlsVisible
        _cameraUiState.value = _cameraUiState.value.copy(isManualControlsVisible = !current)
    }

    fun setCaptureFormat(format: CaptureFormat) {
        _cameraUiState.value = _cameraUiState.value.copy(captureFormat = format)
    }

    @androidx.annotation.OptIn(androidx.camera.camera2.interop.ExperimentalCamera2Interop::class)
    private fun applyManualSettings(settings: CameraManualSettings) {
        try {
            val cameraControl = cameraController.cameraControl ?: return
            val camera2Control = Camera2CameraControl.from(cameraControl)
            val builder = CaptureRequestOptions.Builder()

            if (settings.isManualMode) {
                builder.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF)
                builder.setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, settings.iso)
                builder.setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, settings.exposureTimeNs)
                builder.setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_OFF)
                builder.setCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE, settings.focusDistance)
            } else {
                builder.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON)
                builder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AF_MODE,
                    CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                )
                cameraControl.setExposureCompensationIndex(settings.exposureCompensation)
            }

            camera2Control.setCaptureRequestOptions(builder.build())
            cameraControl.setZoomRatio(settings.zoomRatio)
        } catch (ise: IllegalStateException) {
            Log.w(TAG, "Failed to apply manual settings: ${ise.message}", ise)
        } catch (iae: IllegalArgumentException) {
            Log.w(TAG, "Failed to apply manual settings: ${iae.message}", iae)
        }
    }

    /**
     * Starts video recording with proper AudioConfig and VideoRecordEvent lifecycle handling.
     */
    @SuppressLint("MissingPermission")
    fun startRecording() {
        if (mActiveRecording != null) {
            Log.w(TAG, "Recording is already active")
            return
        }

        cameraController.setEnabledUseCases(CameraController.VIDEO_CAPTURE)

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "${CameraConfig.FILENAME}_${System.currentTimeMillis()}.mp4")
            put(MediaStore.MediaColumns.MIME_TYPE, CameraConfig.VIDEO_MIMETYPE)
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Video.Media.RELATIVE_PATH, CameraConfig.VIDEO_FOLDER)
                put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis())
            }
        }

        val outputFileOptions = MediaStoreOutputOptions
            .Builder(
                getApplication<Application>().contentResolver,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            )
            .setContentValues(contentValues)
            .build()

        val hasAudioPermission = ContextCompat.checkSelfPermission(
            getApplication(),
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        // Immutability pattern: reassign/chain AudioConfig correctly
        val audioConfig = if (hasAudioPermission) AudioConfig.create(true) else AudioConfig.create(false)

        val mVideoRecordEventListener = Consumer<VideoRecordEvent> { videoRecordEvent ->
            when (videoRecordEvent) {
                is VideoRecordEvent.Start -> {
                    viewModelScope.launch {
                        _recordState.emit(RecordState.RECORDING)
                    }
                }
                is VideoRecordEvent.Pause -> {
                    viewModelScope.launch {
                        _recordState.emit(RecordState.PAUSED)
                    }
                }
                is VideoRecordEvent.Resume -> {
                    viewModelScope.launch {
                        _recordState.emit(RecordState.RECORDING)
                    }
                }
                is VideoRecordEvent.Finalize -> {
                    mActiveRecording = null
                    val uri = videoRecordEvent.outputResults.outputUri
                    if (videoRecordEvent.error == ERROR_NONE) {
                        Log.i(TAG, "Video record succeeded: $uri")
                        viewModelScope.launch {
                            _recordState.emit(RecordState.FINALIZED)
                        }
                    } else {
                        Log.e(TAG, "Video saving failed: ${videoRecordEvent.error}")
                        viewModelScope.launch {
                            _recordState.emit(RecordState.IDLE)
                        }
                    }
                }
            }
        }

        try {
            mActiveRecording = cameraController.startRecording(
                outputFileOptions,
                audioConfig,
                Dispatchers.Default.asExecutor(),
                mVideoRecordEventListener
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Audio or video recording permission revoked at runtime", e)
            if (hasAudioPermission) {
                // Retry without audio capture
                try {
                    mActiveRecording = cameraController.startRecording(
                        outputFileOptions,
                        AudioConfig.create(false),
                        Dispatchers.Default.asExecutor(),
                        mVideoRecordEventListener
                    )
                } catch (inner: SecurityException) {
                    Log.e(TAG, "Camera recording permission error: ${inner.message}")
                    viewModelScope.launch {
                        _recordState.emit(RecordState.IDLE)
                    }
                }
            } else {
                viewModelScope.launch {
                    _recordState.emit(RecordState.IDLE)
                }
            }
        }
    }

    /**
     * Pauses or resumes an active recording.
     */
    fun pauseResumeRecording() {
        mActiveRecording?.let { recording ->
            when (_recordState.value) {
                RecordState.RECORDING -> recording.pause()
                RecordState.PAUSED -> recording.resume()
                else -> Unit
            }
        }
    }

    /**
     * Stops the active video recording safely.
     */
    fun stopRecording() {
        mActiveRecording?.stop()
        mActiveRecording = null
    }

    fun setAnalysis(analyzer: ImageAnalysis.Analyzer) {
        cameraController.imageAnalysisTargetSize = CameraController.OutputSize(AspectRatio.RATIO_4_3)
        cameraController.imageAnalysisBackpressureStrategy = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
        cameraController.setImageAnalysisAnalyzer(Dispatchers.Default.asExecutor(), analyzer)
        if (!cameraController.isImageAnalysisEnabled) {
            cameraController.setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
        }
        viewModelScope.launch {
            val currentCameraUiState = _cameraUiState.value
            _cameraUiState.emit(currentCameraUiState.copy(analysisState = AnalysisState.READY))
        }
    }

    fun clearAnalysis() {
        cameraController.clearImageAnalysisAnalyzer()
        Log.d("CameraX", "clearAnalysis")
    }

    fun stopPreview() {
        viewModelScope.launch {
            _cameraUiState.emit(
                _cameraUiState.value.copy(
                    cameraState = MediaState.PREVIEW_STOPPED,
                    analysisState = AnalysisState.ANALYSIS_STOPPED
                )
            )
        }
    }

    fun switchCamera() {
        val currentCameraUiState = _cameraUiState.value
        if (currentCameraUiState.cameraState == MediaState.READY) {
            if (currentCameraUiState.availableCameraLens.size <= 1) return

            val camLensFacing = currentCameraUiState.cameraLens
            val newCameraUiState = if (camLensFacing == CameraSelector.LENS_FACING_BACK) {
                currentCameraUiState.copy(cameraLens = CameraSelector.LENS_FACING_FRONT)
            } else {
                currentCameraUiState.copy(cameraLens = CameraSelector.LENS_FACING_BACK)
            }

            viewModelScope.launch {
                _cameraUiState.emit(newCameraUiState.copy(cameraState = MediaState.NOT_READY))
                _captureUiState.emit(CaptureState.CaptureNotReady)
            }
        }
    }

    /**
     * Captures a photo supporting JPEG, RAW, or RAW+JPEG depending on selected CaptureFormat.
     */
    fun capturePhoto() {
        if (!cameraController.isImageCaptureEnabled) {
            cameraController.setEnabledUseCases(CameraController.IMAGE_CAPTURE)
        }
        viewModelScope.launch {
            _captureUiState.emit(CaptureState.CaptureStarted)
        }

        val format = _cameraUiState.value.captureFormat
        val filename = "${CameraConfig.FILENAME}_${System.currentTimeMillis()}"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(
                MediaStore.MediaColumns.MIME_TYPE,
                if (format == CaptureFormat.RAW) "image/x-adobe-dng" else CameraConfig.IMAGE_MIMETYPE
            )
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, CameraConfig.IMAGE_FOLDER)
                put(MediaStore.Images.Media.IS_PENDING, 1)
                put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
            }
        }

        val metadata = ImageCapture.Metadata().apply {
            isReversedHorizontal = _cameraUiState.value.cameraLens == CameraSelector.LENS_FACING_FRONT
        }

        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(
            getApplication<Application>().contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )
            .setMetadata(metadata)
            .build()

        try {
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
                }
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Camera permission revoked at runtime during capture", e)
            viewModelScope.launch {
                _captureUiState.emit(CaptureState.CaptureNotReady)
            }
        }
    }

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
