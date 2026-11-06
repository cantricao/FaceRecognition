package com.example.detectionexample.compose

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat.startActivity
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionsRequired
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun Permission(
    listPermission: List<String>,
    explanation: String,
    onPermissionGrant: @Composable (() -> Unit),
    onPermissionNotAvailable: @Composable (() -> Unit),
    modifier: Modifier = Modifier
) {
    // Track if the user doesn't want to see the rationale any more.
    var doNotShowRationale by rememberSaveable { mutableStateOf(false) }

    val cameraPermissionState = rememberMultiplePermissionsState(listPermission)

    PermissionsRequired(
        multiplePermissionsState = cameraPermissionState,
        permissionsNotGrantedContent = {
            if (doNotShowRationale) {
                onPermissionNotAvailable()
            } else {
                Rationale(
                    onDoNotShowRationale = { doNotShowRationale = true },
                    onRequestPermission = { cameraPermissionState.launchMultiplePermissionRequest() },
                    explanation = explanation,
                    modifier = modifier
                )
            }
        },
        permissionsNotAvailableContent = onPermissionNotAvailable,
        content = onPermissionGrant
    )
}

@Composable
private fun Rationale(
    onDoNotShowRationale: () -> Unit,
    onRequestPermission: () -> Unit,
    explanation: String,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = modifier.wrapContentHeight(),
        onDismissRequest = onDoNotShowRationale,
        title = {
            Text(text = "Permission")
        },
        text = {
            Text(explanation)
        },
        confirmButton = {
            Button(onClick = onRequestPermission) {
                Text("OK")
            }
        },
        dismissButton = {
            Button(onClick = onDoNotShowRationale) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun PermissionNotAvailableContent(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Snackbar(
        modifier = modifier,
        action = {
            Button(onClick = {
                val settingsIntent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null)
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(context, settingsIntent, null)
            }) {
                Text("Setting")
            }
        }
    ) {
        Text(text = "Go to Setting to setup the Camera Detection")
    }
}
