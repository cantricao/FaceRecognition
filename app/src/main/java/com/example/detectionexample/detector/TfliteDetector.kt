package com.example.detectionexample.detector

import android.content.Context
import android.graphics.Bitmap
import android.media.Image
import android.util.Log
import com.example.detectionexample.models.Recognition
import kotlinx.coroutines.flow.Flow
import org.tensorflow.lite.DataType
import org.tensorflow.lite.InterpreterApi
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.model.Model
import java.io.Closeable
import java.io.IOException
import java.nio.ByteBuffer

abstract class TfliteDetector(context: Context, modelPath: String, labelPath: String,
                              device: Model.Device) : Closeable, DetectorDataSource {

    private val numberOfThread = if (device == Model.Device.CPU) 4 else 1
    protected val imageDetector: Model by lazy {
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

    protected val imageSizeX: Int by lazy { imageDetector.getInputTensor(0).shape()[1] }

    protected val imageSizeY: Int by lazy { imageDetector.getInputTensor(0).shape()[2] }

    protected val inputDataType: DataType by lazy { imageDetector.getInputTensor(0).dataType() }

    protected var imageWidth: Int = 0
    protected var imageHeight: Int = 0

    protected val labels: MutableList<String> by lazy { FileUtil.loadLabels(context, labelPath) }

    private val imageProcessor: ImageProcessor by lazy {
        ImageProcessor.Builder()
            .add(ResizeOp(imageSizeY, imageSizeX, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
            .add(NormalizeOp(127.5f, 127.5f))
            .build()
    }

    override fun detectInImage(image: Bitmap): Flow<List<Recognition>> {
        imageWidth = image.width
        imageHeight = image.height

        val inputImage = TensorImage.fromBitmap(image)
        val processedImage = imageProcessor.process(inputImage)
        outputMapBuffer.forEach { (_, buffer) -> buffer.rewind() }
        imageDetector.run(arrayOf(processedImage.buffer), outputMapBuffer)
        outputMapBuffer.forEach { (_, buffer) -> buffer.flip() }
        return getResult()
    }

    override fun detectInImage(image: Image): Flow<List<Recognition>> {
        imageWidth = image.width
        imageHeight = image.height

        val inputImage = TensorImage(inputDataType)
        inputImage.load(image)
        val processedImage = imageProcessor.process(inputImage)
        outputMapBuffer.forEach { (_, buffer) -> buffer.rewind() }
        imageDetector.run(arrayOf(processedImage.buffer), outputMapBuffer)
        outputMapBuffer.forEach { (_, buffer) -> buffer.flip() }
        return getResult()
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

    abstract fun getResult(): Flow<List<Recognition>>
    var threshold = 50.0f
    abstract val outputMapBuffer: Map<Int, ByteBuffer>

    companion object {
        const val TAG = "Detector"
    }
}