package com.example.detectionexample.compose.video

import android.net.Uri
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.LegacyPlayerControlView
import androidx.media3.ui.PlayerView
import com.example.detectionexample.R
import com.example.detectionexample.grafika.TextureMovieEncoder
import com.example.detectionexample.uistate.MediaState
import com.example.detectionexample.uistate.RecordState
import com.example.detectionexample.viewmodels.AnalysisViewModel
import com.example.detectionexample.viewmodels.VideoViewModel
import com.example.detectionexample.views.BitmapOverlayVideoProcessor
import com.example.detectionexample.views.VideoProcessingGLSurfaceView

val sampleVideoUri: Uri = Uri.parse("asset:///face-demographics-walking-and-pause.mp4")
@Composable
@UnstableApi
fun  VideoPlayer(viewModel: AnalysisViewModel = hiltViewModel(),
                 videoViewModel: VideoViewModel = hiltViewModel()
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    LaunchedEffect( videoViewModel.videoState){
        videoViewModel.videoState.collect { videoState ->
            when (videoState) {
                MediaState.NOT_READY -> {
                    videoViewModel.loadVideo(sampleVideoUri)
                }
                MediaState.READY -> Unit
                MediaState.PREVIEW_STOPPED -> Unit
            }
        }

    }
    DisposableEffect(lifecycleOwner) {
        // Create an observer that triggers our remembered callbacks
        // for sending analytics events
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> videoViewModel.exoPlayer.play()
                Lifecycle.Event.ON_PAUSE -> {
                    videoViewModel.exoPlayer.pause()
                    videoViewModel.stopRecording()
                }
                Lifecycle.Event.ON_STOP -> videoViewModel.exoPlayer.stop()
                Lifecycle.Event.ON_DESTROY -> videoViewModel.exoPlayer.release()
                else -> {}
            }
        }

        // Add the observer to the lifecycle
        lifecycleOwner.lifecycle.addObserver(observer)

        // When the effect leaves the Composition, remove the observer
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }

    }
    viewModel.needUpdateTrackerImageSourceInfo = true

    val trackedObserver = viewModel.trackedObserver.collectAsState()
    val videoRecordEvent = videoViewModel.recordState.collectAsState()

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AndroidView(factory = { cxt ->
            PlayerView(cxt).apply {
                player = videoViewModel.exoPlayer
                useController = false
                val contentFrame = findViewById<FrameLayout>(androidx.media3.ui.R.id.exo_content_frame)
                val bitmapOverlayVideoProcessor = BitmapOverlayVideoProcessor(
                    context,
                    trackedObserver
                )
                val videoProcessor = BitmapOverlayVideoProcessor(context, trackedObserver)
                val sVideoEncoder = TextureMovieEncoder(videoProcessor)
                contentFrame.addView(
                    VideoProcessingGLSurfaceView(cxt,
                        false,
                        bitmapOverlayVideoProcessor, sVideoEncoder,
                        videoRecordEvent,
                        viewModel.bitmapAnalyzer)
                        .apply {
                            setPlayer(videoViewModel.exoPlayer)
                })
            }

        })

        Column(
            modifier = Modifier.align(Alignment.BottomCenter),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FilledTonalIconButton(
                modifier = Modifier
                    .size(92.dp + 32.dp)
                    .padding(PaddingValues(32.dp)),
                onClick = {
                    when (videoRecordEvent.value) {
                        RecordState.RECORDING -> videoViewModel.stopRecording()
                        RecordState.IDLE, RecordState.FINALIZED -> videoViewModel.startRecording()
                        else -> throw IllegalStateException("recordingState in unknown state")
                    }
                },
//                enabled = enableCameraShutter
            ) {
                Icon(
                    painter = painterResource(
                        id =
                        when (videoRecordEvent.value) {
                            RecordState.RECORDING -> R.drawable.ic_baseline_stop_24
                            else -> R.drawable.ic_baseline_play_arrow_24

                        }
                    ),
                    contentDescription = "Shutter button",
                    modifier = Modifier.size(48.dp)
                )
            }

            AndroidView(factory = { mContext ->
                LegacyPlayerControlView(mContext).apply {
                    player = videoViewModel.exoPlayer
                    showTimeoutMs = 0
                }
            })
        }

        FilledTonalIconButton(
            onClick = { },
//            enabled = enableClosePhotoPreview,
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close Photo Preview")
        }
    }

}

