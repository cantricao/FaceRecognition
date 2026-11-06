package com.example.detectionexample.credentials.digital

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.DigitalCredential
import androidx.credentials.ExperimentalDigitalCredentialApi
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetDigitalCredentialOption
import androidx.credentials.exceptions.GetCredentialException
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
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Base64

@OptIn(ExperimentalDigitalCredentialApi::class)
@RunWith(RobolectricTestRunner::class)
class VerifiedEmailManagerTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var mockContext: Context
    private lateinit var mockDataStore: DataStore<Preferences>
    private lateinit var mockCredentialManager: CredentialManager
    private lateinit var manager: VerifiedEmailManager

    private fun base64Url(str: String): String {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(str.toByteArray(Charsets.UTF_8))
    }

    private fun buildSampleSdJwt(): String {
        val header = base64Url("""{"alg":"ES256","typ":"vc+sd-jwt"}""")
        val payload = base64Url("""{"iss":"https://verifiablecredentials-pa.googleapis.com","vct":"UserInfoCredential"}""")
        val issuerJwt = "$header.$payload.sig"
        val disclosureEmail = base64Url("""["salt1","email","alex@gmail.com"]""")
        val disclosureName = base64Url("""["salt2","name","Alex Smith"]""")
        val disclosureVerified = base64Url("""["salt3","email_verified",true]""")
        return "$issuerJwt~$disclosureEmail~$disclosureName~$disclosureVerified"
    }

    @Before
    fun setUp() {
        mockContext = mockk(relaxed = true)
        mockDataStore = mockk(relaxed = true) {
            every { data } returns flowOf(preferencesOf())
        }
        mockCredentialManager = mockk(relaxed = true)

        manager = VerifiedEmailManager(mockContext, mockDataStore)
        manager.credentialManager = mockCredentialManager
    }

    @Test
    fun createOpenId4VpRequest_includesRequiredOpenId4VpFields() {
        val nonce = "test-secure-nonce-12345"
        val requestJsonString = VerifiedEmailManager.createOpenId4VpRequest(nonce)

        val json = JSONObject(requestJsonString)
        val requestsArray = json.getJSONArray("requests")
        assertEquals(1, requestsArray.length())

        val requestObj = requestsArray.getJSONObject(0)
        assertEquals("openid4vp-v1-unsigned", requestObj.getString("protocol"))

        val data = requestObj.getJSONObject("data")
        assertEquals("vp_token", data.getString("response_type"))
        assertEquals("dc_api", data.getString("response_mode"))
        assertEquals(nonce, data.getString("nonce"))

        val dcql = data.getJSONObject("dcql_query")
        val credentials = dcql.getJSONArray("credentials")
        val credObj = credentials.getJSONObject(0)
        assertEquals("user_info_query", credObj.getString("id"))
        assertEquals("dc+sd-jwt", credObj.getString("format"))
    }

    @Test
    fun requestVerifiedEmail_whenDigitalCredentialReturned_parsesUserInfo() = runTest {
        val sampleSdJwt = buildSampleSdJwt()
        val responseJson = """
            {
              "protocol": "openid4vp-v1-unsigned",
              "data": {
                "vp_token": {
                  "user_info_query": ["$sampleSdJwt"]
                }
              }
            }
        """.trimIndent()

        val mockDigitalCredential = DigitalCredential(responseJson)
        val mockResponse = mockk<GetCredentialResponse>(relaxed = true) {
            every { credential } returns mockDigitalCredential
        }
        val requestSlot = slot<GetCredentialRequest>()

        coEvery {
            mockCredentialManager.getCredential(eq(mockContext), capture(requestSlot))
        } returns mockResponse

        val result = manager.requestVerifiedEmail(mockContext, customNonce = "fixed-nonce")

        assertTrue(result.isSuccess)
        val userInfo = result.getOrNull()
        assertNotNull(userInfo)
        assertEquals("alex@gmail.com", userInfo!!.email)
        assertEquals("Alex Smith", userInfo.displayName)
        assertTrue(userInfo.isEmailVerified)

        val options = requestSlot.captured.credentialOptions
        assertEquals(1, options.size)
        assertTrue(options[0] is GetDigitalCredentialOption)
    }

    @Test
    fun requestVerifiedEmail_whenExceptionThrown_returnsFailure() = runTest {
        val mockException = mockk<GetCredentialException>(relaxed = true) {
            every { message } returns "Verification cancelled by user"
        }

        coEvery {
            mockCredentialManager.getCredential(eq(mockContext), any<GetCredentialRequest>())
        } throws mockException

        val result = manager.requestVerifiedEmail(mockContext)

        assertTrue(result.isFailure)
        assertEquals(mockException, result.exceptionOrNull())
    }

    @Test
    fun parseCredentialResponse_supportsBothStandardAndLegacyFormats() {
        val sampleSdJwt = buildSampleSdJwt()

        // 1. W3C standard format (nested under "data")
        val standardJson = """
            {
              "protocol": "openid4vp-v1-unsigned",
              "data": {
                "vp_token": {
                  "user_info_query": ["$sampleSdJwt"]
                }
              }
            }
        """.trimIndent()

        val userInfoStandard = manager.parseCredentialResponse(standardJson, "nonce1")
        assertEquals("alex@gmail.com", userInfoStandard.email)
        assertEquals("Alex Smith", userInfoStandard.displayName)

        // 2. Legacy format (direct "vp_token" at root)
        val legacyJson = """
            {
              "protocol": "openid4vp-v1-unsigned",
              "vp_token": {
                "user_info_query": ["$sampleSdJwt"]
              }
            }
        """.trimIndent()

        val userInfoLegacy = manager.parseCredentialResponse(legacyJson, "nonce2")
        assertEquals("alex@gmail.com", userInfoLegacy.email)
        assertEquals("Alex Smith", userInfoLegacy.displayName)
    }
}
