package com.example.detectionexample.compose.dialog

import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bumptech.glide.request.RequestOptions
import com.example.detectionexample.config.Util
import com.example.detectionexample.glide.GlideCropBitmapTransformation
import com.example.detectionexample.viewmodels.AnalysisViewModel
import com.example.detectionexample.viewmodels.DatastoreViewModel
import com.skydoves.landscapist.glide.GlideImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DateFormat
import java.util.Date

@OptIn(
    ExperimentalComposeUiApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun LoadPhotoDialog(
    modifier: Modifier = Modifier,
    viewModel: AnalysisViewModel = viewModel(),
    datastoreViewModel: DatastoreViewModel = viewModel()
) {
    val context = LocalContext.current
    var listOfUri: List<Uri> by rememberSaveable { mutableStateOf(emptyList()) }
    val multiplePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = PickMultipleVisualMedia(),
        onResult = { uris -> listOfUri = uris }
    )

    fun createCustomImageFile(): Uri {
        val timeStamp: String = DateFormat.getDateTimeInstance().format(Date())
        val storageDir: File? = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val imageFile = File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        )
        return FileProvider.getUriForFile(context, "com.example.detectionexample.fileprovider", imageFile)
    }

    var progressLoader by rememberSaveable { mutableStateOf(0.0f) }
    val animatedProgressLoadImage by animateFloatAsState(
        targetValue = progressLoader,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        finishedListener = { progressLoader = 0f }
    )

    val listOfUriAndTrackedObject by produceState(
        initialValue = mutableListOf(),
        listOfUri
    ) {
        value = listOfUri.map { uri ->
            progressLoader += 1f / listOfUri.size
            Pair(uri, viewModel.trackedObserver.value)
        }
    }

    var progress by rememberSaveable { mutableStateOf(0.0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        finishedListener = { viewModel.isProcessingFrame = true }
    )

    var isOpenDialog by rememberSaveable { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()

    if (isOpenDialog) {
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
                val trackedObjectsState by viewModel.trackedObserver.collectAsState(initial = listOf())
                Column {
                    ListItem(
                        modifier = Modifier.clickable {
                            multiplePhotoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                            Toast.makeText(context, "Select ${listOfUri.size} images", Toast.LENGTH_SHORT).show()
                        },
                        headlineContent = { Text("Load Photos From Gallery") }
                    )

                    LinearProgressIndicator(
                        progress = { animatedProgressLoadImage },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(1),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        state = gridState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                    ) {
                        items(
                            items = listOfUriAndTrackedObject,
                            key = { (uri, _) -> uri.toString() },
                            contentType = { "uri_item" }
                        ) { (uri, listObject) ->
                            val trackedRecognition = listObject[0]
                            var textField by rememberSaveable(
                                trackedRecognition.title
                            ) { mutableStateOf(trackedRecognition.title) }

                            OutlinedTextField(
                                value = textField,
                                onValueChange = {
                                    textField = it
                                    trackedRecognition.title = textField
                                },
                                modifier = Modifier.padding(8.dp),
                                leadingIcon = {
                                    GlideImage(
                                        imageModel = { uri },
                                        modifier = Modifier.size(24.dp),
                                        requestOptions = {
                                            RequestOptions()
                                                .transform(GlideCropBitmapTransformation(trackedRecognition.location))
                                        },
                                    )
                                },
                            )
                        }
                    }
                }
            },
            confirmButton = {
                val isProgressIdle = progress == 0f || progress >= 100f
                val isLoaderIdle = progressLoader == 0f || progressLoader >= 100f
                if (isProgressIdle && isLoaderIdle) {
                    TextButton(
                        onClick = {
                            scope.launch {
                                if (listOfUriAndTrackedObject.isEmpty()) {
                                    viewModel.isProcessingFrame = true
                                }
                                val trackedObjectsState = viewModel.trackedObserver.value
                                val totalObjects = trackedObjectsState.size.coerceAtLeast(1)
                                listOfUriAndTrackedObject.forEach { (uri, listObjectObject) ->
                                    val bitmap = withContext(Dispatchers.IO) {
                                        Util.getBitmap(context, uri)
                                    }
                                    listObjectObject.forEach { trackedRecognition ->
                                        datastoreViewModel.addPerson(trackedRecognition, bitmap)
                                        progress += 1f / totalObjects
                                    }
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
            },
            dismissButton = {
                CircularProgressIndicator(
                    progress = { animatedProgress },
                )
            }
        )
    }
}
