package com.example.detectionexample.compose.dialog

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.detectionexample.viewmodels.AnalysisViewModel
import com.example.detectionexample.viewmodels.DatastoreViewModel

@Composable
fun DisplayNameListViewDialog(
    viewModel: AnalysisViewModel = viewModel(),
    datastoreViewModel: DatastoreViewModel = viewModel()
) {
    var canEdit by rememberSaveable { mutableStateOf(false) }
    val listOfPerson = remember { datastoreViewModel.getRegisteredPerson().toMutableStateList() }
    var selectedPersonIds by remember { mutableStateOf(emptySet<String>()) }
    val gridState = rememberLazyGridState()

    AlertDialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .wrapContentHeight()
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { canEdit = true },
                )
            },
        onDismissRequest = {
            viewModel.isProcessingFrame = true
        },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(260.dp),
                state = gridState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                items(
                    items = listOfPerson,
                    key = { it.id },
                    contentType = { "person" }
                ) { person ->
                    Card(
                        modifier = Modifier.padding(4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        val faceBitmap = remember(person.face) { person.face.asImageBitmap() }
                        val isChecked = selectedPersonIds.contains(person.id)
                        ListItem(
                            leadingContent = {
                                Image(
                                    bitmap = faceBitmap,
                                    contentDescription = "Face Icon"
                                )
                            },
                            headlineContent = { Text(text = person.name) },
                            trailingContent = {
                                if (canEdit) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            selectedPersonIds = if (checked) {
                                                selectedPersonIds + person.id
                                            } else {
                                                selectedPersonIds - person.id
                                            }
                                        },
                                        enabled = true
                                    )
                                }
                            }
                        )
                    }
                }
            }
        },
        title = {
            if (datastoreViewModel.isRegisteredPersonEmpty()) {
                Text(text = "No Faces Added!!")
            } else {
                if (canEdit) {
                    Text(text = "Select Recognition to delete:")
                } else {
                    Text(text = "Recognitions: (Long Tap to edit)")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (canEdit) {
                    val toDelete = listOfPerson.filter { selectedPersonIds.contains(it.id) }
                    toDelete.forEach { person ->
                        datastoreViewModel.removeRegisteredPerson(person)
                        listOfPerson.remove(person)
                    }
                    selectedPersonIds = emptySet()
                }
                if (!datastoreViewModel.isRegisteredPersonEmpty()) {
                    canEdit = !canEdit
                } else {
                    viewModel.isProcessingFrame = true
                }
            }) {
                if (!canEdit) {
                    Text(text = "Edit", color = Color.Red)
                } else {
                    Text(text = "Delete")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.isProcessingFrame = true }) {
                Text(text = "Close")
            }
        },
    )
}
