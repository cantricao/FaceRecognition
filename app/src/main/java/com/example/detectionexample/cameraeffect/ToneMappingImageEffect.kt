package com.example.detectionexample.cameraeffect

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.camera.core.CameraEffect
import androidx.camera.core.ImageProcessor
import androidx.camera.core.ImageProcessor.Response
import androidx.camera.core.ImageProxy
import androidx.camera.core.imagecapture.RgbaImageProxy
import androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor
import androidx.compose.runtime.State
import com.example.detectionexample.models.TrackedRecognition
import com.example.detectionexample.views.DetectionDrawable

/**
 * A image effect that applies the same tone mapping as [ToneMappingSurfaceProcessor].
 */
@SuppressLint("RestrictedApi")
class ToneMappingImageEffect(trackedObjectsState: State<List<TrackedRecognition>>) : CameraEffect(
    IMAGE_CAPTURE, mainThreadExecutor(), ToneMappingImageProcessor(trackedObjectsState), {}
) {

    fun isInvoked(): Boolean {
        return (imageProcessor as ToneMappingImageProcessor).processoed
    }

    private class ToneMappingImageProcessor(private val trackedObjectsState: State<List<TrackedRecognition>>) : ImageProcessor {

        var processoed = false

        override fun process(request: ImageProcessor.Request): Response {
            processoed = true
            val inputImage = request.inputImage as RgbaImageProxy
            val bitmap = inputImage.createBitmap()
            applyToneMapping(bitmap)
            val outputImage = createOutputImage(bitmap, inputImage)
            inputImage.close()
            return Response { outputImage }
        }

        /**
         * Creates output image
         */
        private fun createOutputImage(newBitmap: Bitmap, imageIn: ImageProxy): ImageProxy {
            return RgbaImageProxy(
                newBitmap,
                imageIn.cropRect,
                imageIn.imageInfo.rotationDegrees,
                imageIn.imageInfo.sensorToBufferTransformMatrix,
                imageIn.imageInfo.timestamp
            )
        }

        /**
         * Applies the same color matrix as [ToneMappingSurfaceProcessor].
         */
        private fun applyToneMapping(bitmap: Bitmap) {
            val canvas = Canvas(bitmap)
            val detectionDrawable = DetectionDrawable(trackedObjectsState, bitmap.width, bitmap.height)
            detectionDrawable.draw(canvas)
        }
    }
}