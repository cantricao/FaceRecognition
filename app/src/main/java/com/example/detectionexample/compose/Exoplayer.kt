package com.example.detectionexample.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.ui.LegacyPlayerControlView
import androidx.media3.ui.PlayerView
import com.example.detectionexample.config.OverlayViewConfig
import com.example.detectionexample.customexoplayer.VideoView
import com.example.detectionexample.viewmodels.DetectionViewModel


@Composable
fun VideoPlayer(viewModel: DetectionViewModel = viewModel()) {
    if (viewModel.isStaringCamera) return

    val context = LocalContext.current


    val activity =  VideoView(context, viewModel)

    val exoPlayer =  remember { activity.createPlayer() }

    viewModel.needUpdateTrackerImageSourceInfo = true

    LaunchedEffect(viewModel.isProcessingFrame) {
        val mediaItem = MediaItem.fromUri(viewModel.sampleVideoUri)
        exoPlayer.apply {
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = viewModel.isProcessingFrame
        }
    }

    Box (Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AndroidView(factory = { mContext -> activity.createVideoFrameView(mContext) },
            modifier = Modifier.fillMaxSize().onSizeChanged {
                OverlayViewConfig.modifier = Modifier.size(it.width.dp, it.height.dp)
            })

        AndroidView(factory = { mContext -> PlayerView(mContext).apply {
            player = exoPlayer
        } },

            modifier = Modifier.align(alignment = Alignment.BottomCenter))
    }

}

