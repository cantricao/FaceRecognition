package com.example.detectionexample.agent

import java.util.UUID

sealed interface AgentMessageRole {
    data object User : AgentMessageRole
    data object Assistant : AgentMessageRole
    data class ToolCall(val name: String, val args: Map<String, Any?>) : AgentMessageRole
    data class ToolResponse(val name: String, val response: Map<String, Any?>) : AgentMessageRole
}

data class AgentChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: AgentMessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
)

data class HitlRequest(
    val toolName: String,
    val args: Map<String, Any?>,
    val onResponse: (Boolean) -> Unit,
)

data class AgentChatUiState(
    val messages: List<AgentChatMessage> = emptyList(),
    val currentStreamingResponse: String = "",
    val activeToolName: String? = null,
    val isRunning: Boolean = false,
    val pendingHitlRequest: HitlRequest? = null,
    val errorMessage: String? = null,
)
