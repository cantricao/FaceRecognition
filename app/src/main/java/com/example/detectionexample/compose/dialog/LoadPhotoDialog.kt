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
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bumptech.glide.request.RequestOptions
import com.example.detectionexample.config.CropTransformation
import com.example.detectionexample.config.Util
import com.example.detectionexample.viewmodels.DetectionViewModel
import com.skydoves.landscapist.glide.GlideImage
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.text.DateFormat
import java.util.*


@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterialApi::class)
@Composable
fun LoadPhotoDialog(
    viewModel: DetectionViewModel = viewModel(),
) {
    val context = LocalContext.current
    val trackedObjectsState by viewModel.trackedObserver.collectAsState(initial = listOf())
    var listOfUri by remember { mutableStateOf(listOf<Uri>()) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) {
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
        val imageFile =  File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        )
        return FileProvider.getUriForFile(context, "com.example.android.fileprovider", imageFile)
    }

    var progressLoader by remember { mutableStateOf(0.0f) }
    val animatedProgressLoadImage by animateFloatAsState(
        targetValue = progressLoader,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        finishedListener = {  progressLoader = 0f}
    )

    val listOfUriAndTrackedObject by produceState(
            initialValue = mutableListOf(),
            listOfUri
        ) {
        value = listOfUri.map { uri ->
            val bitmap = Util.getBitmap(context, uri)
            viewModel.observeTrackedObject(System.currentTimeMillis(), false, bitmap)
            progressLoader += 1f/listOfUri.size
            Pair(uri, viewModel.trackedObserver.value)
        }
    }

    var progress by remember { mutableStateOf(0.0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        finishedListener = { viewModel.isProcessingFrame = true }
    )


    val drawerState = rememberBottomDrawerState(BottomDrawerValue.Expanded)

    val scope = rememberCoroutineScope()

    BottomDrawer(
        gesturesEnabled  = false,
        drawerState = drawerState,
        drawerContent = {
            Column {
                ListItem(modifier = Modifier.clickable {
                    scope.launch {
                        drawerState.close()
                        takenPhotoUri = createImageUri()
                        takePhoto.launch(takenPhotoUri)
                    }

                }) {
                    Text("Take Photo From Default Camera")
                }
                ListItem(modifier = Modifier.clickable {
                    scope.launch {
                        drawerState.close()
                        launcher.launch("image/*")
                    }

                }) {
                    Text("Load Photos From Galary")
                }
            }
            ListItem(modifier = Modifier.clickable {
                scope.launch {
                    viewModel.isProcessingFrame = true
                    drawerState.close()
                }
            }) {
                Text("Go back to the camera")
            }
        }
    ) {
        if(listOfUriAndTrackedObject.isNotEmpty()) {
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
                                            imageModel = uri,
                                            modifier = Modifier.size(24.dp),
                                            requestOptions = {
                                                RequestOptions()
                                                    .transform(CropTransformation(trackedRecognition.location))
                                            },
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
                        progress = animatedProgressLoadImage,
                    )
                    CircularProgressIndicator(
                        progress = animatedProgress,
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (listOfUriAndTrackedObject.isEmpty())
                                viewModel.isProcessingFrame = true
                            listOfUriAndTrackedObject.forEach { (uri, listObjectObject) ->
                                val bitmap = Util.getBitmap(context, uri)
                                listObjectObject.forEach { trackedRecognition ->
                                    viewModel.addPerson(trackedRecognition, bitmap)
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

                }
            )
        }
    }
}