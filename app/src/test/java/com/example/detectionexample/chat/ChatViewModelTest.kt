package com.example.detectionexample.chat

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ChatViewModelTest {

    private lateinit var app: Application
    private lateinit var viewModel: ChatViewModel

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        viewModel = ChatViewModel(app)
    }

    @Test
    fun initialState_hasExpectedDefaults() {
        val state = viewModel.state.value
        assertFalse(state.ready)
        assertFalse(state.busy)
        assertTrue(state.messages.isEmpty())
        assertTrue(state.currentReply.isEmpty())
        assertNotNull(state.modelPath)
    }

    @Test
    fun checkModelExists_whenFileDoesNotExist_setsModelExistsFalse() {
        if (viewModel.defaultModelFile.exists()) {
            viewModel.defaultModelFile.delete()
        }

        val exists = viewModel.checkModelExists()

        assertFalse(exists)
        assertFalse(viewModel.state.value.modelExists)
    }

    @Test
    fun checkModelExists_whenFileExists_setsModelExistsTrue() {
        val modelFile = viewModel.defaultModelFile
        modelFile.writeText("dummy-litert-content")

        try {
            val exists = viewModel.checkModelExists()
            assertTrue(exists)
            assertTrue(viewModel.state.value.modelExists)
        } finally {
            modelFile.delete()
        }
    }

    @Test
    fun chatMessage_dataClass_preservesFields() {
        val msg = ChatMessage(sender = "user", text = "Hello LiteRT-LM")
        assertEquals("user", msg.sender)
        assertEquals("Hello LiteRT-LM", msg.text)
    }
}
