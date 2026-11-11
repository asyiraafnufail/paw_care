package com.pawcare.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val PawShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

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
    onBackground = Color(0xFF191C1A),
    surface = PawSurface,
    onSurface = Color(0xFF191C1A),
    surfaceVariant = PawSurfaceVariant,
    onSurfaceVariant = Color(0xFF3F4946),
    outline = PawOutline,
    error = StatusCancelledColor,
    onError = Color(0xFFFFFFFF)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF81C784),
    onPrimary = Color(0xFF1B5E20),
    primaryContainer = Color(0xFF2E7D32),
    onPrimaryContainer = Color(0xFFE8F5E9),
    secondary = Color(0xFF81D4FA),
    onSecondary = Color(0xFF01579B),
    secondaryContainer = Color(0xFF0288D1),
    onSecondaryContainer = Color(0xFFE1F5FE),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE1E3DF),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE1E3DF),
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFBFC9C2),
    outline = Color(0xFF757575),
    error = Color(0xFFEF9A9A),
    onError = Color(0xFF601410)
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
        shapes = PawShapes,
        content = content
    )
}
