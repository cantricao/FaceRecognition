package com.example.detectionexample.viewmodels

import android.app.Application
import android.graphics.Bitmap
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.util.EventLogger
import com.example.detectionexample.MainApplication
import com.example.detectionexample.config.CameraConfig
import com.example.detectionexample.uistate.CaptureState
import com.example.detectionexample.uistate.MediaState
import com.example.detectionexample.uistate.RecordState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


@UnstableApi @HiltViewModel
class VideoViewModel @Inject constructor(application: Application) : AndroidViewModel(application) {

    private val _recordState: MutableStateFlow<RecordState> = MutableStateFlow(RecordState.IDLE)
    val recordState: StateFlow<RecordState> = _recordState
    val exoPlayer = ExoPlayer.Builder(
                getApplication(),
                DefaultRenderersFactory(getApplication())
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
        viewModelScope.launch {
            _recordState.emit(RecordState.FINALIZED)
        }
    }

    private var mediaProjectionManager: MediaProjectionManager? = null

    fun startRecording() {
        viewModelScope.launch {
            _recordState.emit(RecordState.RECORDING)
        }
        Log.i(CameraConfig.TAG, "Recording started")

    }
}