package com.example.detectionexample.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.concurrent.Executors
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

data class ChatMessage(
    val sender: String,
    val text: String
)

data class ChatState(
    val ready: Boolean = false,
    val busy: Boolean = false,
    val currentReply: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val error: String? = null,
    val modelExists: Boolean = false,
    val modelPath: String = ""
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    app: Application
) : AndroidViewModel(app) {

    private val executor = Executors.newSingleThreadExecutor()
    private val scope = CoroutineScope(
        SupervisorJob() + executor.asCoroutineDispatcher()
    )
    private val mutex = Mutex()
    private var engine: Engine? = null
    private var conversation: Conversation? = null

    val defaultModelFile: File by lazy {
        File(getApplication<Application>().filesDir, "Qwen3-0.6B.litertlm")
    }

    private val _state = MutableStateFlow(
        ChatState(
            modelPath = defaultModelFile.absolutePath,
            modelExists = defaultModelFile.exists() && defaultModelFile.length() > 0
        )
    )
    val state: StateFlow<ChatState> = _state.asStateFlow()

    fun checkModelExists(): Boolean {
        val exists = defaultModelFile.exists() && defaultModelFile.length() > 0
        _state.update {
            it.copy(
                modelExists = exists,
                modelPath = defaultModelFile.absolutePath
            )
        }
        return exists
    }

    fun load(modelPath: String, backend: Backend = Backend.GPU()) {
        scope.launch {
            mutex.withLock {
                val file = File(modelPath)
                if (!file.exists() || file.length() == 0L) {
                    _state.update {
                        it.copy(
                            ready = false,
                            modelExists = false,
                            error = "Model file not found at: $modelPath"
                        )
                    }
                    return@launch
                }

                if (engine?.engineConfig?.modelPath == modelPath) return@launch
                release()

                try {
                    val config = EngineConfig(
                        modelPath = modelPath,
                        backend = backend,
                        cacheDir = getApplication<Application>().cacheDir.path
                    )
                    val newEngine = Engine(config).also { it.initialize() }
                    engine = newEngine

                    val samplerConfig = SamplerConfig(
                        topK = 10,
                        topP = 0.95,
                        temperature = 0.8
                    )
                    val newConversation = newEngine.createConversation(
                        ConversationConfig(
                            systemInstruction = Contents.of(
                                "You are a helpful and concise on-device AI assistant."
                            ),
                            samplerConfig = samplerConfig
                        )
                    )
                    conversation = newConversation

                    _state.update {
                        it.copy(
                            ready = true,
                            modelExists = true,
                            error = null
                        )
                    }
                } catch (e: Exception) {
                    release()
                    _state.update {
                        it.copy(
                            error = e.message ?: "Failed to initialize LiteRT-LM engine."
                        )
                    }
                }
            }
        }
    }

    fun send(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        scope.launch {
            if (!_state.value.ready || _state.value.busy) return@launch

            _state.update {
                it.copy(
                    busy = true,
                    currentReply = "",
                    error = null,
                    messages = it.messages + ChatMessage(sender = "user", text = trimmed)
                )
            }

            try {
                val replies = checkNotNull(conversation).sendMessageAsync(trimmed)
                var accumulated = ""
                replies.collect { message ->
                    accumulated += message.toString()
                    _state.update { it.copy(currentReply = accumulated) }
                }
                _state.update {
                    it.copy(
                        currentReply = "",
                        messages = it.messages + ChatMessage(sender = "model", text = accumulated)
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }

    private suspend fun release() {
        _state.update { it.copy(ready = false) }
        while (_state.value.busy) {
            conversation?.cancelProcess()
            delay(100)
        }
        conversation?.close()
        conversation = null
        engine?.close()
        engine = null
        _state.update {
            it.copy(
                ready = false,
                busy = false,
                currentReply = "",
                error = null
            )
        }
    }

    override fun onCleared() {
        scope.launch {
            mutex.withLock { release() }
            executor.shutdown()
        }.invokeOnCompletion { scope.cancel() }
    }
}
