package com.example.detectionexample.repository

import android.content.Context
import android.graphics.Bitmap
import com.example.detectionexample.config.ModelConfig
import com.example.detectionexample.detector.BlazeFaceTfliteDetector
import com.example.detectionexample.detector.DetectorDataSource
import com.example.detectionexample.detector.FaceMLDetector
import com.example.detectionexample.detector.FaceYuNetOpencvDetector
import com.example.detectionexample.detector.MobilenetSSDTfliteDetector
import com.example.detectionexample.detector.TfliteDetectorHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.support.model.Model
import java.io.Closeable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TfliteRepository @Inject constructor(@ApplicationContext val context: Context): DetectorRepository, Closeable {
    private var device = ModelConfig.DETECTOR_DEFAULT_DEVICE
    private var modelName = ModelConfig.DETECTOR_DEFAULT_MODEL
    private var _detector: DetectorDataSource? = null
    private var objectDetectorListener:TfliteDetectorHelper.DetectorListener? = null


    @Synchronized
    fun createModel() {
        _detector?.clearObjectDetector()
        _detector = when (modelName) {
            ModelConfig.BLAZEFACE_MODEL_NAME -> BlazeFaceTfliteDetector(context, modelName, device, this.objectDetectorListener)
            ModelConfig.MLKIT_CODENAME -> FaceMLDetector(this.objectDetectorListener)
            ModelConfig.OPENCV_CODENAME -> FaceYuNetOpencvDetector(context, this.objectDetectorListener)
            else -> MobilenetSSDTfliteDetector(context, modelName, device, objectDetectorListener)
        }
    }

    fun init(objectDetectorListener: TfliteDetectorHelper.DetectorListener?){
        this.objectDetectorListener = objectDetectorListener
        createModel()
    }

//    override fun setThreshold(threshold: Float) {
//        _detector!!.setThreshold(threshold)
//    }

    @Synchronized
    override fun detectInImage(bitmap: Bitmap) {
        if(objectDetectorListener != null) {
            _detector!!.detectInImage(bitmap)
        }
    }

    override fun setFileModelName(filename: String) {
        modelName = filename
        createModel()
    }

    override fun setModelDevice(name: String) {
        device = when (name) {
            "CPU" -> Model.Device.CPU
            "GPU" -> Model.Device.GPU
            else -> Model.Device.NNAPI
        }
        createModel()
    }

    override fun close() {
        _detector?.clearObjectDetector()
    }
}