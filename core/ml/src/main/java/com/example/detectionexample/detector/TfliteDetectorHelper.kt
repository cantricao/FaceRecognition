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
import com.example.detectionexample.config.DeviceType
import com.example.detectionexample.core.common.util.traceSection
import com.example.detectionexample.litert.LiteRtEnvironment
import com.example.detectionexample.models.Recognition
import com.google.ai.edge.litert.Accelerator
import com.google.ai.edge.litert.CompiledModel
import com.google.ai.edge.litert.LiteRtException
import com.google.ai.edge.litert.TensorBuffer
import com.google.ai.edge.litert.support.common.ops.NormalizeOp
import com.google.ai.edge.litert.support.image.ImageProcessor
import com.google.ai.edge.litert.support.image.TensorImage
import com.google.ai.edge.litert.support.image.ops.ResizeOp
import com.google.ai.edge.litert.support.tensorbuffer.TensorBufferFloat
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

abstract class TfliteDetectorHelper(
    val context: Context,
    private var modelName: String,
    private val labelPath: String,
    private var currentDelegate: DeviceType,
    private val objectDetectorListener: DetectorListener?,
    var threshold: Float = 50f,
    private var numThreads: Int = 2,
) : DetectorDataSource {

    protected val executor = Executors.newSingleThreadExecutor()
    protected var imageDetector: CompiledModel? = null
    protected val inputs = mutableListOf<TensorBuffer>()
    protected val outputs = mutableListOf<TensorBuffer>()

    abstract val imageSizeX: Int
    abstract val imageSizeY: Int

    protected var imageWidth: Int = 0
    protected var imageHeight: Int = 0

    protected val labels: MutableList<String> by lazy {
        try {
            context.assets.open(labelPath).bufferedReader().use { it.readLines().toMutableList() }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading labels from $labelPath: ${e.message}")
            mutableListOf()
        }
    }

    private val imageProcessor by lazy {
        ImageProcessor.Builder()
            .add(ResizeOp(imageSizeY, imageSizeX, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
            .add(NormalizeOp(127.5f, 127.5f))
            .build()
    }

    companion object {
        const val TAG = "Detector"
    }

    init {
        executor.submit { setupObjectDetector() }
    }

    override fun clearObjectDetector() {
        Log.d(TAG, "clearObjectDetector invoke")
        try {
            executor.submit { closeInternal() }.get(1, TimeUnit.SECONDS)
        } catch (e: Exception) {
            Log.e(TAG, "Exception thrown while trying to close Detector: $e")
        } finally {
            executor.shutdown()
        }
    }

    private fun closeInternal() {
        try {
            (inputs + outputs).forEach { it.close() }
            inputs.clear()
            outputs.clear()
            imageDetector?.close()
            imageDetector = null
        } catch (e: Exception) {
            Log.e(TAG, "Exception closing detector model: $e")
        }
    }

    private fun setupObjectDetector() {
        closeInternal()
        val options = when (currentDelegate) {
            DeviceType.CPU -> CompiledModel.Options(Accelerator.CPU).apply {
                cpuOptions = CompiledModel.CpuOptions(numThreads = numThreads)
            }
            DeviceType.GPU, DeviceType.NNAPI -> {
                CompiledModel.Options(Accelerator.GPU, Accelerator.CPU)
            }
            DeviceType.NPU -> {
                CompiledModel.Options(Accelerator.NPU, Accelerator.GPU, Accelerator.CPU).apply {
                    try {
                        qualcommOptions = CompiledModel.QualcommOptions(
                            htpPerformanceMode = CompiledModel.QualcommOptions.HtpPerformanceMode.BURST,
                            optimizationLevel = CompiledModel.QualcommOptions.OptimizationLevel.HTP_OPTIMIZE_FOR_INFERENCE
                        )
                    } catch (e: Throwable) {
                        Log.w(TAG, "Qualcomm NPU options not applicable: ${e.message}")
                    }
                }
            }
        }

        try {
            Log.d(TAG, "Loading model: $modelName with delegate $currentDelegate")
            imageDetector = try {
                CompiledModel.create(
                    context.assets,
                    modelName,
                    options,
                    LiteRtEnvironment.environment
                )
            } catch (e: LiteRtException) {
                if (currentDelegate != DeviceType.CPU) {
                    Log.w(TAG, "GPU compilation failed, falling back to CPU: ${e.message}")
                    traceSection("LiteRT:GPUFallbackToCPU") {
                        CompiledModel.create(
                            context.assets,
                            modelName,
                            CompiledModel.Options(Accelerator.CPU).apply {
                                cpuOptions = CompiledModel.CpuOptions(numThreads = numThreads)
                            },
                            LiteRtEnvironment.environment
                        )
                    }
                } else {
                    throw e
                }
            }

            imageDetector?.let { model ->
                inputs += model.createInputBuffers()
                outputs += model.createOutputBuffers()
                // Warm-up inference
                inputs[0].writeFloat(FloatArray(imageSizeX * imageSizeY * 3))
                model.run(inputs, outputs)
            }
        } catch (e: Exception) {
            closeInternal()
            objectDetectorListener?.onError(
                "Object detector failed to initialize: ${e.message}"
            )
            Log.e(TAG, "LiteRT failed to load model with error: " + e.message, e)
        }
    }

    override fun detectInImage(image: Bitmap) {
        executor.submit {
            if (imageDetector == null) {
                setupObjectDetector()
            }
            val detector = imageDetector ?: return@submit

            var inferenceTime = SystemClock.uptimeMillis()

            imageWidth = image.width
            imageHeight = image.height

            val tensorImage = traceSection("LiteRT:Detector:Preprocess") {
                val processed = imageProcessor.process(TensorImage.fromBitmap(image))
                val pixels = (processed.tensorBuffer as? TensorBufferFloat)?.floatArray
                    ?: processed.tensorBuffer.floatArray
                inputs[0].writeFloat(pixels)
                processed
            }

            traceSection("LiteRT:Detector:Inference") {
                detector.run(inputs, outputs)
            }
            inferenceTime = SystemClock.uptimeMillis() - inferenceTime

            val results = traceSection("LiteRT:Detector:DecodeResults") {
                getResult(outputs)
            }

            objectDetectorListener?.onResults(
                results,
                inferenceTime,
                tensorImage.height,
                tensorImage.width
            )
        }
    }

    abstract fun getResult(outputs: List<TensorBuffer>): Flow<List<Recognition>>

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
