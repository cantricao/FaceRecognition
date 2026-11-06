package com.example.detectionexample.agent

import android.content.Context
import com.google.mlkit.genai.agent.Agent
import com.google.mlkit.genai.agent.AgentEvent
import com.google.mlkit.genai.agent.AgentRunner
import com.google.mlkit.genai.agent.AgentSession
import com.google.mlkit.genai.agent.AppFunctionAgentTools
import com.google.mlkit.genai.prompt.Content
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On-device autonomous agent coordinator using Google ML Kit GenAI Agent API.
 * Orchestrates multi-turn reasoning loops and typed AppFunctions tool calls via Gemini Nano.
 */
@Singleton
class FaceRecognitionAgentManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        const val AGENT_NAME = "FaceRecognitionAgent"
        const val AGENT_INSTRUCTIONS = """
            You are FaceAgent, an autonomous on-device assistant for the FaceRecognition app.
            You can interact with user face data, verify enrollments, list enrolled persons,
            search for identities, delete records, and open the live camera detection viewfinder.
            Always provide concise, helpful answers. When calling tools, explain what action is being taken.
        """
    }

    private var agentRunner: AgentRunner? = null

    /**
     * Initializes or returns the cached [AgentRunner] using the declarative AppFunctions path.
     */
    fun getOrCreateRunner(): AgentRunner {
        return agentRunner ?: synchronized(this) {
            agentRunner ?: run {
                val agent = Agent.builder()
                    .setName(AGENT_NAME)
                    .setInstructions(AGENT_INSTRUCTIONS)
                    .enableAppFunctions(true)
                    .addAppFunctionIds("searchPersons", "getRegisteredPersonCount", "deletePersonById", "openLiveDetection")
                    .setMaxTurns(10)
                    .build()

                AgentRunner(context, agent).also { agentRunner = it }
            }
        }
    }

    /**
     * Initializes an [AgentRunner] with Human-In-The-Loop (HITL) confirmation wrapping
     * for sensitive tools such as deletion.
     */
    suspend fun createHitlRunner(
        onConfirmSensitiveTool: suspend (toolName: String, args: Map<String, Any>) -> Boolean
    ): AgentRunner {
        val discoveredTools = AppFunctionAgentTools.discover(context)
        val decoratedTools = discoveredTools.map { tool ->
            if (tool.name.contains("delete", ignoreCase = true)) {
                HitlConfirmationTool(tool, onConfirmSensitiveTool)
            } else {
                tool
            }
        }

        val agent = Agent.builder()
            .setName(AGENT_NAME)
            .setInstructions(AGENT_INSTRUCTIONS)
            .addTools(decoratedTools)
            .setMaxTurns(10)
            .build()

        return AgentRunner(context, agent)
    }

    /**
     * Creates or retrieves a persistent multi-turn session.
     */
    fun getSession(sessionId: String): AgentSession {
        return getOrCreateRunner().getOrCreateSession(sessionId)
    }

    /**
     * Executes a single-turn query ephemerally without persisting dialogue history.
     */
    fun runSingleTurn(prompt: String): Flow<AgentEvent> {
        val content = Content.builder().text(prompt).build()
        return getOrCreateRunner().runSingleTurnAsync(content)
    }

    /**
     * Releases active runner and cleans up in-flight resources.
     */
    fun close() {
        synchronized(this) {
            agentRunner?.close()
            agentRunner = null
        }
    }
}
