package com.example.detectionexample.compose.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.detectionexample.viewmodels.AnalysisViewModel
import com.example.detectionexample.viewmodels.DatastoreViewModel


// Create some simple sample data
private val data = mapOf(
    "android" to listOf("kotlin", "java", "flutter"),
    "kotlin" to listOf("backend", "android", "desktop"),
    "desktop" to listOf("kotlin", "java", "flutter"),
    "backend" to listOf("kotlin", "java"),
    "java" to listOf("backend", "android", "desktop"),
    "flutter" to listOf("android", "desktop")
)


val actions = arrayOf(
    "View Recognition List",
    "Update Recognition List",
    "Save Recognitions To Datastore",
    "Load Recognitions From Datastore",
    "Clear All Recognitions",
    "Import Photo",
    "Hyper parameters",
)

@Composable
@Preview
fun BottomSheet(
    viewModel: AnalysisViewModel = viewModel(),
    datastoreViewModel: DatastoreViewModel = viewModel()
) {
    val context = LocalContext.current

    var selectedTopic: String by rememberSaveable { mutableStateOf(data.keys.first()) }

    var chooseAction by remember { mutableStateOf(-1) }

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text("Related Content", style = MaterialTheme.typography.titleLarge)

        LazyColumn {
            items(
                data.getValue(selectedTopic),
                key = { it }
            ) { relatedTopic ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedTopic = relatedTopic
                        }
                ) {
                    Text(
                        text = relatedTopic,
                        modifier = Modifier
                            .padding(16.dp)

                    )
                }
            }
        }
    }
}

//    val drawerState = rememberBottomDrawerState(BottomDrawerValue.Closed)
//    BottomDrawer(
//        drawerState = drawerState,
//        drawerContent = {
//            Column {
//                actions.forEachIndexed { index, action ->
//                    ListItem (modifier = Modifier.clickable {
//                        chooseAction = index
//                        scope.launch {
//                            viewModel.isProcessingFrame = false
//                            drawerState.close()
//                            chooseAction = index
//                        }
//                    }, headlineContent =  {
//                        Text(text = action)
//                    })
//                }
//            }
//        },
//        content = {
//            if (!viewModel.isProcessingFrame) {
//                when (chooseAction) {
//                    0 -> DisplayNameListViewDialog()
//                    1 -> AddPersonDialog()
//                    2 -> {
//                        datastoreViewModel.saveAllToDatastore()
//                        viewModel.isProcessingFrame = true
//                        Toast.makeText(context, "Recognitions Saved", Toast.LENGTH_SHORT).show()
//                    }
//                    3 -> {
//                        datastoreViewModel.loadAllToDatastore()
//                        viewModel.isProcessingFrame = true
//                        Toast.makeText(context, "Recognitions Loaded", Toast.LENGTH_SHORT)
//                            .show()
//                    }
//                    4 -> ClearNameListDialog()
//                    5 -> LoadPhotoDialog()
//                    6 -> HyperParameterDialog()
//
//                }
//            } else {
//                chooseAction = -1
//            }
//        }
//    )
//}




