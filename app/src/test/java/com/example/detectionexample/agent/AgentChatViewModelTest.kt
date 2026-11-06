package com.example.detectionexample.agent

import androidx.lifecycle.SavedStateHandle
import com.example.detectionexample.util.MainDispatcherRule
import com.google.mlkit.genai.agent.AgentEvent
import com.google.mlkit.genai.agent.AgentSession
import com.google.mlkit.genai.common.GenAiException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AgentChatViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var mockAgentManager: FaceRecognitionAgentManager
    private lateinit var mockSession: AgentSession
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var viewModel: AgentChatViewModel

    @Before
    fun setUp() {
        mockAgentManager = mockk(relaxed = true)
        mockSession = mockk(relaxed = true)
        savedStateHandle = SavedStateHandle(mapOf("face_agent_session_id" to "test-session-123"))

        every { mockAgentManager.getSession("test-session-123") } returns mockSession

        viewModel = AgentChatViewModel(
            agentManager = mockAgentManager,
            savedStateHandle = savedStateHandle,
        )
    }

    @Test
    fun initialState_isIdleWithSessionId() {
        val state = viewModel.uiState.value
        assertEquals("test-session-123", viewModel.sessionId)
        assertTrue(state.messages.isEmpty())
        assertFalse(state.isRunning)
        assertNull(state.activeToolName)
        assertNull(state.errorMessage)
        assertEquals("", state.currentStreamingResponse)
    }

    @Test
    fun sendMessage_whenBlank_doesNotEmitMessage() {
        viewModel.sendMessage("   ")
        val state = viewModel.uiState.value
        assertTrue(state.messages.isEmpty())
        assertFalse(state.isRunning)
    }

    @Test
    fun sendMessage_collectsFunctionCallAndResponseAndTextMessage() = runTest {
        val callEvent = AgentEvent.FunctionCall(
            name = "searchPersons",
            args = mapOf("query" to "Alice"),
        )
        val responseEvent = AgentEvent.FunctionResponse(
            name = "searchPersons",
            response = mapOf("count" to 1),
        )
        val textEvent = AgentEvent.TextMessage(
            text = "Found Alice in registered persons.",
            isPartial = false,
        )

        every { mockSession.runAsync(any()) } returns flowOf(callEvent, responseEvent, textEvent)

        viewModel.sendMessage("Find Alice")

        val state = viewModel.uiState.value
        assertFalse(state.isRunning)
        assertNull(state.activeToolName)
        assertNull(state.errorMessage)
        assertEquals("", state.currentStreamingResponse)

        // Expected messages: User, ToolCall, ToolResponse, Assistant
        assertEquals(4, state.messages.size)
        assertEquals("Find Alice", state.messages[0].content)
        assertTrue(state.messages[0].role is AgentMessageRole.User)

        assertTrue(state.messages[1].role is AgentMessageRole.ToolCall)
        assertEquals("Calling tool: searchPersons({query=Alice})", state.messages[1].content)

        assertTrue(state.messages[2].role is AgentMessageRole.ToolResponse)
        assertEquals("Tool searchPersons returned: {count=1}", state.messages[2].content)

        assertTrue(state.messages[3].role is AgentMessageRole.Assistant)
        assertEquals("Found Alice in registered persons.", state.messages[3].content)
    }

    @Test
    fun sendMessage_whenGenAiExceptionNotAvailable_setsDescriptiveErrorMessage() = runTest {
        val exception = GenAiException(
            "AICore is not installed",
            null,
            GenAiException.ErrorCode.NOT_AVAILABLE,
        )
        every { mockSession.runAsync(any()) } returns flow { throw exception }

        viewModel.sendMessage("Who is enrolled?")

        val state = viewModel.uiState.value
        assertFalse(state.isRunning)
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("not available"))
    }

    @Test
    fun sendMessage_whenGenAiExceptionMaxTurnsExceeded_setsTurnLimitErrorMessage() = runTest {
        val exception = GenAiException(
            "Turn limit exceeded",
            null,
            GenAiException.ErrorCode.MAX_TURNS_EXCEEDED,
        )
        every { mockSession.runAsync(any()) } returns flow { throw exception }

        viewModel.sendMessage("Infinite loop query")

        val state = viewModel.uiState.value
        assertFalse(state.isRunning)
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("maximum tool reasoning turns"))
    }

    @Test
    fun clearChat_deletesSessionAndResetsUiState() = runTest {
        coEvery { mockSession.delete() } returns Unit

        viewModel.clearChat()

        val state = viewModel.uiState.value
        assertTrue(state.messages.isEmpty())
        assertFalse(state.isRunning)
        assertNull(state.errorMessage)
        coVerify(exactly = 1) { mockSession.delete() }
    }
}
