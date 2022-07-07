package com.example.detectionexample.compose

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.detectionexample.compose.dialog.*
import com.example.detectionexample.viewmodels.DetectionViewModel
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterialApi::class)
@Composable
@Preview
fun BottomSheet(viewModel: DetectionViewModel = viewModel()) {
    val context = LocalContext.current

    var chooseAction by remember { mutableStateOf(-1) }

    val scope = rememberCoroutineScope()
    val actions = arrayOf(
        "View Recognition List",
        "Update Recognition List",
        "Save Recognitions To Datastore",
        "Load Recognitions From Datastore",
        "Clear All Recognitions",
        "Import Photo",
        "Hyperparameters",
    )

    val drawerState = rememberBottomDrawerState(BottomDrawerValue.Closed)
    BottomDrawer(
        modifier = Modifier.pointerInput(Unit){
        detectTapGestures(
            onLongPress = {
                if(!viewModel.isCaptureImage){
                    Toast.makeText(context, "Capture image is processing", Toast.LENGTH_SHORT).show()
                    viewModel.isCaptureImage = true
                } else {
                    Toast.makeText(context, "Wait for Capture image ", Toast.LENGTH_SHORT).show()
                }

            },
        )
    },
        drawerState = drawerState,
        drawerContent = {
            Column {
                actions.forEachIndexed { index, action ->
                    ListItem(Modifier.clickable {
                        chooseAction = index
                        scope.launch {
                            viewModel.isProcessingFrame = false
                            drawerState.close()
                            chooseAction = index
                        }
                    }) {
                        Text(text = action)
                    }
                }
            }
        },
        content = {
            if (!viewModel.isProcessingFrame) {
                when (chooseAction) {
                    0 -> DisplayNameListViewDialog()
                    1 -> AddPersonDialog()
                    2 -> {
                        viewModel.saveAllToDatastore()
                        Toast.makeText(context, "Recognitions Saved", Toast.LENGTH_SHORT).show()
                    }
                    3 -> {
                        viewModel.loadAllToDatastore()
                        Toast.makeText(context, "Recognitions Loaded", Toast.LENGTH_SHORT)
                            .show()
                    }
                    4 -> ClearNameListDialog()
                    5 -> LoadPhotoDialog()
                    6 -> HyperparameterDialog()

                }
            } else {
                chooseAction = -1
            }
        }
    )
}




