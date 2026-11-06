package com.example.detectionexample.wear.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.dynamicColorScheme

/**
 * Material 3 Theme for Wear OS.
 */
@Composable
fun WearTheme(
    content: @Composable () -> Unit,
) {
    val dynamicColors = dynamicColorScheme(LocalContext.current)

    MaterialTheme(
        colorScheme = dynamicColors ?: MaterialTheme.colorScheme,
        content = content,
    )
}
