package com.example.detectionexample.detector

import android.content.Context
import android.graphics.RectF
import com.example.detectionexample.config.ModelConfig
import com.example.detectionexample.models.Recognition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.tensorflow.lite.support.model.Model
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import kotlin.math.min

/**
 * Wrapper class for tflite food classification model
 */
class MobilenetSSDTfliteDetector(context: Context, modelPath: String, device: Model.Device):
    TfliteDetector(context, modelPath, ModelConfig.COCO_LABEL_PATH, device) {

    // location: array of shape [Batchsize, NUM_DETECTIONS,4]
    // contains the location of detected boxes
    private val location by lazy {
        imageDetector.getOutputTensor(0).let {
            TensorBuffer.createFixedSize(
                it.shape(),
                it.dataType()
            )
        }
    }

    // category: array of shape [Batchsize, NUM_DETECTIONS]
    // contains the classes of detected boxes
    private val category by lazy {
        imageDetector.getOutputTensor(1).let {
            TensorBuffer.createFixedSize(
                it.shape(),
                it.dataType()
            )
        }
    }

    // outputScores: array of shape [Batchsize, NUM_DETECTIONS]
    // contains the scores of detected boxes
    private var score = imageDetector.getOutputTensor(2).let {
        TensorBuffer.createFixedSize(
            it.shape(),
            it.dataType()
        )
    }

    // numDetections: array of shape [Batchsize]
    // contains the number of detected boxes
    private val numberOfDetections  by lazy {
        imageDetector.getOutputTensor(3).let {
            TensorBuffer.createFixedSize(
                it.shape(),
                it.dataType()
            )
        }
    }

    override val outputMapBuffer by lazy {
        mapOf(
            0 to location.buffer,
            1 to category.buffer,
            2 to score.buffer,
            3 to numberOfDetections.buffer
        )
    }
    private val numDetections by lazy { min(numberOfDetections.getIntValue(0), location.shape[1]) }


    override fun getResult(): Flow<List<Recognition>> {
        //get detection result
        return MutableStateFlow((0 until numDetections).map {
                Recognition(
                    "$it",
                    labels[category.getIntValue(it)],
                    score.getFloatValue(it) * 100,
                    RectF(
                        (location.getFloatValue(4 * it + 1) * imageWidth),
                        (location.getFloatValue(4 * it + 0) * imageHeight),
                        (location.getFloatValue(4 * it + 3) * imageWidth),
                        (location.getFloatValue(4 * it + 2) * imageHeight)
                    )
                )
            }.filter { it.confidence >= threshold})
    }
}