package com.example.detectionexample.compose.camera

import android.net.Uri
import android.widget.Toast
import androidx.camera.extensions.ExtensionMode
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.detectionexample.R
import com.example.detectionexample.config.OverlayViewConfig
import com.example.detectionexample.glide.GlideOverlayBitmapTransformation
import com.example.detectionexample.models.TrackedRecognition
import com.example.detectionexample.uistate.AnalysisState
import com.example.detectionexample.uistate.CameraUiAction
import com.example.detectionexample.uistate.CameraUiState
import com.example.detectionexample.uistate.MediaState
import com.example.detectionexample.uistate.RecordState
import com.example.detectionexample.viewmodels.AnalysisViewModel
import com.example.detectionexample.viewmodels.CameraXViewModel
import com.example.detectionexample.views.DetectionDrawable
import com.skydoves.landscapist.glide.GlideImage
import kotlinx.coroutines.flow.collectLatest


@Composable
fun CameraPreview(viewModel: AnalysisViewModel = hiltViewModel(), cameraViewModel: CameraXViewModel = hiltViewModel()) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    OverlayViewConfig.setContextToFixTextSize(context)
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            controller = cameraViewModel.cameraController
        }
    }

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
        enablePhotoPreview = true
        glideUri = uri
        enableClosePhotoPreview = true
    }

    val trackedObjectsState = viewModel.trackedObserver.collectAsState()

    val drawable = remember { DetectionDrawable(trackedObjectsState, previewView.width, previewView.height) }


    var cameraState by remember { mutableStateOf(CameraUiState()) }

    LaunchedEffect(cameraViewModel.cameraUiState) {
        cameraViewModel.cameraUiState.collectLatest { state ->
            when (state.cameraState) {
                MediaState.NOT_READY -> cameraViewModel.initializeCamera()
                MediaState.READY -> {
                    when (state.analysisState) {
                        AnalysisState.NOT_READY -> Unit
                        AnalysisState.READY, AnalysisState.ANALYSIS_STOPPED -> {
                            cameraViewModel.startPreview(lifecycleOwner, trackedObjectsState)
                        }
                    }
                }

                MediaState.PREVIEW_STOPPED -> Unit
            }

            when (state.analysisState) {
                AnalysisState.NOT_READY -> cameraViewModel.setAnalysis(viewModel.analyzer)
                AnalysisState.READY -> Unit
                AnalysisState.ANALYSIS_STOPPED -> cameraViewModel.clearAnalysis()
            }
            cameraState = state
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

    val videoRecordEvent by cameraViewModel.recordState.collectAsState(initial = RecordState.IDLE)

    produceState<List<TrackedRecognition>>(
        initialValue = mutableListOf(),
        trackedObjectsState.value
    ) {
        previewView.overlay.clear()
        previewView.overlay.add(drawable)
        value = trackedObjectsState.value
    }

    produceState<CameraUiAction>(initialValue = CameraUiAction.RecordButtonClick, cameraViewModel.action){
        cameraViewModel.action.collectLatest { action ->
            when (action) {
                is CameraUiAction.SelectCameraExtension -> cameraViewModel.setExtensionMode(action.extension)
                CameraUiAction.ClosePhotoPreviewClick -> {
                    hidePhoto()
                    showCameraControls()
                    cameraViewModel.startPreview(lifecycleOwner, trackedObjectsState)
                }

                CameraUiAction.ShutterButtonClick -> {
                    cameraViewModel.capturePhoto()
                }

                CameraUiAction.SwitchCameraClick -> cameraViewModel.switchCamera()
                CameraUiAction.RecordButtonClick -> Unit
            }
            value = action
        }
    }

    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize(),
        )
        Column(modifier = Modifier.align(Alignment.BottomEnd)) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(24.dp),

                ) {
                items(cameraState.availableExtensions) { extension ->
                    TextButton(
                        onClick = {
                            cameraViewModel.setAction(CameraUiAction.SelectCameraExtension(extension))
                        },
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
                        when (videoRecordEvent) {
                            RecordState.RECORDING -> cameraViewModel.stopRecording()
                            RecordState.IDLE, RecordState.FINALIZED -> cameraViewModel.startRecording()
                            else -> throw IllegalStateException("recordingState in unknown state")
                        }
                    },
                    enabled = enableCameraShutter
                ) {
                    Icon(
                        painter = painterResource(
                            id =
                            when (videoRecordEvent) {
                                RecordState.RECORDING -> R.drawable.ic_baseline_stop_24
                                else -> R.drawable.ic_baseline_play_arrow_24

                            }
                        ),
                        contentDescription = "Shutter button",
                        modifier = Modifier.size(48.dp)
                    )
                }

                FilledTonalIconButton(
                    modifier = Modifier
                        .size(92.dp + 32.dp)
                        .padding(PaddingValues(32.dp)),
                    onClick = { cameraViewModel.setAction(CameraUiAction.ShutterButtonClick) },
                    enabled = enableCameraShutter
                ) {
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
                    enabled = enableSwitchLens
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_baseline_flip_camera_android_24),
                        contentDescription = "switch lens",
                        modifier = Modifier.size(48.dp)
                    )
                }

            }
        }
        if (enablePhotoPreview) {
            GlideImage(
                imageModel = { glideUri },
                modifier = Modifier.fillMaxSize(),
                requestOptions = {
                    RequestOptions()
                        .transform(GlideOverlayBitmapTransformation(trackedObjectsState))
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                },
            )
            FilledTonalIconButton(
                onClick = { cameraViewModel.setAction(CameraUiAction.ClosePhotoPreviewClick) },
                enabled = enableClosePhotoPreview,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close Photo Preview")
            }
        }

    }
}




