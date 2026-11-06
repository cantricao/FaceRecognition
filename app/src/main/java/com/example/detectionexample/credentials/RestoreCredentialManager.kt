package com.example.detectionexample.credentials

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CreateRestoreCredentialRequest
import androidx.credentials.CreateRestoreCredentialResponse
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetRestoreCredentialOption
import androidx.credentials.RestoreCredential
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.restorecredential.CreateRestoreCredentialDomException
import androidx.credentials.exceptions.restorecredential.E2eeUnavailableException
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the Android Credential Manager Restore Credentials lifecycle.
 *
 * Implements a resilient two-tier restoration architecture:
 * - Silent restoration during device onboarding via [com.example.detectionexample.backup.FaceRecognitionBackupAgent]
 * - Foreground failover & registration in [com.example.detectionexample.MainActivity]
 */
@Singleton
class RestoreCredentialManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>,
) {
    companion object {
        private const val TAG = "RestoreCredentialMgr"

        val KEY_HAS_SYNCED_RESTORE_CREDENTIAL = booleanPreferencesKey("has_synced_restore_credential")
        val KEY_RESTORE_TIMESTAMP = longPreferencesKey("restore_credential_timestamp")
        val KEY_RESTORED_USER_ID = stringPreferencesKey("restored_user_id")

        private const val DEFAULT_RP_ID = "com.example.detectionexample"
        private const val DEFAULT_USER_ID_BASE64 = "dXNlci1mYWNlcmVjLTEyMw=="
        private const val DEFAULT_CHALLENGE_BASE64 = "dGVzdC1jaGFsbGVuZ2UtMTIzNDU2Nzg5MDEyMzQ1Njc4OTA="

        /**
         * Valid WebAuthn JSON payload conforming to PublicKeyCredentialCreationOptionsJSON.
         */
        fun getDefaultCreationJson(
            rpId: String = DEFAULT_RP_ID,
            userIdBase64: String = DEFAULT_USER_ID_BASE64,
            userName: String = "user@facerecognition.local",
            displayName: String = "FaceRecognition User",
        ): String {
            return """
                {
                  "challenge": "$DEFAULT_CHALLENGE_BASE64",
                  "rp": {
                    "name": "FaceRecognition App",
                    "id": "$rpId"
                  },
                  "user": {
                    "id": "$userIdBase64",
                    "name": "$userName",
                    "displayName": "$displayName"
                  },
                  "pubKeyCredParams": [
                    { "type": "public-key", "alg": -7 },
                    { "type": "public-key", "alg": -257 }
                  ],
                  "timeout": 60000,
                  "attestation": "none",
                  "authenticatorSelection": {
                    "authenticatorAttachment": "platform",
                    "requireResidentKey": true,
                    "residentKey": "required",
                    "userVerification": "preferred"
                  }
                }
            """.trimIndent()
        }

        /**
         * Valid WebAuthn JSON payload conforming to PublicKeyCredentialRequestOptionsJSON.
         * User verification is "discouraged" so background restoration can proceed silently.
         */
        fun getDefaultRequestJson(rpId: String = DEFAULT_RP_ID): String {
            return """
                {
                  "challenge": "$DEFAULT_CHALLENGE_BASE64",
                  "timeout": 60000,
                  "rpId": "$rpId",
                  "userVerification": "discouraged"
                }
            """.trimIndent()
        }
    }

    internal var credentialManager: CredentialManager = CredentialManager.create(context)

    /**
     * Flow tracking whether a restore key has been successfully created and synced to the cloud/local device.
     */
    val hasSyncedRestoreCredential: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_HAS_SYNCED_RESTORE_CREDENTIAL] ?: false
        }

    /**
     * Creates a restore key using Android Credential Manager.
     *
     * Fallback strategy:
     * 1. Attempts with [isCloudBackupEnabled] = true for cross-device cloud restoration.
     * 2. If [E2eeUnavailableException] is thrown (user lacks screen lock or backup), retries with
     *    [isCloudBackupEnabled] = false to preserve local restoration.
     */
    suspend fun createRestoreKey(
        activityContext: Context = context,
        creationOptionsJson: String = getDefaultCreationJson(),
    ): Result<CreateRestoreCredentialResponse> {
        return try {
            val response = try {
                Log.d(TAG, "Attempting createCredential with isCloudBackupEnabled = true")
                val primaryRequest = CreateRestoreCredentialRequest(
                    requestJson = creationOptionsJson,
                    isCloudBackupEnabled = true
                )
                credentialManager.createCredential(activityContext, primaryRequest)
            } catch (e: E2eeUnavailableException) {
                Log.w(TAG, "E2eeUnavailableException caught: device lacks screen lock/cloud backup. Retrying with isCloudBackupEnabled = false", e)
                val fallbackRequest = CreateRestoreCredentialRequest(
                    requestJson = creationOptionsJson,
                    isCloudBackupEnabled = false
                )
                credentialManager.createCredential(activityContext, fallbackRequest)
            }

            if (response is CreateRestoreCredentialResponse) {
                markRestoreCredentialSynced(true)
                Log.i(TAG, "Restore credential successfully created and registered.")
                Result.success(response)
            } else {
                val errorMsg = "Unexpected response type: ${response::class.java.name}"
                Log.e(TAG, errorMsg)
                Result.failure(IllegalStateException(errorMsg))
            }
        } catch (e: CreateRestoreCredentialDomException) {
            Log.e(TAG, "CreateRestoreCredentialDomException: Invalid WebAuthn JSON payload", e)
            Result.failure(e)
        } catch (e: CreateCredentialException) {
            Log.e(TAG, "CreateCredentialException while creating restore key", e)
            Result.failure(e)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "IllegalArgumentException: invalid user or parameters", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error creating restore key", e)
            Result.failure(e)
        }
    }

    /**
     * Fetches the restore key from Credential Manager.
     *
     * Called in Tier 1 ([com.example.detectionexample.backup.FaceRecognitionBackupAgent.onRestoreFinished])
     * and Tier 2 ([com.example.detectionexample.MainActivity.onCreate]).
     */
    suspend fun getRestoreKey(
        activityContext: Context = context,
        authenticationJson: String = getDefaultRequestJson(),
    ): Result<RestoreCredential> {
        return try {
            // Note: DON'T chain GetRestoreCredentialOption with any other CredentialOption
            val restoreOption = GetRestoreCredentialOption(authenticationJson)
            val getRequest = GetCredentialRequest(listOf(restoreOption))

            val response = credentialManager.getCredential(activityContext, getRequest)
            val credential = response.credential

            if (credential is RestoreCredential) {
                Log.i(TAG, "Successfully retrieved RestoreCredential: ${credential.type}")
                saveRestoredSession("restored_user")
                markRestoreCredentialSynced(true)
                Result.success(credential)
            } else {
                val errorMsg = "Expected RestoreCredential, but received ${credential::class.java.name}"
                Log.w(TAG, errorMsg)
                Result.failure(IllegalStateException(errorMsg))
            }
        } catch (e: GetCredentialException) {
            Log.w(TAG, "GetCredentialException while retrieving restore key: ${e.message}")
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error retrieving restore key", e)
            Result.failure(e)
        }
    }

    /**
     * Clears the restore credential state from Credential Manager upon user sign-out or session invalidation.
     *
     * Uses [ClearCredentialStateRequest.TYPE_CLEAR_RESTORE_CREDENTIAL] to specifically target system restore keys.
     */
    suspend fun clearRestoreKey(): Result<Unit> {
        return try {
            val clearRequest = ClearCredentialStateRequest(
                ClearCredentialStateRequest.TYPE_CLEAR_RESTORE_CREDENTIAL
            )
            credentialManager.clearCredentialState(clearRequest)
            markRestoreCredentialSynced(false)
            Log.i(TAG, "Restore credential state cleared successfully on sign out.")
            Result.success(Unit)
        } catch (e: ClearCredentialException) {
            Log.e(TAG, "ClearCredentialException while clearing restore key", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error clearing restore key", e)
            Result.failure(e)
        }
    }

    private suspend fun markRestoreCredentialSynced(synced: Boolean) {
        try {
            dataStore.edit { preferences ->
                preferences[KEY_HAS_SYNCED_RESTORE_CREDENTIAL] = synced
                preferences[KEY_RESTORE_TIMESTAMP] = System.currentTimeMillis()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to persist restore credential sync flag", e)
        }
    }

    private suspend fun saveRestoredSession(userId: String) {
        try {
            dataStore.edit { preferences ->
                preferences[KEY_RESTORED_USER_ID] = userId
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to persist restored session userId", e)
        }
    }
}
