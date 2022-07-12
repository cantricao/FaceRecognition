package com.example.detectionexample.compose

import android.content.ContentValues
import android.content.Context
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.AttributeSet
import android.util.Log
import androidx.camera.core.*
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.*
import com.example.detectionexample.config.CameraConfig
import com.example.detectionexample.viewmodels.DetectionViewModel
import com.example.detectionexample.worker.SaveImageWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.Executors


@Composable
fun CameraPreview(viewModel: DetectionViewModel = viewModel()) {
    if (!viewModel.isStaringCamera) return
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    val worker by lazy { WorkManager.getInstance(context) }
    val previewView = PreviewView(context)
    var lensFacing =  remember { CameraSelector.LENS_FACING_BACK }
    val imageCapture = remember { ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
        .setTargetAspectRatio(AspectRatio.RATIO_16_9)
        .build() }

    val cameraProviderFuture =  ProcessCameraProvider.getInstance(context)
    val cameraProvider by remember { mutableStateOf(cameraProviderFuture.get()) }
    val imageAnalysis = remember {
        ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .setOutputImageRotationEnabled(true)
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .build()
            .apply {
                setAnalyzer(viewModel.analysisExecutor, viewModel.analyzer)
            }
    }
    val trackedObjectsState by viewModel.trackedObserver.collectAsState(initial = listOf())

    val preview = Preview.Builder().build()
    val displayManager by lazy {
        context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    }
    val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) {
            if (displayId == previewView.display.displayId) {
                Log.d(CameraConfig.TAG, "Rotation changed: ${previewView.display.rotation}")
                imageCapture.targetRotation = previewView.display.rotation
                imageAnalysis.targetRotation = previewView.display.rotation
                viewModel.needUpdateTrackerImageSourceInfo = true
            }
        }
    }
    val scope = rememberCoroutineScope()
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    var cameraSelector = remember {
        CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()
    }

    viewModel.captureUri = produceState<Uri>(initialValue = Uri.EMPTY, viewModel.isCaptureImage) {
        if(viewModel.isCaptureImage) {
            scope.launch(Dispatchers.IO) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, CameraConfig.FILENAME)
                    put(MediaStore.MediaColumns.MIME_TYPE, CameraConfig.MIMETYPE)
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, CameraConfig.FOLDER)
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                        put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
                    }
                }


                // Setup image capture metadata
                val metadata = ImageCapture.Metadata().apply {

                    // Mirror image when using the front camera
                    isReversedHorizontal = lensFacing == CameraSelector.LENS_FACING_FRONT
                }

                // Create output options object which contains file + metadata
                val outputOptions = ImageCapture.OutputFileOptions.Builder(
                    context.contentResolver,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    .setMetadata(metadata)
                    .build()

                // Setup image capture listener which is triggered after photo has been taken
                imageCapture.takePicture(outputOptions, cameraExecutor, object :
                    ImageCapture.OnImageSavedCallback {
                    override fun onError(exception: ImageCaptureException) {
                        Log.e(CameraConfig.TAG, "Photo capture failed: ${exception.message}", exception)
                        value = Uri.EMPTY
                        viewModel.isCaptureImage = false
                    }

                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        val jsonString = Json.encodeToString(trackedObjectsState)
                        Log.d("trackedObjectsState1:", jsonString)
                        val request = OneTimeWorkRequestBuilder<SaveImageWorker>()
                            .setInputData(workDataOf(
                                CameraConfig.KEY_CAPTURE_IMAGE to outputFileResults.savedUri.toString(),
                                CameraConfig.KEY_CAPTURE_TRACKER_OBJECT to jsonString
                            ))
                            .build()
                        worker.enqueueUniqueWork(CameraConfig.KEY_CAPTURE_IMAGE,ExistingWorkPolicy.KEEP,  request)
                        viewModel.isCaptureImage = false
                        Log.d(CameraConfig.TAG, "Photo capture succeeded: $value")
                    }
                })
            }
        }
        value = Uri.EMPTY
        viewModel.isCaptureImage = false
    }.value

    LaunchedEffect(Unit){
        // Create an extensions manager
        val extensionsManager =
            ExtensionsManager.getInstanceAsync(context, cameraProvider).await()
        // Query if extension is available.
        if (extensionsManager.isExtensionAvailable(
                cameraSelector,
                ExtensionMode.BOKEH
            )
        ){
            cameraSelector = extensionsManager.getExtensionEnabledCameraSelector(
                cameraSelector,
                ExtensionMode.BOKEH
            )
        }
    }

    fun bindPreview() {
        preview.setSurfaceProvider(previewView.surfaceProvider)
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageAnalysis,
            imageCapture)
    }


    fun hasBackCamera(): Boolean {
        return cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
    }

    fun hasFrontCamera(): Boolean {
        return cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
    }
    AndroidView(
        factory = { ctx ->
            displayManager.registerDisplayListener(displayListener, null)
            cameraProviderFuture.addListener({
                // Select lensFacing depending on the available cameras
                lensFacing = when {
                    hasBackCamera() -> CameraSelector.LENS_FACING_BACK
                    hasFrontCamera() -> CameraSelector.LENS_FACING_FRONT
                    else -> throw IllegalStateException("Back and front camera are unavailable")
                }
                bindPreview()
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )

    DisposableEffect(lifecycleOwner) {
        onDispose {
            cameraProvider.unbindAll()
            cameraExecutor.shutdown()
            displayManager.unregisterDisplayListener(displayListener)
        }
    }
}




