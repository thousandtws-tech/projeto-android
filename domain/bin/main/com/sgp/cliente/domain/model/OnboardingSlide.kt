package com.sgp.cliente.domain.model

/**
 * Slide do onboarding publicado no CMS do provedor — página da
 * primeira abertura do app (mídia + título + subtítulo). [videoUrl]
 * presente toca no card no lugar da imagem. Feed vazio = o app mostra
 * as páginas embutidas (fallback offline).
 */
data class OnboardingSlide(
    val id: String,
    val title: String,
    val subtitle: String,
    val imageUrl: String? = null,
    val videoUrl: String? = null,
    val position: Int = 0,
)
