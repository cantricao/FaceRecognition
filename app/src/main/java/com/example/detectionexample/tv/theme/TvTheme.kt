package com.example.detectionexample.tv.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.ColorScheme
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Typography
import androidx.tv.material3.darkColorScheme

/**
 * 10-foot TV color scheme with high contrast optimized for TV panels
 * viewed from approximately 3 meters away.
 */
private val TvDarkColorScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFF80D8FF),
    onPrimary = Color(0xFF00363A),
    primaryContainer = Color(0xFF004D40),
    onPrimaryContainer = Color(0xFFA7FFEB),
    secondary = Color(0xFFFFD54F),
    onSecondary = Color(0xFF3E2723),
    background = Color(0xFF121212),
    onBackground = Color(0xFFEEEEEE),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFCCCCCC),
    border = Color(0xFF444444)
)

/**
 * Modern Jetpack Compose for TV Theme wrapping [MaterialTheme] from androidx.tv.material3.
 */
@Composable
fun TvTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TvDarkColorScheme,
        typography = Typography(),
        content = content
    )
}
