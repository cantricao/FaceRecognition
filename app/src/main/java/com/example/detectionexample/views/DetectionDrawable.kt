package com.example.detectionexample.views

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import androidx.compose.runtime.State
import com.example.detectionexample.config.OverlayViewConfig
import com.example.detectionexample.models.TrackedRecognition

class DetectionDrawable(
    private val trackedObjectsState: State<List<TrackedRecognition>>,
    private val screenWith: Int,
    private val screenHeight: Int) : Drawable() {
    override fun draw(canvas: Canvas) {
        var labelString: String
        val frameToCanvasMatrix =
            OverlayViewConfig.getTransformationMatrix(screenWith.toFloat(), screenHeight.toFloat())

        trackedObjectsState.value.forEach {
            val trackedPos = RectF(it.location)
            frameToCanvasMatrix.mapRect(trackedPos)
            val cornerSize = trackedPos.width().coerceAtMost(trackedPos.height()) / 8.0f
            OverlayViewConfig.boxPaint.color = it.color
            OverlayViewConfig.rectTextBox.color = it.color
            canvas.drawRoundRect(trackedPos, cornerSize, cornerSize, OverlayViewConfig.boxPaint)
            labelString = if (it.title.isNotEmpty())
                String.format(
                    "%s %.2f",
                    it.title,
                    it.detectionConfidence
                )
            else ""
            val width = OverlayViewConfig.exteriorPaint.measureText(labelString)
            val textSize = OverlayViewConfig.exteriorPaint.textSize
            canvas.drawRect(
                trackedPos.left + cornerSize,
                trackedPos.top + textSize,
                trackedPos.left + cornerSize + width,
                trackedPos.top,
                OverlayViewConfig.rectTextBox
            )
            canvas.drawText(
                labelString,
                trackedPos.left + cornerSize,
                trackedPos.top + textSize,
                OverlayViewConfig.interiorPaint
            )
            it.landmark.forEach { pointF ->
                val point = floatArrayOf(pointF.x, pointF.y)
                frameToCanvasMatrix.mapPoints(point)
                canvas.drawCircle(
                    point[0],
                    point[1],
                    OverlayViewConfig.LANDMARK_RADIUS_SIZE,
                    OverlayViewConfig.rectTextBox
                )
            }
        }
    }

    override fun setAlpha(alpha: Int) {
        OverlayViewConfig.boxPaint.alpha = alpha
        OverlayViewConfig.exteriorPaint.alpha = alpha
        OverlayViewConfig.interiorPaint.alpha = alpha
        OverlayViewConfig.rectTextBox.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        OverlayViewConfig.boxPaint.colorFilter = colorFilter
        OverlayViewConfig.exteriorPaint.colorFilter = colorFilter
        OverlayViewConfig.interiorPaint.colorFilter = colorFilter
        OverlayViewConfig.rectTextBox.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java",
        ReplaceWith("PixelFormat.TRANSLUCENT", "android.graphics.PixelFormat")
    )
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}