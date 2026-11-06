package com.example.detectionexample.wear.screens

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnDefaults
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.material3.AppCard
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListHeaderDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight

/**
 * Main dashboard screen on Wear OS with Wear Compose Material 3 components:
 * ScreenScaffold, TransformingLazyColumn with rotary snapping, AppCard, Buttons, and EdgeButton.
 */
@Composable
fun WearHomeScreen(
    onNavigateToIdentities: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val columnState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()
    var isDetectionActive by remember { mutableStateOf(true) }

    ScreenScaffold(
        scrollState = columnState,
        edgeButton = {
            EdgeButton(
                onClick = onNavigateToIdentities,
                modifier = Modifier.scrollable(
                    columnState,
                    orientation = Orientation.Vertical,
                    reverseDirection = true,
                    overscrollEffect = rememberOverscrollEffect(),
                )
            ) {
                Text(
                    text = "Identities",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        },
        modifier = modifier,
    ) { contentPadding ->
        TransformingLazyColumn(
            state = columnState,
            contentPadding = contentPadding,
            flingBehavior = TransformingLazyColumnDefaults.snapFlingBehavior(columnState),
            rotaryScrollableBehavior = RotaryScrollableDefaults.snapBehavior(columnState),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                ListHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),
                    transformation = SurfaceTransformation(transformationSpec),
                ) {
                    Text(
                        text = "Face Recognition",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }

            item {
                AppCard(
                    onClick = { isDetectionActive = !isDetectionActive },
                    appName = { Text("Model Status", style = MaterialTheme.typography.labelSmall) },
                    title = {
                        Text(
                            text = if (isDetectionActive) "Active • MobileFaceNet" else "Paused",
                            style = MaterialTheme.typography.titleSmall,
                        )
                    },
                    time = { Text("Live", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                    transformation = SurfaceTransformation(transformationSpec),
                ) {
                    Text(
                        text = if (isDetectionActive) "4 enrolled identities active" else "Detection paused",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            item {
                Button(
                    onClick = { isDetectionActive = !isDetectionActive },
                    label = {
                        Text(
                            text = if (isDetectionActive) "Pause Detection" else "Resume Detection",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                    transformation = SurfaceTransformation(transformationSpec),
                )
            }

            item {
                Button(
                    onClick = onNavigateToIdentities,
                    label = {
                        Text(
                            text = "Enrolled Persons",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                    transformation = SurfaceTransformation(transformationSpec),
                )
            }
        }
    }
}
