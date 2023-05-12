package com.example.detectionexample.compose.dialog

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.detectionexample.R
import com.example.detectionexample.config.ModelConfig
import com.example.detectionexample.viewmodels.AnalysisViewModel
import org.tensorflow.lite.gpu.CompatibilityList

@Composable
fun HyperParameterDialog(
    viewModel: AnalysisViewModel = viewModel(),
) {
    val context = LocalContext.current
    var expandedModelSlider by remember { mutableStateOf(false) }
    val listTfliteFile = context.assets.list("")
        ?.filter {
            ".tflite" in it
                    && it !=  ModelConfig.MOBILE_FACENET_MODEL_NAME } as MutableList<String>
    listTfliteFile.add(ModelConfig.MLKIT_CODENAME)
    listTfliteFile.add(ModelConfig.OPENCV_CODENAME)
    var expandedDeviceSlider by remember { mutableStateOf(false) }
    val deviceItem = listOf(
        ("CPU" to true),
        ("GPU" to CompatibilityList().isDelegateSupportedOnThisDevice),
        ("NNAPI" to (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P))
    )
    var textField by remember { mutableStateOf(viewModel.threshold.toString()) }
    AlertDialog(
        onDismissRequest = {
            viewModel.isProcessingFrame = true
        },
        title = {
            Text(text = "Test Hyperparameter")
        },
        confirmButton = {
            TextButton(onClick = {
                viewModel.isProcessingFrame = true
                viewModel.threshold = textField.toFloat()
            }) {
                Text(text = "Update")
            }

        },
        text = {
            Column {
                OutlinedTextField(
                    value = textField,
                    onValueChange = {
                        textField = it
                    },
                    label = {
                        Text("Threshold Value:")
                    },
                    keyboardOptions = KeyboardOptions.Default.copy (
                        keyboardType = KeyboardType.Number
                    )
                )
                ListItem(
                    headlineContent = {
                        OutlinedButton(
                            onClick = { expandedModelSlider = true },
                        ) {
                            Text("Model")
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "Model"
                            )
                            DropdownMenu(
                                expanded = expandedModelSlider,
                                onDismissRequest = { expandedModelSlider = false }
                            ) {
                                listTfliteFile.forEach { filename ->
                                    DropdownMenuItem(onClick = {
                                        viewModel.setModelName(filename)
                                        expandedModelSlider = false
                                    }, text = {
                                        Text(filename.split(".")[0])
                                    })
                                }
                            }
                        }
                    },
                    leadingContent = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_twotone_model_training_24),
                            contentDescription = "Model"
                        )
                    }
                )

                ListItem(
                    headlineContent = {
                        OutlinedButton(
                            onClick = { expandedDeviceSlider = true },
                        ) {
                            Text("Devices")
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "Devices"
                            )
                            DropdownMenu(
                                expanded = expandedDeviceSlider,
                                onDismissRequest = { expandedDeviceSlider = false }
                            ) {
                                deviceItem.forEach { (name, isEnable) ->
                                    DropdownMenuItem(onClick = {
                                        viewModel.setModelDevice(name)
                                        expandedDeviceSlider = false
                                    }, enabled = isEnable,
                                        text = {
                                            Text(name)
                                        })
                                }
                            }

                        }
                    },
                    leadingContent = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_baseline_device_hub_24),
                            contentDescription = "Devices"
                        )
                    }
                )
            }
        }
    )
}