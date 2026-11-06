package com.example.detectionexample.glance.utils

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Size
import android.view.WindowManager
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

object ImageUtils {

    private const val DEFAULT_MAX_BYTES_PER_IMAGE = 1_500_000
    private const val BYTES_PER_PIXEL = 4

    fun decodeSampledBitmapFromResource(
        res: Resources,
        resId: Int,
        requestedWidth: Int,
        requestedHeight: Int,
        preferredConfig: Bitmap.Config = Bitmap.Config.RGB_565,
        maxBytesPerImage: Int = DEFAULT_MAX_BYTES_PER_IMAGE,
    ): Bitmap {
        val bytesPerPixel = when (preferredConfig) {
            Bitmap.Config.ALPHA_8 -> 1
            Bitmap.Config.RGB_565 -> 2
            Bitmap.Config.RGBA_F16 -> 8
            else -> 4
        }

        fun calculateInSampleSize(
            originalSize: Size,
            requestedSize: Size,
            effectiveBytesPerPixel: Int = bytesPerPixel,
        ): Int {
            val (height: Int, width: Int) = originalSize.height to originalSize.width
            val reqHeight = max(1, requestedSize.height)
            val reqWidth = max(1, requestedSize.width)
            var inSampleSize = 1
            while (
                (
                    height / (inSampleSize * 2) >= reqHeight &&
                    width / (inSampleSize * 2) >= reqWidth
                ) ||
                (height / inSampleSize > reqHeight * 2) ||
                (width / inSampleSize > reqWidth * 2)
            ) {
                inSampleSize *= 2
            }
            while (
                (
                    max(1, width / inSampleSize).toLong() *
                    max(1, height / inSampleSize).toLong() *
                    effectiveBytesPerPixel
                ) > maxBytesPerImage
            ) {
                inSampleSize *= 2
            }
            return inSampleSize
        }

        return BitmapFactory.Options().run {
            inJustDecodeBounds = true
            inPreferredConfig = preferredConfig
            inScaled = false
            BitmapFactory.decodeResource(res, resId, this)
            val originalWidth: Int = outWidth
            val originalHeight: Int = outHeight
            val effectiveBytesPerPixel = when (outConfig ?: preferredConfig) {
                Bitmap.Config.ALPHA_8 -> 1
                Bitmap.Config.RGB_565 -> 2
                Bitmap.Config.RGBA_F16 -> 8
                else -> 4
            }

            inSampleSize = calculateInSampleSize(
                originalSize = Size(originalWidth, originalHeight),
                requestedSize = Size(requestedWidth, requestedHeight),
                effectiveBytesPerPixel = effectiveBytesPerPixel,
            )
            inJustDecodeBounds = false
            BitmapFactory.decodeResource(res, resId, this)
        }
    }

    fun Context.getMaxWidgetMemoryAllowedSizeInBytes(): Int {
        val windowManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getSystemService(WindowManager::class.java)
        } else {
            null
        }
        val size = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && windowManager != null) {
            val bounds = windowManager.currentWindowMetrics.bounds
            Size(bounds.width(), bounds.height())
        } else {
            resources.displayMetrics.run { Size(widthPixels, heightPixels) }
        }
        return 6 * size.width * size.height
    }

    fun getMaxPossibleImageSize(aspectRatio: Double, memoryLimitBytes: Int, maxImages: Int): Size {
        val limit = (memoryLimitBytes / 4) / max(1, maxImages)
        val maxSizeAllowedPerPixel: Int = limit / BYTES_PER_PIXEL
        val side = sqrt(maxSizeAllowedPerPixel.toDouble()).toInt()
        val width = if (aspectRatio > 1) side else max(1, (side * aspectRatio).roundToInt())
        val height = if (aspectRatio > 1) max(1, (side / aspectRatio).roundToInt()) else side
        return Size(width, height)
    }

    fun scaleDownBitmapSafely(source: Bitmap, maxDim: Int): Bitmap {
        if (source.width <= maxDim && source.height <= maxDim) return source
        val ratio = source.width.toFloat() / source.height.toFloat()
        val width: Int
        val height: Int
        if (ratio > 1) {
            width = maxDim
            height = max(1, (maxDim / ratio).roundToInt())
        } else {
            height = maxDim
            width = max(1, (maxDim * ratio).roundToInt())
        }
        return Bitmap.createScaledBitmap(source, width, height, true)
    }
}
