/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.detectionexample.detector

import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.RectF
import android.os.SystemClock
import android.util.Log
import com.example.detectionexample.config.ScopedExecutor
import com.example.detectionexample.models.Recognition
import com.google.android.gms.tasks.TaskExecutors
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Locale

/** Face Detector Demo.  */
class FaceMLDetector(private val objectDetectorListener: TfliteDetectorHelper.DetectorListener?): DetectorDataSource{

  private var detector: FaceDetector? = null
  private var faceResult: List<Face> = listOf()
  private val executor: ScopedExecutor

  init {
    setupObjectDetector()
    executor = ScopedExecutor(TaskExecutors.MAIN_THREAD)
  }

  private fun setupObjectDetector() {
    val options = FaceDetectorOptions.Builder()
      .setClassificationMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
      .enableTracking()
      .build()

    detector = FaceDetection.getClient(options)

    Log.v(MANUAL_TESTING_LOG, "Face detector options: $options")
  }

  override fun clearObjectDetector() {
    detector?.close()
    detector = null
    executor.shutdown()
  }

  override fun detectInImage(image: Bitmap) {
    if (detector == null){
      setupObjectDetector()
    }

    var inferenceTime = SystemClock.uptimeMillis()
    val inputImage = InputImage.fromBitmap(image, 0)
    detector!!.process(inputImage).addOnSuccessListener(executor) { results: List<Face> ->
      for (face in results) {
        logExtrasForTesting(face)
      }
      faceResult = results
      inferenceTime = SystemClock.uptimeMillis() - inferenceTime
      objectDetectorListener?.onResults(
        getResult(),
        inferenceTime,
        inputImage.height,
        inputImage.width)
    }.addOnFailureListener(executor) { e: Exception ->
      faceResult = listOf()
      Log.e(TAG, "Face detection failed $e")
      objectDetectorListener?.onResults(
        getResult(),
        inferenceTime,
        inputImage.height,
        inputImage.width)

    }
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



  companion object {
    private const val TAG = "FaceMLDetector"
    private const val MANUAL_TESTING_LOG = "FaceMLDetector"
    private fun logExtrasForTesting(face: Face?) {
      if (face != null) {
        Log.v(
          MANUAL_TESTING_LOG,
          "face bounding box: " + face.boundingBox.flattenToString()
        )
        Log.v(
          MANUAL_TESTING_LOG,
          "face Euler Angle X: " + face.headEulerAngleX
        )
        Log.v(
          MANUAL_TESTING_LOG,
          "face Euler Angle Y: " + face.headEulerAngleY
        )
        Log.v(
          MANUAL_TESTING_LOG,
          "face Euler Angle Z: " + face.headEulerAngleZ
        )
        // All landmarks
        val landMarkTypes = intArrayOf(
          FaceLandmark.MOUTH_BOTTOM,
          FaceLandmark.MOUTH_RIGHT,
          FaceLandmark.MOUTH_LEFT,
          FaceLandmark.RIGHT_EYE,
          FaceLandmark.LEFT_EYE,
          FaceLandmark.RIGHT_EAR,
          FaceLandmark.LEFT_EAR,
          FaceLandmark.RIGHT_CHEEK,
          FaceLandmark.LEFT_CHEEK,
          FaceLandmark.NOSE_BASE
        )
        val landMarkTypesStrings = arrayOf(
          "MOUTH_BOTTOM",
          "MOUTH_RIGHT",
          "MOUTH_LEFT",
          "RIGHT_EYE",
          "LEFT_EYE",
          "RIGHT_EAR",
          "LEFT_EAR",
          "RIGHT_CHEEK",
          "LEFT_CHEEK",
          "NOSE_BASE"
        )
        for (i in landMarkTypes.indices) {
          val landmark = face.getLandmark(landMarkTypes[i])
          if (landmark == null) {
            Log.v(
              MANUAL_TESTING_LOG,
              "No landmark of type: " + landMarkTypesStrings[i] + " has been detected"
            )
          } else {
            val landmarkPosition = landmark.position
            val landmarkPositionStr =
              String.format(Locale.US, "x: %f , y: %f", landmarkPosition.x, landmarkPosition.y)
            Log.v(
              MANUAL_TESTING_LOG,
              "Position for face landmark: " +
                landMarkTypesStrings[i] +
                " is :" +
                landmarkPositionStr
            )
          }
        }
        Log.v(
          MANUAL_TESTING_LOG,
          "face left eye open probability: " + face.leftEyeOpenProbability
        )
        Log.v(
          MANUAL_TESTING_LOG,
          "face right eye open probability: " + face.rightEyeOpenProbability
        )
        Log.v(
          MANUAL_TESTING_LOG,
          "face smiling probability: " + face.smilingProbability
        )
        Log.v(
          MANUAL_TESTING_LOG,
          "face tracking id: " + face.trackingId
        )
      }
    }
  }
}
