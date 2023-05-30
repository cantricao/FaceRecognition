package com.example.detectionexample.config

import android.content.Context
import android.graphics.Matrix
import android.util.TypedValue
import androidx.compose.ui.graphics.NativePaint
import androidx.compose.ui.graphics.Paint

object OverlayViewConfig  {

    val boxPaint = android.graphics.Paint().apply {
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 3.0f
        strokeCap = android.graphics.Paint.Cap.ROUND
        strokeJoin = android.graphics.Paint.Join.ROUND
        strokeMiter = 100f
    }

    val rectTextBox = android.graphics.Paint(boxPaint).apply {
        style = android.graphics.Paint.Style.FILL
        alpha = 160
    }


    private var frameWidth = 0
    private var frameHeight = 0
    private var sensorOrientation = 0f

    //BorderedText
    val interiorPaint: NativePaint = Paint().asFrameworkPaint().apply {
        color = android.graphics.Color.WHITE
        style = android.graphics.Paint.Style.FILL
        isAntiAlias = false
        alpha = 255
    }

    val exteriorPaint: NativePaint = Paint().asFrameworkPaint().apply {
        color = android.graphics.Color.BLACK
        style = android.graphics.Paint.Style.FILL_AND_STROKE
        strokeWidth = textSize / 8
        isAntiAlias = false
        alpha = 255
    }

    fun setFrameConfiguration(
        width: Int, height: Int, sensorOrientation: Int
    ) {
        this.sensorOrientation = sensorOrientation.toFloat()
        if(sensorOrientation != 90){
            frameWidth = width
            frameHeight = height
        }
        else {
            frameWidth = height
            frameHeight = width
        }
    }

    private const val TEXT_SIZE_DIP = 18f

    const val LANDMARK_RADIUS_SIZE = 3f

    fun setContextToFixTextSize(context: Context){
        val textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, context.resources.displayMetrics
        )
        interiorPaint.textSize = textSize
        exteriorPaint.textSize = textSize
        exteriorPaint.strokeWidth = textSize / 8
    }


    /**
     * Returns a transformation matrix from one reference frame into another. Handles cropping (if
     * maintaining aspect ratio is desired) and rotation.
     * @return The transformation fulfilling the desired requirements.
     */
    fun getTransformationMatrix(
        screenWidth: Float, screenHeight:Float
    ): Matrix {
        val matrix = Matrix()
        val scaleFactorX = screenWidth * 1f / frameWidth.toFloat()
        val scaleFactorY = screenHeight * 1f / frameHeight.toFloat()
        matrix.setScale(scaleFactorX, scaleFactorY )
        return matrix
    }
}