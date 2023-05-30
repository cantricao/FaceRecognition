/*
 * Copyright 2022 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.detectionexample.detector

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import com.example.detectionexample.models.Recognition
import kotlinx.coroutines.flow.Flow
import org.tensorflow.lite.DataType
import org.tensorflow.lite.InterpreterApi
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.model.Model
import java.io.IOException
import java.nio.ByteBuffer

abstract class TfliteDetectorHelper(
    val context: Context,
    private var modelName: String,
    private val labelPath: String,
    private var currentDelegate: Model.Device,
    private val objectDetectorListener: DetectorListener?,
    var threshold: Float = 50f,
    private var numThreads: Int = 2,

    ): DetectorDataSource {



    // For this example this needs to be a var so it can be reset on changes. If the ObjectDetector
    // will not change, a lazy val would be preferable.
    protected var imageDetector: Model? = null
    protected var imageSizeX: Int = 0
    protected var imageSizeY: Int = 0

    private lateinit var inputDataType: DataType

    protected var imageWidth: Int = 0
    protected var imageHeight: Int = 0

    protected lateinit var labels: MutableList<String>

    companion object {
        const val TAG = "Detector"
    }

    init {
        setupObjectDetector()
    }

    override fun clearObjectDetector() {
        Log.d(TAG, "close invoke")
        try {
            imageDetector?.close()
        } catch (e: IOException) {
            Log.e(
                TAG,
                "Exception thrown while trying to close Detector: $e"
            )
        }
        imageDetector = null
    }

    // Initialize the object detector using current settings on the
    // thread that is using it. CPU and NNAPI delegates can be used with detectors
    // that are created on the main thread and used on a background thread, but
    // the GPU delegate needs to be used on the thread that initialized the detector
    private fun setupObjectDetector() {
        // Create the base options for the detector using specifies max results and score threshold
//        val optionsBuilder =
//            ObjectDetector.ObjectDetectorOptions.builder()
//                .setScoreThreshold(threshold)
//                .setMaxResults(maxResults)

        // Set general detection options, including number of used threads
        val baseOptionsBuilder = Model.Options.Builder().setNumThreads(numThreads)
            .setTfLiteRuntime(InterpreterApi.Options.TfLiteRuntime.PREFER_SYSTEM_OVER_APPLICATION)

        // Use the specified hardware for running the model. Default to CPU
        when (currentDelegate) {
            Model.Device.CPU -> {
                // Default
            }
            Model.Device.GPU -> {
                if (CompatibilityList().isDelegateSupportedOnThisDevice) {
                    baseOptionsBuilder.setDevice(currentDelegate)
                } else {
                    objectDetectorListener?.onError("GPU is not supported on this device")
                }
            }
            Model.Device.NNAPI -> {
                baseOptionsBuilder.setDevice(currentDelegate)
            }
        }

        try {
            Log.d(TAG, modelName)
            imageDetector =
                Model.createModel(context, modelName, baseOptionsBuilder.build())

            imageSizeX = imageDetector!!.getInputTensor(0).shape()[1]
            imageSizeY = imageDetector!!.getInputTensor(0).shape()[2]
            inputDataType = imageDetector!!.getInputTensor(0).dataType()
            labels = FileUtil.loadLabels(context, labelPath)
        } catch (e: IllegalStateException) {
            objectDetectorListener?.onError(
                "Object detector failed to initialize. See error logs for details"
            )
            Log.e("Test", "TFLite failed to load model with error: " + e.message)
        }
    }

    override fun detectInImage(image: Bitmap) {
        if (imageDetector == null) {
            setupObjectDetector()
        }

        // Inference time is the difference between the system time at the start and finish of the
        // process
        var inferenceTime = SystemClock.uptimeMillis()

        imageWidth = image.width
        imageHeight = image.height

        // Create preprocessor for the image.
        // See https://www.tensorflow.org/lite/inference_with_metadata/
        //            lite_support#imageprocessor_architecture
        val imageProcessor =
            ImageProcessor.Builder()
                .add(ResizeOp(imageSizeY, imageSizeX, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
                .add(NormalizeOp(127.5f, 127.5f))
                .build()

        // Preprocess the image and convert it into a TensorImage for detection.
        val tensorImage = imageProcessor.process(TensorImage.fromBitmap(image))

//        val results = imageDetector?.detect(tensorImage)

        outputMapBuffer.forEach { (_, buffer) -> buffer.rewind() }
        imageDetector?.run(arrayOf(tensorImage.buffer), outputMapBuffer)
        outputMapBuffer.forEach { (_, buffer) -> buffer.flip() }
        inferenceTime = SystemClock.uptimeMillis() - inferenceTime

        objectDetectorListener?.onResults(
            getResult(),
            inferenceTime,
            tensorImage.height,
            tensorImage.width)
    }

    abstract fun getResult(): Flow<List<Recognition>>
    abstract val outputMapBuffer: Map<Int, ByteBuffer>

    interface DetectorListener {
        fun onError(error: String)
        fun onResults(
            results: Flow<List<Recognition>>,
            inferenceTime: Long,
            imageHeight: Int,
            imageWidth: Int
        )
    }
}
