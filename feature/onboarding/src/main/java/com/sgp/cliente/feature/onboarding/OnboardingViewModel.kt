package com.sgp.cliente.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sgp.cliente.domain.model.OnboardingSlide
import com.sgp.cliente.domain.repository.SettingsRepository
import com.sgp.cliente.domain.usecase.onboarding.ObserveOnboardingUseCase
import com.sgp.cliente.domain.usecase.onboarding.RefreshOnboardingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Marks the first-run onboarding as completed so it is shown only once. */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    observeOnboarding: ObserveOnboardingUseCase,
    private val refreshOnboarding: RefreshOnboardingUseCase,
) : ViewModel() {

    /** Slides do CMS — vazio = tela mostra as páginas embutidas. */
    val slides: StateFlow<List<OnboardingSlide>> = observeOnboarding()

    init {
        // Cache quente já pode estar preenchido pelo sinal FCM; o fetch
        // cobre instalação nova / app recém-aberto.
        viewModelScope.launch { refreshOnboarding() }
    }

    fun complete() {
        viewModelScope.launch { settingsRepository.setOnboardingSeen() }
    }
}
