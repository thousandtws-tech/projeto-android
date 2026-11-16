package com.sgp.cliente.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = BrandNavy,
    onPrimary = Slate99,
    primaryContainer = BrandSky,
    onPrimaryContainer = BrandInkDark,
    secondary = BrandRoyal,
    onSecondary = Slate99,
    secondaryContainer = Brand90,
    onSecondaryContainer = BrandInk,
    tertiary = BrandCyan,
    onTertiary = BrandInkDark,
    error = Error40,
    onError = Slate99,
    errorContainer = Error90,
    onErrorContainer = Error40,
    background = Slate99,
    onBackground = Slate10,
    surface = Slate99,
    onSurface = Slate10,
    surfaceVariant = Slate95,
    onSurfaceVariant = Slate20,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Slate99,
    surfaceContainer = Slate97,
    surfaceContainerHigh = Slate95,
    surfaceContainerHighest = Slate90,
    outline = Slate40,
    inverseSurface = Slate10,
    inverseOnSurface = Slate95,
    inversePrimary = BrandSky,
)

private val DarkColors = darkColorScheme(
    primary = BrandSky,
    onPrimary = BrandInkDark,
    primaryContainer = Brand30,
    onPrimaryContainer = Brand95,
    secondary = BrandElectric,
    onSecondary = Slate99,
    secondaryContainer = Brand20,
    onSecondaryContainer = Brand95,
    tertiary = BrandCyan,
    onTertiary = BrandInkDark,
    error = Error80,
    onError = Error40,
    errorContainer = Error40,
    onErrorContainer = Error90,
    background = Slate10,
    onBackground = Slate95,
    surface = Slate10,
    onSurface = Slate95,
    surfaceVariant = Slate20,
    onSurfaceVariant = Slate95,
    surfaceContainerLowest = Slate05,
    surfaceContainerLow = Slate10,
    surfaceContainer = Slate15,
    surfaceContainerHigh = Slate20,
    surfaceContainerHighest = Slate25,
    outline = Slate60,
    inverseSurface = Slate95,
    inverseOnSurface = Slate10,
    inversePrimary = BrandNavy,
)

/**
 * App theme. Dynamic color is intentionally OFF so the provider's brand
 * identity stays consistent.
 */
@Composable
fun SgpTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = SgpTypography,
        content = content,
    )
}
