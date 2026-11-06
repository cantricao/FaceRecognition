package com.example.detectionexample.extractor

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.detectionexample.config.DeviceType
import com.example.detectionexample.core.common.util.traceSection
import com.example.detectionexample.litert.LiteRtEnvironment
import com.google.ai.edge.litert.Accelerator
import com.google.ai.edge.litert.CompiledModel
import com.google.ai.edge.litert.LiteRtException
import com.google.ai.edge.litert.TensorBuffer
import com.google.ai.edge.litert.support.common.ops.NormalizeOp
import com.google.ai.edge.litert.support.image.ImageProcessor
import com.google.ai.edge.litert.support.image.TensorImage
import com.google.ai.edge.litert.support.image.ops.ResizeOp
import com.google.ai.edge.litert.support.tensorbuffer.TensorBufferFloat
import java.io.Closeable
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

open class TfliteExtractor(
    val context: Context,
    modelPath: String,
    device: DeviceType
) : Closeable {

    private val numberOfThread = if (device == DeviceType.CPU) 4 else 1
    private val imageSizeX = 112
    private val imageSizeY = 112

    private val executor = Executors.newSingleThreadExecutor()
    private var model: CompiledModel? = null
    private val inputs = mutableListOf<TensorBuffer>()
    private val outputs = mutableListOf<TensorBuffer>()

    private val imageProcessor: ImageProcessor by lazy {
        ImageProcessor.Builder()
            .add(ResizeOp(imageSizeY, imageSizeX, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
            .add(NormalizeOp(127.5f, 127.5f))
            .build()
    }

    init {
        executor.submit {
            try {
                val options = when (device) {
                    DeviceType.CPU -> CompiledModel.Options(Accelerator.CPU).apply {
                        cpuOptions = CompiledModel.CpuOptions(numThreads = numberOfThread)
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
                model = try {
                    CompiledModel.create(
                        context.assets,
                        modelPath,
                        options,
                        LiteRtEnvironment.environment
                    )
                } catch (e: LiteRtException) {
                    if (device != DeviceType.CPU) {
                        Log.w(TAG, "GPU compilation failed, falling back to CPU: ${e.message}")
                        traceSection("LiteRT:Extractor:GPUFallbackToCPU") {
                            CompiledModel.create(
                                context.assets,
                                modelPath,
                                CompiledModel.Options(Accelerator.CPU).apply {
                                    cpuOptions = CompiledModel.CpuOptions(numThreads = 4)
                                },
                                LiteRtEnvironment.environment
                            )
                        }
                    } else {
                        throw e
                    }
                }
                model?.let { cm ->
                    inputs += cm.createInputBuffers()
                    outputs += cm.createOutputBuffers()
                    // Warm-up inference
                    inputs[0].writeFloat(FloatArray(imageSizeX * imageSizeY * 3))
                    cm.run(inputs, outputs)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize LiteRT Extractor model: ${e.message}", e)
                closeInternal()
                throw e
            }
        }.get()
    }

    fun extractImage(image: Bitmap): FloatArray {
        return executor.submit(
            Callable {
            val cm = model ?: error("Model is not initialized or closed")
            val pixels = traceSection("LiteRT:Extractor:Preprocess") {
                val tensorImage = imageProcessor.process(TensorImage.fromBitmap(image))
                (tensorImage.tensorBuffer as? TensorBufferFloat)?.floatArray
                    ?: tensorImage.tensorBuffer.floatArray
            }
            inputs[0].writeFloat(pixels)
            traceSection("LiteRT:Extractor:Inference") {
                cm.run(inputs, outputs)
            }
            outputs[0].readFloat()
        }
        ).get()
    }

    private fun closeInternal() {
        try {
            (inputs + outputs).forEach { it.close() }
            inputs.clear()
            outputs.clear()
            model?.close()
            model = null
        } catch (e: Exception) {
            Log.e(TAG, "Exception closing LiteRT extractor: ${e.message}", e)
        }
    }

    override fun close() {
        Log.d(TAG, "close invoke")
        try {
            executor.submit { closeInternal() }.get(1, TimeUnit.SECONDS)
        } catch (e: Exception) {
            Log.e(TAG, "Exception while closing detector executor: $e")
        } finally {
            executor.shutdown()
        }
    }

    companion object {
        const val TAG = "Extractor"
    }
}
