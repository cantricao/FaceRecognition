package com.example.detectionexample.compose.dialog

import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.detectionexample.viewmodels.AnalysisViewModel
import com.example.detectionexample.viewmodels.DatastoreViewModel
import kotlinx.coroutines.launch

@Composable
fun ClearNameListDialog(
    viewModel: AnalysisViewModel = viewModel(),
    datastoreViewModel: DatastoreViewModel = viewModel()
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
                    datastoreViewModel.clearRegisteredPerson()
                    Toast.makeText(context, "Recognitions Cleared", Toast.LENGTH_SHORT).show()
                }
            }) {
                Text(text = "Delete All")
            }

        },
    )

}