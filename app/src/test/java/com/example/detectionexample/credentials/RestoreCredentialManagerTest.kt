package com.example.detectionexample.credentials

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CreateRestoreCredentialRequest
import androidx.credentials.CreateRestoreCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetRestoreCredentialOption
import androidx.credentials.RestoreCredential
import androidx.credentials.exceptions.domerrors.NotSupportedError
import androidx.credentials.exceptions.restorecredential.CreateRestoreCredentialDomException
import androidx.credentials.exceptions.restorecredential.E2eeUnavailableException
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.preferencesOf
import com.example.detectionexample.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RestoreCredentialManagerTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var mockContext: Context
    private lateinit var mockDataStore: DataStore<Preferences>
    private lateinit var mockCredentialManager: CredentialManager
    private lateinit var restoreManager: RestoreCredentialManager

    @Before
    fun setUp() {
        mockContext = mockk(relaxed = true)
        mockDataStore = mockk(relaxed = true) {
            every { data } returns flowOf(preferencesOf())
        }
        mockCredentialManager = mockk(relaxed = true)

        restoreManager = RestoreCredentialManager(mockContext, mockDataStore)
        restoreManager.credentialManager = mockCredentialManager
    }

    @Test
    fun createRestoreKey_primaryCloudBackupSucceeds() = runTest {
        val expectedResponse = CreateRestoreCredentialResponse("""{"id":"test-key"}""")
        val requestSlot = slot<CreateRestoreCredentialRequest>()

        coEvery {
            mockCredentialManager.createCredential(eq(mockContext), capture(requestSlot))
        } returns expectedResponse

        val result = restoreManager.createRestoreKey(mockContext)

        assertTrue(result.isSuccess)
        assertEquals(expectedResponse, result.getOrNull())
        assertTrue("Cloud backup should be enabled for primary attempt", requestSlot.captured.isCloudBackupEnabled)
    }

    @Test
    fun createRestoreKey_whenE2eeUnavailable_retriesWithCloudBackupDisabled() = runTest {
        val capturedRequests = mutableListOf<CreateRestoreCredentialRequest>()
        val fallbackResponse = CreateRestoreCredentialResponse("""{"id":"fallback-key"}""")

        coEvery {
            mockCredentialManager.createCredential(eq(mockContext), any())
        } answers {
            val req = secondArg<CreateRestoreCredentialRequest>()
            capturedRequests.add(req)
            if (req.isCloudBackupEnabled) {
                throw E2eeUnavailableException("Device lacks screen lock or backup")
            } else {
                fallbackResponse
            }
        }

        val result = restoreManager.createRestoreKey(mockContext)

        assertTrue(result.isSuccess)
        assertEquals(fallbackResponse, result.getOrNull())
        assertEquals(2, capturedRequests.size)
        assertTrue("First attempt must have isCloudBackupEnabled = true", capturedRequests[0].isCloudBackupEnabled)
        assertEquals(
            "Fallback attempt must have isCloudBackupEnabled = false",
            false,
            capturedRequests[1].isCloudBackupEnabled
        )
    }

    @Test
    fun createRestoreKey_whenDomException_returnsFailure() = runTest {
        coEvery {
            mockCredentialManager.createCredential(eq(mockContext), any())
        } throws CreateRestoreCredentialDomException(NotSupportedError(), "WebAuthn schema error")

        val result = restoreManager.createRestoreKey(mockContext)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is CreateRestoreCredentialDomException)
    }

    @Test
    fun getRestoreKey_whenRestoreCredentialReturned_returnsSuccess() = runTest {
        val mockRestoreCredential = mockk<RestoreCredential>(relaxed = true)
        val mockGetResponse = mockk<GetCredentialResponse>(relaxed = true) {
            every { credential } returns mockRestoreCredential
        }
        val requestSlot = slot<GetCredentialRequest>()

        coEvery {
            mockCredentialManager.getCredential(eq(mockContext), capture(requestSlot))
        } returns mockGetResponse

        val result = restoreManager.getRestoreKey(mockContext)

        assertTrue(result.isSuccess)
        assertEquals(mockRestoreCredential, result.getOrNull())

        val credentialOptions = requestSlot.captured.credentialOptions
        assertEquals(1, credentialOptions.size)
        assertTrue("Option must be GetRestoreCredentialOption", credentialOptions[0] is GetRestoreCredentialOption)
    }

    @Test
    fun clearRestoreKey_callsClearWith_TYPE_CLEAR_RESTORE_CREDENTIAL() = runTest {
        val requestSlot = slot<ClearCredentialStateRequest>()
        coEvery {
            mockCredentialManager.clearCredentialState(capture(requestSlot))
        } returns Unit

        val result = restoreManager.clearRestoreKey()

        assertTrue(result.isSuccess)
        assertEquals(
            ClearCredentialStateRequest.TYPE_CLEAR_RESTORE_CREDENTIAL,
            requestSlot.captured.requestType
        )
    }
}
