package com.example.detectionexample.viewmodels

import android.graphics.*
import android.net.Uri
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.detectionexample.config.ModelConfig
import com.example.detectionexample.config.OverlayViewConfig
import com.example.detectionexample.domain.DetectorUsecase
import com.example.detectionexample.models.BitmapProxy
import com.example.detectionexample.models.Person
import com.example.detectionexample.models.TrackedRecognition
import com.example.detectionexample.repository.ExtractorRepository
import com.example.detectionexample.repository.RecognizedPersonRepository
import com.example.detectionexample.repository.TrackedObjectRepository
import com.example.detectionexample.view.BitmapOverlayVideoProcessor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import kotlin.streams.toList
import kotlin.system.measureTimeMillis


@HiltViewModel
class DetectionViewModel @Inject constructor(
    private val trackedObjectRepository: TrackedObjectRepository,
    private val detector: DetectorUsecase,
    private val recognizedPersonRepository: RecognizedPersonRepository,
    private val extractorRepository: ExtractorRepository
    ) : ViewModel() {

    val sampleVideoUri: Uri = Uri.parse("asset:///face-demographics-walking-and-pause.mp4")
    var isCaptureImage by mutableStateOf(false)
    var captureUri by mutableStateOf(Uri.EMPTY)
    private val _trackedObjects: MutableStateFlow<List<TrackedRecognition>> =
        MutableStateFlow(emptyList())
    val trackedObserver: StateFlow<List<TrackedRecognition>> = _trackedObjects

    private var modelFilename = ModelConfig.DETECTOR_DEFAULT_MODEL
    val repository
        get() = detector(modelFilename)

    val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    var isStaringCamera by mutableStateOf(false)
    var isProcessingFrame by mutableStateOf(true)

    private val analysisMatrix: Matrix = Matrix()
    private lateinit var analysisBitmap: Bitmap
    private lateinit var processBitmap: Bitmap

    var threshold by mutableStateOf(1F)
    private var detectedTime by mutableStateOf(0L)
    private var statedTime = System.currentTimeMillis()
    var needUpdateTrackerImageSourceInfo by  mutableStateOf(true)

    fun getCropBitmapByCPU(cropRectF: RectF, srcBitmap: Bitmap = processBitmap): Bitmap {
        val resultBitmap = Bitmap.createBitmap(
            cropRectF.width().toInt(),
            cropRectF.height().toInt(),
            Bitmap.Config.ARGB_8888
        )
        val resultCanvas = Canvas(resultBitmap)
        val resultMatrix = Matrix()
            .apply {
                postTranslate(-cropRectF.left, -cropRectF.top)
            }
        val processBitmapShader =
            BitmapShader(
                srcBitmap,
                Shader.TileMode.CLAMP,
                Shader.TileMode.CLAMP
            )
        processBitmapShader.setLocalMatrix(resultMatrix)

        val paintProcess = Paint(Paint.FILTER_BITMAP_FLAG)
            .apply {
                color = Color.WHITE
                shader = processBitmapShader
            }
//        resultCanvas.drawBitmap(srcBitmap, resultMatrix, paintProcess)
        resultCanvas.drawRect(
            RectF(0f, 0f, cropRectF.width(), cropRectF.height()),
            paintProcess
        )

        return resultBitmap
    }

    fun observeTrackedObject(timestamp: Long, isRecognizedFace : Boolean, srcBitmap: Bitmap = processBitmap) {
        viewModelScope.launch {
            var result = repository.detectInImage(srcBitmap)
            if(isRecognizedFace) {
                result = result.map { recognitions ->
                    recognitions.parallelStream().map { recognize ->
                        val faces = getCropBitmapByCPU(recognize.location)
                        val emb = extractorRepository.extractImage(faces)
                        val neighbor = recognizedPersonRepository.findNearest(emb, threshold)
                        recognize.apply {
                            neighbor?.first?.let { title = it }
                            neighbor?.second?.let { confidence = it }
                        }
                    }.toList()
                }
            }
            trackedObjectRepository.trackResults(result)
            _trackedObjects.value = trackedObjectRepository.trackedObjects.stateIn(viewModelScope).value
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

    fun setModelDevice(name: String) {
        repository.setModelDevice(name)
    }

    fun addPerson(trackedRecognition: TrackedRecognition, face: Bitmap) {
        val person = Person(
            id = UUID.randomUUID().toString(),
            name = trackedRecognition.title,
            score = (trackedRecognition.detectionConfidence * 100).toInt(),
            embeddings = extractorRepository.extractImage(face),
            face = face
        )
        recognizedPersonRepository.registerPerson(person)
        Log.d(TAG, "Register: ${person.name}")
    }

    var videoAnalyzer = object : BitmapOverlayVideoProcessor.Analyzer {
        override fun analyze(image: BitmapProxy) {
                if (!isProcessingFrame) {
                    return
                }
                if (needUpdateTrackerImageSourceInfo) {
                    analysisBitmap =
                        Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
                    analysisMatrix.setRotate(
                        image.rotationDegrees.toFloat(),
                    )
                    val scaleX = if (image.flipX) -1f else 1f

                    val scaleY = if (image.flipY) -1f else 1f
                    analysisMatrix.setScale(scaleX, scaleY)

                    val rotated = (image.rotationDegrees) % 180 != 90
                    OverlayViewConfig.setFrameConfiguration(
                        if (rotated) image.width else image.height,
                        if (rotated) image.height else image.width, 0
                    )
                    needUpdateTrackerImageSourceInfo = false
                }
            analysisBitmap.copyPixelsFromBuffer(image.buffer)

            processBitmap = Bitmap.createBitmap(
                analysisBitmap,
                0,
                0,
                image.width,
                image.height,
                analysisMatrix,
                true
            )

            observeTrackedObject(image.timestamp, !recognizedPersonRepository.isEmpty())
        }

    }

    override fun onCleared() {
        extractorRepository.close()
    }

    var analyzer = ImageAnalysis.Analyzer { imageProxy ->
        if (!isProcessingFrame) {
            imageProxy.close()
            return@Analyzer
        }
        if (needUpdateTrackerImageSourceInfo) {
            processBitmap =
                Bitmap.createBitmap(imageProxy.width, imageProxy.height, Bitmap.Config.ARGB_8888)
            OverlayViewConfig.setFrameConfiguration(
                imageProxy.width,
                imageProxy.height, 0
            )
            needUpdateTrackerImageSourceInfo = false
        }
        processBitmap.copyPixelsFromBuffer(imageProxy.planes[0].buffer)
        observeTrackedObject(imageProxy.imageInfo.timestamp, !recognizedPersonRepository.isEmpty())
        imageProxy.close()
    }

    fun isRegisteredPersonEmpty() = recognizedPersonRepository.isEmpty()
    fun getRegisteredPerson() = recognizedPersonRepository.getRegisteredPerson()
    fun clearRegisteredPerson() {
        viewModelScope.launch(Dispatchers.IO) {
            recognizedPersonRepository.clearRegisteredPerson()
        }
    }
    fun removeRegisteredPerson(person: Person) =
        recognizedPersonRepository.removeRegisteredPerson(person)

    fun saveAllToDatastore() {
        viewModelScope.launch(Dispatchers.IO) {
            recognizedPersonRepository.saveAllToDatastore()
            isProcessingFrame = true
        }
    }

    fun loadAllToDatastore() {
        viewModelScope.launch(Dispatchers.IO) {
            recognizedPersonRepository.loadAllFromDatastore()
            isProcessingFrame = true
        }
    }

    companion object {
        private const val TAG = "View Model"
    }
}