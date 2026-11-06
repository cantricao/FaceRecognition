package com.example.detectionexample.tv.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.CompactCard
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.example.detectionexample.tv.model.TvMediaCatalog
import com.example.detectionexample.tv.model.TvVideo

/**
 * Modern 10-foot TV SearchScreen replacing legacy SearchSupportFragment.
 *
 * Implements:
 * - Focusable TV Surface wrapping [OutlinedTextField] with focused border outline.
 * - Back-key interception (Key.Back, Key.Escape) to clear focus without exiting.
 * - Enter key D-pad navigation down to the results grid.
 * - Live filtering in [LazyVerticalGrid].
 */
@Composable
fun SearchScreen(
    onVideoClick: (TvVideo) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val inputFocusRequester = remember { FocusRequester() }

    val filteredVideos = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            TvMediaCatalog.sampleVideos
        } else {
            TvMediaCatalog.sampleVideos.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                    it.description.contains(searchQuery, ignoreCase = true) ||
                    it.category.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    LaunchedEffect(Unit) {
        inputFocusRequester.requestFocus()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF101010))
            .padding(horizontal = 48.dp, vertical = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Search Feeds & Logs",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )
            Button(onClick = onBack) {
                Text("Close Search")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // TV Surface wrapper for D-pad focus outline and IME attachment
        Surface(
            onClick = { inputFocusRequester.requestFocus() },
            border = ClickableSurfaceDefaults.border(
                focusedBorder = Border(border = BorderStroke(2.dp, Color(0xFF80D8FF)))
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.01f),
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .focusRequester(inputFocusRequester)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Type camera, person, or category name...", color = Color.Gray) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    focusManager.moveFocus(FocusDirection.Down)
                }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .onPreviewKeyEvent { event ->
                        // Intercept Back & Escape on KeyUp to clear focus without exiting
                        if ((event.key == Key.Back || event.key == Key.Escape) && event.type == KeyEventType.KeyUp) {
                            focusManager.clearFocus()
                            true
                        } else if (event.type == KeyEventType.KeyDown &&
                            (event.key == Key.Enter || event.key == Key.NumPadEnter || event.key == Key.DirectionDown)
                        ) {
                            focusManager.moveFocus(FocusDirection.Down)
                            true
                        } else {
                            false
                        }
                    }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Search Results in 4-column 10-foot grid
        Text(
            text = "Results (${filteredVideos.size})",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFFAAAAAA)
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            contentPadding = PaddingValues(bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredVideos, key = { it.id }) { video ->
                CompactCard(
                    onClick = { onVideoClick(video) },
                    scale = CardDefaults.scale(focusedScale = 1.08f),
                    image = {
                        AsyncImage(
                            model = video.cardImageUrl,
                            contentDescription = video.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    },
                    title = { Text(video.title) },
                    subtitle = { Text(video.category) },
                    modifier = Modifier.height(180.dp)
                )
            }
        }
    }
}
