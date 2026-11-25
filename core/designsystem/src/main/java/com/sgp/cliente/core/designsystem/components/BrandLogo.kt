package com.sgp.cliente.core.designsystem.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil3.compose.SubcomposeAsyncImage
import com.sgp.cliente.core.designsystem.R

/**
 * URL da logo do tenant provida na raiz (MainActivity) — ambient como o
 * tema: qualquer `BrandLogo` veste a marca remota sem threading de
 * parâmetro pela árvore. `null` = identidade embutida do build.
 */
val LocalBrandLogoUrl = compositionLocalOf<String?> { null }

/**
 * Lockup da marca do tenant — veste a logo remota do `GET /branding`
 * quando configurada no CRM e cai na identidade embutida do build em
 * qualquer outro caso (sem URL, rede fora, imagem quebrada). O fallback
 * nunca falha: drawable empacotado.
 *
 * [tint] só pinta o fallback embutido — a logo remota já vem colorida
 * no arquivo e seria destruída pelo filtro.
 */
@Composable
fun BrandLogo(
    contentDescription: String?,
    modifier: Modifier = Modifier,
    logoUrl: String? = LocalBrandLogoUrl.current,
    tint: Color? = null,
) {
    val trimmed = logoUrl?.trim()?.takeIf(String::isNotBlank)
    if (trimmed != null) {
        SubcomposeAsyncImage(
            model = trimmed,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            // Rede fora / imagem inválida / ainda carregando → lockup
            // embutido, mesmo espaço, sem flicker nem quebra de layout.
            loading = { FallbackLogo(contentDescription, tint, Modifier.fillMaxSize()) },
            error = { FallbackLogo(contentDescription, tint, Modifier.fillMaxSize()) },
            modifier = modifier,
        )
    } else {
        FallbackLogo(contentDescription, tint, modifier)
    }
}

@Composable
private fun FallbackLogo(contentDescription: String?, tint: Color?, modifier: Modifier) {
    Image(
        painter = painterResource(R.drawable.jdfibra_lockup),
        contentDescription = contentDescription,
        colorFilter = tint?.let { ColorFilter.tint(it) },
        modifier = modifier,
    )
}
