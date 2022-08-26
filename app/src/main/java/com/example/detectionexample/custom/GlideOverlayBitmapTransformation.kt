package com.example.detectionexample.custom

import android.graphics.*
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.example.detectionexample.config.Util
import com.example.detectionexample.models.TrackedRecognition
import java.security.MessageDigest

class GlideOverlayBitmapTransformation(private val trackedObjectsState: List<TrackedRecognition>) : BitmapTransformation() {
    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update((ID + trackedObjectsState.toString()).toByteArray(CHARSET))
    }

    override fun transform(
        pool: BitmapPool,
        toTransform: Bitmap,
        outWidth: Int,
        outHeight: Int
    ): Bitmap {

        val bitmap = pool[outWidth, outHeight, Bitmap.Config.ARGB_8888]
        bitmap.density = toTransform.density

        val canvas = Canvas(bitmap)

        val overlayBitmap = Util.drawBitmapOverlay(
            trackedObjectsState,
            outWidth,
            outHeight)

        canvas.drawBitmap(overlayBitmap, Matrix(), null)
//        canvas.drawRect(
//            RectF(0f, 0f, rectF.width(), rectF.height()),
//            paint
//        )
        return bitmap
    }

    override fun toString(): String {
        return "GlideCropBitmapTransformation(rectF= $trackedObjectsState)"
    }
    override fun equals(other: Any?): Boolean {
        return other is GlideCropBitmapTransformation && trackedObjectsState == trackedObjectsState
    }

    override fun hashCode(): Int {
        return ID.hashCode() + trackedObjectsState.hashCode()
    }

    companion object {
        private const val VERSION = 1
        private const val ID = "com.example.detectionexample.config.$VERSION"
    }
}