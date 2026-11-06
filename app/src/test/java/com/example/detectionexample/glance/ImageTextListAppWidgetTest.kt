package com.example.detectionexample.glance

import android.content.Context
import android.graphics.Bitmap
import androidx.glance.appwidget.AppWidgetId
import androidx.glance.appwidget.SizeMode
import androidx.test.core.app.ApplicationProvider
import com.example.detectionexample.glance.data.RecognizedPersonGlanceRepository
import com.example.detectionexample.glance.data.RefreshWidgetAction
import com.example.detectionexample.glance.utils.ImageUtils
import com.example.detectionexample.glance.utils.calculateWidgetMemoryCapBytes
import com.example.detectionexample.glance.utils.computeIfAbsent
import com.example.detectionexample.glance.utils.decodeDownsampledWidgetBitmap
import com.example.detectionexample.glance.utils.removeIf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
class ImageTextListAppWidgetTest {

    @Test
    fun widget_hasSizeModeExact() {
        val widget = ImageTextListAppWidget()
        assertEquals(SizeMode.Exact, widget.sizeMode)
    }

    @Test
    fun receiver_hasCorrectGlanceAppWidget() {
        val receiver = ImageTextListAppWidgetReceiver()
        assertNotNull(receiver.glanceAppWidget)
        assertTrue(receiver.glanceAppWidget is ImageTextListAppWidget)
    }

    @Test
    fun repository_loadsItems_andEmitsViaFlow() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = RecognizedPersonGlanceRepository()

        val items = repo.load(context)
        assertTrue(items.isNotEmpty())

        val flowItems = repo.data().first()
        assertEquals(items.size, flowItems.size)
        assertEquals(items.first().key, flowItems.first().key)
    }

    @Test
    fun repository_companion_managesGlanceIdInstances() {
        val glanceId1 = AppWidgetId(101)
        val glanceId2 = AppWidgetId(102)

        val repo1 = RecognizedPersonGlanceRepository.getRepo(glanceId1)
        val repo1Same = RecognizedPersonGlanceRepository.getRepo(glanceId1)
        val repo2 = RecognizedPersonGlanceRepository.getRepo(glanceId2)

        assertEquals(repo1, repo1Same)
        assertNotNull(repo2)

        RecognizedPersonGlanceRepository.cleanUp(glanceId1)
    }

    @Test
    fun imageUtils_scalesDownBitmapSafely() {
        val bitmap = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888)
        val scaled = ImageUtils.scaleDownBitmapSafely(bitmap, 50)

        assertEquals(50, scaled.width)
        assertEquals(25, scaled.height)
    }

    @Test
    fun collectionsKtx_operatesCorrectly() {
        val map = mutableMapOf<String, Int>()
        val value = map.computeIfAbsent("key1") { 42 }
        assertEquals(42, value)
        assertEquals(42, map["key1"])

        val list = mutableListOf(1, 2, 3, 4, 5)
        val removed = list.removeIf { it % 2 == 0 }
        assertTrue(removed)
        assertEquals(listOf(1, 3, 5), list)
    }

    @Test
    fun widgetBitmapUtils_calculatesMemoryCap() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val capBytes = calculateWidgetMemoryCapBytes(context)
        assertTrue(capBytes > 0L)
    }

    @Test
    fun widgetBitmapUtils_decodesDownsampledBitmapFromFile() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tempFile = File(context.cacheDir, "test_hero.png")
        val sampleBitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888)
        FileOutputStream(tempFile).use { out ->
            sampleBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        val decoded = decodeDownsampledWidgetBitmap(
            context = context,
            sourceFile = tempFile,
            distinctImageCount = 1,
            maxWidthDp = 68,
            maxHeightDp = 68,
            config = Bitmap.Config.RGB_565,
        )

        assertNotNull(decoded)
        assertTrue(decoded!!.width <= 400)
        tempFile.delete()
    }

    @Test
    fun widgetBitmapUtils_decodesDownsampledBitmapFromBitmap() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sampleBitmap = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888)

        val downsampled = decodeDownsampledWidgetBitmap(
            context = context,
            bitmap = sampleBitmap,
            distinctImageCount = 2,
            maxWidthDp = 68,
            maxHeightDp = 68,
            config = Bitmap.Config.RGB_565,
        )

        assertNotNull(downsampled)
        assertTrue(downsampled.width <= 300)
    }

    @Test
    fun refreshWidgetAction_hasPublicZeroArgConstructor() {
        val action = RefreshWidgetAction::class.java.getDeclaredConstructor().newInstance()
        assertNotNull(action)
    }

    @Test
    fun widget_compositionErrorFallbackRunsWithoutCrash() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val widget = ImageTextListAppWidget()
        val glanceId = AppWidgetId(101)

        widget.onCompositionError(
            context = context,
            glanceId = glanceId,
            appWidgetId = 101,
            throwable = RuntimeException("Test error"),
        )
    }
}
