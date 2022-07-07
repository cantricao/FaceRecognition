package com.example.detectionexample.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.detectionexample.view.BitmapOverlayVideoProcessor
import com.example.detectionexample.view.VideoProcessingGLSurfaceView
import com.example.detectionexample.viewmodels.DetectionViewModel
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.util.Util


@Composable
fun VideoPlayer(viewModel: DetectionViewModel = viewModel()) {
    if (viewModel.isStaringCamera) return
    val context = LocalContext.current
    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
    val exoPlayer = ExoPlayer.Builder(context).build()
        .apply {
            videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
        }
    val playerView = StyledPlayerView(context).apply {
        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
        player = exoPlayer
    }
    viewModel.needUpdateTrackerImageSourceInfo = true
    val videoProcessingGLSurfaceView = VideoProcessingGLSurfaceView(
        context, false, BitmapOverlayVideoProcessor(context, viewModel.videoAnalyzer, viewModel.analysisExecutor))
    LaunchedEffect(exoPlayer, viewModel.isProcessingFrame) {
        val mediaItem = MediaItem.fromUri(viewModel.sampleVideoUri)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = viewModel.isProcessingFrame

        videoProcessingGLSurfaceView.setPlayer(exoPlayer)
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    if (Util.SDK_INT > 23) {
                        playerView.onResume()
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (Util.SDK_INT <= 23) {
                        playerView.onResume()
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    if (Util.SDK_INT <= 23) {
                        playerView.onPause()
                        playerView.player = null
                        exoPlayer.release()
                    }
                }
                Lifecycle.Event.ON_STOP -> {
                    if (Util.SDK_INT > 23) {
                        playerView.onPause()
                        playerView.player = null
                        videoProcessingGLSurfaceView.setPlayer(null)
                        exoPlayer.release()
                    }
                }
                else -> { /* other stuff */
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    AndroidView(factory = { playerView })
    AndroidView(factory = { videoProcessingGLSurfaceView })
}