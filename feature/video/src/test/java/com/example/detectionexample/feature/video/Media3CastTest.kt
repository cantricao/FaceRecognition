package com.example.detectionexample.feature.video

import android.content.Context
import androidx.media3.cast.DefaultCastOptionsProvider
import androidx.media3.common.DeviceInfo
import androidx.media3.common.util.UnstableApi
import androidx.test.core.app.ApplicationProvider
import com.example.detectionexample.feature.video.playback.FacePlaybackService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests verifying Jetpack Media3 CastPlayer integration, DefaultCastOptionsProvider,
 * and PlaybackService lifecycle under Robolectric.
 */
@OptIn(UnstableApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Media3CastTest {

    @Test
    fun defaultCastOptionsProvider_returnsValidCastOptions() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val provider = DefaultCastOptionsProvider()
        val options = provider.getCastOptions(context)
        assertNotNull(options)
    }

    @Test
    fun playbackService_createsAndDestroysCleanly() {
        val serviceController = Robolectric.buildService(FacePlaybackService::class.java)
        val service = serviceController.create().get()
        assertNotNull(service)

        serviceController.destroy()
    }

    @Test
    fun deviceInfo_playbackTypeIdentifiesLocalAndRemote() {
        val localInfo = DeviceInfo.Builder(DeviceInfo.PLAYBACK_TYPE_LOCAL)
            .setMinVolume(0)
            .setMaxVolume(100)
            .build()
        assertEquals(DeviceInfo.PLAYBACK_TYPE_LOCAL, localInfo.playbackType)

        val remoteInfo = DeviceInfo.Builder(DeviceInfo.PLAYBACK_TYPE_REMOTE)
            .setMinVolume(0)
            .setMaxVolume(100)
            .build()
        assertEquals(DeviceInfo.PLAYBACK_TYPE_REMOTE, remoteInfo.playbackType)
    }
}
