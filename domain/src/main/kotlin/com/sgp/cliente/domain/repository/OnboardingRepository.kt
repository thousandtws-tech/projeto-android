package com.sgp.cliente.domain.repository

import com.sgp.cliente.core.common.result.AppResult
import com.sgp.cliente.domain.model.OnboardingSlide
import kotlinx.coroutines.flow.StateFlow

/**
 * Feed de slides de onboarding do CMS — cache quente atualizado por
 * [refresh] (init do onboarding) e pelo push silencioso `cms_refresh`
 * que o backend dispara a cada mutação no CMS. Feed vazio/falha não
 * derruba a tela: o caller cai nas páginas embutidas.
 */
interface OnboardingRepository {
    /** Slides publicados, na ordem da paginação — vazio até o primeiro refresh. */
    val slides: StateFlow<List<OnboardingSlide>>
    suspend fun refresh(): AppResult<Unit>
}
