package com.example.detectionexample.compose.dialog

import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.text.DateFormat
import java.util.*


@OptIn(ExperimentalComposeUiApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun LoadPhotoDialog(
    viewModel: AnalysisViewModel = viewModel(),
    datastoreViewModel: DatastoreViewModel = viewModel()
) {
    val context = LocalContext.current
    val trackedObjectsState by viewModel.trackedObserver.collectAsState(initial = listOf())
    var listOfUri by remember { mutableStateOf(listOf<Uri>()) }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) {
            listOfUri = it
        }
    var takenPhotoUri by remember { mutableStateOf(Uri.EMPTY) }
    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) {
        if (it) {
            listOfUri = listOf(takenPhotoUri)
        }
    }

    @Throws(IOException::class)
    fun createImageUri(): Uri {
        // Create an image file name
        val timeStamp: String = DateFormat.getDateTimeInstance().format(Date())
        val storageDir: File? = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val imageFile = File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        )
        return FileProvider.getUriForFile(context, "com.example.detectionexample.fileprovider", imageFile)
    }

    var progressLoader by remember { mutableStateOf(0.0f) }
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
            val bitmap = Util.getBitmap(context, uri)
//            viewModel.observeTrackedObject(
//                System.currentTimeMillis(),
//                false,
////                viewModel.detectInImage(bitmap, 0)
//            )

            progressLoader += 1f / listOfUri.size
            Pair(uri, viewModel.trackedObserver.value)
        }
    }

    var progress by remember { mutableStateOf(0.0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        finishedListener = { viewModel.isProcessingFrame = true }
    )


    var isOpenDialog by remember { mutableStateOf(true)}

    val scope = rememberCoroutineScope()

    if (isOpenDialog) {
        AlertDialog(
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier.wrapContentHeight(),
            onDismissRequest = {
                viewModel.isProcessingFrame = true
            },
            title = {
                Text(text = "Load photo")
            },
            text = {
                Column {
                    ListItem(modifier = Modifier.clickable {
                        scope.launch {
                            isOpenDialog = false
                            takenPhotoUri = createImageUri()
                            takePhoto.launch(takenPhotoUri)
                            Toast.makeText(context, "Select ${listOfUri.size} images", Toast.LENGTH_SHORT).show()
                        }

                    }, headlineContent = { Text("Take Photo From Default Camera") }
                    )
                    ListItem(modifier = Modifier.clickable {
                        scope.launch {
                            isOpenDialog = false
                            launcher.launch("image/*")
                            Toast.makeText(context, "Select ${listOfUri.size} images", Toast.LENGTH_SHORT).show()
                        }
                    }, headlineContent = {
                        Text("Load Photos From Gallery")
                    })
                }
            },
            confirmButton = {}
        )
    }
    if (listOfUriAndTrackedObject.isNotEmpty() && !viewModel.isProcessingFrame) {
        AlertDialog(
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier.wrapContentHeight(),
            onDismissRequest = {
                viewModel.isProcessingFrame = true
            },
            title = {
                if (listOfUriAndTrackedObject.isEmpty())
                    Text(text = "No Faces Added!!")
                else
                    Text(text = "List Of Faces")
            },
            text = {
                LazyColumn(contentPadding = PaddingValues(1.dp)) {
                    listOfUriAndTrackedObject.forEach { (uri, listObjectObject) ->
                        items(listObjectObject, key = { it.hashCode() }) { trackedRecognition ->
                            var textField by remember { mutableStateOf(trackedRecognition.title) }
                            OutlinedTextField(
                                value = textField,
                                onValueChange = {
                                    textField = it
                                    trackedRecognition.title = textField
                                },
                                leadingIcon = {
                                    GlideImage(
                                        imageModel = { uri },
                                        modifier = Modifier.size(24.dp),
                                        requestOptions = {
                                            RequestOptions()
                                                .transform(GlideCropBitmapTransformation(trackedRecognition.location))
                                        },
//                                        contentDescription = "Face Icon"
                                    )
                                },
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if((progress == 0f || progress == 100f) && (progressLoader == 0f || progressLoader == 100f)){
                    TextButton(
                        onClick = {
                            if (listOfUriAndTrackedObject.isEmpty())
                                viewModel.isProcessingFrame = true
                            listOfUriAndTrackedObject.forEach { (uri, listObjectObject) ->
                                val bitmap = Util.getBitmap(context, uri)
                                listObjectObject.forEach { trackedRecognition ->
                                    datastoreViewModel.addPerson(trackedRecognition, bitmap)
                                    progress += 1f / trackedObjectsState.size
                                }
                            }
                            Toast.makeText(
                                context,
                                "Add ${listOfUriAndTrackedObject.size} images",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    ) {
                        Text("Confirm")
                    }
                } else {
                    CircularProgressIndicator(
                        progress = animatedProgressLoadImage,
                    )
                    CircularProgressIndicator(
                        progress = animatedProgress,
                    )
                }

            }
        )
    }
}