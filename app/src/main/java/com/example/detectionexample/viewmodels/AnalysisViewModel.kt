package com.example.detectionexample.viewmodels

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.detectionexample.config.ModelConfig
import com.example.detectionexample.config.OverlayViewConfig
import com.example.detectionexample.config.Util
import com.example.detectionexample.detector.TfliteDetectorHelper
import com.example.detectionexample.models.Recognition
import com.example.detectionexample.models.TrackedRecognition
import com.example.detectionexample.repository.ExtractorRepository
import com.example.detectionexample.repository.RecognizedPersonRepository
import com.example.detectionexample.repository.TfliteRepository
import com.example.detectionexample.repository.TrackedObjectRepository
import com.example.detectionexample.views.BitmapOverlayVideoProcessor
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
    private val recognizedPersonRepository: RecognizedPersonRepository,
    private val extractorRepository: ExtractorRepository,
    private val tfliteRepository: TfliteRepository
    ) : ViewModel() {

    private val _trackedObjects: MutableStateFlow<List<TrackedRecognition>> =
        MutableStateFlow(emptyList())
    val trackedObserver: StateFlow<List<TrackedRecognition>> = _trackedObjects

    private var modelFilename = ModelConfig.DETECTOR_DEFAULT_MODEL

    var isProcessingFrame by mutableStateOf(true)

    lateinit var processingBitmap: Bitmap
        private set

    private var processingMatrix = Matrix()


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
        tfliteRepository.setFileModelName(filename)
        recognizedPersonRepository.clear()
    }

    fun setModelDevice(name: String) = tfliteRepository.setModelDevice(name)

    var bitmapAnalyzer = object : BitmapOverlayVideoProcessor.BitmapAnalyzer {
        override fun analyze(image: Bitmap, timestamp: Long) {
            if (!isProcessingFrame) {
                return
            }
            if (needUpdateTrackerImageSourceInfo) {
                processingMatrix.reset()
                processingMatrix.postScale(1f, -1f)

                needUpdateTrackerImageSourceInfo = false
            }
            OverlayViewConfig.setFrameConfiguration(
                image.width,
                image.height, 0
            )
            if (::processingBitmap.isInitialized){
                processingBitmap.recycle()
            }
            processingBitmap = Bitmap.createBitmap(image, 0, 0, image.width, image.height, processingMatrix, true)
            detectInImage(processingBitmap)
            image.recycle()
        }
    }

    override fun onCleared() {
        extractorRepository.close() }


    private val objectDetectorListener = object: TfliteDetectorHelper.DetectorListener {

        override fun onError(error: String) {
            viewModelScope.launch {
                _trackedObjects.value = listOf()
            }
        }

        override fun onResults(
            results: Flow<List<Recognition>>,
            inferenceTime: Long,
            imageHeight: Int,
            imageWidth: Int
        ) {

            observeTrackedObject(
                System.currentTimeMillis(),
                false,
                results
            )
        }
    }

    init {
        tfliteRepository.init(objectDetectorListener)
    }

    var analyzer = ImageAnalysis.Analyzer { imageProxy ->
        if (!isProcessingFrame) {
            imageProxy.close()
            return@Analyzer
        }
        if (needUpdateTrackerImageSourceInfo) {
            processingMatrix.reset()
            processingMatrix.postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
            OverlayViewConfig.setFrameConfiguration(
                imageProxy.width,
                imageProxy.height, imageProxy.imageInfo.rotationDegrees
            )
            needUpdateTrackerImageSourceInfo = false
        }
        processingBitmap = Bitmap.createBitmap(imageProxy.toBitmap(), 0, 0, imageProxy.width, imageProxy.height, processingMatrix, true)
        detectInImage(processingBitmap)

        imageProxy.close()
    }

    private fun detectInImage(bitmap: Bitmap) {
        tfliteRepository.detectInImage(bitmap)
    }


    companion object {
        private const val TAG = "View Model"
    }
}