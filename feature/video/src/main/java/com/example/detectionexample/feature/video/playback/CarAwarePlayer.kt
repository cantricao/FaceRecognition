package com.example.detectionexample.feature.video.playback

import android.car.Car
import android.car.drivingstate.CarUxRestrictions
import android.car.drivingstate.CarUxRestrictionsManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.media3.common.ForwardingSimpleBasePlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * Distraction-aware Media3 Player for Android for Cars (Android Automotive OS).
 * Wraps ExoPlayer and integrates with CarUxRestrictionsManager:
 * - Automatically pauses video playback when driving restrictions activate (DD-2, DD-3)
 * - Removes COMMAND_PLAY_PAUSE from available commands when driving so external MediaSession controls cannot resume video (MC-1)
 * - Rejects playback resumption attempts while distraction optimization is active
 * - Resumes playback when restrictions clear if previously paused by driving restrictions
 */
@UnstableApi
class CarAwarePlayer(
    context: Context,
    player: Player = ExoPlayer.Builder(context).build()
) : ForwardingSimpleBasePlayer(player) {

    private var shouldPreventPlay = false
    private var pausedByUxRestrictions = false
    private lateinit var carUxRestrictionsManager: CarUxRestrictionsManager

    init {
        if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)) {
            try {
                val car = Car.createCar(context)
                carUxRestrictionsManager =
                    car.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as CarUxRestrictionsManager
                shouldPreventPlay =
                    carUxRestrictionsManager.currentCarUxRestrictions.isRequiresDistractionOptimization
                invalidateState()

                carUxRestrictionsManager.registerListener { restrictions: CarUxRestrictions ->
                    setDistractionOptimizationRequired(restrictions.isRequiresDistractionOptimization)
                }
            } catch (e: Exception) {
                // If Car service is unavailable, continue with standard playback
                shouldPreventPlay = false
            }
        }
    }

    /**
     * Updates distraction optimization state (DD-2, DD-3, MC-1).
     * Internal for unit testing driving restriction transitions.
     */
    internal fun setDistractionOptimizationRequired(required: Boolean) {
        shouldPreventPlay = required
        if (!shouldPreventPlay && pausedByUxRestrictions) {
            pausedByUxRestrictions = false
            handleSetPlayWhenReady(true)
            invalidateState()
        } else if (shouldPreventPlay && isPlaying) {
            pausedByUxRestrictions = true
            handleSetPlayWhenReady(false)
            invalidateState()
        } else {
            invalidateState()
        }
    }

    override fun getState(): State {
        val state = super.getState()
        return state.buildUpon()
            .setAvailableCommands(
                state.availableCommands.buildUpon()
                    .removeIf(COMMAND_PLAY_PAUSE, shouldPreventPlay)
                    .build()
            )
            .build()
    }

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        if (playWhenReady && shouldPreventPlay) {
            return Futures.immediateVoidFuture()
        }
        return super.handleSetPlayWhenReady(playWhenReady)
    }

    override fun handleRelease(): ListenableFuture<*> {
        if (::carUxRestrictionsManager.isInitialized) {
            try {
                carUxRestrictionsManager.unregisterListener()
            } catch (_: Exception) {
            }
        }
        return super.handleRelease()
    }
}
