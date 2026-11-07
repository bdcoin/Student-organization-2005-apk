package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Emerald400Compat = Color(0xFF34D399)

private val DarkColorScheme = darkColorScheme(
    primary = Emerald500,
    onPrimary = Emerald950,
    primaryContainer = Emerald900,
    onPrimaryContainer = Emerald100,
    secondary = Gold400,
    onSecondary = Slate950,
    secondaryContainer = Color(0xFF452704),
    onSecondaryContainer = Gold100,
    tertiary = Emerald400Compat,
    background = Slate950,
    onBackground = Slate50,
    surface = Slate900,
    onSurface = Slate50,
    surfaceVariant = Slate800,
    onSurfaceVariant = Slate200,
    error = Color(0xFFF87171),
    onError = Slate950
)

private val LightColorScheme = lightColorScheme(
    primary = Emerald900,
    onPrimary = Color.White,
    primaryContainer = Emerald100,
    onPrimaryContainer = Emerald950,
    secondary = Gold600,
    onSecondary = Color.White,
    secondaryContainer = Gold100,
    onSecondaryContainer = Color(0xFF451A03),
    tertiary = Emerald700,
    background = Slate50,
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate700,
    error = Crimson600,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
