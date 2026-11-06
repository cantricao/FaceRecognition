package com.example.detectionexample.agent

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.genai.agent.AgentEvent
import com.google.mlkit.genai.agent.AgentSession
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.prompt.Content
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel managing multi-turn conversational Agent sessions, observing
 * [AgentEvent] tool calls and responses, and handling [GenAiException] errors.
 */
@HiltViewModel
class AgentChatViewModel @Inject constructor(
    private val agentManager: FaceRecognitionAgentManager,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    companion object {
        private const val KEY_SESSION = "face_agent_session_id"
    }

    val sessionId: String = savedStateHandle[KEY_SESSION] ?: UUID.randomUUID().toString().also {
        savedStateHandle[KEY_SESSION] = it
    }

    private val session: AgentSession by lazy {
        agentManager.getSession(sessionId)
    }

    private val _uiState = MutableStateFlow(AgentChatUiState())
    val uiState: StateFlow<AgentChatUiState> = _uiState.asStateFlow()

    fun sendMessage(prompt: String) {
        if (prompt.isBlank() || _uiState.value.isRunning) return
        val trimmedPrompt = prompt.trim()
        val userMessage = AgentChatMessage(
            role = AgentMessageRole.User,
            content = trimmedPrompt,
        )

        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                isRunning = true,
                errorMessage = null,
                currentStreamingResponse = "",
            )
        }

        viewModelScope.launch {
            try {
                val content = Content.builder().text(trimmedPrompt).build()
                var fullResponse = ""

                session.runAsync(content).collect { event ->
                    when (event) {
                        is AgentEvent.FunctionCall -> {
                            val callMessage = AgentChatMessage(
                                role = AgentMessageRole.ToolCall(event.name, event.args),
                                content = "Calling tool: ${event.name}(${event.args})",
                            )
                            _uiState.update { state ->
                                state.copy(
                                    messages = state.messages + callMessage,
                                    activeToolName = event.name,
                                )
                            }
                        }
                        is AgentEvent.FunctionResponse -> {
                            val responseMessage = AgentChatMessage(
                                role = AgentMessageRole.ToolResponse(event.name, event.response),
                                content = "Tool ${event.name} returned: ${event.response}",
                            )
                            _uiState.update { state ->
                                state.copy(
                                    messages = state.messages + responseMessage,
                                    activeToolName = null,
                                )
                            }
                        }
                        is AgentEvent.TextMessage -> {
                            fullResponse += event.text
                            _uiState.update { state ->
                                state.copy(currentStreamingResponse = fullResponse)
                            }
                        }
                        else -> {
                            // Required: AgentEvent is non-exhaustive for forward compatibility
                        }
                    }
                }

                if (fullResponse.isNotBlank()) {
                    val assistantMessage = AgentChatMessage(
                        role = AgentMessageRole.Assistant,
                        content = fullResponse,
                    )
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages + assistantMessage,
                            currentStreamingResponse = "",
                            isRunning = false,
                        )
                    }
                } else {
                    _uiState.update { it.copy(isRunning = false, currentStreamingResponse = "") }
                }
            } catch (e: GenAiException) {
                val errorMsg = when (e.errorCode) {
                    GenAiException.ErrorCode.MAX_TURNS_EXCEEDED ->
                        "Agent reached maximum tool reasoning turns (10) without completing."
                    GenAiException.ErrorCode.NOT_AVAILABLE ->
                        "Gemini Nano on-device model or AICore is not available on this device."
                    GenAiException.ErrorCode.BUSY ->
                        "AICore system is busy, or battery/thermal quota has been reached."
                    else -> e.localizedMessage ?: "AI Agent error (${e.errorCode})"
                }
                _uiState.update { state ->
                    state.copy(
                        isRunning = false,
                        errorMessage = errorMsg,
                        currentStreamingResponse = "",
                    )
                }
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(
                        isRunning = false,
                        errorMessage = e.localizedMessage ?: "Unexpected error during execution",
                        currentStreamingResponse = "",
                    )
                }
            }
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            try {
                session.delete()
            } catch (_: Exception) {
            }
            _uiState.update { AgentChatUiState() }
        }
    }

    override fun onCleared() {
        super.onCleared()
        session.close()
    }
}
