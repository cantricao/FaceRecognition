package com.example.detectionexample.compose.dialog

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.detectionexample.config.ModelConfig
import com.example.detectionexample.viewmodels.AnalysisViewModel
import org.tensorflow.lite.gpu.CompatibilityList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HyperparameterDialog(
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
                    headlineText = {
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
                            painter = painterResource(
                                id =
                                context.resources.getIdentifier(
                                    "ic_twotone_model_training_24",
                                    "drawable",
                                    context.packageName
                                )
                            ),
                            contentDescription = "Model"
                        )
                    }
                )

                ListItem(
                    headlineText = {
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
                            painter = painterResource(
                                id = context.resources.getIdentifier(
                                    "ic_baseline_device_hub_24",
                                    "drawable",
                                    context.packageName
                                )
                            ),
                            contentDescription = "Devices"
                        )
                    }
                )
            }
        }
    )
}