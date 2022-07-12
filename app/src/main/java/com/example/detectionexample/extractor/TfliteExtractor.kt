package com.example.detectionexample.extractor

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.InterpreterApi
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.model.Model
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.Closeable
import java.io.IOException
import java.nio.ByteBuffer

open class TfliteExtractor(val context: Context, modelPath: String, device: Model.Device): Closeable {

    private val numberOfThread = if (device == Model.Device.CPU) 4 else 1

    private val embeddings by lazy {
        imageDetector.getOutputTensor(0).let {
            TensorBuffer.createFixedSize(
                it.shape(),
                it.dataType()
            )
        }
    }

    private val imageDetector: Model by lazy {
        Model.createModel(
            context,
            modelPath,
            Model.Options.Builder()
                .setDevice(device)
                .setNumThreads(numberOfThread)
                .setTfLiteRuntime(InterpreterApi.Options.TfLiteRuntime.PREFER_SYSTEM_OVER_APPLICATION)
                .build()
        )
    }


    private val imageSizeX by lazy { imageDetector.getInputTensor(0).shape()[1] }

    private val imageSizeY by lazy { imageDetector.getInputTensor(0).shape()[2] }

    private var imageWidth: Int = 0
    private var imageHeight: Int = 0

    private val imageProcessor: ImageProcessor by lazy {
        ImageProcessor.Builder()
            .add(ResizeOp(imageSizeY, imageSizeX, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
            .add(NormalizeOp(127.5f, 127.5f))
            .build()
    }

    fun extractImage(image: Bitmap): FloatArray {
        setImageSize(image)
        val inputImage = TensorImage.fromBitmap(image)
        val processedImage = imageProcessor.process(inputImage)
        outputMapBuffer.forEach { (_, buffer) -> buffer.rewind() }
        imageDetector.run(arrayOf(processedImage.buffer), outputMapBuffer)
        outputMapBuffer.forEach { (_, buffer) -> buffer.flip() }
        return getResult()
    }


    private fun setImageSize(image: Bitmap) {
        imageWidth = image.width
        imageHeight = image.height
    }

    override fun close() {
        Log.d(TAG, "close invoke")
        try {
            imageDetector.close()
        } catch (e: IOException) {
            Log.e(
                TAG,
                "Exception thrown while trying to close Detector: $e"
            )
        }
    }

    private fun getResult(): FloatArray {
        return embeddings.floatArray
    }

    private val outputMapBuffer: Map<Int, ByteBuffer> by lazy {
        mapOf(0 to embeddings.buffer)
    }


    companion object {
        const val TAG = "Extractor"
    }
}