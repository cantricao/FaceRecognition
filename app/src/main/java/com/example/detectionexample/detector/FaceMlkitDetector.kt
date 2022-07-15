package com.example.detectionexample.detector

import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.RectF
import androidx.annotation.WorkerThread
import com.example.detectionexample.models.Recognition
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FaceMlkitDetector {
    // Real-time contour detection
    private val realTimeOpts by lazy {
        FaceDetectorOptions.Builder()
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .enableTracking()
            .build()
    }

    private val detector by lazy { FaceDetection.getClient(realTimeOpts) }

    private var faceResult: List<Face> = listOf()

    @WorkerThread
    fun detectInImage(image: Bitmap): Flow<List<Recognition>> {
        val inputImage = InputImage.fromBitmap(image, 0)
        val result = detector.process(inputImage)
        faceResult = Tasks.await(result)
        return getResult()
    }

    private fun getResult(): Flow<List<Recognition>> {
        return MutableStateFlow(
            faceResult.map { face ->
                val listOfPoints = mutableListOf<PointF>()
                val listOfLandmark = listOf(
                    FaceLandmark.MOUTH_BOTTOM,
                    FaceLandmark.MOUTH_RIGHT,
                    FaceLandmark.MOUTH_LEFT,
                    FaceLandmark.RIGHT_EYE,
                    FaceLandmark.LEFT_EYE,
                    FaceLandmark.RIGHT_EAR,
                    FaceLandmark.LEFT_EAR,
                    FaceLandmark.RIGHT_CHEEK,
                    FaceLandmark.LEFT_CHEEK,
                    FaceLandmark.NOSE_BASE,
                )
                listOfLandmark.forEach { faceLandmark ->
                    face.getLandmark(faceLandmark)?.let { listOfPoints.add(it.position) }
                }

                Recognition(
                    "${face.trackingId}",
                    "",
                    100.0f,
                    RectF(face.boundingBox),
                    listOfPoints
                )
            })
    }
}