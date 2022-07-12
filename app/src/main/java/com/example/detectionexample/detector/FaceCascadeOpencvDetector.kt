package com.example.detectionexample.detector

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import com.example.detectionexample.R
import com.example.detectionexample.config.ModelConfig
import com.example.detectionexample.models.Recognition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfRect
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.CascadeClassifier
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream


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
    private val result: MatOfRect = MatOfRect()
    private val inputImage = Mat()

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
        imageDetector?.detectMultiScale(inputImage, result)
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
                )
            )
        })
    }
}