package com.pawcare.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = PawPrimary,
    onPrimary = PawOnPrimary,
    primaryContainer = PawPrimaryContainer,
    onPrimaryContainer = PawOnPrimaryContainer,
    secondary = PawSecondary,
    onSecondary = PawOnSecondary,
    secondaryContainer = PawSecondaryContainer,
    onSecondaryContainer = PawOnSecondaryContainer,
    background = PawBackground,
    surface = PawSurface,
    surfaceVariant = PawSurfaceVariant,
    outline = PawOutline,
    error = StatusCancelledColor
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF81C784),
    onPrimary = Color(0xFF1B5E20),
    primaryContainer = Color(0xFF2E7D32),
    onPrimaryContainer = Color(0xFFE8F5E9),
    secondary = Color(0xFF81D4FA),
    onSecondary = Color(0xFF01579B),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    surfaceVariant = Color(0xFF2C2C2C),
    outline = Color(0xFF757575),
    error = Color(0xFFEF9A9A)
)

@Composable
fun PawCareTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
