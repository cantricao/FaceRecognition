package com.example.detectionexample.detector

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.RectF
import android.os.SystemClock
import android.util.Log
import com.example.detectionexample.models.Recognition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.dnn.Dnn
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.FaceDetectorYN
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class FaceYuNetOpencvDetector(
    val context: Context,
    private val objectDetectorListener: TfliteDetectorHelper.DetectorListener?
) : DetectorDataSource {

    private val imageDetector: FaceDetectorYN
    private val imageSize = Size(320.0, 320.0)

    private val result: Mat = Mat()
    private var inputImage = Mat()
    private val listOfPoints = mutableListOf<PointF>()

    init {
        val yunet = getPath("face_detection_yunet_2022mar.onnx", context)

//        DNN_BACKEND_VKCOM - DNN_TARGET_VULKAN,
//        DNN_BACKEND_OPENCV - DNN_TARGET_CPU, DNN_TARGET_OPENCL_FP16, DNN_TARGET_OPENCL
//        DNN_BACKEND_TIMVX - DNN_TARGET_NPU
        imageDetector = FaceDetectorYN.create(
            yunet, "", imageSize,
            0.9f, 0.3f, 5000,
            Dnn.DNN_BACKEND_OPENCV, Dnn.DNN_TARGET_OPENCL
        )
        Log.d(TAG, "Loaded model success")
    }

    companion object {
        const val TAG = "YNetFace"
        init {
            if (!OpenCVLoader.initDebug()) {
                Log.e("OpenCV", "Unable to load OpenCV!")
            } else {
                Log.d("OpenCV", "OpenCV loaded Successfully!")
            }
        }
    }

    override fun detectInImage(image: Bitmap) {
        var inferenceTime = SystemClock.uptimeMillis()
        Utils.bitmapToMat(image, inputImage)
        Log.d(TAG, "put image to model successes")
        Imgproc.resize(inputImage, inputImage, imageSize)
        Imgproc.cvtColor(inputImage, inputImage, Imgproc.COLOR_BGRA2RGB)
        imageDetector.detect(inputImage, result)
        Log.d(TAG, "get result")
        listOfPoints.clear()
        inferenceTime = SystemClock.uptimeMillis() - inferenceTime

        objectDetectorListener?.onResults(
            getResult(image.height, image.width),
            inferenceTime,
            image.height,
            image.width
        )
    }

    override fun clearObjectDetector() {
        // Native YuNet detector handles its own lifecycle in JNI
    }

    // Upload file to storage and return a path.
    private fun getPath(file: String, context: Context): String? {
        val outFile = File(context.filesDir, file)
        // Avoid redundant file copy if model is already extracted to internal storage
        if (outFile.exists() && outFile.length() > 0) {
            return outFile.absolutePath
        }
        val assetManager = context.assets
        try {
            assetManager.open(file).use { inputStream ->
                FileOutputStream(outFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            return outFile.absolutePath
        } catch (ex: IOException) {
            Log.e(TAG, "Failed to copy asset file $file to storage: ${ex.message}", ex)
        }
        return ""
    }

    private fun getResult(imageHeight: Int, imageWidth: Int): Flow<List<Recognition>> {
        listOfPoints.clear()

        return MutableStateFlow(
            (0 until result.rows()).map {
            for (i in 4..12 step 2) {
                listOfPoints.add(
                    PointF(
                        (result.get(it, i)[0] * imageWidth / imageSize.width).toFloat(),
                        (result.get(it, i + 1)[0] * imageHeight / imageSize.height).toFloat()
                    )
                )
            }
            Recognition(
                "$it",
                "",
                (result.get(it, 14)[0]).toFloat(),
                RectF(
                    (result.get(it, 0)[0] * imageWidth / imageSize.width).toFloat(),
                    (result.get(it, 1)[0] * imageHeight / imageSize.height).toFloat(),
                    ((result.get(it, 0)[0] + result.get(it, 2)[0]) * imageWidth / imageSize.width).toFloat(),
                    ((result.get(it, 1)[0] + result.get(it, 3)[0]) * imageHeight / imageSize.height).toFloat()
                ),
                listOfPoints
            )
        }
        )
    }
}
