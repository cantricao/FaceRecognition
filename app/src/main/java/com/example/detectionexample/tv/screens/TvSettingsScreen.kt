package com.example.detectionexample.tv.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

/**
 * 10-foot TV Settings Screen replacing legacy LeanbackSettingsFragment and PreferenceFragment.
 *
 * Implements:
 * - Direct TV ListItem with Switch controls.
 * - Initial D-pad focus on the first preference row via [FocusRequester].
 * - Clean overscan-safe layout with 48.dp margin.
 */
@Composable
fun TvSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var autoPlayNext by remember { mutableStateOf(true) }
    var highSensitivity by remember { mutableStateOf(true) }
    var drawOverlays by remember { mutableStateOf(true) }
    var audioAlerts by remember { mutableStateOf(false) }

    val firstItemFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        firstItemFocusRequester.requestFocus()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(horizontal = 48.dp, vertical = 32.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "TV Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )
                Text(
                    text = "Configure Face Recognition & Playback preferences for 10-foot display",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFAAAAAA)
                )
            }
            Button(onClick = onBack) {
                Text("Back to Browse")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            item {
                ListItem(
                    selected = false,
                    onClick = { autoPlayNext = !autoPlayNext },
                    headlineContent = { Text("Autoplay Next Feed") },
                    supportingContent = { Text("Automatically transition to the next surveillance clip when playback completes") },
                    trailingContent = {
                        Switch(
                            checked = autoPlayNext,
                            onCheckedChange = null,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF80D8FF),
                                checkedTrackColor = Color(0xFF004D40)
                            )
                        )
                    },
                    modifier = Modifier.focusRequester(firstItemFocusRequester)
                )
            }

            item {
                ListItem(
                    selected = false,
                    onClick = { highSensitivity = !highSensitivity },
                    headlineContent = { Text("High Sensitivity Recognition") },
                    supportingContent = { Text("Lower cosine similarity threshold for identifying persons at long distance") },
                    trailingContent = {
                        Switch(
                            checked = highSensitivity,
                            onCheckedChange = null,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF80D8FF),
                                checkedTrackColor = Color(0xFF004D40)
                            )
                        )
                    }
                )
            }

            item {
                ListItem(
                    selected = false,
                    onClick = { drawOverlays = !drawOverlays },
                    headlineContent = { Text("Real-Time Bounding Box Overlays") },
                    supportingContent = { Text("Render color-coded detection boxes and landmarks on active video feed") },
                    trailingContent = {
                        Switch(
                            checked = drawOverlays,
                            onCheckedChange = null,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF80D8FF),
                                checkedTrackColor = Color(0xFF004D40)
                            )
                        )
                    }
                )
            }

            item {
                ListItem(
                    selected = false,
                    onClick = { audioAlerts = !audioAlerts },
                    headlineContent = { Text("Audio Alert on Unknown Faces") },
                    supportingContent = { Text("Play acoustic chime when an unrecognized visitor is detected") },
                    trailingContent = {
                        Switch(
                            checked = audioAlerts,
                            onCheckedChange = null,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF80D8FF),
                                checkedTrackColor = Color(0xFF004D40)
                            )
                        )
                    }
                )
            }
        }
    }
}
