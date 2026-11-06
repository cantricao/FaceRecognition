package com.example.detectionexample.credentials.digital

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.DigitalCredential
import androidx.credentials.ExperimentalDigitalCredentialApi
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetDigitalCredentialOption
import androidx.credentials.exceptions.GetCredentialException
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinates on-device Verified Email retrieval using the Android Credential Manager
 * Digital Credentials Verifier API and OpenID4VP (OpenID for Verifiable Presentations).
 *
 * Allows friction-free, OTP-less verification of user email addresses issued by Google Accounts.
 */
@Singleton
class VerifiedEmailManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>,
) {
    companion object {
        private const val TAG = "VerifiedEmailManager"

        val KEY_VERIFIED_EMAIL = stringPreferencesKey("verified_email")
        val KEY_VERIFIED_NAME = stringPreferencesKey("verified_display_name")

        /**
         * Generates a 32-byte cryptographically secure, unguessable random nonce
         * formatted as URL-safe Base64 without padding to prevent replay attacks.
         */
        fun generateSecureRandomNonce(): String {
            val randomBytes = ByteArray(32)
            SecureRandom().nextBytes(randomBytes)
            return Base64.encodeToString(randomBytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        }

        /**
         * Constructs an OpenID4VP request JSON for the digital credentials verifier.
         */
        fun createOpenId4VpRequest(nonce: String): String {
            return """
                {
                  "requests": [
                    {
                      "protocol": "openid4vp-v1-unsigned",
                      "data": {
                        "response_type": "vp_token",
                        "response_mode": "dc_api",
                        "nonce": "$nonce",
                        "dcql_query": {
                          "credentials": [
                            {
                              "id": "user_info_query",
                              "format": "dc+sd-jwt",
                              "meta": {
                                "vct_values": ["UserInfoCredential"]
                              },
                              "claims": [
                                {"path": ["email"]},
                                {"path": ["name"]},
                                {"path": ["given_name"]},
                                {"path": ["family_name"]},
                                {"path": ["picture"]},
                                {"path": ["hd"]},
                                {"path": ["email_verified"]}
                              ]
                            }
                          ]
                        }
                      }
                    }
                  ]
                }
            """.trimIndent()
        }
    }

    internal var credentialManager: CredentialManager = CredentialManager.create(context)

    /**
     * Presents the Credential Manager system UI to the user to request their verified email and name.
     *
     * @param activityContext Context of the active Activity required by Credential Manager UI.
     * @param customNonce Optional predefined nonce; generates a secure random nonce if null.
     */
    @OptIn(ExperimentalDigitalCredentialApi::class)
    suspend fun requestVerifiedEmail(
        activityContext: Context,
        customNonce: String? = null,
    ): Result<VerifiedUserInfo> {
        val nonce = customNonce ?: generateSecureRandomNonce()
        val openId4vpRequest = createOpenId4VpRequest(nonce)

        return try {
            val getDigitalCredentialOption = GetDigitalCredentialOption(requestJson = openId4vpRequest)
            val request = GetCredentialRequest(listOf(getDigitalCredentialOption))

            Log.d(TAG, "Requesting Digital Credential with nonce: $nonce")
            val result = credentialManager.getCredential(activityContext, request)

            when (val credential = result.credential) {
                is DigitalCredential -> {
                    val responseJsonString = credential.credentialJson
                    Log.i(TAG, "Successfully received digital credential response.")

                    // Parse client-side claims for preliminary UI display
                    val userInfo = parseCredentialResponse(responseJsonString, nonce)

                    // Persist verified user details locally
                    saveVerifiedUser(userInfo.email, userInfo.displayName)

                    // TODO: Developer Note - Full cryptographic validation must be completed on your server!
                    // Send `responseJsonString` and `nonce` to your backend endpoint for:
                    // 1. Issuer verification (iss == https://verifiablecredentials-pa.googleapis.com)
                    // 2. Public key signature verification against Google's JWKs endpoint
                    // 3. Nonce freshness check to prevent replay attacks
                    // 4. Presenter identity binding confirmation via the 'cnf' claim

                    Result.success(userInfo)
                }
                else -> {
                    val errorMsg = "Unexpected credential returned: ${credential::class.java.name}"
                    Log.w(TAG, errorMsg)
                    Result.failure(IllegalStateException(errorMsg))
                }
            }
        } catch (e: GetCredentialException) {
            Log.w(TAG, "GetCredentialException during verified email retrieval: ${e.message}")
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected exception during verified email retrieval", e)
            Result.failure(e)
        }
    }

    /**
     * Extracts and parses the SD-JWT claims from the Credential Manager response JSON.
     * Supports both W3C standard (nested `data.vp_token`) and legacy root `vp_token` formats.
     */
    fun parseCredentialResponse(responseJsonString: String, nonce: String): VerifiedUserInfo {
        return try {
            val responseData = JSONObject(responseJsonString)
            val dataObject = if (responseData.has("data")) {
                responseData.getJSONObject("data")
            } else {
                responseData
            }

            val vpToken = dataObject.getJSONObject("vp_token")
            val credentialId = vpToken.keys().next()
            val rawSdJwt = vpToken.getJSONArray(credentialId).getString(0)

            val claims = SdJwtParser.parse(rawSdJwt)

            val email = claims.optString("email", "")
            val name = claims.optString("name", email)
            val givenName = claims.optString("given_name").takeIf { it.isNotEmpty() }
            val familyName = claims.optString("family_name").takeIf { it.isNotEmpty() }
            val picture = claims.optString("picture").takeIf { it.isNotEmpty() }
            val emailVerified = claims.optBoolean("email_verified", false)
            val hd = claims.optString("hd").takeIf { it.isNotEmpty() }

            VerifiedUserInfo(
                email = email,
                displayName = name,
                givenName = givenName,
                familyName = familyName,
                pictureUrl = picture,
                isEmailVerified = emailVerified,
                hostedDomain = hd,
                nonce = nonce,
                rawSdJwt = rawSdJwt,
                rawResponseJson = responseJsonString,
            )
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing digital credential response JSON", e)
            VerifiedUserInfo(
                email = "",
                displayName = "",
                nonce = nonce,
                rawResponseJson = responseJsonString
            )
        }
    }

    private suspend fun saveVerifiedUser(email: String, displayName: String) {
        try {
            dataStore.edit { preferences ->
                preferences[KEY_VERIFIED_EMAIL] = email
                preferences[KEY_VERIFIED_NAME] = displayName
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to persist verified user preferences", e)
        }
    }
}
