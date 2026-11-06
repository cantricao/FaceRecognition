package com.example.detectionexample.feature.video

import android.content.Context
import android.content.pm.PackageManager
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.test.core.app.ApplicationProvider
import com.example.detectionexample.feature.video.playback.CarAwarePlayer
import com.example.detectionexample.feature.video.playback.CarUtils
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Unit tests verifying Android for Cars (Android Automotive OS) parked app adaptations:
 * - AN-1: In-app back affordance decision logic depending on FEATURE_AUTOMOTIVE and FEATURE_DISPLAY_COMPATIBILITY
 * - DD-2, DD-3, MC-1: Driving distraction optimizations stripping COMMAND_PLAY_PAUSE and preventing playback while in motion
 */
@OptIn(UnstableApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CarParkedAppTest {

    @Test
    fun shouldShowInAppBack_falseOnStandardMobile() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val shadowPm = shadowOf(context.packageManager)
        shadowPm.setSystemFeature(PackageManager.FEATURE_AUTOMOTIVE, false)

        assertFalse(CarUtils.isAutomotive(context))
        assertFalse(CarUtils.shouldShowInAppBack(context))
    }

    @Test
    fun shouldShowInAppBack_trueOnAutomotiveWithoutDisplayCompatibility() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val shadowPm = shadowOf(context.packageManager)
        shadowPm.setSystemFeature(PackageManager.FEATURE_AUTOMOTIVE, true)
        shadowPm.setSystemFeature(CarUtils.FEATURE_DISPLAY_COMPATIBILITY, false)

        assertTrue(CarUtils.isAutomotive(context))
        assertFalse(CarUtils.isDisplayCompatibility(context))
        assertTrue(CarUtils.shouldShowInAppBack(context))
    }

    @Test
    fun shouldShowInAppBack_falseOnAutomotiveWithDisplayCompatibility() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val shadowPm = shadowOf(context.packageManager)
        shadowPm.setSystemFeature(PackageManager.FEATURE_AUTOMOTIVE, true)
        shadowPm.setSystemFeature(CarUtils.FEATURE_DISPLAY_COMPATIBILITY, true)

        assertTrue(CarUtils.isAutomotive(context))
        assertTrue(CarUtils.isDisplayCompatibility(context))
        assertFalse(CarUtils.shouldShowInAppBack(context))
    }

    @Test
    fun carAwarePlayer_stripsPlayPauseCommandUnderDistractionRestrictions() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val carAwarePlayer = CarAwarePlayer(context)

        // Initially without restrictions, COMMAND_PLAY_PAUSE should be available
        assertTrue(carAwarePlayer.availableCommands.contains(Player.COMMAND_PLAY_PAUSE))

        // When vehicle moves / distraction optimization required, COMMAND_PLAY_PAUSE is removed (MC-1)
        carAwarePlayer.setDistractionOptimizationRequired(true)
        assertFalse(carAwarePlayer.availableCommands.contains(Player.COMMAND_PLAY_PAUSE))

        // When parked again, COMMAND_PLAY_PAUSE is restored
        carAwarePlayer.setDistractionOptimizationRequired(false)
        assertTrue(carAwarePlayer.availableCommands.contains(Player.COMMAND_PLAY_PAUSE))

        carAwarePlayer.release()
    }
}
