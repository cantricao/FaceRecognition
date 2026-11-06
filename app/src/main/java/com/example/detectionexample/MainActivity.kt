package com.example.detectionexample

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.core.content.IntentSanitizer
import androidx.lifecycle.lifecycleScope
import com.example.detectionexample.compose.FaceRecognitionApp
import com.example.detectionexample.compose.Permission
import com.example.detectionexample.compose.PermissionNotAvailableContent
import com.example.detectionexample.credentials.RestoreCredentialManager
import com.example.detectionexample.ui.theme.DetectionExampleTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    @Inject
    lateinit var restoreCredentialManager: RestoreCredentialManager

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Apply car display cutout mode (AR-2: LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS in values-car-v30)
            val cutoutMode = resources.getInteger(R.integer.windowLayoutInDisplayCutoutMode)
            if (cutoutMode != 0) {
                window.attributes.layoutInDisplayCutoutMode = cutoutMode
            }
            // Track controllable system bar insets so vehicle climate and system bars remain visible (AR-1)
            window.insetsController?.addOnControllableInsetsChangedListener { _, typeMask ->
                Log.d(TAG, "Controllable insets changed: $typeMask")
            }
        }
        validateIncomingIntent(intent)

        lifecycleScope.launch {
            initiateTier2RestoreCredentials()
        }

        setContent {
            val listPermission = remember {
                buildList {
                    add(Manifest.permission.CAMERA)
                    // Request legacy storage permission only on API 28 and below
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                        add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    }
                }
            }
            val explanation = "Use camera for detection and store image"
            DetectionExampleTheme {
                Surface {
                    Permission(
                        listPermission = listPermission,
                        explanation = explanation,
                        onPermissionGrant = {
                            FaceRecognitionApp()
                        },
                        onPermissionNotAvailable = {
                            PermissionNotAvailableContent()
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(newIntent: Intent) {
        super.onNewIntent(newIntent)
        // Keep active intent reference updated
        intent = newIntent
        validateIncomingIntent(newIntent)
    }

    /**
     * Validates and sanitizes incoming intents to prevent intent redirection and unauthorized flag propagation.
     */
    private fun validateIncomingIntent(incomingIntent: Intent?) {
        if (incomingIntent == null) return

        // 1. Guard against unauthorized URI permission grant bypass
        val dangerousUriFlags = (
            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
            )

        if ((incomingIntent.flags and dangerousUriFlags) != 0) {
            Log.w(TAG, "Stripped unauthorized URI permission grant flags from incoming intent")
            incomingIntent.flags = incomingIntent.flags and dangerousUriFlags.inv()
        }

        // 2. Allowlist launcher action
        val action = incomingIntent.action
        if (action == null || action == Intent.ACTION_MAIN) {
            return
        }

        // 3. Sanitize any dynamic or nested intent extras via AndroidX IntentSanitizer
        try {
            val sanitizer = IntentSanitizer.Builder()
                .allowComponent(ComponentName(this, MainActivity::class.java))
                .allowAction(Intent.ACTION_MAIN)
                .allowCategory(Intent.CATEGORY_LAUNCHER)
                .allowCategory(Intent.CATEGORY_DEFAULT)
                .build()

            sanitizer.sanitizeByThrowing(incomingIntent)
        } catch (e: SecurityException) {
            Log.w(TAG, "Blocked unauthorized incoming intent parameters", e)
        }
    }

    /**
     * Tier 2 (Secondary - Foreground) Restoration.
     *
     * Runs in Launcher Activity.onCreate() to catch failovers if background restoration
     * didn't complete (e.g., network dropped during device onboarding) or registers a
     * fresh restore key for active users.
     */
    private suspend fun initiateTier2RestoreCredentials() {
        try {
            val hasSynced = restoreCredentialManager.hasSyncedRestoreCredential.first()
            if (!hasSynced) {
                Log.d(TAG, "Tier 2: Checking for existing Restore Credentials...")
                val restoreResult = restoreCredentialManager.getRestoreKey(this@MainActivity)
                if (restoreResult.isSuccess) {
                    Log.i(TAG, "Tier 2: Successfully signed in via Restore Credentials in foreground!")
                } else {
                    Log.d(TAG, "Tier 2: No existing restore key. Registering restore key for current user.")
                    restoreCredentialManager.createRestoreKey(this@MainActivity)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Tier 2: Restore Credentials initialization error: ${e.message}")
        }
    }
}
