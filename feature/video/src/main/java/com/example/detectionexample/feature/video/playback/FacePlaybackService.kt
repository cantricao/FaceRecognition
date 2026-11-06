package com.example.detectionexample.feature.video.playback

import androidx.media3.cast.CastPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Background playback service hosting Media3 ExoPlayer and CastPlayer.
 * Conforms to Media3 Cast architecture standards by passing CastPlayer (with local ExoPlayer configured)
 * directly to MediaSession.Builder for seamless Output Switcher and Cast routing.
 */
@OptIn(UnstableApi::class)
class FacePlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var exoPlayer: ExoPlayer? = null
    private var castPlayer: CastPlayer? = null

    override fun onCreate() {
        super.onCreate()
        val localExo = ExoPlayer.Builder(this).build()
        exoPlayer = localExo
        val carAwarePlayer = CarAwarePlayer(this, localExo)

        val player: Player = try {
            val cast = CastPlayer.Builder(this)
                .setLocalPlayer(carAwarePlayer)
                .build()
            castPlayer = cast
            cast
        } catch (_: Exception) {
            // Fallback to carAwarePlayer if CastContext / Google Play services is unavailable in environment
            carAwarePlayer
        }

        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        exoPlayer?.release()
        exoPlayer = null
        castPlayer?.release()
        castPlayer = null
        super.onDestroy()
    }
}
