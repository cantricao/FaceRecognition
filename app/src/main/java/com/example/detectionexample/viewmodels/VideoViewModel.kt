package com.example.detectionexample.viewmodels

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.media.MediaFormat
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.util.EventLogger
import com.example.detectionexample.MainApplication
import com.example.detectionexample.config.CameraConfig
import com.example.detectionexample.config.Util
import com.example.detectionexample.custom.BitmapEncoding
import com.example.detectionexample.custom.ExoplayerCustomRenderersFactory
import com.example.detectionexample.record.BitmapToVideoEncoder
import com.example.detectionexample.uistate.CaptureState
import com.example.detectionexample.uistate.MediaState
import com.example.detectionexample.uistate.RecordState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.nio.ByteBuffer
import javax.inject.Inject


@UnstableApi @HiltViewModel
class VideoViewModel @Inject constructor(application: Application) : AndroidViewModel(application) {
    val bitmapEncoding: BitmapEncoding = object :BitmapEncoding {
        override fun queueFrame(bitmap: Bitmap) {
            encoder.queueFrame(bitmap)
        }

    }
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
//                    val colorFormat = androidMediaFormat.getInteger(MediaFormat.KEY_COLOR_FORMAT)
//                    Log.d("Image Format", colorFormat.toString())
                    Util.yuvToRgb(bytes, bitmap, ImageFormat.YUV_420_888)
                    viewModelScope.launch {
                        _bitmap.emit(bitmap)
                    }
                } catch (e: Exception) {
                    Log.e("TAG", "onFrame: error: " + e.message)
                }
            }
        }
    }


    private val _recordState: MutableStateFlow<RecordState> = MutableStateFlow(RecordState.IDLE)
    val recordState: Flow<RecordState> = _recordState

    private val encoderCallback = object : BitmapToVideoEncoder.IBitmapToVideoEncoderCallback{
        override fun onEncodingComplete(outputFile: File?) {
            viewModelScope.launch {
                _recordState.emit(RecordState.FINALIZED)
                Toast.makeText(getApplication(), "File recording completed\n${outputFile?.absolutePath}\n__" , Toast.LENGTH_LONG).show()
            }
            Log.d("BitmapToVideoEncoder", "File recording completed\n${outputFile?.absolutePath}\n__")
        }

    }

    private val encoder = BitmapToVideoEncoder(encoderCallback)


    private val renderersFactory: ExoplayerCustomRenderersFactory =
        ExoplayerCustomRenderersFactory(getApplication()).setVideoFrameDataListener(videoFrameDataListener)
//    val exoPlayer = ExoPlayer.Builder(getApplication(), renderersFactory).build()
    val exoPlayer = ExoPlayer.Builder(
                getApplication(),
                DefaultRenderersFactory(getApplication())
                    .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            ).build()




    private val _videoState: MutableStateFlow<MediaState> = MutableStateFlow(MediaState.NOT_READY)
    private val _bitmap : MutableStateFlow<Bitmap> = MutableStateFlow(Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888))
    private val _captureUiState: MutableStateFlow<CaptureState> =
        MutableStateFlow(CaptureState.CaptureNotReady)

    val videoState: Flow<MediaState> = _videoState
    val bitmap: Flow<Bitmap> = _bitmap
    val captureUiState: Flow<CaptureState> = _captureUiState


    fun loadVideo(uri: Uri){
        viewModelScope.launch {
            val mediaItem = MediaItem.fromUri(uri)
            exoPlayer.apply {
                setMediaItem(mediaItem)
                prepare()
                playWhenReady = true
                addAnalyticsListener(EventLogger())
            }
            mediaProjectionManager =
                getApplication<MainApplication>().applicationContext.getSystemService(android.content.Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

            _videoState.emit(MediaState.READY)

        }
    }

    fun capturePhoto() {
        viewModelScope.launch {
            _captureUiState.emit(CaptureState.CaptureStarted)
        }
    }

    fun stopRecording() {
        encoder.stopEncoding()
    }

    private var mediaProjectionManager: MediaProjectionManager? = null

    fun startRecording() {
        val outputFile: File by lazy { Util.createFile("mp4") }
        val permissionIntent: Intent = mediaProjectionManager!!.createScreenCaptureIntent()

        encoder.startEncoding(_bitmap.value.width, _bitmap.value.height, outputFile)
        viewModelScope.launch {
            _recordState.emit(RecordState.RECORDING)
        }
        Log.i(CameraConfig.TAG, "Recording started")

    }
}