package com.example.detectionexample.credentials.digital

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VerifiedEmailUiState(
    val isLoading: Boolean = false,
    val verifiedUser: VerifiedUserInfo? = null,
    val errorMessage: String? = null,
    val isPasskeyCreated: Boolean = false,
)

/**
 * ViewModel managing the state of OTP-less verified email retrieval.
 */
@HiltViewModel
class VerifiedEmailViewModel @Inject constructor(
    val verifiedEmailManager: VerifiedEmailManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(VerifiedEmailUiState())
    val uiState: StateFlow<VerifiedEmailUiState> = _uiState.asStateFlow()

    /**
     * Triggers the Credential Manager OpenID4VP digital credential request.
     */
    fun verifyEmail(activityContext: Context) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = verifiedEmailManager.requestVerifiedEmail(activityContext)
            result.onSuccess { userInfo ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        verifiedUser = userInfo,
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.localizedMessage ?: "Failed to verify email credential."
                    )
                }
            }
        }
    }

    fun markPasskeyCreated() {
        _uiState.update { it.copy(isPasskeyCreated = true) }
    }

    fun resetState() {
        _uiState.value = VerifiedEmailUiState()
    }
}
