package com.example.detectionexample.compose.dialog

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.detectionexample.viewmodels.AnalysisViewModel
import com.example.detectionexample.viewmodels.DatastoreViewModel

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DisplayNameListViewDialog(
    viewModel: AnalysisViewModel = viewModel(),
    datastoreViewModel: DatastoreViewModel = viewModel()
){
    var canEdit by remember { mutableStateOf(false)}
    val listOfPerson = remember { datastoreViewModel.getRegisteredPerson().toMutableStateList() }
    val checkedItems = remember { List(listOfPerson.size) { false }.toMutableStateList() }
    AlertDialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.wrapContentHeight()
            .pointerInput(Unit){
            detectTapGestures(
                onLongPress = { canEdit = true },
            )
        },
        onDismissRequest = {
            viewModel.isProcessingFrame = true
        },
        text = {
            LazyColumn {
                itemsIndexed(listOfPerson) { index ,person ->
                    ListItem( leadingContent = {
                        Image(
                            bitmap = person.face.asImageBitmap(),
                            contentDescription = "Face Icon"
                        ) },
                        headlineText = { Text(text = person.name) },
                        trailingContent = {
                            if(canEdit)
                                Checkbox(
                                    checked = checkedItems[index],
                                    onCheckedChange = { checkedItems[index] = it },
                                    enabled = canEdit
                                )}
                    )
                }
            }
        },
        title = {
            if(datastoreViewModel.isRegisteredPersonEmpty())
                Text(text = "No Faces Added!!")
            else {
                if (canEdit)
                    Text(text = "Select Recognition to delete:")
                else
                    Text(text = "Recognitions: (Long Tap to edit)")
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if(canEdit){
                    val listDeletedPerson = listOfPerson.zip(checkedItems).filter { (_, checkedItem) -> checkedItem }
                    listDeletedPerson.forEach { (person, checkedItem) ->
                        datastoreViewModel.removeRegisteredPerson(person)
                        listOfPerson.remove(person)
                        checkedItems.remove(checkedItem)
                    }
                }
                if(!datastoreViewModel.isRegisteredPersonEmpty())
                    canEdit = !canEdit
                else
                    viewModel.isProcessingFrame = true

            }) {
                if (!canEdit)
                    Text(text = "Edit", color = Color.Red)
                else
                    Text(text = "Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.isProcessingFrame = true }) {
                Text(text = "Close")
            }
        },
    )
}
