package com.sgp.cliente.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.core.graphics.toColorInt

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

/** `#rrggbb`/`#aarrggbb` → [Color]; qualquer outra coisa → `null`
 * (o backend valida, mas a UI nunca confia em string crua). */
internal fun parseBrandColor(hex: String?): Color? {
    val value = hex?.trim()?.takeIf { it.isNotBlank() } ?: return null
    return runCatching { Color(value.toColorInt()) }.getOrNull()
}

/** Mistura linear com [other] — [ratio] = peso do [other] (0..1). */
private fun Color.blend(other: Color, ratio: Float): Color = Color(
    red = red + (other.red - red) * ratio,
    green = green + (other.green - green) * ratio,
    blue = blue + (other.blue - blue) * ratio,
)

/** Container tonal derivado da seed — claro vira pastel, escuro vira tom profundo. */
private fun Color.tonalContainer(darkTheme: Boolean): Color =
    blend(if (darkTheme) Color.Black else Color.White, if (darkTheme) 0.72f else 0.84f)

/** Texto/ícone sobre a container — leitura garantida sobre o tom derivado. */
private fun Color.onTonalContainer(darkTheme: Boolean): Color =
    blend(if (darkTheme) Color.White else Color.Black, if (darkTheme) 0.82f else 0.74f)

/** Contraste sobre a cor cheia — branco ou tinta escura pela luminância. */
private fun Color.contrastOn(): Color =
    if (luminance() > 0.32f) BrandInkDark else Color.White

/**
 * App theme. Dynamic color is intentionally OFF so the provider's brand
 * identity stays consistent.
 *
 * White-label: [brandPrimary]/[brandAccent] (hex `#rrggbb`, vindos do
 * `GET /branding` do CRM) viram seeds de `primary`/`secondary`/`tertiary`
 * com containers derivadas por mistura tonal. Nulos/inválidos = paleta
 * embutida — o app nunca abre sem cor por config quebrada.
 */
@Composable
fun SgpTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    brandPrimary: String? = null,
    brandAccent: String? = null,
    content: @Composable () -> Unit,
) {
    val scheme = remember(darkTheme, brandPrimary, brandAccent) {
        buildScheme(
            darkTheme = darkTheme,
            seedPrimary = parseBrandColor(brandPrimary),
            seedAccent = parseBrandColor(brandAccent),
        )
    }
    MaterialTheme(
        colorScheme = scheme,
        typography = SgpTypography,
        content = content,
    )
}

private fun buildScheme(darkTheme: Boolean, seedPrimary: Color?, seedAccent: Color?) =
    when {
        seedPrimary == null && seedAccent == null -> if (darkTheme) DarkColors else LightColors
        else -> {
            val base = if (darkTheme) DarkColors else LightColors
            val primary = seedPrimary ?: base.primary
            val accent = seedAccent ?: seedPrimary ?: base.secondary
            base.copy(
                primary = primary,
                onPrimary = primary.contrastOn(),
                primaryContainer = primary.tonalContainer(darkTheme),
                onPrimaryContainer = primary.onTonalContainer(darkTheme),
                secondary = accent,
                onSecondary = accent.contrastOn(),
                secondaryContainer = accent.tonalContainer(darkTheme),
                onSecondaryContainer = accent.onTonalContainer(darkTheme),
                tertiary = accent.tonalContainer(darkTheme)
                    .blend(if (darkTheme) Color.White else Color.Black, 0.4f),
                onTertiary = accent.contrastOn(),
                inversePrimary = primary,
            )
        }
    }
