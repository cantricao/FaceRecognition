package com.example.detectionexample.repository

import android.content.Context
import android.graphics.Bitmap
import com.example.detectionexample.config.ModelConfig
import com.example.detectionexample.detector.BlazeFaceTfliteDetector
import com.example.detectionexample.detector.MobilenetSSDTfliteDetector
import com.example.detectionexample.detector.TfliteDetector
import com.example.detectionexample.models.Recognition
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import org.tensorflow.lite.support.model.Model
import java.io.Closeable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TfliteRepository @Inject constructor(@ApplicationContext val context: Context): DetectorRepository, Closeable {
    private var device = ModelConfig.DETECTOR_DEFAULT_DEVICE
    private var modelName = ModelConfig.DETECTOR_DEFAULT_MODEL
    private var _detector: TfliteDetector? = null

    init {
        createModel()
    }

    @Synchronized
    fun createModel(){
        _detector?.close()
        _detector = when (modelName) {
            ModelConfig.BLAZEFACE_MODEL_NAME -> BlazeFaceTfliteDetector(context, modelName, device)
            else -> MobilenetSSDTfliteDetector(context, modelName, device)
        }
    }

    override fun setThreshold(threshold: Float){
        _detector!!.threshold = threshold
    }

    @Synchronized
    override fun detectInImage(bitmap: Bitmap): Flow<List<Recognition>>{
        return _detector!!.detectInImage(bitmap)
    }

    override fun setFileModelName(filename: String) {
        modelName = filename
        createModel()
    }

    override fun setModelDevice(name: String) {
        device = when(name){
            "CPU" -> Model.Device.CPU
            "GPU" -> Model.Device.GPU
            else -> Model.Device.NNAPI
        }
        createModel()
    }

    override fun close() {
        _detector?.close()
    }
}