package com.example.detectionexample.credentials.digital

import android.content.Context
import com.example.detectionexample.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VerifiedEmailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var mockManager: VerifiedEmailManager
    private lateinit var mockContext: Context
    private lateinit var viewModel: VerifiedEmailViewModel

    @Before
    fun setUp() {
        mockManager = mockk(relaxed = true)
        mockContext = mockk(relaxed = true)
        viewModel = VerifiedEmailViewModel(mockManager)
    }

    @Test
    fun initialState_isIdle() {
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.verifiedUser)
        assertNull(state.errorMessage)
        assertFalse(state.isPasskeyCreated)
    }

    @Test
    fun verifyEmail_whenSuccessful_updatesUiStateWithVerifiedUser() = runTest {
        val expectedUser = VerifiedUserInfo(
            email = "test.user@gmail.com",
            displayName = "Test User",
            isEmailVerified = true
        )
        coEvery { mockManager.requestVerifiedEmail(eq(mockContext), any()) } returns Result.success(expectedUser)

        viewModel.verifyEmail(mockContext)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(expectedUser, state.verifiedUser)
        assertNull(state.errorMessage)
    }

    @Test
    fun verifyEmail_whenFailure_updatesUiStateWithErrorMessage() = runTest {
        coEvery { mockManager.requestVerifiedEmail(eq(mockContext), any()) } returns Result.failure(
            RuntimeException("Verification dialog cancelled")
        )

        viewModel.verifyEmail(mockContext)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.verifiedUser)
        assertEquals("Verification dialog cancelled", state.errorMessage)
    }

    @Test
    fun markPasskeyCreated_and_resetState_behaveCorrectly() {
        viewModel.markPasskeyCreated()
        assertTrue(viewModel.uiState.value.isPasskeyCreated)

        viewModel.resetState()
        val resetState = viewModel.uiState.value
        assertFalse(resetState.isPasskeyCreated)
        assertNull(resetState.verifiedUser)
    }
}
