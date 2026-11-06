package com.example.detectionexample.compose.dialog

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.detectionexample.config.Util
import com.example.detectionexample.viewmodels.AnalysisViewModel
import com.example.detectionexample.viewmodels.DatastoreViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AddPersonDialog(
    modifier: Modifier = Modifier,
    viewModel: AnalysisViewModel = viewModel(),
    datastoreViewModel: DatastoreViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var progress by rememberSaveable { mutableStateOf(0.0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        finishedListener = { viewModel.isProcessingFrame = true }
    )

    val trackedObjectsState by viewModel.trackedObserver.collectAsState(initial = listOf())
    val gridState = rememberLazyGridState()

    val listOfFaceAndTrackedObject by produceState<List<FaceAndTrackedObject>>(
        initialValue = emptyList(),
        trackedObjectsState
    ) {
        value = withContext(Dispatchers.Default) {
            trackedObjectsState.map {
                val face = Util.getCropBitmapByCPU(it.location, viewModel.processingBitmap)
                FaceAndTrackedObject(face, it)
            }
        }
    }

    AlertDialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = modifier
            .wrapContentHeight()
            .imePadding(),
        onDismissRequest = {
            viewModel.isProcessingFrame = true
        },
        title = {
            Text(text = "Add Recognitions")
        },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                state = gridState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                items(
                    items = listOfFaceAndTrackedObject,
                    key = { item ->
                        val loc = item.trackedRecognition.location
                        "${item.trackedRecognition.title}_${item.trackedRecognition.detectionConfidence}_${loc.left}"
                    },
                    contentType = { "face_item" }
                ) { item ->
                    val bitmap = item.face
                    val trackedRecognition = item.trackedRecognition
                    Card(
                        modifier = Modifier.padding(4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        var textField by rememberSaveable(
                            trackedRecognition.title
                        ) { mutableStateOf(trackedRecognition.title) }
                        val faceBitmap = remember(bitmap) { bitmap.asImageBitmap() }
                        OutlinedTextField(
                            value = textField,
                            onValueChange = {
                                textField = it
                                trackedRecognition.title = textField
                            },
                            modifier = Modifier.padding(8.dp),
                            leadingIcon = {
                                Image(
                                    modifier = Modifier.size(24.dp),
                                    bitmap = faceBitmap,
                                    contentDescription = "Face Icon"
                                )
                            },
                        )
                    }
                }
            }
        },
        dismissButton = {
            CircularProgressIndicator(
                progress = { animatedProgress },
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    scope.launch {
                        if (trackedObjectsState.isEmpty()) {
                            viewModel.isProcessingFrame = true
                        }
                        val totalObjects = trackedObjectsState.size.coerceAtLeast(1)
                        listOfFaceAndTrackedObject.forEachIndexed { index, item ->
                            datastoreViewModel.addPerson(item.trackedRecognition, item.face)
                            progress = index * 1f / totalObjects
                        }
                        Toast.makeText(
                            context,
                            "Add ${trackedObjectsState.size} people",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            ) {
                Text("Confirm")
            }
        }
    )
}
