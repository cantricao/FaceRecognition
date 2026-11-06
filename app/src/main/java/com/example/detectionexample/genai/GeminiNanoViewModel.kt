package com.example.detectionexample.genai

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class FaceAnalysisUiState(
    val status: GeminiNanoStatus = GeminiNanoStatus.Checking,
    val isAnalyzing: Boolean = false,
    val analysisResult: FaceAttributeAnalysis? = null,
    val textSummary: String? = null,
    val error: String? = null
)

@HiltViewModel
class GeminiNanoViewModel @Inject constructor(
    private val nanoFaceAnalyzer: GeminiNanoFaceAnalyzer
) : ViewModel() {

    private val _uiState = MutableStateFlow(FaceAnalysisUiState())
    val uiState: StateFlow<FaceAnalysisUiState> = _uiState.asStateFlow()

    init {
        checkStatus()
    }

    fun checkStatus() {
        viewModelScope.launch {
            val status = nanoFaceAnalyzer.checkStatus()
            _uiState.update { it.copy(status = status) }
        }
    }

    fun downloadModel() {
        viewModelScope.launch {
            nanoFaceAnalyzer.downloadNanoModel { progressBytes ->
                _uiState.update {
                    it.copy(status = GeminiNanoStatus.Downloadable(progressBytes))
                }
            }
            _uiState.update { it.copy(status = nanoFaceAnalyzer.status.value) }
        }
    }

    fun analyzeFace(faceBitmap: Bitmap) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzing = true, error = null) }
            val result = withContext(Dispatchers.IO) {
                nanoFaceAnalyzer.analyzeFace(faceBitmap)
            }
            _uiState.update {
                it.copy(
                    isAnalyzing = false,
                    analysisResult = result,
                    textSummary = result?.summary
                )
            }
        }
    }

    fun analyzeTextWithPrefixCaching(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzing = true, error = null) }
            val result = withContext(Dispatchers.IO) {
                nanoFaceAnalyzer.analyzeTextWithPrefixCaching(query)
            }
            _uiState.update {
                it.copy(
                    isAnalyzing = false,
                    textSummary = result
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        nanoFaceAnalyzer.close()
    }
}
