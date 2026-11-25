package com.sgp.cliente.domain.usecase.banner

import com.sgp.cliente.core.common.result.AppResult
import com.sgp.cliente.domain.model.Banner
import com.sgp.cliente.domain.repository.BannerRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

/** Banners publicados do CMS para o carrossel — flow quente que emite
 * de novo quando o push silencioso `cms_refresh` dispara [BannerRepository.refresh]. */
class ObserveBannersUseCase @Inject constructor(
    private val bannerRepository: BannerRepository,
) {
    operator fun invoke(): StateFlow<List<Banner>> = bannerRepository.banners
}

/** Rebusca o feed no backend — init da dashboard, pull-to-refresh e o
 * sinal `cms_refresh` do FCM passam por aqui. */
class RefreshBannersUseCase @Inject constructor(
    private val bannerRepository: BannerRepository,
) {
    suspend operator fun invoke(): AppResult<Unit> = bannerRepository.refresh()
}
