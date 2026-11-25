package com.sgp.cliente.domain.usecase.onboarding

import com.sgp.cliente.core.common.result.AppResult
import com.sgp.cliente.domain.model.OnboardingSlide
import com.sgp.cliente.domain.repository.OnboardingRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

/** Slides publicados do CMS para o onboarding — flow quente que emite
 * de novo quando o push silencioso `cms_refresh` dispara [OnboardingRepository.refresh]. */
class ObserveOnboardingUseCase @Inject constructor(
    private val onboardingRepository: OnboardingRepository,
) {
    operator fun invoke(): StateFlow<List<OnboardingSlide>> = onboardingRepository.slides
}

/** Rebusca o feed no backend — init do onboarding e o sinal
 * `cms_refresh` do FCM passam por aqui. */
class RefreshOnboardingUseCase @Inject constructor(
    private val onboardingRepository: OnboardingRepository,
) {
    suspend operator fun invoke(): AppResult<Unit> = onboardingRepository.refresh()
}
