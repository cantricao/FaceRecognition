package com.example.detectionexample.wear.screens

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnDefaults
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.material3.AppCard
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListHeaderDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TitleCard
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight

/**
 * Detail view for an enrolled face identity on Wear OS using Wear Compose Material 3.
 * Adheres to AppScaffold / ScreenScaffold hierarchy, rotary snapping, and EdgeButton back affordance.
 */
@Composable
fun WearDetailScreen(
    id: String,
    name: String,
    confidence: Float,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val columnState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    ScreenScaffold(
        scrollState = columnState,
        edgeButton = {
            EdgeButton(
                onClick = onBack,
                modifier = Modifier.scrollable(
                    columnState,
                    orientation = Orientation.Vertical,
                    reverseDirection = true,
                    overscrollEffect = rememberOverscrollEffect(),
                )
            ) {
                Text(
                    text = "Back",
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
                        text = "Face Profile",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }

            item {
                AppCard(
                    onClick = { /* Detail inspection */ },
                    appName = { Text("Identity #$id", style = MaterialTheme.typography.labelSmall) },
                    title = {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    time = { Text("Enrolled", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(CardDefaults.minimumVerticalListContentPadding),
                    transformation = SurfaceTransformation(transformationSpec),
                ) {
                    Text(
                        text = "Matching Confidence: $confidence%\nModel: MobileFaceNet\nEmbedding dimension: 192",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                TitleCard(
                    onClick = { /* Biometric telemetry */ },
                    title = {
                        Text(
                            text = "Liveness Verified",
                            style = MaterialTheme.typography.titleSmall,
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(CardDefaults.minimumVerticalListContentPadding),
                    transformation = SurfaceTransformation(transformationSpec),
                ) {
                    Text(
                        text = "Anti-spoof score: 0.96\nTracking state: Locked",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }
    }
}
