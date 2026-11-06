package com.example.detectionexample.compose.video

import android.content.ComponentName
import android.content.res.Configuration
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.cast.MediaRouteButton
import androidx.media3.common.DeviceInfo
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionToken
import com.example.detectionexample.compose.adaptive.FormFactorPreviews
import com.example.detectionexample.core.ui.R
import com.example.detectionexample.feature.video.playback.CarUtils
import com.example.detectionexample.feature.video.playback.FacePlaybackService
import com.example.detectionexample.ui.theme.DetectionExampleTheme
import com.example.detectionexample.uistate.MediaState
import com.example.detectionexample.uistate.RecordState
import com.example.detectionexample.viewmodels.AnalysisViewModel
import com.example.detectionexample.viewmodels.VideoViewModel
import kotlinx.coroutines.delay
import androidx.media3.ui.compose.material3.Player as Material3Player

val sampleVideoUri: Uri = Uri.parse("asset:///face-demographics-walking-and-pause.mp4")

/**
 * Modern Jetpack Compose video player screen with Jetpack Media3 CastPlayer integration.
 * Adheres strictly to Media3 Cast guidelines:
 * - Uses MediaRouteButton composable from androidx.media3.cast
 * - Zero AndroidView in the Compose UI hierarchy
 * - Uses Material3 Player composable (androidx.media3.ui.compose.material3.Player)
 * - Forces recomposition on playback location shifts using key(deviceInfo)
 */
@OptIn(ExperimentalMaterial3Api::class, UnstableApi::class)
@Composable
fun VideoPlayer(
    modifier: Modifier = Modifier,
    viewModel: AnalysisViewModel = hiltViewModel(),
    videoViewModel: VideoViewModel = hiltViewModel(),
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val configuration = LocalConfiguration.current

    // Determine whether explicit in-app back affordance must be displayed for Android Automotive OS (AN-1)
    val showInAppBack: Boolean = remember(context) { CarUtils.shouldShowInAppBack(context) }

    // Preserve media playback state across configuration changes
    var savedPositionMs by rememberSaveable { mutableLongStateOf(0L) }
    var savedPlayWhenReady by rememberSaveable { mutableStateOf(true) }

    // Connect to background FacePlaybackService via MediaController
    val activePlayer = rememberMediaController(
        serviceClass = FacePlaybackService::class.java,
        fallbackPlayer = videoViewModel.exoPlayer
    )
    val deviceInfo = rememberDeviceInfo(activePlayer)

    LaunchedEffect(videoViewModel.videoState) {
        videoViewModel.videoState.collect { videoState ->
            when (videoState) {
                MediaState.NOT_READY -> {
                    videoViewModel.loadVideo(sampleVideoUri)
                    if (savedPositionMs > 0L) {
                        videoViewModel.exoPlayer.seekTo(savedPositionMs)
                    }
                    videoViewModel.exoPlayer.playWhenReady = savedPlayWhenReady
                }
                MediaState.READY -> {
                    if (savedPositionMs > 0L) {
                        videoViewModel.exoPlayer.seekTo(savedPositionMs)
                    }
                    videoViewModel.exoPlayer.playWhenReady = savedPlayWhenReady
                }
                MediaState.PREVIEW_STOPPED -> Unit
            }
        }
    }

    DisposableEffect(lifecycleOwner, activePlayer) {
        val player = activePlayer ?: videoViewModel.exoPlayer
        val playerListener = object : Player.Listener {
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                savedPlayWhenReady = playWhenReady
                savedPositionMs = player.currentPosition
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                savedPositionMs = newPosition.positionMs
            }
        }
        player.addListener(playerListener)

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    if (savedPlayWhenReady) {
                        player.play()
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    val wasPlaying = player.playWhenReady
                    savedPositionMs = player.currentPosition
                    player.pause()
                    savedPlayWhenReady = wasPlaying
                    videoViewModel.stopRecording()
                }
                Lifecycle.Event.ON_STOP -> player.stop()
                Lifecycle.Event.ON_DESTROY -> Unit
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            player.removeListener(playerListener)
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(activePlayer) {
        val player = activePlayer ?: videoViewModel.exoPlayer
        while (true) {
            if (player.isPlaying) {
                savedPositionMs = player.currentPosition
            }
            delay(250)
        }
    }

    SideEffect {
        viewModel.needUpdateTrackerImageSourceInfo = true
    }

    val videoRecordEvent = videoViewModel.recordState.collectAsState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val isLandscapeOrTablet = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE ||
            configuration.screenWidthDp >= 600

    // Key constraint on DeviceInfo forces full recomposition when playback location shifts between local and Cast
    activePlayer?.let { player ->
        key(deviceInfo) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .focusRequester(focusRequester)
                    .focusable()
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown) {
                            when (event.key) {
                                Key.Spacebar -> {
                                    if (player.isPlaying) player.pause() else player.play()
                                    true
                                }
                                Key.R -> {
                                    when (videoRecordEvent.value) {
                                        RecordState.RECORDING -> videoViewModel.stopRecording()
                                        RecordState.IDLE, RecordState.FINALIZED -> videoViewModel.startRecording()
                                        else -> Unit
                                    }
                                    true
                                }
                                else -> false
                            }
                        } else {
                            false
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (showInAppBack) {
                    FilledTonalIconButton(
                        onClick = {
                            if (onBack != null) {
                                onBack()
                            } else {
                                (context as? ComponentActivity)?.onBackPressedDispatcher?.onBackPressed()
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .statusBarsPadding()
                            .displayCutoutPadding()
                            .padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }

                if (isLandscapeOrTablet) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            Material3Player(
                                player = player,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(0.35f)
                                .fillMaxHeight()
                                .navigationBarsPadding()
                                .displayCutoutPadding()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CastAndPlaybackControls(
                                videoViewModel = videoViewModel,
                                videoRecordEvent = videoRecordEvent,
                                deviceInfo = deviceInfo
                            )
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Material3Player(
                                player = player,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CastAndPlaybackControls(
                                videoViewModel = videoViewModel,
                                videoRecordEvent = videoRecordEvent,
                                deviceInfo = deviceInfo
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Controls section featuring MediaRouteButton composable placed alongside playback & record controls.
 */
@OptIn(ExperimentalMaterial3Api::class, UnstableApi::class)
@Composable
private fun CastAndPlaybackControls(
    videoViewModel: VideoViewModel,
    videoRecordEvent: androidx.compose.runtime.State<RecordState>,
    deviceInfo: DeviceInfo?
) {
    val isRemote = deviceInfo?.playbackType == DeviceInfo.PLAYBACK_TYPE_REMOTE

    Surface(
        color = if (isRemote) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Text(
            text = if (isRemote) "Casting to Remote Device" else "Local Playback",
            style = MaterialTheme.typography.labelMedium,
            color = if (isRemote) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // MediaRouteButton Composable from androidx.media3.cast
        MediaRouteButton(
            modifier = Modifier.size(48.dp)
        )

        // Video Recording Control
        TooltipBox(
            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
            tooltip = {
                PlainTooltip {
                    Text(
                        if (videoRecordEvent.value == RecordState.RECORDING) "Stop recording (R)" else "Record video (R)"
                    )
                }
            },
            state = rememberTooltipState()
        ) {
            FilledTonalIconButton(
                modifier = Modifier.size(64.dp),
                onClick = {
                    when (videoRecordEvent.value) {
                        RecordState.RECORDING -> videoViewModel.stopRecording()
                        RecordState.IDLE, RecordState.FINALIZED -> videoViewModel.startRecording()
                        else -> Unit
                    }
                }
            ) {
                Icon(
                    painter = painterResource(
                        id = when (videoRecordEvent.value) {
                            RecordState.RECORDING -> R.drawable.ic_baseline_stop_24
                            else -> R.drawable.ic_baseline_play_arrow_24
                        }
                    ),
                    contentDescription = if (videoRecordEvent.value == RecordState.RECORDING) "Stop Recording" else "Start Recording",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

/**
 * Connects to MediaSessionService using MediaController and handles lifecycle release.
 */
@OptIn(UnstableApi::class)
@Composable
private fun rememberMediaController(
    serviceClass: Class<out MediaSessionService> = FacePlaybackService::class.java,
    fallbackPlayer: Player? = null
): Player? {
    val context = LocalContext.current
    var controller by remember { mutableStateOf<Player?>(fallbackPlayer) }

    DisposableEffect(context, serviceClass) {
        val sessionToken = SessionToken(context, ComponentName(context, serviceClass))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        val executor = ContextCompat.getMainExecutor(context)

        controllerFuture.addListener(
            {
                try {
                    controller = controllerFuture.get()
                } catch (_: Exception) {
                    if (controller == null) {
                        controller = fallbackPlayer
                    }
                }
            },
            executor
        )

        onDispose {
            controllerFuture.cancel(true)
            if (controller != fallbackPlayer) {
                controller?.release()
            }
            controller = null
        }
    }

    return controller ?: fallbackPlayer
}

/**
 * Listens for DeviceInfo changes to detect playback location shifts between local and Cast remote.
 */
@Composable
private fun rememberDeviceInfo(player: Player?): DeviceInfo? {
    var deviceInfo by remember(player) { mutableStateOf(player?.deviceInfo) }
    DisposableEffect(player) {
        val activePlayer = player ?: return@DisposableEffect onDispose {}
        deviceInfo = activePlayer.deviceInfo
        val listener = object : Player.Listener {
            override fun onDeviceInfoChanged(info: DeviceInfo) {
                deviceInfo = info
            }
        }
        activePlayer.addListener(listener)
        onDispose { activePlayer.removeListener(listener) }
    }
    return deviceInfo
}

@OptIn(UnstableApi::class)
@FormFactorPreviews
@Composable
private fun VideoPlayerAdaptivePreview() {
    DetectionExampleTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Video Player Adaptive Preview",
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}
