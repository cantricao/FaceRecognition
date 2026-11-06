package com.example.detectionexample.viewmodels

import android.graphics.Bitmap
import android.graphics.Canvas
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
import com.example.detectionexample.core.common.util.traceSection
import com.example.detectionexample.detector.TfliteDetectorHelper
import com.example.detectionexample.models.Recognition
import com.example.detectionexample.models.TrackedRecognition
import com.example.detectionexample.repository.ExtractorRepository
import com.example.detectionexample.repository.RecognizedPersonRepository
import com.example.detectionexample.repository.TfliteRepository
import com.example.detectionexample.repository.TrackedObjectRepository
import com.example.detectionexample.views.BitmapOverlayVideoProcessor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
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

    // Reusable double-buffers for CameraX analysis to eliminate per-frame allocations & GC pauses
    private var frontBitmap: Bitmap? = null
    private var backBitmap: Bitmap? = null
    private var bufferCanvas: Canvas? = null
    private val isAnalyzing = AtomicBoolean(false)

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
        viewModelScope.launch(Dispatchers.Default) {
            if (isRecognizedFace) {
                val resultWithRegister = traceSection("FaceRecognition:Pipeline") {
                    result.map { recognitions ->
                        recognitions.parallelStream().map { recognize ->
                            val faces = traceSection("FaceRecognition:CropBitmap") {
                                Util.getCropBitmapByCPU(recognize.location, processingBitmap)
                            }
                            val emb = traceSection("FaceRecognition:FeatureExtraction") {
                                extractorRepository.extractImage(faces)
                            }
                            val neighbor = traceSection("FaceRecognition:VectorSearch") {
                                recognizedPersonRepository.findNearest(emb, threshold)
                            }
                            // Recycle cropped face bitmap immediately to reduce GC churn
                            faces.recycle()
                            recognize.apply {
                                neighbor?.first?.let { title = it }
                                neighbor?.second?.let { confidence = it }
                            }
                        }.toList()
                    }
                }
                trackedObjectRepository.trackResults(resultWithRegister)
            } else {
                trackedObjectRepository.trackResults(result)
            }
            val trackedResult = trackedObjectRepository.trackedObjects.stateIn(viewModelScope).value
            withContext(Dispatchers.Main.immediate) {
                _trackedObjects.value = trackedResult
                detectedTime = System.currentTimeMillis() - statedTime
                statedTime = System.currentTimeMillis()
            }
            Log.d(
                TAG,
                "Processing ${_trackedObjects.value.size} results from ${timestamp.toString(3)} in $detectedTime ms"
            )
        }
    }

    fun setModelName(filename: String) {
        modelFilename = filename
        viewModelScope.launch(Dispatchers.Default) {
            tfliteRepository.setFileModelName(filename)
            recognizedPersonRepository.clear()
        }
    }

    fun setModelDevice(name: String) {
        viewModelScope.launch(Dispatchers.Default) {
            tfliteRepository.setModelDevice(name)
        }
    }

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
                image.height,
                0
            )
            if (::processingBitmap.isInitialized) {
                processingBitmap.recycle()
            }
            processingBitmap = Bitmap.createBitmap(image, 0, 0, image.width, image.height, processingMatrix, true)
            detectInImage(processingBitmap)
            image.recycle()
        }
    }

    override fun onCleared() {
        if (::processingBitmap.isInitialized && !processingBitmap.isRecycled) {
            processingBitmap.recycle()
        }
        frontBitmap?.recycle()
        backBitmap?.recycle()
        extractorRepository.close()
    }

    private val objectDetectorListener = object : TfliteDetectorHelper.DetectorListener {

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
        viewModelScope.launch(Dispatchers.Default) {
            tfliteRepository.init(objectDetectorListener)
        }
    }

    var analyzer = ImageAnalysis.Analyzer { imageProxy ->
        if (!isProcessingFrame) {
            imageProxy.close()
            return@Analyzer
        }

        // Drop frame immediately if inference is already in progress to preserve STRATEGY_KEEP_ONLY_LATEST
        // and eliminate CameraServer buffer queue starvation
        if (!isAnalyzing.compareAndSet(false, true)) {
            imageProxy.close()
            return@Analyzer
        }

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val isRotated = rotationDegrees == 90 || rotationDegrees == 270
        val targetWidth = if (isRotated) imageProxy.height else imageProxy.width
        val targetHeight = if (isRotated) imageProxy.width else imageProxy.height

        if (needUpdateTrackerImageSourceInfo) {
            OverlayViewConfig.setFrameConfiguration(
                imageProxy.width,
                imageProxy.height,
                rotationDegrees
            )
            needUpdateTrackerImageSourceInfo = false
        }

        // Allocate reusable double buffers only once when dimensions change
        if (backBitmap == null || backBitmap?.width != targetWidth || backBitmap?.height != targetHeight) {
            frontBitmap?.recycle()
            backBitmap?.recycle()
            frontBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            backBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            bufferCanvas = Canvas(backBitmap!!)
        }

        // Copy and rotate frame in-place into back buffer
        traceSection("CameraX:BitmapConversion") {
            val srcBitmap = imageProxy.toBitmap()
            processingMatrix.reset()
            if (rotationDegrees != 0) {
                processingMatrix.postRotate(rotationDegrees.toFloat())
                when (rotationDegrees) {
                    90 -> processingMatrix.postTranslate(targetWidth.toFloat(), 0f)
                    180 -> processingMatrix.postTranslate(targetWidth.toFloat(), targetHeight.toFloat())
                    270 -> processingMatrix.postTranslate(0f, targetHeight.toFloat())
                }
            }
            bufferCanvas?.drawBitmap(srcBitmap, processingMatrix, null)
            // Immediately recycle the raw srcBitmap from toBitmap() to eliminate GC churn
            srcBitmap.recycle()
        }

        // IMMEDIATELY close imageProxy to release CameraX ImageReader buffer back to CameraServer (<2ms)
        imageProxy.close()

        // Swap buffers: backBitmap becomes the new active processingBitmap
        val currentBack = backBitmap!!
        backBitmap = frontBitmap
        frontBitmap = currentBack
        bufferCanvas?.setBitmap(backBitmap)
        processingBitmap = currentBack

        // Execute detection asynchronously on Dispatchers.Default
        viewModelScope.launch(Dispatchers.Default) {
            try {
                traceSection("CameraX:DetectInImage") {
                    detectInImage(currentBack)
                }
            } finally {
                isAnalyzing.set(false)
            }
        }
    }

    private fun detectInImage(bitmap: Bitmap) {
        tfliteRepository.detectInImage(bitmap)
    }

    companion object {
        private const val TAG = "View Model"
    }
}
