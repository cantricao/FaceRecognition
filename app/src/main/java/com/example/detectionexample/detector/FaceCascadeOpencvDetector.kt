package com.example.detectionexample.detector

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.RectF
import android.media.Image
import android.util.Log
import com.example.detectionexample.R
import com.example.detectionexample.config.ModelConfig
import com.example.detectionexample.models.Recognition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfByte
import org.opencv.core.MatOfRect
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.CascadeClassifier
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.ByteBuffer


class FaceCascadeOpencvDetector(val context: Context) :DetectorDataSource {

    private val imageDetector: CascadeClassifier? by lazy {
        val `is`: InputStream =
            context.resources.openRawResource(R.raw.lbpcascade_frontalface_improved)
        val cascadeDir: File = context.getDir(ModelConfig.CASCADE_DIRNAME, Context.MODE_PRIVATE)
        val caseFile = File(cascadeDir, ModelConfig.CASCADE_FILENAME)
        val fos = FileOutputStream(caseFile)

        val buffer = ByteArray(4096)
        var bytesRead: Int

        while (`is`.read(buffer).also { bytesRead = it } != -1) {
            fos.write(buffer, 0, bytesRead)
        }
        `is`.close()
        fos.close()
        CascadeClassifier(caseFile.absolutePath)
            .takeUnless { it.empty() }
            .also { cascadeDir.delete() }
    }
    private val eyesCascade: CascadeClassifier? by lazy {
        val `is`: InputStream =
            context.resources.openRawResource(R.raw.haarcascade_eye_tree_eyeglasses)
        val cascadeDir: File = context.getDir(ModelConfig.CASCADE_DIRNAME, Context.MODE_PRIVATE)
        val caseFile = File(cascadeDir, ModelConfig.CASCADE_FILENAME)
        val fos = FileOutputStream(caseFile)

        val buffer = ByteArray(4096)
        var bytesRead: Int

        while (`is`.read(buffer).also { bytesRead = it } != -1) {
            fos.write(buffer, 0, bytesRead)
        }
        `is`.close()
        fos.close()
        CascadeClassifier(caseFile.absolutePath)
            .takeUnless { it.empty() }
            .also { cascadeDir.delete() }
    }
    private val result: MatOfRect = MatOfRect()
    private var inputImage = Mat()
    private val listOfPoints = mutableListOf<PointF>()

    companion object {
        init {
            if (!OpenCVLoader.initDebug())
                Log.e("OpenCV", "Unable to load OpenCV!")
            else
                Log.d("OpenCV", "OpenCV loaded Successfully!")
        }
    }

    override fun detectInImage(image: Bitmap): Flow<List<Recognition>> {
        Utils.bitmapToMat(image, inputImage)
        Imgproc.cvtColor(inputImage, inputImage, Imgproc.COLOR_BGR2GRAY)
        Imgproc.equalizeHist(inputImage, inputImage)
        imageDetector?.detectMultiScale(inputImage, result)
        listOfPoints.clear()
        result.toList().forEach { face ->
            val faceROI: Mat = inputImage.submat(face)
            val eyes = MatOfRect()
            eyesCascade?.detectMultiScale(faceROI, eyes)
            eyes.toList().forEach { eye ->
                listOfPoints.add(
                    PointF(face.x + eye.x + eye.width / 2f, face.y + eye.y + eye.height / 2f)
                )

            }
        }

        return getResult()
    }


    override fun detectInImage(image: Image): Flow<List<Recognition>> {
        val bb: ByteBuffer = image.planes[0].buffer
        val buf = ByteArray(bb.remaining())
        bb.get(buf)
        inputImage = Imgcodecs.imdecode(MatOfByte(*buf), Imgcodecs.IMREAD_GRAYSCALE)
        val b = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(inputImage, b)
//        Imgproc.cvtColor(inputImage, inputImage, Imgproc.COLOR_YUV2GRAY_I420)
        Imgproc.equalizeHist(inputImage, inputImage)
        imageDetector?.detectMultiScale(inputImage, result)
        listOfPoints.clear()
        result.toList().forEach { face ->
            val faceROI: Mat = inputImage.submat(face)
            val eyes = MatOfRect()
            eyesCascade?.detectMultiScale(faceROI, eyes)
            eyes.toList().forEach { eye ->
                listOfPoints.add(
                    PointF(face.x + eye.x + eye.width / 2f, face.y + eye.y + eye.height / 2f)
                )

            }
        }

        return getResult()
    }

    private fun getResult(): Flow<List<Recognition>> {
        return MutableStateFlow(result.toArray().map {
            Recognition(
                "$it",
                "",
                100.0f,
                RectF(
                    (it.tl().x.toFloat()),
                    (it.tl().y.toFloat()),
                    (it.br().x.toFloat()),
                    (it.br().y.toFloat())
                ),
                listOfPoints
            )
        })
    }
}