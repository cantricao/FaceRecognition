package com.example.detectionexample.config

import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.work.WorkManager
import com.example.detectionexample.R
import com.example.detectionexample.models.TrackedRecognition
import jp.co.cyberagent.android.gpuimage.GPUImageNativeLibrary
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object Util {

    fun getCropBitmapByCPU(cropRectF: RectF, srcBitmap: Bitmap): Bitmap {
        val resultBitmap = Bitmap.createBitmap(
            cropRectF.width().toInt(),
            cropRectF.height().toInt(),
            Bitmap.Config.ARGB_8888
        )
        val resultCanvas = Canvas(resultBitmap)
        val resultMatrix = Matrix()
            .apply {
                postTranslate(-cropRectF.left, -cropRectF.top)
            }
        val processBitmapShader =
            BitmapShader(
                srcBitmap,
                Shader.TileMode.CLAMP,
                Shader.TileMode.CLAMP
            )
        processBitmapShader.setLocalMatrix(resultMatrix)

        val paintProcess = Paint(Paint.FILTER_BITMAP_FLAG)
            .apply {
                color = Color.WHITE
                shader = processBitmapShader
            }
//        resultCanvas.drawBitmap(srcBitmap, resultMatrix, paintProcess)
        resultCanvas.drawRect(
            RectF(0f, 0f, cropRectF.width(), cropRectF.height()),
            paintProcess
        )

        return resultBitmap
    }

    fun drawBitmapOverlay(
        trackedObjectsState: List<TrackedRecognition>,
        screenWith: Int,
        screenHeight: Int
    ): Bitmap {
        val bitmapOverlay = Bitmap.createBitmap(screenWith, screenHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmapOverlay)
        var labelString: String
        val frameToCanvasMatrix =
            OverlayViewConfig.getTransformationMatrix(screenWith.toFloat(), screenHeight.toFloat())
        trackedObjectsState.forEach {
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
        return bitmapOverlay
    }

    fun getBitmap(
        context: Context,
        uri: Uri
    ): Bitmap {
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(
                ImageDecoder.createSource(
                    context.contentResolver,
                    uri
                )
            ) { decoder, _, _ -> decoder.isMutableRequired = true }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                .copy(Bitmap.Config.ARGB_8888, true)
        }
        return bitmap
    }

    fun createNotification(
        context: Context,
        workRequestId: UUID,
        notificationTitle: String
    ): Notification {
        val channelId = context.getString(R.string.notification_channel_id)
        val cancelText = context.getString(R.string.cancel_processing)
        val name = context.getString(R.string.channel_name)
        // This PendingIntent can be used to cancel the Worker.
        val cancelIntent = WorkManager.getInstance(context).createCancelPendingIntent(workRequestId)

        val builder = NotificationCompat.Builder(context, channelId)
            .setContentTitle(notificationTitle)
            .setTicker(notificationTitle)
            .setSmallIcon(R.drawable.baseline_gradient)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_delete, cancelText, cancelIntent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(context, channelId, name).also {
                builder.setChannelId(it.id)
            }
        }
        return builder.build()
    }

    /**
     * Create the required notification channel for O+ devices.
     */
    @TargetApi(Build.VERSION_CODES.O)
    fun createNotificationChannel(
        context: Context,
        channelId: String,
        name: String,
        notificationImportance: Int = NotificationManager.IMPORTANCE_HIGH
    ): NotificationChannel {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return NotificationChannel(
            channelId, name, notificationImportance
        ).also { channel ->
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun yuvToRgb(yuvBuffer: ByteArray, output: Bitmap, imageFormat: Int){
        val outputBuffer = IntArray(output.width * output.height)
        when(imageFormat){
            ImageFormat.NV21 ->
                GPUImageNativeLibrary.YUVtoRBGA(yuvBuffer, output.width, output.height, outputBuffer)
            ImageFormat.YUV_420_888 ->
                GPUImageNativeLibrary.YUVtoRBGA(yuvBuffer, output.width, output.height, outputBuffer)
            else -> throw UnsupportedOperationException()
        }

        output.setPixels(outputBuffer, 0, output.width, 0, 0, output.width, output.height)
    }

    fun createFile(extension: String): File {
        val sdf = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.US)
        return File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "VID_${sdf.format(Date())}.$extension")
    }

}