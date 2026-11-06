package com.example.detectionexample.tv.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.CompactCard
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.example.detectionexample.tv.model.TvVideo

/**
 * Modern 10-foot BrowseScreen implementing immersive list architecture,
 * dynamic backdrop preview, 2D D-pad spatial focus navigation, and row focus recollection.
 *
 * Fully replaces legacy Leanback BrowseSupportFragment, Presenter, and ArrayObjectAdapter.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BrowseScreen(
    categories: Map<String, List<TvVideo>>,
    onVideoClick: (TvVideo) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onGuidedStepClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val initialVideo = remember(categories) { categories.values.firstOrNull()?.firstOrNull() }
    var focusedVideo by remember { mutableStateOf(initialVideo) }
    var focusedCategoryIndex by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    val firstCardFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        firstCardFocusRequester.requestFocus()
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        // Dynamic Backdrop image reflecting the currently focused video
        AsyncImage(
            model = focusedVideo?.bgImageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Gradient Scrim to ensure text legibility and 10-foot contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.5f),
                            Color.Black.copy(alpha = 0.85f),
                            Color.Black
                        )
                    )
                )
        )

        // Pivot scrolling layout keeping active row visible at 35% from the top
        PositionFocusedItemInLazyLayout(parentFraction = 0.35f, childFraction = 0.5f) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(top = 36.dp, bottom = 64.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Top App Action Bar for TV (Search, Settings, Guided Onboarding)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 48.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onSearchClick
                        ) {
                            Text("Search")
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Button(
                            onClick = onSettingsClick
                        ) {
                            Text("Settings")
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Button(
                            onClick = onGuidedStepClick
                        ) {
                            Text("Guided Help")
                        }
                    }
                }

                // Categorized Rows with Reshaping Immersive Hero Row
                itemsIndexed(categories.entries.toList()) { catIndex, (categoryName, videos) ->
                    Column {
                        if (catIndex == 0 && focusedCategoryIndex == 0) {
                            // Immersive Hero Banner on Row 0
                            Column(
                                modifier = Modifier
                                    .heightIn(min = 180.dp)
                                    .padding(horizontal = 48.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = focusedVideo?.title ?: categoryName,
                                    style = MaterialTheme.typography.displayMedium,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = focusedVideo?.description ?: "",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color(0xFFDDDDDD),
                                    modifier = Modifier.fillMaxWidth(0.65f)
                                )
                            }
                        } else {
                            Text(
                                text = categoryName,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 48.dp, vertical = 8.dp)
                            )
                        }

                        // Category LazyRow with Row Focus Recollection (Modifier.focusRestorer)
                        LazyRow(
                            modifier = Modifier.focusRestorer(),
                            contentPadding = PaddingValues(horizontal = 48.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            itemsIndexed(videos) { vidIndex, video ->
                                CompactCard(
                                    onClick = { onVideoClick(video) },
                                    scale = CardDefaults.scale(focusedScale = 1.1f),
                                    image = {
                                        AsyncImage(
                                            model = video.cardImageUrl,
                                            contentDescription = video.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    },
                                    title = { Text(video.title) },
                                    subtitle = { Text(video.duration) },
                                    modifier = Modifier
                                        .width(260.dp)
                                        .height(180.dp)
                                        .then(
                                            if (catIndex == 0 && vidIndex == 0) {
                                                Modifier.focusRequester(firstCardFocusRequester)
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .onFocusChanged { focusState ->
                                            if (focusState.isFocused) {
                                                focusedVideo = video
                                                focusedCategoryIndex = catIndex
                                            }
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Custom BringIntoViewSpec providing TV pivot scrolling behavior.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PositionFocusedItemInLazyLayout(
    parentFraction: Float = 0.35f,
    childFraction: Float = 0.5f,
    content: @Composable () -> Unit
) {
    val bringIntoViewSpec = remember(parentFraction, childFraction) {
        object : BringIntoViewSpec {
            override fun calculateScrollDistance(
                offset: Float,
                size: Float,
                containerSize: Float
            ): Float {
                if (offset >= 0f && offset <= containerSize * 0.45f) {
                    return 0f
                }
                val initialTargetForLeadingEdge = parentFraction * containerSize - (childFraction * size)
                val targetForLeadingEdge = if (size <= containerSize && (containerSize - initialTargetForLeadingEdge) < size) {
                    containerSize - size
                } else {
                    initialTargetForLeadingEdge
                }
                return offset - targetForLeadingEdge
            }
        }
    }
    CompositionLocalProvider(LocalBringIntoViewSpec provides bringIntoViewSpec, content = content)
}
