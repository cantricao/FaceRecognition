package com.example.detectionexample.compose.camera

import android.content.res.Configuration
import android.net.Uri
import androidx.annotation.StringRes
import androidx.camera.extensions.ExtensionMode
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.detectionexample.compose.adaptive.FormFactorPreviews
import com.example.detectionexample.config.OverlayViewConfig
import com.example.detectionexample.core.ui.R
import com.example.detectionexample.glide.GlideOverlayBitmapTransformation
import com.example.detectionexample.ui.theme.DetectionExampleTheme
import com.example.detectionexample.uistate.AnalysisState
import com.example.detectionexample.uistate.CameraManualSettings
import com.example.detectionexample.uistate.CameraUiAction
import com.example.detectionexample.uistate.CameraUiState
import com.example.detectionexample.uistate.CaptureFormat
import com.example.detectionexample.uistate.MediaState
import com.example.detectionexample.uistate.RecordState
import com.example.detectionexample.viewmodels.AnalysisViewModel
import com.example.detectionexample.viewmodels.CameraXViewModel
import com.example.detectionexample.views.DetectionDrawable
import com.skydoves.landscapist.glide.GlideImage
import kotlinx.coroutines.flow.collectLatest
import java.util.Locale

private val SelectedButtonColor = Color.White.copy(alpha = 0.2f)
private val SelectedButtonShape = RoundedCornerShape(8.dp)

@StringRes
private fun getExtensionNameRes(extension: Int): Int = when (extension) {
    ExtensionMode.AUTO -> R.string.camera_mode_auto
    ExtensionMode.NIGHT -> R.string.camera_mode_night
    ExtensionMode.HDR -> R.string.camera_mode_hdr
    ExtensionMode.FACE_RETOUCH -> R.string.camera_mode_face_retouch
    ExtensionMode.BOKEH -> R.string.camera_mode_bokeh
    else -> R.string.camera_mode_none
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    viewModel: AnalysisViewModel = hiltViewModel(),
    cameraViewModel: CameraXViewModel = hiltViewModel()
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    OverlayViewConfig.setContextToFixTextSize(context)

    val isWideScreen = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE ||
            configuration.screenWidthDp >= 600

    val trackedObjectsState = viewModel.trackedObserver.collectAsState()
    val drawable = remember { DetectionDrawable(trackedObjectsState, 0, 0) }

    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.PERFORMANCE
            scaleType = PreviewView.ScaleType.FILL_CENTER
            controller = cameraViewModel.cameraController
            addOnLayoutChangeListener { _, left, top, right, bottom, _, _, _, _ ->
                drawable.setBounds(0, 0, right - left, bottom - top)
            }
        }
    }

    var enableCameraShutter by rememberSaveable { mutableStateOf(true) }
    var enableSwitchLens by rememberSaveable { mutableStateOf(true) }
    var enableClosePhotoPreview by rememberSaveable { mutableStateOf(false) }
    var enablePhotoPreview by rememberSaveable { mutableStateOf(false) }
    var enableExtensionSelector by rememberSaveable { mutableStateOf(true) }
    var glideUri by rememberSaveable { mutableStateOf(Uri.EMPTY) }
    var showTelemetryInspector by rememberSaveable { mutableStateOf(configuration.screenWidthDp >= 840) }

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

    val videoRecordEvent by cameraViewModel.recordState.collectAsState(initial = RecordState.IDLE)

    DisposableEffect(previewView, drawable) {
        previewView.overlay.add(drawable)
        onDispose {
            previewView.overlay.remove(drawable)
        }
    }

    LaunchedEffect(viewModel.trackedObserver) {
        viewModel.trackedObserver.collect {
            drawable.invalidateSelf()
        }
    }

    LaunchedEffect(cameraViewModel.action) {
        cameraViewModel.action.collectLatest { action ->
            when (action) {
                is CameraUiAction.SelectCameraExtension -> cameraViewModel.setExtensionMode(action.extension)
                CameraUiAction.ClosePhotoPreviewClick -> {
                    hidePhoto()
                    showCameraControls()
                    cameraViewModel.startPreview(lifecycleOwner, trackedObjectsState)
                }
                CameraUiAction.ShutterButtonClick -> cameraViewModel.capturePhoto()
                CameraUiAction.SwitchCameraClick -> cameraViewModel.switchCamera()
                CameraUiAction.RecordButtonClick -> Unit
                CameraUiAction.PauseResumeRecordClick -> cameraViewModel.pauseResumeRecording()
                CameraUiAction.ToggleManualControlsClick -> cameraViewModel.toggleManualControls()
                is CameraUiAction.UpdateManualSettings -> cameraViewModel.updateManualSettings(action.settings)
                is CameraUiAction.SetCaptureFormat -> cameraViewModel.setCaptureFormat(action.format)
            }
        }
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.Spacebar, Key.Enter -> {
                            if (enableCameraShutter && videoRecordEvent == RecordState.IDLE) {
                                cameraViewModel.setAction(CameraUiAction.ShutterButtonClick)
                            }
                            true
                        }
                        Key.C -> {
                            if (enableSwitchLens && videoRecordEvent == RecordState.IDLE) {
                                cameraViewModel.setAction(CameraUiAction.SwitchCameraClick)
                            }
                            true
                        }
                        Key.R -> {
                            when (videoRecordEvent) {
                                RecordState.RECORDING, RecordState.PAUSED -> cameraViewModel.stopRecording()
                                RecordState.IDLE, RecordState.FINALIZED -> cameraViewModel.startRecording()
                                else -> Unit
                            }
                            true
                        }
                        Key.M -> {
                            cameraViewModel.toggleManualControls()
                            true
                        }
                        Key.T -> {
                            showTelemetryInspector = !showTelemetryInspector
                            true
                        }
                        Key.Escape -> {
                            if (cameraState.isManualControlsVisible) {
                                cameraViewModel.toggleManualControls()
                                true
                            } else {
                                false
                            }
                        }
                        else -> false
                    }
                } else {
                    false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize(),
        )

        // Top Control Bar (Settings, Manual Controls & Telemetry toggle)
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                tooltip = { PlainTooltip { Text("Toggle telemetry inspector (T)") } },
                state = rememberTooltipState()
            ) {
                FilledTonalIconButton(
                    onClick = { showTelemetryInspector = !showTelemetryInspector },
                    modifier = Modifier.semantics(mergeDescendants = true) {}
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Detection Telemetry",
                        tint = if (showTelemetryInspector) MaterialTheme.colorScheme.primary else Color.White
                    )
                }
            }

            TooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                tooltip = { PlainTooltip { Text("Manual camera controls (M)") } },
                state = rememberTooltipState()
            ) {
                FilledTonalIconButton(
                    onClick = { cameraViewModel.toggleManualControls() },
                    modifier = Modifier.semantics(mergeDescendants = true) {}
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Manual Camera Controls",
                        tint = if (cameraState.manualSettings.isManualMode) MaterialTheme.colorScheme.primary else Color.White
                    )
                }
            }
        }

        // Adaptive Controls: Side panel on wide/landscape/tabletop screens, Bottom row on compact phones
        if (isWideScreen) {
            // Extensions Selector along top-start
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(top = 16.dp, start = 16.dp)
            ) {
                items(
                    items = cameraState.availableExtensions,
                    key = { it },
                    contentType = { "extension" }
                ) { extension ->
                    val isSelected = cameraState.extensionMode == extension
                    TextButton(
                        onClick = {
                            cameraViewModel.setAction(CameraUiAction.SelectCameraExtension(extension))
                        },
                        enabled = enableExtensionSelector,
                        modifier = Modifier
                            .semantics(mergeDescendants = true) {}
                            .then(
                                if (isSelected) {
                                    Modifier.background(
                                    SelectedButtonColor,
                                    SelectedButtonShape
                                )
                                } else {
                                    Modifier
                                }
                            )
                    ) {
                        Text(
                            text = stringResource(getExtensionNameRes(extension)),
                            textAlign = TextAlign.Center,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // Right-side vertical control rail for tablets / landscape
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .navigationBarsPadding()
                    .displayCutoutPadding()
                    .padding(end = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Record Button
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = {
                        PlainTooltip {
                            Text(
                                if (videoRecordEvent == RecordState.RECORDING) "Stop recording (R)" else "Record video (R)"
                            )
                        }
                    },
                    state = rememberTooltipState()
                ) {
                    FilledTonalIconButton(
                        modifier = Modifier
                            .size(72.dp)
                            .semantics(mergeDescendants = true) {},
                        onClick = {
                            when (videoRecordEvent) {
                                RecordState.RECORDING, RecordState.PAUSED -> cameraViewModel.stopRecording()
                                RecordState.IDLE, RecordState.FINALIZED -> cameraViewModel.startRecording()
                                else -> Unit
                            }
                        },
                        enabled = enableCameraShutter
                    ) {
                        Icon(
                            painter = painterResource(
                                id = when (videoRecordEvent) {
                                    RecordState.RECORDING, RecordState.PAUSED -> R.drawable.ic_baseline_stop_24
                                    else -> R.drawable.ic_baseline_play_arrow_24
                                }
                            ),
                            contentDescription = if (videoRecordEvent == RecordState.RECORDING) "Stop Recording" else "Start Recording",
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // Shutter Button
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = { PlainTooltip { Text("Take photo (Space)") } },
                    state = rememberTooltipState()
                ) {
                    FilledTonalIconButton(
                        modifier = Modifier
                            .size(88.dp)
                            .semantics(mergeDescendants = true) {},
                        onClick = { cameraViewModel.setAction(CameraUiAction.ShutterButtonClick) },
                        enabled = enableCameraShutter && videoRecordEvent == RecordState.IDLE
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_baseline_camera_24),
                            contentDescription = "Take Photo",
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }

                // Switch Camera Lens Button
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = { PlainTooltip { Text("Switch camera lens (C)") } },
                    state = rememberTooltipState()
                ) {
                    FilledTonalIconButton(
                        modifier = Modifier
                            .size(72.dp)
                            .semantics(mergeDescendants = true) {},
                        onClick = { cameraViewModel.setAction(CameraUiAction.SwitchCameraClick) },
                        enabled = enableSwitchLens && videoRecordEvent == RecordState.IDLE
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_baseline_flip_camera_android_24),
                            contentDescription = "Switch Camera Lens",
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
        } else {
            // Standard compact bottom controls
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
            ) {
                // Extensions Selector
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(
                        items = cameraState.availableExtensions,
                        key = { it },
                        contentType = { "extension" }
                    ) { extension ->
                        val isSelected = cameraState.extensionMode == extension
                        TextButton(
                            onClick = {
                                cameraViewModel.setAction(CameraUiAction.SelectCameraExtension(extension))
                            },
                            enabled = enableExtensionSelector,
                            modifier = Modifier
                                .semantics(mergeDescendants = true) {}
                                .then(
                                    if (isSelected) {
                                        Modifier.background(
                                        SelectedButtonColor,
                                        SelectedButtonShape
                                    )
                                    } else {
                                        Modifier
                                    }
                                )
                        ) {
                            Text(
                                text = stringResource(getExtensionNameRes(extension)),
                                textAlign = TextAlign.Center,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                // Main Action Buttons
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Record Button (Start / Stop)
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = {
                            PlainTooltip {
                                Text(
                                    if (videoRecordEvent == RecordState.RECORDING) "Stop recording (R)" else "Record video (R)"
                                )
                            }
                        },
                        state = rememberTooltipState()
                    ) {
                        FilledTonalIconButton(
                            modifier = Modifier
                                .size(80.dp)
                                .padding(12.dp)
                                .semantics(mergeDescendants = true) {},
                            onClick = {
                                when (videoRecordEvent) {
                                    RecordState.RECORDING, RecordState.PAUSED -> cameraViewModel.stopRecording()
                                    RecordState.IDLE, RecordState.FINALIZED -> cameraViewModel.startRecording()
                                    else -> Unit
                                }
                            },
                            enabled = enableCameraShutter
                        ) {
                            Icon(
                                painter = painterResource(
                                    id = when (videoRecordEvent) {
                                        RecordState.RECORDING, RecordState.PAUSED -> R.drawable.ic_baseline_stop_24
                                        else -> R.drawable.ic_baseline_play_arrow_24
                                    }
                                ),
                                contentDescription = if (videoRecordEvent == RecordState.RECORDING) "Stop Recording" else "Start Recording",
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    // Pause/Resume recording button (visible only when recording/paused)
                    if (videoRecordEvent == RecordState.RECORDING || videoRecordEvent == RecordState.PAUSED) {
                        FilledTonalIconButton(
                            modifier = Modifier
                                .size(72.dp)
                                .padding(8.dp)
                                .semantics(mergeDescendants = true) {},
                            onClick = { cameraViewModel.pauseResumeRecording() }
                        ) {
                            Text(
                                text = if (videoRecordEvent == RecordState.PAUSED) "RESUME" else "PAUSE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Shutter Button (Capture Photo)
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text("Take photo (Space)") } },
                        state = rememberTooltipState()
                    ) {
                        FilledTonalIconButton(
                            modifier = Modifier
                                .size(96.dp)
                                .padding(12.dp)
                                .semantics(mergeDescendants = true) {},
                            onClick = { cameraViewModel.setAction(CameraUiAction.ShutterButtonClick) },
                            enabled = enableCameraShutter && videoRecordEvent == RecordState.IDLE
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_baseline_camera_24),
                                contentDescription = "Take Photo",
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }

                    // Switch Camera Lens Button
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text("Switch camera lens (C)") } },
                        state = rememberTooltipState()
                    ) {
                        FilledTonalIconButton(
                            modifier = Modifier
                                .size(80.dp)
                                .padding(12.dp)
                                .semantics(mergeDescendants = true) {},
                            onClick = { cameraViewModel.setAction(CameraUiAction.SwitchCameraClick) },
                            enabled = enableSwitchLens && videoRecordEvent == RecordState.IDLE
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_baseline_flip_camera_android_24),
                                contentDescription = "Switch Camera Lens",
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
            }
        }

        // Manual Controls Panel Overlay (Adaptive width constrained)
        AnimatedVisibility(
            visible = cameraState.isManualControlsVisible,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier
                .align(if (isWideScreen) Alignment.CenterEnd else Alignment.BottomCenter)
                .safeDrawingPadding()
                .padding(16.dp)
                .widthIn(max = 440.dp)
        ) {
            ManualSettingsCard(
                settings = cameraState.manualSettings,
                isRawSupported = cameraState.isRawSupported,
                selectedFormat = cameraState.captureFormat,
                onSettingsChange = { newSettings ->
                    cameraViewModel.updateManualSettings(newSettings)
                },
                onFormatChange = { format ->
                    cameraViewModel.setCaptureFormat(format)
                },
                onClose = { cameraViewModel.toggleManualControls() }
            )
        }

        // Real-Time Detection Telemetry Inspector Pane (Adaptive layout)
        AnimatedVisibility(
            visible = showTelemetryInspector,
            enter = slideInHorizontally { -it } + fadeIn(),
            exit = slideOutHorizontally { -it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            DetectionInspectorPane(
                trackedRecognitions = trackedObjectsState.value
            )
        }

        // Captured Photo Preview
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
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 16.dp, end = 16.dp)
                    .semantics(mergeDescendants = true) {}
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close Photo Preview")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualSettingsCard(
    settings: CameraManualSettings,
    isRawSupported: Boolean,
    selectedFormat: CaptureFormat,
    onSettingsChange: (CameraManualSettings) -> Unit,
    onFormatChange: (CaptureFormat) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Manual Camera Controls",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onClose) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close Manual Controls")
                }
            }

            // Capture Format Selection: JPEG / RAW
            Text(text = "Capture Format", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedFormat == CaptureFormat.JPEG,
                    onClick = { onFormatChange(CaptureFormat.JPEG) },
                    label = { Text("JPEG") }
                )
                FilterChip(
                    selected = selectedFormat == CaptureFormat.RAW,
                    onClick = {
                        if (isRawSupported) {
                            onFormatChange(CaptureFormat.RAW)
                        }
                    },
                    enabled = isRawSupported,
                    label = { Text(if (isRawSupported) "RAW (DNG)" else "RAW (Unsupported)") }
                )
            }

            // Manual Mode Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Enable Manual Exposure & Focus", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = settings.isManualMode,
                    onCheckedChange = { onSettingsChange(settings.copy(isManualMode = it)) }
                )
            }

            if (settings.isManualMode) {
                // ISO Slider
                Text(text = "ISO Sensitivity: ${settings.iso}", style = MaterialTheme.typography.labelSmall)
                Slider(
                    value = settings.iso.toFloat(),
                    onValueChange = { onSettingsChange(settings.copy(iso = it.toInt())) },
                    valueRange = 100f..3200f
                )

                // Exposure Time Slider (ms)
                val expMs = (settings.exposureTimeNs / 1_000_000).toFloat().coerceIn(1f, 100f)
                Text(
                    text = "Exposure Time: ${String.format(Locale.US, "%.1f", expMs)} ms",
                    style = MaterialTheme.typography.labelSmall
                )
                Slider(
                    value = expMs,
                    onValueChange = { onSettingsChange(settings.copy(exposureTimeNs = (it * 1_000_000).toLong())) },
                    valueRange = 1f..100f
                )

                // Focus Distance Slider
                Text(
                    text = "Focus Distance: ${String.format(Locale.US, "%.2f", settings.focusDistance)}",
                    style = MaterialTheme.typography.labelSmall
                )
                Slider(
                    value = settings.focusDistance,
                    onValueChange = { onSettingsChange(settings.copy(focusDistance = it)) },
                    valueRange = 0.0f..10.0f
                )
            } else {
                // Exposure Compensation Index for Auto Mode
                Text(
                    text = "Exposure Compensation: ${settings.exposureCompensation}",
                    style = MaterialTheme.typography.labelSmall
                )
                Slider(
                    value = settings.exposureCompensation.toFloat(),
                    onValueChange = { onSettingsChange(settings.copy(exposureCompensation = it.toInt())) },
                    valueRange = -4f..4f,
                    steps = 7
                )
            }
        }
    }
}

@FormFactorPreviews
@Composable
private fun CameraPreviewAdaptivePreview() {
    DetectionExampleTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Camera Preview Adaptive Preview",
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}
