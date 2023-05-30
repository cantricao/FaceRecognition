package com.example.detectionexample.glide

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.runtime.State
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.example.detectionexample.models.TrackedRecognition
import com.example.detectionexample.views.DetectionDrawable
import java.security.MessageDigest

class GlideOverlayBitmapTransformation(private val trackedObjectsState: State<List<TrackedRecognition>>) : BitmapTransformation() {
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
        val detectionDrawable = DetectionDrawable(trackedObjectsState, bitmap.width, bitmap.height)
        detectionDrawable.draw(canvas)
        return bitmap
    }

    override fun toString(): String {
        return "GlideCropBitmapTransformation(rectF= $trackedObjectsState)"
    }
    override fun equals(other: Any?): Boolean {
        return other is GlideCropBitmapTransformation && toString() == other.toString()
    }

    override fun hashCode(): Int {
        return ID.hashCode() + trackedObjectsState.hashCode()
    }

    companion object {
        private const val VERSION = 1
        private const val ID = "com.example.detectionexample.config.$VERSION"
    }
}