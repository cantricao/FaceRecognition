package com.example.detectionexample.genai

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.prompt.GenerateContentRequest
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.ImagePart
import com.google.mlkit.genai.prompt.PromptPrefix
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import com.google.mlkit.genai.prompt.generateTypedContentRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.Closeable
import javax.inject.Inject
import javax.inject.Singleton

sealed interface GeminiNanoStatus {
    data object Checking : GeminiNanoStatus
    data object Available : GeminiNanoStatus
    data class Downloadable(val progressBytes: Long = 0) : GeminiNanoStatus
    data object Downloading : GeminiNanoStatus
    data object Unavailable : GeminiNanoStatus
    data class Error(val message: String) : GeminiNanoStatus
}

@Singleton
class GeminiNanoFaceAnalyzer @Inject constructor() : Closeable {

    companion object {
        private const val TAG = "GeminiNanoFaceAnalyzer"

        // Static prompt prefix (>200 words) for prefix caching on supported devices
        const val SYSTEM_PROMPT_PREFIX = """
You are an expert on-device computer vision and facial analytics intelligence engine running locally on Android using Google ML Kit GenAI Prompt API powered by Gemini Nano.
Your purpose is to provide thorough, objective, respectful, and highly accurate analysis of facial crops, detection telemetry, and biometric registration queries.
When analyzing images or evaluating face profiles, you must rigorously adhere to the following principles:
1. Facial Expressions: Accurately identify primary and micro facial expressions, such as happy, smiling, relaxed, neutral, concentrated, surprised, frowning, or blinking.
2. Demographics and Appearance: Provide objective and generalized age group brackets, such as child (0-12), adolescent (13-17), young adult (18-35), mature adult (36-60), or senior (60+). Do not make derogatory, biased, or speculative assertions.
3. Facial Accessories and Occlusions: Carefully identify any objects worn on or covering the face, including eyeglasses, wire-rim spectacles, sunglasses, safety goggles, face masks, headscarves, beanies, or caps.
4. Image Clarity and Technical Capture Quality: Inspect whether the crop is sharp, well-lit, under-exposed, over-exposed, washed out, blurry, pixelated, or noisy.
5. Privacy and Ethical Boundaries: Never speculate on private personal identities, sensitive medical or health information, or emotional states beyond observable visual features.
6. Tone and Conciseness: Maintain a professional, neutral, and helpful tone at all times. Deliver clear, actionable, and succinct summaries suitable for on-device mobile applications.
"""
    }

    private var _generativeModel: GenerativeModel? = null
    val generativeModel: GenerativeModel
        get() {
            if (_generativeModel == null) {
                _generativeModel = Generation.getClient()
            }
            return _generativeModel!!
        }

    private val _status = MutableStateFlow<GeminiNanoStatus>(GeminiNanoStatus.Checking)
    val status: StateFlow<GeminiNanoStatus> = _status.asStateFlow()

    suspend fun checkStatus(): GeminiNanoStatus {
        _status.value = GeminiNanoStatus.Checking
        return try {
            val featureStatus = generativeModel.checkStatus()
            val newStatus = when (featureStatus) {
                FeatureStatus.AVAILABLE -> GeminiNanoStatus.Available
                FeatureStatus.DOWNLOADABLE -> GeminiNanoStatus.Downloadable(0)
                FeatureStatus.DOWNLOADING -> GeminiNanoStatus.Downloading
                FeatureStatus.UNAVAILABLE -> GeminiNanoStatus.Unavailable
                else -> GeminiNanoStatus.Unavailable
            }
            _status.value = newStatus
            newStatus
        } catch (e: Exception) {
            val errStatus = GeminiNanoStatus.Error(e.message ?: "Failed to check Gemini Nano status")
            _status.value = errStatus
            errStatus
        }
    }

    suspend fun downloadNanoModel(onProgress: (Long) -> Unit = {}) {
        _status.value = GeminiNanoStatus.Downloading
        try {
            generativeModel.download().collect { downloadStatus ->
                when (downloadStatus) {
                    is DownloadStatus.DownloadStarted -> {
                        Log.d(TAG, "Starting download for Gemini Nano...")
                    }
                    is DownloadStatus.DownloadProgress -> {
                        _status.value = GeminiNanoStatus.Downloadable(downloadStatus.totalBytesDownloaded)
                        onProgress(downloadStatus.totalBytesDownloaded)
                    }
                    is DownloadStatus.DownloadCompleted -> {
                        Log.d(TAG, "Gemini Nano download completed successfully.")
                        _status.value = GeminiNanoStatus.Available
                    }
                    is DownloadStatus.DownloadFailed -> {
                        Log.e(TAG, "Gemini Nano download failed: ${downloadStatus.e.message}")
                        _status.value = GeminiNanoStatus.Error(downloadStatus.e.message ?: "Download failed")
                    }
                }
            }
        } catch (e: Exception) {
            _status.value = GeminiNanoStatus.Error(e.message ?: "Download exception")
        }
    }

    suspend fun analyzeFace(face: Bitmap): FaceAttributeAnalysis? {
        val promptText = "Analyze the facial expression, age group, accessories, and image clarity of this detected face."

        return try {
            val isStructuredAvailable = generativeModel.isStructuredOutputFeatureAvailable()
            if (isStructuredAvailable) {
                val baseRequest = GenerateContentRequest.Builder(
                    ImagePart(face),
                    TextPart(promptText)
                ).build()
                val typedRequest = generateTypedContentRequest(
                    baseRequest,
                    FaceAttributeAnalysis::class
                )
                val response = generativeModel.generateContent(typedRequest)
                response.candidates.firstOrNull()?.response
            } else {
                // Fallback to unstructured prompt
                val baseRequest = GenerateContentRequest.Builder(
                    ImagePart(face),
                    TextPart(promptText)
                ).build()
                val response = generativeModel.generateContent(baseRequest)
                val textOutput = response.candidates.firstOrNull()?.text ?: ""
                FaceAttributeAnalysis(summary = textOutput)
            }
        } catch (e: GenAiException) {
            Log.e(TAG, "Gemini Nano analysis error: ${e.message} (code: ${e.errorCode})")
            FaceAttributeAnalysis(summary = "Analysis error: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error generating face analysis: ${e.message}")
            FaceAttributeAnalysis(summary = "Error: ${e.message}")
        }
    }

    suspend fun analyzeTextWithPrefixCaching(dynamicQuery: String): String {
        return try {
            val response = generativeModel.generateContent(
                generateContentRequest(
                    TextPart(dynamicQuery)
                ) {
                    promptPrefix = PromptPrefix(SYSTEM_PROMPT_PREFIX)
                }
            )
            response.candidates.firstOrNull()?.text ?: "No response generated."
        } catch (e: Exception) {
            "Analysis error: ${e.message}"
        }
    }

    override fun close() {
        _generativeModel?.close()
        _generativeModel = null
    }
}
