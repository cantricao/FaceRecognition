package com.example.detectionexample.custom

import android.graphics.*
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import java.security.MessageDigest


class GlideCropBitmapTransformation(val rectF: RectF) : BitmapTransformation() {
    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update((ID + rectF).toByteArray(CHARSET))
    }

    override fun transform(
        pool: BitmapPool,
        toTransform: Bitmap,
        outWidth: Int,
        outHeight: Int
    ): Bitmap {
        val width = rectF.width().toInt()
        val height = rectF.height().toInt()

        val bitmap = pool[width, height, Bitmap.Config.ARGB_8888]
        bitmap.density = toTransform.density

        val resultMatrix = Matrix()
            .apply {
                postTranslate(-rectF.left, -rectF.top)
            }

        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
            .apply { color = Color.WHITE }
        val canvas = Canvas(bitmap)

        canvas.drawBitmap(toTransform, resultMatrix, paint)
//        canvas.drawRect(
//            RectF(0f, 0f, rectF.width(), rectF.height()),
//            paint
//        )
        return bitmap
    }

    override fun toString(): String {
        return "GlideCropBitmapTransformation(rectF= ${rectF.toShortString()})"
    }
    override fun equals(other: Any?): Boolean {
        return other is GlideCropBitmapTransformation && other.rectF == rectF
    }

    override fun hashCode(): Int {
        return ID.hashCode() + rectF.hashCode()
    }

    companion object {
        private const val VERSION = 1
        private const val ID = "com.example.detectionexample.config.$VERSION"
    }
}