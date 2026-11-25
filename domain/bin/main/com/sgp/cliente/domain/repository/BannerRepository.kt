package com.sgp.cliente.domain.repository

import com.sgp.cliente.core.common.result.AppResult
import com.sgp.cliente.domain.model.Banner
import kotlinx.coroutines.flow.StateFlow

/**
 * Feed de banners do CMS — cache quente atualizado por [refresh] (init,
 * pull-to-refresh) e pelo push silencioso `cms_refresh` que o backend
 * dispara a cada mutação no CMS. Falha de rede não derruba a tela: o
 * caller decide se o carrossel aparece ou não.
 */
interface BannerRepository {
    /** Banners publicados, na ordem do carrossel — vazio até o primeiro refresh. */
    val banners: StateFlow<List<Banner>>
    suspend fun refresh(): AppResult<Unit>
}
