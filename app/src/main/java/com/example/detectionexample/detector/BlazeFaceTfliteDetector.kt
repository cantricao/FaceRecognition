package com.example.detectionexample.detector

import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.RectF
import com.example.detectionexample.config.ModelConfig
import com.example.detectionexample.models.Recognition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.tensorflow.lite.support.model.Model
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.sqrt


class BlazeFaceTfliteDetector(context: Context, modelPath: String, device: Model.Device, objectDetectorListener: DetectorListener?):
    TfliteDetectorHelper(
        context,
        modelPath,
        ModelConfig.BLAZEFACE_LABEL_NAME,
        device,
        objectDetectorListener
    ) {

    override fun getResult(): Flow<List<Recognition>> {
        val detections: MutableList<Recognition> = ArrayList()
        for (it in 0 until NUM_BOXES) {
            var score: Float = outputScores.getFloatValue(it)
            score = score.coerceIn(-100.0f, 100.0f)
            score = 1.0f / (1.0f + exp(-score))
            if (score < MIN_SCORE_THRESH){
                continue
            }
            var xCenter: Float = outputBoxes.getFloatValue(it * NUM_COORDS)
            var yCenter: Float = outputBoxes.getFloatValue(it  * NUM_COORDS + 1)
            var w: Float = outputBoxes.getFloatValue(it  * NUM_COORDS + 2)
            var h: Float = outputBoxes.getFloatValue(it  * NUM_COORDS + 3)
            xCenter = xCenter / imageSizeX * anchors[it].w + anchors[it].x_center
            yCenter = yCenter / imageSizeY * anchors[it].h + anchors[it].y_center

            h = h / imageSizeY * anchors[it].h
            w = w / imageSizeX * anchors[it].w

            val ymin = (yCenter - h / 2f )
            val xmin = (xCenter - w / 2f )
            val ymax = (yCenter + h / 2f )
            val xmax = (xCenter + w / 2f )

            val listPoint = List(6) { PointF() }
            for (i in 0 until 6 ){
                listPoint[i].x = outputBoxes.getFloatValue(it * NUM_COORDS + 4 + 2 * i + 0)
                listPoint[i].x = listPoint[i].x / imageSizeX * anchors[it].w + anchors[it].x_center
                listPoint[i].y = outputBoxes.getFloatValue(it * NUM_COORDS + 4 + 2 * i + 1)
                listPoint[i].y = listPoint[i].y / imageSizeY * anchors[it].h + anchors[it].y_center
            }
            detections.add(Recognition("", "", score,
                RectF(xmin, ymin, xmax, ymax), listPoint))
        }
        if(detections.isEmpty()){
            return MutableStateFlow(detections)
        }
        val indexedScores: List<IndexedScore> = detections.mapIndexed {
             index, detect -> IndexedScore(index, detect.confidence)
        }.sortedBy { it.score }

        val result = weightedNonMaxSuppression(indexedScores, detections)

        return MutableStateFlow(
            result.map {
                val locationFitToBitmap = RectF(it.location)
                matrix.mapRect(locationFitToBitmap)
                val newLandmark = it.landmark.map { p ->
                    val newPointF = floatArrayOf(p.x, p.y)
                    matrix.mapPoints(newPointF)
                    PointF(newPointF[0], newPointF[1])
                }

                Recognition(
                    it.id,
                    it.title,
                    it.confidence,
                    locationFitToBitmap,
                    newLandmark
                )
            }
        )

    }

    private val outputBoxes by lazy {
        imageDetector?.getOutputTensor(0).let {
            TensorBuffer.createFixedSize(
                it?.shape(),
                it?.dataType()
            )
        }
    }

    private val outputScores by lazy {
        imageDetector?.getOutputTensor(1).let {
            TensorBuffer.createFixedSize(
                it?.shape(),
                it?.dataType()
            )
        }
    }

    override val outputMapBuffer by lazy {
        mapOf(
            0 to outputBoxes.buffer,
            1 to outputScores.buffer,
        )
    }

    private data class Anchor (
        val x_center: Float = 0f,
        val y_center: Float = 0f,
        val h: Float = 0f,
        val w: Float = 0f,
    )

    private data class IndexedScore(
        val index: Int,
        val score: Float
    )



    companion object {
        // Only return this many results.
        private const val NUM_BOXES = 896
        private const val NUM_COORDS = 16

        private const val MIN_SCORE_THRESH = 0.95f

        private val strides = intArrayOf(8, 16, 16, 16)

        private const val ASPECT_RATIOS_SIZE = 1

        private const val MIN_SCALE = 0.1484375f
        private const val MAX_SCALE = 0.75f

        private const val ANCHOR_OFFSET_X = 0.5f
        private const val ANCHOR_OFFSET_Y = 0.5f

        private const val MIN_SUPPRESSION_THRESHOLD = 0.3f
    }

    private val anchors: List<Anchor> by lazy { generateAnchors() }

    private val matrix by lazy {
        Matrix().also { it.setScale(imageWidth.toFloat(), imageHeight.toFloat()) }
    }

    private fun calculateScale(
        strideIndex: Int, numStrides: Int
    ): Float = MIN_SCALE +
                (MAX_SCALE - MIN_SCALE) * 1.0f * strideIndex / (numStrides - 1.0f)

    private fun generateAnchors(): List<Anchor> {
        val anchors: MutableList<Anchor> = ArrayList()
        var layerId = 0
        while (layerId < strides.size) {
            val anchorHeight: MutableList<Float> = ArrayList()
            val anchorWidth: MutableList<Float> = ArrayList()
            val aspectRatios: MutableList<Float> = ArrayList()
            val scales: MutableList<Float> = ArrayList()

            // For same strides, we merge the anchors in the same order.
            var lastSameStrideLayer = layerId
            while (lastSameStrideLayer < strides.size &&
                strides[lastSameStrideLayer] == strides[layerId]
            ) {
                val scale = calculateScale(
                    lastSameStrideLayer,
                    strides.size
                )
                for (aspect_ratio_id in 0 until ASPECT_RATIOS_SIZE) {
                    aspectRatios.add(1.0f)
                    scales.add(scale)
                }
                val scaleNext =
                    if (lastSameStrideLayer == strides.size - 1) 1.0f else calculateScale(
                        lastSameStrideLayer + 1,
                        strides.size
                    )
                scales.add(sqrt((scale * scaleNext)))
                aspectRatios.add(1.0f)
                lastSameStrideLayer++
            }
            for (i in aspectRatios.indices) {
                val ratioSqrt = sqrt(aspectRatios[i])
                anchorHeight.add(scales[i] / ratioSqrt)
                anchorWidth.add(scales[i] * ratioSqrt)
            }
            val stride: Int = strides[layerId]
            val featureMapHeight = ceil(1.0f * imageSizeY / stride).toInt()
            val featureMapWidth = ceil(1.0f * imageSizeX / stride).toInt()
            for (y in 0 until featureMapHeight) {
                for (x in 0 until featureMapWidth) {
                    for (anchor_id in anchorHeight.indices) {
                        val xCenter: Float = (x + ANCHOR_OFFSET_X) * 1.0f / featureMapWidth
                        val yCenter: Float = (y + ANCHOR_OFFSET_Y) * 1.0f / featureMapHeight
                        val newAnchor = Anchor(xCenter, yCenter, 1.0f, 1.0f)
                        anchors.add(newAnchor)
                    }
                }
            }
            layerId = lastSameStrideLayer
        }
        return anchors
    }

    private fun weightedNonMaxSuppression(
        indexed_scores: List<IndexedScore>,
        detections: List<Recognition>
    ): MutableList<Recognition> {
        var remainedIndexedScores: MutableList<IndexedScore> = indexed_scores as MutableList<IndexedScore>
        val remained: MutableList<IndexedScore> = ArrayList()
        val candidates: MutableList<IndexedScore> = ArrayList()
        val outputLocations: MutableList<Recognition> = ArrayList()
        while (remainedIndexedScores.isNotEmpty()) {
            val detection = detections[remainedIndexedScores[0].index]
            if (detection.confidence < - 1f){
                break
            }
            remained.clear()
            candidates.clear()
            val location = RectF(detection.location)
            // This includes the first box.
            for (indexed_score in remainedIndexedScores) {
                val restLocation = RectF(detections[indexed_score.index].location)
                val similarity = overlapSimilarity(restLocation, location)
                if (similarity > MIN_SUPPRESSION_THRESHOLD) {
                    candidates.add(indexed_score)
                } else {
                    remained.add(indexed_score)
                }
            }
            val weightedLocation = RectF(detection.location)
            if (candidates.isNotEmpty()) {
                var wXmin = 0.0f
                var wYmin = 0.0f
                var wXmax = 0.0f
                var wYmax = 0.0f
                var totalScore = 0.0f
                for ( c in candidates) {
                    totalScore += c.score
                    val box = detections[c.index].location
                    wXmin += box.left * c.score
                    wYmin += box.top * c.score
                    wXmax += box.right * c.score
                    wYmax += box.bottom * c.score
                }
                weightedLocation.left = wXmin / totalScore
                weightedLocation.top = wYmin / totalScore
                weightedLocation.right = wXmax / totalScore
                weightedLocation.bottom = wYmax / totalScore
            }
            remainedIndexedScores = remained
            outputLocations.add(
                Recognition(
                    detection.id,
                    detection.title,
                    detection.confidence,
                    weightedLocation,
                    detection.landmark
                )
            )
        }
        return outputLocations
    }

    // Computes an overlap similarity between two rectangles. Similarity measure is
    // defined by overlap_type parameter.
    private fun overlapSimilarity(rect1: RectF, rect2: RectF): Float {
        if (!RectF.intersects(rect1, rect2)) return 0.0f
        val intersection = RectF()
        intersection.setIntersect(rect1, rect2)
        val intersectionArea = intersection.height() * intersection.width()
        val normalization = rect1.height() * rect1.width() +rect2.height() * rect2.width() - intersectionArea
        return if (normalization > 0.0f) intersectionArea / normalization else 0.0f
    }
}