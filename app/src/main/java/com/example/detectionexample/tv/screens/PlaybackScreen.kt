package com.example.detectionexample.tv.screens

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.material3.buttons.PlayPauseButton
import androidx.media3.ui.compose.material3.buttons.SeekBackButton
import androidx.media3.ui.compose.material3.buttons.SeekForwardButton
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.example.detectionexample.tv.model.TvVideo
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Modern Media3 video playback screen for Android TV using Jetpack Compose.
 *
 * Implements:
 * - Direct rendering via [PlayerSurface] (no legacy AndroidView wrappers).
 * - Translucent overlay with Media3 Compose Material 3 transport buttons.
 * - Symmetrical 2D remote D-pad seeking and play/pause control.
 * - Auto-hiding HUD with coroutine lifecycle management.
 *
 * Fully replaces legacy Leanback VideoSupportFragment and PlaybackTransportRowGlue.
 */
@OptIn(UnstableApi::class)
@Composable
fun PlaybackScreen(
    video: TvVideo,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build()
    }
    var showControls by remember { mutableStateOf(true) }
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    var autoHideJob by remember { mutableStateOf<Job?>(null) }

    fun scheduleAutoHide() {
        autoHideJob?.cancel()
        autoHideJob = coroutineScope.launch {
            delay(5000L)
            showControls = false
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    onBack()
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            autoHideJob?.cancel()
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(video) {
        focusRequester.requestFocus()
        scheduleAutoHide()

        val mediaItem = MediaItem.fromUri(video.videoUrl)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter, Key.MediaPlayPause, Key.Spacebar -> {
                            if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                            showControls = true
                            scheduleAutoHide()
                            true
                        }
                        Key.DirectionLeft, Key.MediaRewind -> {
                            val newPos = (exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L)
                            exoPlayer.seekTo(newPos)
                            showControls = true
                            scheduleAutoHide()
                            true
                        }
                        Key.DirectionRight, Key.MediaFastForward -> {
                            val duration = exoPlayer.duration.coerceAtLeast(0L)
                            val newPos = (exoPlayer.currentPosition + 10_000L).coerceAtMost(duration)
                            exoPlayer.seekTo(newPos)
                            showControls = true
                            scheduleAutoHide()
                            true
                        }
                        Key.DirectionUp, Key.DirectionDown -> {
                            showControls = !showControls
                            if (showControls) scheduleAutoHide() else autoHideJob?.cancel()
                            true
                        }
                        Key.Back, Key.Escape -> {
                            if (showControls) {
                                showControls = false
                                autoHideJob?.cancel()
                                true
                            } else {
                                onBack()
                                true
                            }
                        }
                        else -> false
                    }
                } else false
            },
        contentAlignment = Alignment.Center
    ) {
        // Compose Media3 rendering surface
        PlayerSurface(
            player = exoPlayer,
            modifier = Modifier.fillMaxSize()
        )

        // Overlay with 10-foot TV transport controls and metadata HUD
        if (showControls) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x80000000))
                    .padding(horizontal = 48.dp, vertical = 32.dp)
            ) {
                // Top metadata HUD
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = video.title,
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White
                            )
                            Text(
                                text = video.category,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFAAAAAA)
                            )
                        }
                        Button(
                            onClick = onBack
                        ) {
                            Text("Exit Player")
                        }
                    }
                }

                // Bottom Transport Controls (Media3 UI Compose Material 3)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(32.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SeekBackButton(player = exoPlayer)
                        PlayPauseButton(player = exoPlayer)
                        SeekForwardButton(player = exoPlayer)
                    }
                }
            }
        }
    }
}
