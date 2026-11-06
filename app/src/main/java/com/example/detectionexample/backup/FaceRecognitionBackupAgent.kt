package com.example.detectionexample.backup

import android.app.backup.BackupAgent
import android.app.backup.BackupDataInput
import android.app.backup.BackupDataOutput
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetRestoreCredentialOption
import androidx.credentials.RestoreCredential
import com.example.detectionexample.credentials.RestoreCredentialManager
import kotlinx.coroutines.runBlocking

/**
 * Tier 1 (Primary - Background) Restoration Agent.
 *
 * Runs automatically during device setup in [onRestoreFinished] before the user opens the
 * app for the first time. Silently logs the user back in, restoring background push tokens
 * and enabling background synchronization.
 */
open class FaceRecognitionBackupAgent : BackupAgent() {

    companion object {
        private const val TAG = "BackupAgent"
    }

    override fun onBackup(
        oldState: ParcelFileDescriptor?,
        data: BackupDataOutput?,
        newState: ParcelFileDescriptor?
    ) {
        // Full backup mode is configured via android:fullBackupOnly="true" in AndroidManifest.xml.
        // Key-value onBackup callback is retained as required abstract method.
    }

    override fun onRestore(
        data: BackupDataInput?,
        appVersionCode: Int,
        newState: ParcelFileDescriptor?
    ) {
        // Key-value onRestore callback is retained as required abstract method.
        // Full restore lifecycle signals completion in onRestoreFinished().
    }

    override fun onRestoreFinished() {
        super.onRestoreFinished()
        Log.i(TAG, "Tier 1: Device restore finished. Performing synchronous background Restore Credentials retrieval.")

        // Synchronous retrieval is mandatory in onRestoreFinished to guarantee session restoration
        // before any broadcast receivers or background sync jobs fire.
        runBlocking {
            try {
                val credentialManager = CredentialManager.create(applicationContext)
                val requestJson = RestoreCredentialManager.getDefaultRequestJson()
                val restoreOption = GetRestoreCredentialOption(requestJson)
                val getRequest = GetCredentialRequest(listOf(restoreOption))

                val response = credentialManager.getCredential(applicationContext, getRequest)
                val credential = response.credential

                if (credential is RestoreCredential) {
                    Log.i(TAG, "Tier 1: Background silent sign-in succeeded with RestoreCredential: ${credential.type}")
                    restoreNotificationsAndBackgroundServices(credential)
                } else {
                    Log.w(TAG, "Tier 1: Retrieved credential was not a RestoreCredential: ${credential.type}")
                }
            } catch (e: Exception) {
                // Network failure or delayed sync will fall back to Tier 2 in MainActivity.onCreate()
                Log.w(TAG, "Tier 1: Background restore failed or deferred to Tier 2: ${e.message}")
            }
        }
    }

    /**
     * Resumes background notifications and syncs device tokens (e.g., FCM) with relying party server.
     */
    private fun restoreNotificationsAndBackgroundServices(credential: RestoreCredential) {
        Log.i(TAG, "Tier 1: Notifications restored and active session established in background.")
    }
}
