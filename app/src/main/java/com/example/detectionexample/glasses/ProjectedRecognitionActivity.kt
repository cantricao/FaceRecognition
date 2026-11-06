package com.example.detectionexample.glasses

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.xr.glimmer.ActionCard
import androidx.xr.glimmer.Button
import androidx.xr.glimmer.ButtonGroup
import androidx.xr.glimmer.GlimmerTheme
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.TitleChip
import androidx.xr.glimmer.TitleChipDefaults
import androidx.xr.glimmer.googlefonts.createGoogleSansFlexTypography
import androidx.xr.glimmer.onIndirectPointerGesture
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import com.example.detectionexample.models.Person
import com.example.detectionexample.repository.RecognizedPersonRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Projected Activity that runs on the Android host phone and projects its UI to connected
 * display glasses using Jetpack Compose Glimmer.
 *
 * Requirements:
 * - Pure black background (Color.Black) which renders 100% transparent on additive in-lens displays.
 * - Bottom-aligned UI (Alignment.BottomCenter).
 * - High contrast Google Sans Flex typography (>= 18sp).
 * - Touchpad input gesture mapping (tap and swipe).
 * - Glimmer UI primitives only (no Material 3).
 */
@AndroidEntryPoint
@OptIn(ExperimentalComposeUiApi::class, ExperimentalProjectedApi::class)
class ProjectedRecognitionActivity : ComponentActivity() {

    @Inject
    lateinit var recognizedPersonRepository: RecognizedPersonRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable automatic initial focus for focusable elements on display glasses
        ComposeUiFlags.isInitialFocusOnFocusableAvailable = true

        setContent {
            GlimmerTheme(typography = createGoogleSansFlexTypography()) {
                GlassesRecognitionScreen(
                    repository = recognizedPersonRepository,
                    onClose = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun GlassesRecognitionScreen(
    repository: RecognizedPersonRepository,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val persons by repository.personsFlow.collectAsState(initial = emptyList())

    // Default sample person when repository has no registered faces yet
    val samplePersons = remember {
        listOf(
            Person(
                id = "demo_1",
                name = "Alex Johnson",
                score = 94,
                timeStamp = System.currentTimeMillis()
            ),
            Person(
                id = "demo_2",
                name = "Jordan Lee",
                score = 88,
                timeStamp = System.currentTimeMillis()
            ),
            Person(
                id = "demo_3",
                name = "Taylor Smith",
                score = 79,
                timeStamp = System.currentTimeMillis()
            )
        )
    }

    val displayList = if (persons.isNotEmpty()) persons else samplePersons
    var selectedIndex by remember { mutableIntStateOf(0) }
    val currentPerson = displayList.getOrElse(selectedIndex) { displayList.first() }

    var acknowledgedCount by remember { mutableIntStateOf(0) }
    var lastGesture by remember { mutableStateOf("Ready") }

    // On additive displays, pure black renders as 100% transparent.
    // We design from the bottom up and anchor UI to BottomCenter.
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 20.dp, vertical = 24.dp)
            .onIndirectPointerGesture(
                enabled = true,
                onSwipeForward = {
                    if (displayList.isNotEmpty()) {
                        selectedIndex = (selectedIndex + 1) % displayList.size
                        lastGesture = "Next person"
                    }
                },
                onSwipeBackward = {
                    if (displayList.isNotEmpty()) {
                        selectedIndex = if (selectedIndex > 0) selectedIndex - 1 else displayList.size - 1
                        lastGesture = "Previous person"
                    }
                },
                onClick = {
                    acknowledgedCount++
                    lastGesture = "Acknowledge"
                }
            )
            .focusTarget(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Pill header with 3 words or fewer
            TitleChip {
                Text("Face Recognition HUD")
            }

            Spacer(Modifier.height(TitleChipDefaults.associatedContentSpacing))

            // Primary focal card with embedded primary action button
            ActionCard(
                title = {
                    Text(
                        text = currentPerson.name,
                        style = GlimmerTheme.typography.titleMedium
                    )
                },
                subtitle = {
                    Text(
                        text = "Match Confidence: ${currentPerson.score}% • Status: $lastGesture",
                        style = GlimmerTheme.typography.caption
                    )
                },
                action = {
                    Button(
                        onClick = {
                            acknowledgedCount++
                            lastGesture = "Acknowledge"
                        }
                    ) {
                        Text(if (acknowledgedCount > 0) "Acknowledged ($acknowledgedCount)" else "Acknowledge Match")
                    }
                }
            ) {
                Text(
                    text = "ID: ${currentPerson.id} • Active detection in field of view.",
                    style = GlimmerTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(12.dp))

            // ButtonGroup placed outside Card as sibling to avoid focus contention
            ButtonGroup(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        if (displayList.isNotEmpty()) {
                            selectedIndex = if (selectedIndex > 0) selectedIndex - 1 else displayList.size - 1
                            lastGesture = "Previous"
                        }
                    }
                ) {
                    Text("Prev")
                }

                Button(
                    onClick = {
                        if (displayList.isNotEmpty()) {
                            selectedIndex = (selectedIndex + 1) % displayList.size
                            lastGesture = "Next"
                        }
                    }
                ) {
                    Text("Next")
                }

                Button(
                    onClick = onClose
                ) {
                    Text("Dismiss HUD")
                }
            }
        }
    }
}
