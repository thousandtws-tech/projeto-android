package com.sgp.cliente

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sgp.cliente.core.network.api.BaseUrlProvider
import com.sgp.cliente.domain.model.SessionState
import com.sgp.cliente.domain.repository.AuthRepository
import com.sgp.cliente.domain.repository.SettingsRepository
import com.sgp.cliente.domain.usecase.auth.ObserveSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Root view model: exposes the session state used to pick the navigation
 * graph, keeps [BaseUrlProvider] in sync with the persisted override
 * falling back to the flavor default, and discovers the Central App
 * (id + X-TOKEN) on first run when the flavor ships a base URL.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository,
    private val baseUrlProvider: BaseUrlProvider,
) : ViewModel() {

    val sessionState: StateFlow<SessionState> = observeSession()
        .stateIn(viewModelScope, SharingStarted.Eagerly, SessionState.Loading)

    /** First-run flag: `false` while the onboarding has never been shown. */
    val onboardingSeen: StateFlow<Boolean> = settingsRepository.onboardingSeen
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    init {
        viewModelScope.launch {
            // Transitions only — never session contents (no names, CPF, tokens).
            // StateFlow already conflates equal emissions.
            sessionState.collect { state ->
                Timber.tag(SESSION_TAG).i("session -> %s", state.logName)
            }
        }
        viewModelScope.launch {
            settingsRepository.baseUrlOverride.collect { override ->
                baseUrlProvider.update(override ?: EnvConfig.DEFAULT_BASE_URL)
            }
        }
        viewModelScope.launch {
            // Once a central is stored (pre-seeded or user-discovered),
            // startup touches nothing.
            if (!settingsRepository.centralToken.first().isNullOrBlank()) return@launch

            if (EnvConfig.DEFAULT_CENTRAL_TOKEN.isNotBlank()) {
                settingsRepository.setCentral(
                    EnvConfig.DEFAULT_CENTRAL_ID,
                    EnvConfig.DEFAULT_CENTRAL_TOKEN,
                )
            } else {
                // A build may ship only the base URL — discover the central
                // from the effective server; nothing secret lives in source.
                val effectiveUrl =
                    settingsRepository.baseUrlOverride.first() ?: EnvConfig.DEFAULT_BASE_URL
                if (effectiveUrl.isNotBlank()) {
                    runCatching {
                        authRepository.discoverAndSelectCentral(effectiveUrl)
                    }.onFailure { cause ->
                        Timber.tag(SESSION_TAG).w(cause, "central discovery failed")
                    }
                }
            }
        }
    }

    private val SessionState.logName: String
        get() = when (this) {
            SessionState.Loading -> "Loading"
            is SessionState.LoggedIn -> "LoggedIn"
            SessionState.LoggedOut -> "LoggedOut"
        }

    private companion object {
        const val SESSION_TAG = "app.session"
    }
}
