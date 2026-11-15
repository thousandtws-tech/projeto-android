package com.sgp.cliente.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sgp.cliente.core.common.logging.AppLog
import com.sgp.cliente.domain.model.CustomerSession
import com.sgp.cliente.domain.model.SessionState
import com.sgp.cliente.domain.usecase.auth.LogoutUseCase
import com.sgp.cliente.domain.usecase.auth.ObserveSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val session: CustomerSession? = null,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    private val logoutUseCase: LogoutUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeSession().collect { state ->
                _uiState.value = ProfileUiState(
                    session = (state as? SessionState.LoggedIn)?.session,
                )
            }
        }
    }

    // No post-logout event: navigation is driven by the session observer in
    // SgpApp. Emitting to a SharedFlow here used to suspend on the main
    // thread while the store was being torn down, starving the event loop.
    fun logout() {
        AppLog.i(TAG, "logout requested")
        viewModelScope.launch {
            logoutUseCase()
        }
    }

    private companion object {
        const val TAG = "feature.profile"
    }
}
