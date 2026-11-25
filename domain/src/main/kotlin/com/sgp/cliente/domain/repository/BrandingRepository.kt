package com.sgp.cliente.domain.repository

import com.sgp.cliente.core.common.result.AppResult
import com.sgp.cliente.domain.model.TenantBranding
import kotlinx.coroutines.flow.StateFlow

/**
 * Identidade visual do tenant — cache quente atualizado por [refresh]
 * (init do app) e pelo push silencioso `cms_refresh` com `feed=branding`
 * que o backend dispara quando a empresa edita logo/cores no painel.
 * Cache persistido em DataStore: o app veste a última identidade
 * conhecida já no primeiro frame, antes da rede responder.
 */
interface BrandingRepository {
    /** Branding resolvido — campos nulos = identidade embutida do build. */
    val branding: StateFlow<TenantBranding>
    suspend fun refresh(): AppResult<Unit>
}
