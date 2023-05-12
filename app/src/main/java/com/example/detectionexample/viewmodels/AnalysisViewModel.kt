package com.example.detectionexample.viewmodels

import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.detectionexample.config.ModelConfig
import com.example.detectionexample.config.OverlayViewConfig
import com.example.detectionexample.config.Util
import com.example.detectionexample.custom.BitmapAnalyzer
import com.example.detectionexample.custom.BitmapEncoding
import com.example.detectionexample.domain.DetectorUsecase
import com.example.detectionexample.models.Recognition
import com.example.detectionexample.models.TrackedRecognition
import com.example.detectionexample.repository.ExtractorRepository
import com.example.detectionexample.repository.RecognizedPersonRepository
import com.example.detectionexample.repository.TrackedObjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.streams.toList


@HiltViewModel
class AnalysisViewModel @Inject constructor(
    private val trackedObjectRepository: TrackedObjectRepository,
    private val detector: DetectorUsecase,
    private val recognizedPersonRepository: RecognizedPersonRepository,
    private val extractorRepository: ExtractorRepository
    ) : ViewModel() {

    private val _trackedObjects: MutableStateFlow<List<TrackedRecognition>> =
        MutableStateFlow(emptyList())
    val trackedObserver: StateFlow<List<TrackedRecognition>> = _trackedObjects

    private var modelFilename = ModelConfig.DETECTOR_DEFAULT_MODEL
    val repository
        get() = detector(modelFilename)

    var isStaringCamera by mutableStateOf(true)
    var isProcessingFrame by mutableStateOf(true)

    lateinit var processingBitmap: Bitmap
        private set

    private var processingMatrix = Matrix()
    private lateinit var analyzingBitmap: Bitmap

    private var bitmapEncoding: BitmapEncoding? = null

    fun setBitmapEncoding(bitmapEncoding: BitmapEncoding){
        this.bitmapEncoding = bitmapEncoding
    }




    var threshold by mutableStateOf(1F)

    private var detectedTime by mutableStateOf(0L)
    private var statedTime = System.currentTimeMillis()
    var needUpdateTrackerImageSourceInfo by mutableStateOf(true)

    fun observeTrackedObject(
        timestamp: Long,
        isRecognizedFace: Boolean,
        result: Flow<List<Recognition>>
    ) {
        viewModelScope.launch {
            if (isRecognizedFace) {
                val resultWithRegister = result.map { recognitions ->
                    recognitions.parallelStream().map { recognize ->
                        val faces = Util.getCropBitmapByCPU(recognize.location, processingBitmap)
                        val emb = extractorRepository.extractImage(faces)
                        val neighbor = recognizedPersonRepository.findNearest(emb, threshold)
                        recognize.apply {
                            neighbor?.first?.let { title = it }
                            neighbor?.second?.let { confidence = it }
                        }
                    }.toList()
                }
                trackedObjectRepository.trackResults(resultWithRegister)
            } else {
                trackedObjectRepository.trackResults(result)
            }
            _trackedObjects.value =
                trackedObjectRepository.trackedObjects.stateIn(viewModelScope).value
            detectedTime = System.currentTimeMillis() - statedTime
            statedTime = System.currentTimeMillis()
            Log.d(
                TAG,
                "Processing ${_trackedObjects.value.size} results from ${timestamp.toString(3)} in $detectedTime ms"
            )

        }
    }


    fun setModelName(filename: String) {
        modelFilename = filename
        repository.setFileModelName(filename)
        recognizedPersonRepository.clear()
    }

    fun setModelDevice(name: String) = repository.setModelDevice(name)

    var bitmapAnalyzer = object : BitmapAnalyzer {
        override fun analyze(image: Bitmap, timestamp: Long) {
            if (!isProcessingFrame) {
                return
            }
            if (needUpdateTrackerImageSourceInfo) {
                OverlayViewConfig.setFrameConfiguration(
                    image.width,
                    image.height, 0
                )
                needUpdateTrackerImageSourceInfo = false
            }
            processingBitmap = Bitmap.createBitmap(image)
            observeTrackedObject(timestamp,
                !recognizedPersonRepository.isEmpty(),
                detectInImage(processingBitmap))
        }
    }

    override fun onCleared() {
        extractorRepository.close() }


    private fun ImageProxy.toByteArray() : ByteArray{
        val yBuffer = planes[0].buffer // Y
        val vuBuffer = planes[2].buffer // VU

        val ySize = yBuffer.remaining()
        val vuSize = vuBuffer.remaining()

        val nv21 = ByteArray(ySize + vuSize)

        yBuffer.get(nv21, 0, ySize)
        vuBuffer.get(nv21, ySize, vuSize)
        return nv21
    }
    var analyzer = ImageAnalysis.Analyzer { imageProxy ->
        if (!isProcessingFrame) {
            imageProxy.close()
            return@Analyzer
        }
        if (needUpdateTrackerImageSourceInfo) {
            analyzingBitmap = Bitmap.createBitmap(imageProxy.width, imageProxy.height, Bitmap.Config.ARGB_8888)
            processingMatrix.setRotate(imageProxy.imageInfo.rotationDegrees.toFloat(), imageProxy.width / 2f, imageProxy.height / 2f)
            OverlayViewConfig.setFrameConfiguration(
                imageProxy.width,
                imageProxy.height, 0
            )
            needUpdateTrackerImageSourceInfo = false
        }
        Util.yuvToRgb(imageProxy.toByteArray(), analyzingBitmap, ImageFormat.NV21)
        processingBitmap = Bitmap.createBitmap(analyzingBitmap, 0, 0, imageProxy.width, imageProxy.height, processingMatrix, true)
        observeTrackedObject(
            imageProxy.imageInfo.timestamp,
            !recognizedPersonRepository.isEmpty(),
            detectInImage(processingBitmap))
        imageProxy.close()
    }

    fun detectInImage(srcBitmap: Bitmap): Flow<List<Recognition>> {
        return repository.detectInImage(srcBitmap)
    }

    companion object {
        private const val TAG = "View Model"
    }
}