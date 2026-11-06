package com.example.detectionexample.genai

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun GeminiNanoFaceDialog(
    faceBitmap: Bitmap?,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GeminiNanoViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(faceBitmap) {
        if (faceBitmap != null && uiState.status is GeminiNanoStatus.Available) {
            viewModel.analyzeFace(faceBitmap)
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Gemini Nano Inspector",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { viewModel.checkStatus() }) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Check status")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Face Preview
                if (faceBitmap != null) {
                    val imageBitmap = remember(faceBitmap) { faceBitmap.asImageBitmap() }
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = "Inspected Face",
                        modifier = Modifier
                            .size(110.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Status Banner
                when (val status = uiState.status) {
                    is GeminiNanoStatus.Checking -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Checking Gemini Nano availability...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    is GeminiNanoStatus.Available -> {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Gemini Nano Ready (On-Device)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    is GeminiNanoStatus.Downloadable -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Gemini Nano Model Download Needed",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = { viewModel.downloadModel() }) {
                                    Text("Download Nano via AICore")
                                }
                            }
                        }
                    }
                    is GeminiNanoStatus.Downloading -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Downloading Gemini Nano on device...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    is GeminiNanoStatus.Unavailable -> {
                        Text(
                            text = "Gemini Nano is not supported on this device or AICore is not initialized.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    is GeminiNanoStatus.Error -> {
                        Text(
                            text = "Error: ${status.message}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Analysis Content
                if (uiState.isAnalyzing) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Analyzing facial features...", style = MaterialTheme.typography.bodyMedium)
                    }
                } else if (uiState.analysisResult != null) {
                    val result = uiState.analysisResult!!
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            if (result.expression.isNotBlank()) {
                                Text(
                                    text = "Expression: ${result.expression}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (result.ageGroup.isNotBlank()) {
                                Text(
                                    text = "Estimated Age: ${result.ageGroup}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            if (result.accessories.isNotBlank()) {
                                Text(
                                    text = "Accessories: ${result.accessories}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            if (result.imageQuality.isNotBlank()) {
                                Text(
                                    text = "Quality: ${result.imageQuality}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            if (result.summary.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = result.summary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (faceBitmap != null && uiState.status is GeminiNanoStatus.Available && !uiState.isAnalyzing) {
                Button(onClick = { viewModel.analyzeFace(faceBitmap) }) {
                    Text("Re-Analyze")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Close")
            }
        }
    )
}
