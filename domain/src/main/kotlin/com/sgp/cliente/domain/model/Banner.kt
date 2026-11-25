package com.sgp.cliente.domain.model

/**
 * Banner de marketing publicado no CMS do provedor — aparece no
 * carrossel "Novidades" da dashboard. `actionUrl` (opcional) abre no
 * navegador ao tocar no card.
 */
data class Banner(
    val id: String,
    val title: String,
    val imageUrl: String,
    val actionUrl: String? = null,
    val position: Int = 0,
)
