package com.example.detectionexample.wear.previews

import androidx.compose.runtime.Composable
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import com.example.detectionexample.wear.screens.WearDetailScreen
import com.example.detectionexample.wear.screens.WearHomeScreen
import com.example.detectionexample.wear.screens.WearIdentitiesScreen
import com.example.detectionexample.wear.theme.WearTheme

/**
 * Previews for Wear OS companion screens utilizing Wear Preview annotations.
 */
@WearPreviewDevices
@WearPreviewFontScales
@Composable
fun WearHomeScreenPreview() {
    WearTheme {
        WearHomeScreen(
            onNavigateToIdentities = {}
        )
    }
}

@WearPreviewDevices
@Composable
fun WearIdentitiesScreenPreview() {
    WearTheme {
        WearIdentitiesScreen(
            onIdentityClick = {}
        )
    }
}

@WearPreviewDevices
@Composable
fun WearDetailScreenPreview() {
    WearTheme {
        WearDetailScreen(
            id = "1",
            name = "Alice Smith",
            confidence = 98.4f,
            onBack = {}
        )
    }
}
