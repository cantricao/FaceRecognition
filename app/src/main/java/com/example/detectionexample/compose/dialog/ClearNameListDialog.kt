package com.example.detectionexample.compose.dialog

import android.widget.Toast
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.detectionexample.viewmodels.DetectionViewModel
import kotlinx.coroutines.launch

@Composable
fun ClearNameListDialog(
    viewModel: DetectionViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = {
            viewModel.isProcessingFrame = true
        },
        title = {
            Text(text = "Do you want to delete all person from datastore?")
        },
        confirmButton = {
            TextButton(onClick = {
                viewModel.isProcessingFrame = true
                scope.launch {
                    viewModel.clearRegisteredPerson()
                    Toast.makeText(context, "Recognitions Cleared", Toast.LENGTH_SHORT).show()
                }
            }) {
                Text(text = "Delete All")
            }

        },
    )

}