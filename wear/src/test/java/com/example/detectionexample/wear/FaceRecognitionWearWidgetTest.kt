package com.example.detectionexample.wear

import android.annotation.SuppressLint
import android.content.Context
import androidx.glance.wear.core.ContainerInfo
import androidx.glance.wear.core.WearWidgetParams
import androidx.glance.wear.core.WidgetInstanceId
import androidx.test.core.app.ApplicationProvider
import com.example.detectionexample.wear.widget.FaceRecognitionWearWidget
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit test verifying that FaceRecognitionWearWidget serializes Remote Compose
 * rcDocument payloads correctly across SMALL and LARGE container types.
 */
@SuppressLint("RestrictedApi")
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FaceRecognitionWearWidgetTest {

    @Test
    fun provideWidgetData_serializesRemoteComposeDocument() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val widget = FaceRecognitionWearWidget()

        for (containerType in listOf(
            ContainerInfo.CONTAINER_TYPE_SMALL,
            ContainerInfo.CONTAINER_TYPE_LARGE,
        )) {
            val params = WearWidgetParams(
                instanceId = WidgetInstanceId(namespace = "test", id = 1),
                containerType = containerType,
                widthDp = 220f,
                heightDp = 120f,
                horizontalPaddingDp = 8f,
                verticalPaddingDp = 8f,
                cornerRadiusDp = 16f,
            )

            val data = widget.provideWidgetData(context, params)
            val rawContent = data.captureRawContent(context, params)

            assertNotNull(rawContent.rcDocument)
            assertTrue(rawContent.rcDocument.isNotEmpty())
        }
    }
}
