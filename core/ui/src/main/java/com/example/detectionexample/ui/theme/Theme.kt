package com.example.detectionexample.ui.theme

import android.annotation.SuppressLint
import android.app.UiModeManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService

private const val HIGH_CONTRAST_THRESHOLD = 0.75f
private const val MEDIUM_CONTRAST_THRESHOLD = 0.25f

val DetectionExampleDarkColorScheme = darkColorScheme(
    primary = Blue80,
    onPrimary = Blue20,
    primaryContainer = Blue30,
    onPrimaryContainer = Blue90,
    inversePrimary = Blue40,
    secondary = DarkBlue80,
    onSecondary = DarkBlue20,
    secondaryContainer = DarkBlue30,
    onSecondaryContainer = DarkBlue90,
    tertiary = Yellow80,
    onTertiary = Yellow20,
    tertiaryContainer = Yellow30,
    onTertiaryContainer = Yellow90,
    error = Red80,
    onError = Red20,
    errorContainer = Red30,
    onErrorContainer = Red90,
    background = Grey10,
    onBackground = Grey90,
    surface = Grey10,
    onSurface = Grey80,
    inverseSurface = Grey90,
    inverseOnSurface = Grey20,
    surfaceVariant = BlueGrey30,
    onSurfaceVariant = BlueGrey80,
    outline = BlueGrey60,
    outlineVariant = BlueGrey30,
    surfaceContainerLowest = Color(0xFF0F1213),
    surfaceContainerLow = Color(0xFF171A1B),
    surfaceContainer = Grey20,
    surfaceContainerHigh = Color(0xFF383C3D),
    surfaceContainerHighest = Color(0xFF434749),
    surfaceDim = Grey10,
    surfaceBright = Color(0xFF383A3B)
)

val DetectionExampleLightColorScheme = lightColorScheme(
    primary = Blue40,
    onPrimary = Color.White,
    primaryContainer = Blue90,
    onPrimaryContainer = Blue10,
    inversePrimary = Blue80,
    secondary = DarkBlue40,
    onSecondary = Color.White,
    secondaryContainer = DarkBlue90,
    onSecondaryContainer = DarkBlue10,
    tertiary = Yellow40,
    onTertiary = Color.White,
    tertiaryContainer = Yellow90,
    onTertiaryContainer = Yellow10,
    error = Red40,
    onError = Color.White,
    errorContainer = Red90,
    onErrorContainer = Red10,
    background = Grey99,
    onBackground = Grey10,
    surface = Grey99,
    onSurface = Grey10,
    inverseSurface = Grey20,
    inverseOnSurface = Grey95,
    surfaceVariant = BlueGrey90,
    onSurfaceVariant = BlueGrey30,
    outline = BlueGrey50,
    outlineVariant = BlueGrey80,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Grey95,
    surfaceContainer = Grey90,
    surfaceContainerHigh = Color(0xFFD6DADA),
    surfaceContainerHighest = Color(0xFFCBD0D0),
    surfaceDim = Color(0xFFD8DADA),
    surfaceBright = Grey99
)

// High-contrast variants for accessibility
val DetectionExampleDarkHighContrastColorScheme = DetectionExampleDarkColorScheme.copy(
    primary = Color.White,
    onPrimary = Color.Black,
    onSurface = Color.White,
    outline = Color.White
)

val DetectionExampleLightHighContrastColorScheme = DetectionExampleLightColorScheme.copy(
    primary = Color.Black,
    onPrimary = Color.White,
    onSurface = Color.Black,
    outline = Color.Black
)

@Composable
fun rememberContrastLevel(): ContrastLevel {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        return ContrastLevel.Standard
    }
    val context = LocalContext.current
    val uiModeManager = remember(context) { context.getSystemService<UiModeManager>() }
        ?: return ContrastLevel.Standard

    var level by remember { mutableStateOf(uiModeManager.contrastLevel()) }

    DisposableEffect(uiModeManager) {
        val executor = ContextCompat.getMainExecutor(context)
        val listener = UiModeManager.ContrastChangeListener { level = uiModeManager.contrastLevel() }
        uiModeManager.addContrastChangeListener(executor, listener)
        onDispose { uiModeManager.removeContrastChangeListener(listener) }
    }
    return level
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
private fun UiModeManager.contrastLevel(): ContrastLevel = when {
    contrast >= HIGH_CONTRAST_THRESHOLD -> ContrastLevel.High
    contrast >= MEDIUM_CONTRAST_THRESHOLD -> ContrastLevel.Medium
    else -> ContrastLevel.Standard
}

@SuppressLint("NewApi")
@Composable
fun DetectionExampleTheme(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    isDynamicColor: Boolean = true,
    contrastLevel: ContrastLevel = rememberContrastLevel(),
    content: @Composable () -> Unit
) {
    val dynamicColor = isDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val myColorScheme: ColorScheme = when {
        dynamicColor && isDarkTheme -> {
            dynamicDarkColorScheme(LocalContext.current)
        }
        dynamicColor && !isDarkTheme -> {
            dynamicLightColorScheme(LocalContext.current)
        }
        isDarkTheme -> when (contrastLevel) {
            ContrastLevel.High -> DetectionExampleDarkHighContrastColorScheme
            ContrastLevel.Medium -> DetectionExampleDarkColorScheme
            ContrastLevel.Standard -> DetectionExampleDarkColorScheme
        }
        else -> when (contrastLevel) {
            ContrastLevel.High -> DetectionExampleLightHighContrastColorScheme
            ContrastLevel.Medium -> DetectionExampleLightColorScheme
            ContrastLevel.Standard -> DetectionExampleLightColorScheme
        }
    }

    MaterialTheme(
        colorScheme = myColorScheme,
        typography = DetectionExampleTypography,
        content = content
    )
}
