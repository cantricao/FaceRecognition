package com.example.detectionexample.compose

import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.media.MediaFormat
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.LegacyPlayerControlView
import com.example.detectionexample.config.OverlayViewConfig
import com.example.detectionexample.config.Util
import com.example.detectionexample.custom.ExoplayerCustomRenderersFactory
import com.example.detectionexample.viewmodels.AnalysisViewModel
import com.example.detectionexample.viewmodels.VideoViewModel
import java.nio.ByteBuffer


@Composable
fun VideoPlayer(viewModel: AnalysisViewModel = viewModel(),
                videoViewModel: VideoViewModel = viewModel()
) {
    if (viewModel.isStaringCamera) return

    val lifecycleOwner = LocalLifecycleOwner.current

    val trackedObjectsState by viewModel.trackedObserver.collectAsState(initial = listOf())


    var imageBitmap by remember { mutableStateOf(ImageBitmap(1,1)) }

//    val videoFrameDataListener = object : ExoplayerCustomRenderersFactory.VideoFrameDataListener {
//        override fun onFrame(
//            data: ByteBuffer?,
//            androidMediaFormat: MediaFormat,
//            playerFormat: Format
//        ) {
//            // Not in main thread
//            if (data != null) {
//                /*
//                * Color formats of different decoders are different.
//                * We have to apply different raw-data to Bitmap(argb) conversion systems according to color format.
//                * Here we just show YUV to RGB conversion assuming data is YUV formatted.
//                * Following conversion system might not give proper result for all videos.
//                */
//                try {
//                    val width: Int = playerFormat.width
//                    val height: Int = playerFormat.height
//
//                    data.rewind()
//                    val bytes = ByteArray(data.remaining())
//                    data.get(bytes)
//                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
//
//                    Util.yuvToRgb(bytes, bitmap, ImageFormat.YUV_420_888)
//
////                    val finalBitmap = Bitmap.createScaledBitmap(bitmap, 640, 480, true)
//                    imageBitmap = bitmap.asImageBitmap()
//                    viewModel.bitmapAnalyzer.analyze(bitmap, System.currentTimeMillis())
//
//                } catch (e: Exception) {
//                    Log.e("TAG", "onFrame: error: " + e.message)
//                }
//            }
//        }
//    }
//
//
//    val exoPlayer =  remember {
//        val renderersFactory: ExoplayerCustomRenderersFactory =
//            ExoplayerCustomRenderersFactory(context).setVideoFrameDataListener(videoFrameDataListener)
//        ExoPlayer.Builder(context, renderersFactory).build()
//    }
    
    DisposableEffect(lifecycleOwner){
        // Create an observer that triggers our remembered callbacks
        // for sending analytics events
        val observer = LifecycleEventObserver { _, event ->
            when(event){
                Lifecycle.Event.ON_RESUME -> videoViewModel.exoPlayer.play()
                Lifecycle.Event.ON_PAUSE -> videoViewModel.exoPlayer.pause()
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

//    LaunchedEffect(viewModel.isProcessingFrame) {
//        val mediaItem = MediaItem.fromUri(viewModel.sampleVideoUri)
//        exoPlayer.apply {
//            setMediaItem(mediaItem)
//            prepare()
//            playWhenReady = viewModel.isProcessingFrame
//        }
//    }

    val imageOverlay by produceState(
        initialValue = ImageBitmap(1,1), trackedObjectsState
    ) {
        value = Util.drawBitmapOverlay(
                trackedObjectsState,
                imageBitmap.width,
                imageBitmap.height
            ).asImageBitmap()
    }


    Box (Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Image(bitmap = imageBitmap,
            contentDescription = "Video View",
            modifier = Modifier.fillMaxSize()
        )
        Image(bitmap = imageOverlay,
            contentDescription = "Overlay View",
            modifier = Modifier.fillMaxSize()
        )

        AndroidView(factory = { mContext -> LegacyPlayerControlView(mContext).apply {
            player = videoViewModel.exoPlayer
            showTimeoutMs = 0
        }
                              },
            modifier = Modifier.align(alignment = Alignment.BottomCenter))
    }

}

