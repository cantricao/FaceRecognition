package com.example.detectionexample.worker

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.provider.MediaStore
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.example.detectionexample.R
import com.example.detectionexample.config.CameraConfig
import com.example.detectionexample.config.Util
import com.example.detectionexample.config.Util.createNotification
import com.example.detectionexample.models.TrackedRecognition
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class SaveImageWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val NOTIFICATION_ID = 1
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            NOTIFICATION_ID, createNotification(
                applicationContext, id,
                applicationContext.getString(R.string.notification_title_filtering_image)
            )
        )
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val uri = inputData.getString(CameraConfig.KEY_CAPTURE_IMAGE)!!.toUri()
            val json = inputData.getString(CameraConfig.KEY_CAPTURE_TRACKER_OBJECT)
            val trackedObjectsState = json?.let {
                Json.decodeFromString<List<TrackedRecognition>>(it)
            }

            val orientationColumn = arrayOf(MediaStore.Images.Media.ORIENTATION)
            val cur =
                applicationContext.contentResolver.query(uri, orientationColumn, null, null, null)
            val matrix = Matrix()
            if (cur != null && cur.moveToFirst()) {
                val orientation = cur.getInt(cur.getColumnIndexOrThrow(orientationColumn[0]))
                matrix.postRotate(orientation.toFloat())
            }
            cur?.close()

            val capturedBitmap = Util.getBitmap(applicationContext, uri)
            val rotatedBitmap = Bitmap.createBitmap(
                capturedBitmap,
                0,
                0,
                capturedBitmap.width,
                capturedBitmap.height,
                matrix,
                false
            )
            val canvas = Canvas(rotatedBitmap)

            val overlayBitmap = Util.drawBitmapOverlay(
                trackedObjectsState!!,
                rotatedBitmap.width,
                rotatedBitmap.height
            )
            canvas.drawBitmap(overlayBitmap, Matrix(), Paint(Paint.FILTER_BITMAP_FLAG))
            applicationContext.contentResolver.openOutputStream(uri).use { bitmapByteArray ->
                    rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, bitmapByteArray)
                }
            Result.success()
        } catch (t: Throwable) {
            Result.failure()
        } catch (ex: Exception) {
            Result.failure()
        }
    }
}