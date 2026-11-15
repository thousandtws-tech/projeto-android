package com.sgp.cliente.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Brand40,
    onPrimary = Slate99,
    primaryContainer = Brand95,
    onPrimaryContainer = BrandInk,
    secondary = Brand50,
    onSecondary = Slate99,
    secondaryContainer = Brand90,
    onSecondaryContainer = BrandInk,
    tertiary = Brand30,
    onTertiary = Slate99,
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
    outline = Slate40,
    inverseSurface = Slate10,
    inverseOnSurface = Slate95,
    inversePrimary = Brand80,
)

private val DarkColors = darkColorScheme(
    primary = Brand80,
    onPrimary = Brand10,
    primaryContainer = Brand30,
    onPrimaryContainer = Brand95,
    secondary = Brand80,
    onSecondary = Brand10,
    secondaryContainer = Brand20,
    onSecondaryContainer = Brand95,
    tertiary = Brand80,
    onTertiary = Brand10,
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
    outline = Slate60,
    inverseSurface = Slate95,
    inverseOnSurface = Slate10,
    inversePrimary = Brand40,
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
