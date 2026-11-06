package com.example.detectionexample.wear.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnDefaults
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListHeaderDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TitleCard
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight

data class WearIdentityItem(
    val id: String,
    val name: String,
    val confidence: Float,
)

/**
 * Screen displaying enrolled face identities on Wear OS using TransformingLazyColumn and TitleCards.
 */
@Composable
fun WearIdentitiesScreen(
    onIdentityClick: (WearIdentityItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val columnState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    val demoIdentities = listOf(
        WearIdentityItem("1", "Alice Smith", 98.4f),
        WearIdentityItem("2", "Bob Jones", 95.1f),
        WearIdentityItem("3", "Charlie Brown", 91.8f),
        WearIdentityItem("4", "Diana Prince", 89.2f),
    )

    ScreenScaffold(
        scrollState = columnState,
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
                        text = "Identities",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }

            items(demoIdentities) { identity ->
                TitleCard(
                    onClick = { onIdentityClick(identity) },
                    title = {
                        Text(
                            text = identity.name,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(CardDefaults.minimumVerticalListContentPadding),
                    transformation = SurfaceTransformation(transformationSpec),
                ) {
                    Text(
                        text = "Confidence: ${identity.confidence}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }
    }
}
