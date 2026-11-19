package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CelestialDarkColorScheme = darkColorScheme(
    primary = AliceGold,
    onPrimary = OnAliceGold,
    primaryContainer = AliceGoldContainer,
    onPrimaryContainer = AliceGoldBright,
    secondary = YuiCyan,
    onSecondary = OnYuiCyan,
    secondaryContainer = YuiCyanDim,
    onSecondaryContainer = YuiCyan,
    tertiary = YuiRose,
    onTertiary = MidnightAbyss,
    tertiaryContainer = YuiRoseDim,
    onTertiaryContainer = YuiRose,
    background = MidnightAbyss,
    onBackground = StarlightWhite,
    surface = MidnightCanvas,
    onSurface = StarlightWhite,
    surfaceVariant = MidnightSurface,
    onSurfaceVariant = StarlightMuted,
    surfaceContainerLowest = MidnightAbyss,
    surfaceContainerLow = MidnightCanvas,
    surfaceContainer = MidnightSurface,
    surfaceContainerHigh = MidnightSurfaceHigh,
    surfaceContainerHighest = MidnightSurfaceHighest,
    outline = MidnightOutline,
    outlineVariant = MidnightOutlineSoft,
    error = StatusCrimson,
    onError = MidnightAbyss,
    errorContainer = StatusCrimsonContainer,
    onErrorContainer = StatusCrimson
)

private val CelestialLightColorScheme = lightColorScheme(
    primary = LightAliceGold,
    onPrimary = AlabasterSurface,
    primaryContainer = Color(0xFFFFF0CC),
    onPrimaryContainer = Color(0xFF3E2900),
    secondary = LightYuiCyan,
    onSecondary = AlabasterSurface,
    secondaryContainer = Color(0xFFD6F4FA),
    onSecondaryContainer = Color(0xFF083344),
    tertiary = Color(0xFFBE185D),
    onTertiary = AlabasterSurface,
    background = AlabasterCanvas,
    onBackground = LightInk,
    surface = AlabasterSurface,
    onSurface = LightInk,
    surfaceVariant = AlabasterSurfaceHigh,
    onSurfaceVariant = LightInkMuted,
    outline = Color(0xFFC8C2B4),
    outlineVariant = Color(0xFFE4DFD3),
    error = Color(0xFFDC2626),
    onError = AlabasterSurface
)

@Composable
fun AliceYuiTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) CelestialDarkColorScheme else CelestialLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
