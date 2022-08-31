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
import android.content.Context
import android.graphics.Bitmap
import android.hardware.camera2.*
import android.media.MediaFormat
import android.opengl.GLSurfaceView
import android.os.Build
import android.os.ConditionVariable
import android.os.Handler
import android.os.HandlerThread
import android.provider.MediaStore
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Toast
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.core.*
import androidx.camera.core.CameraSelector.LensFacing
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.core.util.Consumer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.example.detectionexample.MainApplication
import com.example.detectionexample.config.CameraConfig
import com.example.detectionexample.config.CameraConfig.TAG
import com.example.detectionexample.config.Util
import com.example.detectionexample.custom.BitmapEncoding
import com.example.detectionexample.custom.GLCameraRender
import com.example.detectionexample.record.BitmapToVideoEncoder
import com.example.detectionexample.record.EncoderWrapper
import com.example.detectionexample.record.HardwarePipeline
import com.example.detectionexample.record.Pipeline
import com.example.detectionexample.uistate.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.delay
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


    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var extensionsManager: ExtensionsManager

    private val imageCaptureBuilder = ImageCapture.Builder()
        .setTargetAspectRatio(AspectRatio.RATIO_16_9)
    private lateinit var imageCapture: ImageCapture

    // build a recorder, which can:
    //   - record video/audio to MediaStore(only shown here), File, ParcelFileDescriptor
    //   - be used create recording(s) (the recording performs recording)


    private val quality = Quality.HD
    private val qualitySelector = QualitySelector.from(quality)
    private val recorder = Recorder.Builder()
        .setQualitySelector(qualitySelector)
        .build()
    private val videoCapture: VideoCapture<Recorder> = VideoCapture.withOutput(recorder)

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

    private val imageAnalysis = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
        .setOutputImageRotationEnabled(true)
//        .setTargetAspectRatio(AspectRatio.RATIO_16_9)
        .build()

    private val previewBuilder = Preview.Builder()
        .setTargetAspectRatio(AspectRatio.RATIO_16_9)
    private lateinit var preview: Preview

    private val _cameraUiState: MutableStateFlow<CameraUiState> = MutableStateFlow(CameraUiState())
    private val _captureUiState: MutableStateFlow<CaptureState> =
        MutableStateFlow(CaptureState.CaptureNotReady)

    private val _recordingState: MutableStateFlow<VideoRecordEvent?> = MutableStateFlow(null)


    private val _recordState: MutableStateFlow<RecordState> = MutableStateFlow(RecordState.IDLE)

    private var _action: MutableSharedFlow<CameraUiAction> = MutableSharedFlow()


    val cameraUiState: Flow<CameraUiState> = _cameraUiState
    val captureUiState: Flow<CaptureState> = _captureUiState
    val action: Flow<CameraUiAction> = _action
    val recordingState: Flow<VideoRecordEvent?> = _recordingState
    val recordState: Flow<RecordState> = _recordState


    private lateinit var pipeline: Pipeline

    /** File where the recording will be saved */
    private val outputFile: File by lazy { Util.createFile( "mp4") }

    /**
     * Setup a [Surface] for the encoder
     */
    private val encoderSurface: Surface by lazy {
        encoderWrapper.getInputSurface()
    }

    /** [EncoderWrapper] utility class */
    private lateinit var encoderWrapper: EncoderWrapper

    private var recordingStartMillis: Long = 0L

    /** [HandlerThread] where all camera operations run */
    private val cameraThread = HandlerThread("CameraThread").apply { start() }

    /** [Handler] corresponding to [cameraThread] */
    private val cameraHandler = Handler(cameraThread.looper)

    @Volatile
    private var recordingStarted = false

    @Volatile
    private var recordingComplete = false

    /** Condition variable for blocking until the recording completes */
    private val cvRecordingStarted = ConditionVariable(false)
    private val cvRecordingComplete = ConditionVariable(false)
    /** Orientation of the camera as 0, 90, 180, or 270 degrees */
    private var orientation: Int = 0



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

            // get the camera selector for the select lens face
            val cameraSelector = cameraLensToSelector(currentCameraUiState.cameraLens)

            // wait for the camera provider instance and extensions manager instance
            cameraProvider = ProcessCameraProvider.getInstance(getApplication()).await()
            extensionsManager =
                ExtensionsManager.getInstanceAsync(getApplication(), cameraProvider).await()

            val availableCameraLens =
                listOf(
                    CameraSelector.LENS_FACING_BACK,
                    CameraSelector.LENS_FACING_FRONT
                ).filter { lensFacing ->
                    cameraProvider.hasCamera(cameraLensToSelector(lensFacing))
                }

            // get the supported extensions for the selected camera lens by filtering the full list
            // of extensions and checking each one if it's available
            val availableExtensions = listOf(
                ExtensionMode.AUTO,
                ExtensionMode.BOKEH,
                ExtensionMode.HDR,
                ExtensionMode.NIGHT,
                ExtensionMode.FACE_RETOUCH
            ).filter { extensionMode ->
                extensionsManager.isExtensionAvailable(cameraSelector, extensionMode)
            }

            // prepare the new camera UI state which is now in the READY state and contains the list
            // of available extensions, available lens faces.
            val newCameraUiState = currentCameraUiState.copy(
                cameraState = MediaState.READY,
                analysisState = AnalysisState.NOT_READY,
                availableExtensions = listOf(ExtensionMode.NONE) + availableExtensions,
                availableCameraLens = availableCameraLens,
                extensionMode = if (availableExtensions.isEmpty()) ExtensionMode.NONE else currentCameraUiState.extensionMode,
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
        lifecycleOwner: LifecycleOwner,
        surfaceProvider: GLSurfaceView
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


//         Create an Extender to attach Camera2 options
        val imageCaptureExtender = Camera2Interop.Extender(imageCaptureBuilder)

        imageCaptureExtender.setSessionCaptureCallback(object : CameraCaptureSession.CaptureCallback(){
            override fun onCaptureCompleted(session: CameraCaptureSession,
                                            request: CaptureRequest,
                                            result: TotalCaptureResult
            ) {
                if (isCurrentlyRecording()) {
                    encoderWrapper.frameAvailable()
                }
            }
        }
    )
        imageCapture = imageCaptureBuilder.build()





        preview = previewBuilder.build()

        val useCaseGroupBuilder = UseCaseGroup.Builder()
            .addUseCase(preview)
            .addUseCase(imageAnalysis)
            .addUseCase(imageCapture)
//            .addUseCase(videoCapture)
        val useCaseGroup = useCaseGroupBuilder.build()
        cameraProvider.unbindAll()
        val camera = cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            useCaseGroup
        )
//        preview.setSurfaceProvider(previewView.surfaceProvider)

        surfaceProvider.apply {
            setEGLContextClientVersion(2)
            val callback = object : GLCameraRender.Callback {
                override fun onSurfaceChanged() {
                }

                override fun onFrameAvailable() {
                    requestRender()
                }
            }
            val cameraRender = GLCameraRender(context, callback)
            cameraRender.type = "Normal"
            setRenderer(cameraRender)
            renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
            preview.setSurfaceProvider(cameraRender)
        }



        val cameraId = Camera2CameraInfo.from(camera.cameraInfo).cameraId
        val cameraManager = getApplication<MainApplication>().applicationContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)

        orientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)!!

        encoderWrapper = createEncoder()

//        pipeline = HardwarePipeline(width, height, fps, false,
//            characteristics, encoderWrapper, surfaceView.holder.surface)

        viewModelScope.launch {
            _cameraUiState.emit(_cameraUiState.value.copy(cameraState = MediaState.READY))
            _captureUiState.emit(CaptureState.CaptureReady)
        }
    }

    val width = 480
    val height = 640
    val fps = 30

    companion object {
        private const val RECORDER_VIDEO_BITRATE: Int = 10_000_000
        private const val MIN_REQUIRED_RECORDING_TIME_MILLIS: Long = 1000L
    }

    private fun createEncoder(): EncoderWrapper {
        val videoEncoder = MediaFormat.MIMETYPE_VIDEO_AVC
        val codecProfile = -1

        var width = this.width
        var height = this.height

        if (orientation == 90 || orientation == 270) {
            width = this.height
            height = this.width
        }
        val orientationHint = 0

        return EncoderWrapper(width, height, RECORDER_VIDEO_BITRATE, this.fps,
            orientationHint, videoEncoder, codecProfile, outputFile)

    }

    private fun isCurrentlyRecording(): Boolean {
        return recordingStarted && !recordingComplete
    }

    private var currentRecording: Recording? = null


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
    fun startRecording() {
        // create MediaStoreOutputOptions for our recorder: resulting our recording!
//        val contentValues = ContentValues().apply {
//            put(MediaStore.MediaColumns.DISPLAY_NAME, "${CameraConfig.FILENAME}.mp4")
//            put(MediaStore.MediaColumns.MIME_TYPE, CameraConfig.VIDEO_MIMETYPE)
//            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
//                put(MediaStore.Video.Media.RELATIVE_PATH, CameraConfig.VIDEO_FOLDER)
//                put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis())
//            }
//
//        }
//        val mediaStoreOutput = MediaStoreOutputOptions.Builder(
//            getApplication<MainApplication>().contentResolver,
//            MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
//            .setContentValues(contentValues)
//            .build()
//
//        // configure Recorder and Start recording to the mediaStoreOutput.
//        currentRecording = videoCapture.output
//            .prepareRecording(getApplication(), mediaStoreOutput)
//            .apply { if (false) withAudioEnabled() }
//            .start(Dispatchers.Main.asExecutor(), captureListener)

        // Prevents screen rotation during the video recording


//        encoder.startEncoding(width, height, outputFile)

        pipeline.actionDown(encoderSurface)
        // Finalizes encoder setup and starts recording
        recordingStarted = true
        encoderWrapper.start()
        cvRecordingStarted.open()
        pipeline.startRecording()
        recordingStartMillis = System.currentTimeMillis()

        viewModelScope.launch {
            _recordState.emit(RecordState.RECORDING)
        }



        Log.i(TAG, "Recording started")
    }

    /**
     * CaptureEvent listener.
     */
    private val captureListener = Consumer<VideoRecordEvent> { event ->
        // cache the recording state
        if (event !is VideoRecordEvent.Status)
            viewModelScope.launch { _recordingState.emit(event) }

//        updateUI(event)

        if (event is VideoRecordEvent.Finalize) {
            // display the captured video
            Log.d(TAG, "File recording completed\n${event.outputResults.outputUri}\n__")
        }
    }


    fun setAnalysis(analyzer: ImageAnalysis.Analyzer){
        imageAnalysis.setAnalyzer(Dispatchers.Default.asExecutor(), analyzer)
        viewModelScope.launch {
            val currentCameraUiState = _cameraUiState.value
            _cameraUiState.emit(currentCameraUiState.copy(analysisState = AnalysisState.READY))
        }
    }

    fun clearAnalysis(){
        cameraProvider.unbind(imageAnalysis)
    }

    /**
     * Stops the preview stream. This should be invoked when the captured image is displayed.
     */
    fun stopPreview() {
        preview.setSurfaceProvider(null)
        viewModelScope.launch {
            _cameraUiState.emit(_cameraUiState.value.copy(cameraState = MediaState.PREVIEW_STOPPED, analysisState = AnalysisState.ANALYSIS_STOPPED))
        }
    }

    fun stopRecording() {
//        currentRecording?.stop()
//        encoder.stopEncoding()

        viewModelScope.launch(Dispatchers.IO) {
            cvRecordingStarted.block()

            /* Wait for at least one frame to process so we don't have an empty video */
            encoderWrapper.waitForFirstFrame()

            pipeline.clearFrameListener()

            /* Wait until the session signals onReady */
            cvRecordingComplete.block()

            // Requires recording of at least MIN_REQUIRED_RECORDING_TIME_MILLIS
            val elapsedTimeMillis = System.currentTimeMillis() - recordingStartMillis
            if (elapsedTimeMillis < MIN_REQUIRED_RECORDING_TIME_MILLIS) {
                delay(MIN_REQUIRED_RECORDING_TIME_MILLIS - elapsedTimeMillis)
            }


            Log.d(TAG, "Recording stopped. Output file: $outputFile")
            encoderWrapper.shutdown()

            pipeline.cleanup()
        }
    }


    fun pauseRecording() {
        currentRecording?.pause()
    }

    fun resumeRecording() {
        currentRecording?.resume()
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


        imageCapture.takePicture(
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
