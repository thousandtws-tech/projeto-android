package com.sgp.cliente.domain.usecase.branding

import com.sgp.cliente.core.common.result.AppResult
import com.sgp.cliente.domain.model.TenantBranding
import com.sgp.cliente.domain.repository.BrandingRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

/** Branding do tenant para a UI — flow quente que emite de novo quando o
 * push silencioso `cms_refresh` dispara [BrandingRepository.refresh]. */
class ObserveBrandingUseCase @Inject constructor(
    private val brandingRepository: BrandingRepository,
) {
    operator fun invoke(): StateFlow<TenantBranding> = brandingRepository.branding
}

/** Rebusca o branding no backend — init da MainActivity e o sinal
 * `cms_refresh` do FCM passam por aqui. */
class RefreshBrandingUseCase @Inject constructor(
    private val brandingRepository: BrandingRepository,
) {
    suspend operator fun invoke(): AppResult<Unit> = brandingRepository.refresh()
}
