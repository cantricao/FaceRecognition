package com.example.detectionexample.detector

import android.content.Context
import android.graphics.RectF
import com.example.detectionexample.config.DeviceType
import com.example.detectionexample.config.ModelConfig
import com.example.detectionexample.models.Recognition
import com.google.ai.edge.litert.TensorBuffer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.min

/**
 * Wrapper class for tflite food/object classification model
 */
class MobilenetSSDTfliteDetector(
    context: Context,
    modelPath: String,
    device: DeviceType,
    objectDetectorListener: DetectorListener?
) : TfliteDetectorHelper(
    context,
    modelPath,
    ModelConfig.COCO_LABEL_PATH,
    device,
    objectDetectorListener
) {

    override val imageSizeX = 300
    override val imageSizeY = 300

    override fun getResult(outputs: List<TensorBuffer>): Flow<List<Recognition>> {
        val location = outputs[0].readFloat()
        val category = outputs[1].readFloat()
        val score = outputs[2].readFloat()
        val numberOfDetections = outputs[3].readFloat()

        val numDetections = min(numberOfDetections.getOrElse(0) { 0f }.toInt(), location.size / 4)

        return MutableStateFlow(
            (0 until numDetections).map {
            val catIndex = category.getOrElse(it) { 0f }.toInt()
            val labelText = if (catIndex in labels.indices) labels[catIndex] else "unknown"
            Recognition(
                "$it",
                labelText,
                score.getOrElse(it) { 0f } * 100,
                RectF(
                    (location[4 * it + 1] * imageWidth),
                    (location[4 * it + 0] * imageHeight),
                    (location[4 * it + 3] * imageWidth),
                    (location[4 * it + 2] * imageHeight)
                )
            )
        }.filter { it.confidence >= threshold }
        )
    }
}
