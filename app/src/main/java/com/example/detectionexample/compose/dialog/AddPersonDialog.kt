package com.example.detectionexample.compose.dialog

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.detectionexample.config.Util
import com.example.detectionexample.viewmodels.DetectionViewModel

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AddPersonDialog(
    viewModel: DetectionViewModel = viewModel(),
) {
    val context = LocalContext.current
    var progress by remember { mutableStateOf(0.0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        finishedListener = { viewModel.isProcessingFrame = true }
    )

    val trackedObjectsState by viewModel.trackedObserver.collectAsState(initial = listOf())

    val listOfFaceAndTrackedObject by produceState(
        initialValue = mutableListOf(), trackedObjectsState
    ) {
        value = trackedObjectsState.map {
            val face = Util.getCropBitmapByCPU(it.location, viewModel.processBitmap)
            Pair(face, it)
        }
    }

    AlertDialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.wrapContentHeight(),
        onDismissRequest = {
            viewModel.isProcessingFrame = true
        },
        title = {
            if (trackedObjectsState.isEmpty())
                Text(text = "No Faces Added!!")
            else
                Text(text = "List Of Faces")
        },
        text = {
            LazyColumn(
                contentPadding = PaddingValues(1.dp)
            )
            {
                items(listOfFaceAndTrackedObject) { (bitmap, trackedRecognition) ->
                    var textField by remember { mutableStateOf(trackedRecognition.title) }
                    OutlinedTextField(
                        value = textField,
                        onValueChange = {
                            textField = it
                            trackedRecognition.title = textField
                        },
                        leadingIcon = {
                            Image(
                                modifier = Modifier.size(24.dp),
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Face Icon"
                            )
                        },
                    )
                }
            }
        },
        dismissButton = {
            CircularProgressIndicator(
            progress = animatedProgress,
        )},
        confirmButton = {
            TextButton(
                onClick = {
                    if (trackedObjectsState.isEmpty())
                        viewModel.isProcessingFrame = true
                    listOfFaceAndTrackedObject.forEachIndexed { index, (face, trackedRecognition) ->
                        viewModel.addPerson(trackedRecognition, face)
                        progress = index * 1f / trackedObjectsState.size
                    }
                    Toast.makeText(context,
                        "Add ${trackedObjectsState.size} people",
                        Toast.LENGTH_SHORT).show()

                }
            ) {
                Text("Confirm")
            }

        }
    )
}