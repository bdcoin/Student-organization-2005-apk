package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

/**
 * Subtle, calibrated Material 3 elevation scale for a clean, modern financial interface.
 * Uses restrained elevation steps paired with surface container tonal shifts and hairline borders.
 */
@Immutable
data class AppElevation(
    val level0: Dp = 0.dp,
    val level1: Dp = 1.dp,
    val level2: Dp = 2.dp,
    val level3: Dp = 3.dp,
    val level4: Dp = 4.dp
)

val LocalAppElevation = staticCompositionLocalOf { AppElevation() }

private val DarkColorScheme = darkColorScheme(
    primary = Sapphire400,
    onPrimary = Sapphire950,
    primaryContainer = Sapphire800,
    onPrimaryContainer = Sapphire100,
    inversePrimary = Sapphire700,
    secondary = Cyan400,
    onSecondary = Sapphire950,
    secondaryContainer = Cyan900,
    onSecondaryContainer = Cyan100,
    tertiary = Champagne400,
    onTertiary = Sapphire950,
    tertiaryContainer = Champagne900,
    onTertiaryContainer = Champagne100,
    background = SurfaceDark,
    onBackground = Slate50,
    surface = SurfaceLowDark,
    onSurface = Slate50,
    surfaceVariant = SurfaceHighDark,
    onSurfaceVariant = Slate200,
    surfaceTint = Sapphire400,
    inverseSurface = Slate100,
    inverseOnSurface = Slate900,
    error = Crimson400,
    onError = Crimson900,
    errorContainer = Crimson900,
    onErrorContainer = Crimson100,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    scrim = Color(0xCC020617),
    surfaceBright = SurfaceBrightDark,
    surfaceDim = SurfaceLowestDark,
    surfaceContainerLowest = SurfaceLowestDark,
    surfaceContainerLow = SurfaceLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceHighDark,
    surfaceContainerHighest = SurfaceHighestDark
)

private val LightColorScheme = lightColorScheme(
    primary = Sapphire700,
    onPrimary = Color.White,
    primaryContainer = Sapphire100,
    onPrimaryContainer = Sapphire900,
    inversePrimary = Sapphire400,
    secondary = Cyan600,
    onSecondary = Color.White,
    secondaryContainer = Cyan100,
    onSecondaryContainer = Cyan900,
    tertiary = Champagne600,
    onTertiary = Color.White,
    tertiaryContainer = Champagne100,
    onTertiaryContainer = Champagne900,
    background = SurfaceLight,
    onBackground = Slate900,
    surface = SurfaceLowestLight,
    onSurface = Slate900,
    surfaceVariant = SurfaceContainerLight,
    onSurfaceVariant = Slate700,
    surfaceTint = Sapphire700,
    inverseSurface = Slate900,
    inverseOnSurface = Slate50,
    error = Crimson600,
    onError = Color.White,
    errorContainer = Crimson100,
    onErrorContainer = Crimson700,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    scrim = Color(0x990F172A),
    surfaceBright = SurfaceLowestLight,
    surfaceDim = SurfaceDimLight,
    surfaceContainerLowest = SurfaceLowestLight,
    surfaceContainerLow = SurfaceLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceHighLight,
    surfaceContainerHighest = SurfaceHighestLight
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

object AppTheme {
    val elevation: AppElevation
        @Composable
        @ReadOnlyComposable
        get() = LocalAppElevation.current
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val elevation = AppElevation()
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, view)
            // TopAppBars and Hero headers use dark Royal Sapphire surfaces, so light status bar icons remain crisp
            insetsController.isAppearanceLightStatusBars = false
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalAppElevation provides elevation) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}
