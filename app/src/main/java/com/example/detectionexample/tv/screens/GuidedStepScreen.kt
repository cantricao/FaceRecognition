package com.example.detectionexample.tv.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.CompactCard
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

/**
 * 10-foot GuidedStepScreen replacing legacy GuidedStepSupportFragment.
 *
 * Implements:
 * - 40% / 60% Split-screen layout (left guidance pane, right action choices).
 * - Constrained reading width (max 600.dp) to avoid eye fatigue.
 * - Initial focus on the first action button via [FocusRequester].
 * - Symmetrical 2D spatial D-pad navigation.
 */
@Composable
fun GuidedStepScreen(
    onStepCompleted: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedOption by remember { mutableIntStateOf(0) }
    val firstActionFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        firstActionFocusRequester.requestFocus()
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF141414))
            .padding(horizontal = 48.dp, vertical = 32.dp)
    ) {
        // Left Guidance Pane (40% width)
        Column(
            modifier = Modifier
                .weight(0.4f)
                .fillMaxHeight()
                .padding(end = 32.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Face Recognition Setup",
                style = MaterialTheme.typography.headlineLarge,
                color = Color(0xFF80D8FF)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Welcome to the 10-foot Face Recognition security dashboard. Choose an automated detection mode to begin monitoring network video streams on your TV.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFCCCCCC),
                modifier = Modifier.widthIn(max = 500.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onBack
            ) {
                Text("Return to Browse")
            }
        }

        // Right Action Pane (60% width)
        Column(
            modifier = Modifier
                .weight(0.6f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            val options = listOf(
                "Standard Mode: BlazeFace + MobileFaceNet with balanced accuracy and frame rate." to "Recommended for standard living room or monitoring office.",
                "High Precision Mode: Multi-angle face embedding matching with confidence threshold >= 90%." to "Best for perimeter security and high-security access control points.",
                "Low Bandwidth Mode: Downsampled stream inference optimized for Wi-Fi TV setups." to "Preserves local network bandwidth while logging recognized visitors."
            )

            options.forEachIndexed { index, (title, subtitle) ->
                CompactCard(
                    onClick = {
                        selectedOption = index
                        onStepCompleted()
                    },
                    scale = CardDefaults.scale(focusedScale = 1.05f),
                    image = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    if (selectedOption == index) Color(0xFF004D40) else Color(0xFF263238)
                                )
                        )
                    },
                    title = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    },
                    subtitle = {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFAAAAAA)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .then(
                            if (index == 0) Modifier.focusRequester(firstActionFocusRequester) else Modifier
                        )
                )
            }
        }
    }
}
