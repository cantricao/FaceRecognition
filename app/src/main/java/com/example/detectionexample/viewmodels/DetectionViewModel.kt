package com.example.detectionexample.viewmodels

import android.graphics.Bitmap
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
import com.example.detectionexample.config.Util
import com.example.detectionexample.domain.DetectorUsecase
import com.example.detectionexample.models.Person
import com.example.detectionexample.models.Recognition
import com.example.detectionexample.models.TrackedRecognition
import com.example.detectionexample.repository.ExtractorRepository
import com.example.detectionexample.repository.RecognizedPersonRepository
import com.example.detectionexample.repository.TrackedObjectRepository
import com.example.detectionexample.customexoplayer.VideoView
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import kotlin.streams.toList


@HiltViewModel
class DetectionViewModel @Inject constructor(
    private val trackedObjectRepository: TrackedObjectRepository,
    private val detector: DetectorUsecase,
    private val recognizedPersonRepository: RecognizedPersonRepository,
    private val extractorRepository: ExtractorRepository
    ) : ViewModel() {

    val sampleVideoUri: Uri = Uri.parse("asset:///face-demographics-walking-and-pause.mp4")
    var isCaptureImage by mutableStateOf(false)
    var captureUri: Uri by mutableStateOf(Uri.EMPTY)
    private val _trackedObjects: MutableStateFlow<List<TrackedRecognition>> =
        MutableStateFlow(emptyList())
    val trackedObserver: StateFlow<List<TrackedRecognition>> = _trackedObjects

    private var modelFilename = ModelConfig.DETECTOR_DEFAULT_MODEL
    val repository
        get() = detector(modelFilename)

    val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    var isStaringCamera by mutableStateOf(false)
    var isProcessingFrame by mutableStateOf(true)

    lateinit var processBitmap: Bitmap
        private set

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
                        val faces = Util.getCropBitmapByCPU(recognize.location, processBitmap)
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

    var videoAnalyzer = object : VideoView.Analyzer {
        override fun analyze(image: Bitmap, timestamp: Long) {
            if (!isProcessingFrame) {
                return
            }
            if (needUpdateTrackerImageSourceInfo) {
                processBitmap =
                    Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
                OverlayViewConfig.setFrameConfiguration(
                    image.width,
                    image.height, 0
                )
                needUpdateTrackerImageSourceInfo = false
            }
            processBitmap = Bitmap.createBitmap(image)
            observeTrackedObject(timestamp,
                !recognizedPersonRepository.isEmpty(),
                detectInImage(processBitmap))
        }
    }

    override fun onCleared() {
        extractorRepository.close()
        analysisExecutor.shutdown()
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
        observeTrackedObject(
            imageProxy.imageInfo.timestamp,
            !recognizedPersonRepository.isEmpty(),
            detectInImage(processBitmap))
        imageProxy.close()
    }

    fun isRegisteredPersonEmpty() = recognizedPersonRepository.isEmpty()
    fun getRegisteredPerson() = recognizedPersonRepository.getRegisteredPerson()
    fun clearRegisteredPerson() {
        viewModelScope.launch(Dispatchers.IO) {
            recognizedPersonRepository.clearRegisteredPerson()
        }
    }

    fun detectInImage(srcBitmap: Bitmap): Flow<List<Recognition>> {
        return repository.detectInImage(srcBitmap)
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