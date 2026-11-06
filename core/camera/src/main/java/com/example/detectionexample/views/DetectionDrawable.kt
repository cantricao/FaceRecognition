package com.example.detectionexample.views

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.RectF
import android.graphics.drawable.Drawable
import androidx.compose.runtime.State
import com.example.detectionexample.config.OverlayViewConfig
import com.example.detectionexample.models.TrackedRecognition
import java.util.Locale

class DetectionDrawable(
    private val trackedObjectsState: State<List<TrackedRecognition>>,
    private val screenWith: Int,
    private val screenHeight: Int
) : Drawable() {

    // Reusable objects to eliminate per-frame allocations and GC pauses
    private val cachedTrackedPos = RectF()
    private val cachedPoint = FloatArray(2)

    override fun draw(canvas: Canvas) {
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        val drawWidth = when {
            bounds.width() > 0 -> bounds.width().toFloat()
            canvas.width > 0 -> canvas.width.toFloat()
            else -> screenWith.toFloat()
        }
        val drawHeight = when {
            bounds.height() > 0 -> bounds.height().toFloat()
            canvas.height > 0 -> canvas.height.toFloat()
            else -> screenHeight.toFloat()
        }
        val frameToCanvasMatrix =
            OverlayViewConfig.getTransformationMatrix(drawWidth, drawHeight)

        trackedObjectsState.value.forEach { recognition ->
            cachedTrackedPos.set(recognition.location)
            frameToCanvasMatrix.mapRect(cachedTrackedPos)
            val cornerSize = cachedTrackedPos.width().coerceAtMost(cachedTrackedPos.height()) / 8.0f
            OverlayViewConfig.boxPaint.color = recognition.color
            OverlayViewConfig.rectTextBox.color = recognition.color
            canvas.drawRoundRect(cachedTrackedPos, cornerSize, cornerSize, OverlayViewConfig.boxPaint)

            val labelString = if (recognition.title.isNotEmpty()) {
                String.format(
                    Locale.US,
                    "%s %.2f",
                    recognition.title,
                    recognition.detectionConfidence
                )
            } else {
                ""
            }

            val width = OverlayViewConfig.exteriorPaint.measureText(labelString)
            val textSize = OverlayViewConfig.exteriorPaint.textSize
            canvas.drawRect(
                cachedTrackedPos.left + cornerSize,
                cachedTrackedPos.top + textSize,
                cachedTrackedPos.left + cornerSize + width,
                cachedTrackedPos.top,
                OverlayViewConfig.rectTextBox
            )
            canvas.drawText(
                labelString,
                cachedTrackedPos.left + cornerSize,
                cachedTrackedPos.top + textSize,
                OverlayViewConfig.interiorPaint
            )

            recognition.landmark.forEach { pointF ->
                cachedPoint[0] = pointF.x
                cachedPoint[1] = pointF.y
                frameToCanvasMatrix.mapPoints(cachedPoint)
                canvas.drawCircle(
                    cachedPoint[0],
                    cachedPoint[1],
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

    @Deprecated(
        "Deprecated in Java",
        ReplaceWith("PixelFormat.TRANSLUCENT", "android.graphics.PixelFormat")
    )
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
