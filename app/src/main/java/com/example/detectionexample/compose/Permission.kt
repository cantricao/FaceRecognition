package com.example.detectionexample.compose

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat.startActivity
import com.google.accompanist.permissions.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun Permission(permission: List<String>,
               explanation: String,
               permissionAvailableContent: @Composable (() -> Unit),
               permissionNotAvailableContent: @Composable (() -> Unit)) {
    // Track if the user doesn't want to see the rationale any more.
    var doNotShowRationale by rememberSaveable { mutableStateOf(false) }

    val cameraPermissionState = rememberMultiplePermissionsState (permission)

    PermissionsRequired(
        multiplePermissionsState = cameraPermissionState,
        permissionsNotGrantedContent = {
            if (doNotShowRationale) {
                permissionNotAvailableContent()
            } else {
                Rationale(
                    onDoNotShowRationale = { doNotShowRationale = true},
                    onRequestPermission = { cameraPermissionState.launchMultiplePermissionRequest() },
                    explanation = explanation
                )
            }
        },
        permissionsNotAvailableContent = permissionNotAvailableContent,
        content = permissionAvailableContent
    )
}



@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun Rationale(
    onDoNotShowRationale: () -> Unit,
    onRequestPermission: () -> Unit,
    explanation: String
) {
    Column {
        AlertDialog(
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier.wrapContentHeight(),
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
            }
        )
    }
}


@Composable
fun PermissionNotAvailableContent() {
    val context = LocalContext.current
    Snackbar(
        action = {
            Button(onClick = {
                startActivity(context,
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    ),
                null)
            }) {
                Text("Setting")
            }
        }) {
        Text(text = "Go to Setting to setup the Camera Detection")
    }
}

