package com.example.detectionexample.compose

import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.ui.LegacyPlayerControlView
import com.example.detectionexample.R
import com.example.detectionexample.config.Util
import com.example.detectionexample.uistate.MediaState
import com.example.detectionexample.viewmodels.AnalysisViewModel
import com.example.detectionexample.viewmodels.VideoViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


@Composable
fun VideoPlayer(viewModel: AnalysisViewModel = viewModel(),
                videoViewModel: VideoViewModel = viewModel()
) {
    if (viewModel.isStaringCamera) return

    val lifecycleOwner = LocalLifecycleOwner.current

    LocalContext.current

    val sampleVideoUri: Uri = Uri.parse("asset:///face-demographics-walking-and-pause.mp4")
//    val sampleVideoUri: Uri = Uri.parse("https://github.com/intel-iot-devkit/sample-videos/raw/master/face-demographics-walking.mp4")

    produceState(initialValue = MediaState.READY) {
        videoViewModel.videoState.collect { videoState ->
            when (videoState) {
                MediaState.NOT_READY -> videoViewModel.loadVideo(sampleVideoUri)
                MediaState.READY -> Unit
                MediaState.PREVIEW_STOPPED -> Unit
            }
            value = videoState

        }
    }

    var mRecordingEnabled by remember { mutableStateOf(false)}

    DisposableEffect(lifecycleOwner){
        // Create an observer that triggers our remembered callbacks
        // for sending analytics events
        val observer = LifecycleEventObserver { _, event ->
            when(event){
                Lifecycle.Event.ON_RESUME -> videoViewModel.exoPlayer.play()
                Lifecycle.Event.ON_PAUSE -> {
                    videoViewModel.exoPlayer.pause()
                    videoViewModel.stopEncoding()
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


    val trackedObjectsState by viewModel.trackedObserver.collectAsState(initial = listOf())


    viewModel.needUpdateTrackerImageSourceInfo = true

    val imageBitmap by produceState(
        initialValue = Bitmap.createBitmap(1 , 1, Bitmap.Config.ARGB_8888), videoViewModel.bitmap
    ) {
        videoViewModel.bitmap.collect { bitmap ->
            viewModel.bitmapAnalyzer.analyze(bitmap, System.currentTimeMillis())
            value = bitmap
        }
    }



    val imageOverlay by produceState(
        initialValue = ImageBitmap(1,1), trackedObjectsState
    ) {
        value = Util.drawBitmapOverlay(
                trackedObjectsState,
                imageBitmap.width,
                imageBitmap.height
            ).asImageBitmap()
    }

    val outputFile: File by lazy { Util.createFile("mp4") }



    Box (Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Image(
            bitmap = imageBitmap.asImageBitmap(),
            contentDescription = "Overlay View",
            modifier = Modifier.fillMaxSize()
        )

        Image(
            bitmap = imageOverlay,
            contentDescription = "Overlay View",
            modifier = Modifier.fillMaxSize()
        )

        Column(modifier = Modifier.align(Alignment.BottomCenter),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally) {
            FilledTonalIconButton(
                modifier = Modifier
                    .size(92.dp + 32.dp)
                    .padding(PaddingValues(32.dp)),
                onClick = {
                    if (mRecordingEnabled) {
                        videoViewModel.stopEncoding()
                    } else {
                        videoViewModel.startEncoding(imageBitmap.width, imageBitmap.height, outputFile)
                    }
                    mRecordingEnabled = !mRecordingEnabled
                },
//                enabled = enableCameraShutter
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_baseline_camera_24),
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

        FilledTonalIconButton(onClick = { },
//            enabled = enableClosePhotoPreview,
            modifier = Modifier.align(Alignment.TopEnd)) {
            Icon(Icons.Default.Close, contentDescription = "Close Photo Preview")
        }
    }

}

