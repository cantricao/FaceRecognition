package com.example.detectionexample.detector

import android.graphics.Bitmap
import android.graphics.RectF
import com.example.detectionexample.models.Recognition
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FaceMlkitDetector() {
    // Real-time contour detection
    private val realTimeOpts by lazy {
        FaceDetectorOptions.Builder()
            .enableTracking()
            .build()
    }

    private val detector by lazy { FaceDetection.getClient(realTimeOpts) }

    private var faceResult: List<Face> = listOf()

    fun detectInImage(image: Bitmap): Flow<List<Recognition>> {
        val inputImage = InputImage.fromBitmap(image, 0)
        val result = detector.process(inputImage)
        faceResult = Tasks.await(result)
        return getResult()
    }

    private fun getResult(): Flow<List<Recognition>> {
        return MutableStateFlow(
            faceResult.map { face ->
            Recognition(
                "${face.trackingId}",
                "",
                100.0f,
                RectF(face.boundingBox)
            )
        })
    }
}