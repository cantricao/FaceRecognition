package com.example.detectionexample.compose

import android.net.Uri
import android.widget.Toast
import androidx.camera.extensions.ExtensionMode
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.detectionexample.R
import com.example.detectionexample.custom.GlideOverlayBitmapTransformation
import com.example.detectionexample.uistate.*
import com.example.detectionexample.viewmodels.AnalysisViewModel
import com.example.detectionexample.viewmodels.CameraViewModel
import com.skydoves.landscapist.glide.GlideImage
import kotlinx.coroutines.flow.collectLatest


@Composable
fun CameraPreview(viewModel: AnalysisViewModel = viewModel(), cameraViewModel: CameraViewModel = viewModel()) {
    if (!viewModel.isStaringCamera) return
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val previewView = remember { PreviewView(context) }


    var enableCameraShutter by remember { mutableStateOf(true) }
    var enableSwitchLens by remember { mutableStateOf(true) }
    var enableClosePhotoPreview by remember { mutableStateOf(false) }
    var enablePhotoPreview by remember { mutableStateOf(false) }
    var enableExtensionSelector by remember { mutableStateOf(true) }
    var glideUri by remember { mutableStateOf(Uri.EMPTY) }

    fun showCameraControls() {
        enableCameraShutter = true
        enableSwitchLens = true
        enableExtensionSelector = true
    }

    fun hidePhoto() {
        enablePhotoPreview = false
        enableClosePhotoPreview = false
        enableExtensionSelector = false
    }

    fun hideCameraControls() {
        enableCameraShutter = false
        enableSwitchLens = false
    }

    fun showCaptureError(errorMessage: String) {
        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
    }

    fun showPhoto(uri: Uri?) {
        if (uri == null) return
        enablePhotoPreview= true
        glideUri = uri
        enableClosePhotoPreview = true
    }

    val trackedObjectsState by viewModel.trackedObserver.collectAsState(initial = listOf())



    produceState<CameraUiAction>(initialValue = CameraUiAction.SelectCameraExtension(ExtensionMode.NONE), cameraViewModel.action){
        cameraViewModel.action.collectLatest { action ->
            when(action){
                is CameraUiAction.SelectCameraExtension -> cameraViewModel.setExtensionMode(action.extension)
                CameraUiAction.ClosePhotoPreviewClick -> {
                    hidePhoto()
                    showCameraControls()
                    cameraViewModel.startPreview(lifecycleOwner, previewView)
                }
                CameraUiAction.ShutterButtonClick -> cameraViewModel.capturePhoto()
                CameraUiAction.SwitchCameraClick -> cameraViewModel.switchCamera()
                CameraUiAction.RecordButtonClick -> Unit
            }
            value = action
        }

    }

    val cameraState by produceState(initialValue = CameraUiState(), cameraViewModel.cameraUiState) {
        cameraViewModel.cameraUiState.collectLatest { cameraState ->
            when(cameraState.analysisState){
                AnalysisState.NOT_READY -> cameraViewModel.setAnalysis(viewModel.analyzer)
                AnalysisState.READY -> Unit
                AnalysisState.ANALYSIS_STOPPED -> cameraViewModel.clearAnalysis()
            }
            when(cameraState.cameraState) {
                MediaState.NOT_READY -> cameraViewModel.initializeCamera()
                MediaState.READY  -> {
                    when (cameraState.analysisState){
                        AnalysisState.NOT_READY -> Unit
                        AnalysisState.READY, AnalysisState.ANALYSIS_STOPPED -> cameraViewModel.startPreview(lifecycleOwner, previewView)
                    }
                }
                MediaState.PREVIEW_STOPPED -> Unit
            }
            value = cameraState
        }

    }

    produceState<CaptureState>(initialValue = CaptureState.CaptureNotReady, cameraViewModel.captureUiState) {
        cameraViewModel.captureUiState.collectLatest { captureUiState ->
            when (captureUiState) {
                CaptureState.CaptureNotReady -> {
                    hidePhoto()
                    enableCameraShutter = true
                    enableSwitchLens = true
                }
                is CaptureState.CaptureFailed -> {
                    val error = captureUiState.exception.message!!
                    showCaptureError(error)
                    cameraViewModel.startPreview(lifecycleOwner, previewView)
                    enableCameraShutter = true
                    enableSwitchLens = true
                }
                is CaptureState.CaptureFinished -> {
                    cameraViewModel.stopPreview()
                    showPhoto(captureUiState.outputResults.savedUri)
                    hideCameraControls()
                }
                CaptureState.CaptureReady -> {
                    enableCameraShutter = true
                    enableSwitchLens = true
                }
                CaptureState.CaptureStarted -> {
                    enableCameraShutter = false
                    enableSwitchLens = false
                }
            }
            value = captureUiState
        }
    }



    val extensionName = mapOf(
        ExtensionMode.AUTO to stringResource(R.string.camera_mode_auto),
        ExtensionMode.NIGHT to stringResource(R.string.camera_mode_night),
        ExtensionMode.HDR to stringResource(R.string.camera_mode_hdr),
        ExtensionMode.FACE_RETOUCH to stringResource(R.string.camera_mode_face_retouch),
        ExtensionMode.BOKEH to stringResource(R.string.camera_mode_bokeh),
        ExtensionMode.NONE to stringResource(R.string.camera_mode_none),
    )
    
    val videoRecordEvent by cameraViewModel.recordingState.collectAsState(initial = null)

    Box(Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center) {
        AndroidView(
            factory = {
                previewView
            },
            modifier = Modifier.fillMaxSize(),
        )
        Column(modifier = Modifier.align(Alignment.BottomEnd)) {
            LazyRow(
//                contentPadding = PaddingValues(24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),

                ) {
                items(cameraState.availableExtensions) { extension ->
                    TextButton(onClick = {
                        cameraViewModel.setAction(CameraUiAction.SelectCameraExtension(extension)) },
                        enabled = enableExtensionSelector
                    ) {
                        Text(
                            text = extensionName[extension]!!,
                            textAlign = TextAlign.Center,
                            color = Color.White
                        )
                    }
                }
            }
            Row(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                FilledTonalIconButton(
                    modifier = Modifier
                        .size(92.dp + 32.dp)
                        .padding(PaddingValues(32.dp)),
                    onClick = {
                        when(videoRecordEvent){
                            is VideoRecordEvent.Start -> cameraViewModel.stopRecording()
                            is VideoRecordEvent.Finalize, null -> cameraViewModel.startRecording()
                            else -> throw IllegalStateException("recordingState in unknown state")
                        }
                    },
                    enabled = enableCameraShutter) {
                    Icon(
                        painter = painterResource(id =
                        when (videoRecordEvent){
                            is VideoRecordEvent.Start -> R.drawable.ic_baseline_stop_24
                            else -> R.drawable.ic_baseline_play_arrow_24

                        }),
                        contentDescription = "Shutter button",
                        modifier = Modifier.size(48.dp)
                    )
                }

                FilledTonalIconButton(
                    modifier = Modifier
                        .size(92.dp + 32.dp)
                        .padding(PaddingValues(32.dp)),
                    onClick = { cameraViewModel.setAction(CameraUiAction.ShutterButtonClick) },
                    enabled = enableCameraShutter) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_baseline_camera_24),
                        contentDescription = "Shutter button",
                        modifier = Modifier.size(48.dp)
                    )
                }

                FilledTonalIconButton(
                    modifier = Modifier
                        .size(92.dp + 32.dp)
                        .padding(PaddingValues(32.dp)),
                    onClick = { cameraViewModel.setAction(CameraUiAction.SwitchCameraClick) },
                    enabled = enableSwitchLens) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_baseline_flip_camera_android_24),
                        contentDescription = "switch lens",
                        modifier = Modifier.size(48.dp)
                    )
                }

            }
        }
        if(enablePhotoPreview){
            GlideImage(
                imageModel = glideUri,
                modifier = Modifier.fillMaxSize(),
                requestOptions = {
                    RequestOptions()
                        .transform(GlideOverlayBitmapTransformation(trackedObjectsState))
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                },
                contentDescription = "Image Preview",
            )
            FilledTonalIconButton(onClick = { cameraViewModel.setAction(CameraUiAction.ClosePhotoPreviewClick) },
                enabled = enableClosePhotoPreview,
                modifier = Modifier.align(Alignment.TopEnd)) {
                Icon(Icons.Default.Close, contentDescription = "Close Photo Preview")
            }
        } else {
            OverlayView()
        }


    }

//                // Setup image capture listener which is triggered after photo has been taken
//                imageCapture.takePicture(outputOptions, cameraExecutor, object :
//                    ImageCapture.OnImageSavedCallback {
//                    override fun onError(exception: ImageCaptureException) {
//                        Log.e(CameraConfig.TAG, "Photo capture failed: ${exception.message}", exception)
//                        value = Uri.EMPTY
//                        viewModel.isCaptureImage = false
//                    }
//
//                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
//                        val jsonString = Json.encodeToString(trackedObjectsState)
//                        Log.d("trackedObjectsState1:", jsonString)
//                        val request = OneTimeWorkRequestBuilder<SaveImageWorker>()
//                            .setInputData(workDataOf(
//                                CameraConfig.KEY_CAPTURE_IMAGE to outputFileResults.savedUri.toString(),
//                                CameraConfig.KEY_CAPTURE_TRACKER_OBJECT to jsonString
//                            ))
//                            .build()
//                        worker.enqueueUniqueWork(CameraConfig.KEY_CAPTURE_IMAGE,ExistingWorkPolicy.KEEP,  request)
//                        viewModel.isCaptureImage = false
//                        Log.d(CameraConfig.TAG, "Photo capture succeeded: $value")
//                    }
//                })
//            }
//        }

//    DisposableEffect(lifecycleOwner) {
//        onDispose {
//            cameraProvider.unbindAll()
//            cameraExecutor.shutdown()
//            displayManager.unregisterDisplayListener(displayListener)
//        }
//    }
}




