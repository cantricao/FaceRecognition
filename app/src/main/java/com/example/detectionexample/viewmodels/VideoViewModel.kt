package com.example.detectionexample.viewmodels

import android.app.Application
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.example.detectionexample.config.Util
import com.example.detectionexample.custom.ExoplayerCustomRenderersFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import javax.inject.Inject


@HiltViewModel
class VideoViewModel @Inject constructor(application: Application) : AndroidViewModel(application) {
    private val videoFrameDataListener = object : ExoplayerCustomRenderersFactory.VideoFrameDataListener {
        override fun onFrame(
            data: ByteBuffer?,
            androidMediaFormat: MediaFormat,
            playerFormat: Format
        ) {
            // Not in main thread
            if (data != null) {
                /*
                * Color formats of different decoders are different.
                * We have to apply different raw-data to Bitmap(argb) conversion systems according to color format.
                * Here we just show YUV to RGB conversion assuming data is YUV formatted.
                * Following conversion system might not give proper result for all videos.
                */
                try {
                    val width: Int = playerFormat.width
                    val height: Int = playerFormat.height

                    data.rewind()
                    val bytes = ByteArray(data.remaining())
                    data.get(bytes)
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

                    Util.yuvToRgb(bytes, bitmap, ImageFormat.YUV_420_888)
//                    imageBitmap = bitmap.asImageBitmap()
//                    viewModel.bitmapAnalyzer.analyze(bitmap, System.currentTimeMillis())

                } catch (e: Exception) {
                    Log.e("TAG", "onFrame: error: " + e.message)
                }
            }
        }
    }
    private val renderersFactory: ExoplayerCustomRenderersFactory =
        ExoplayerCustomRenderersFactory(getApplication()).setVideoFrameDataListener(videoFrameDataListener)
    val exoPlayer = ExoPlayer.Builder(getApplication(), renderersFactory).build()

    fun loadVideo(uri: Uri){
        viewModelScope.launch {
            val mediaItem = MediaItem.fromUri(uri)
            exoPlayer.apply {
                setMediaItem(mediaItem)
                prepare()
                playWhenReady = true
            }
        }
    }





}