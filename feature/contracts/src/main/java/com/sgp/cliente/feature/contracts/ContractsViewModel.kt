package com.sgp.cliente.feature.contracts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sgp.cliente.domain.model.Contract
import com.sgp.cliente.domain.model.SessionState
import com.sgp.cliente.domain.usecase.auth.ObserveSessionUseCase
import com.sgp.cliente.domain.usecase.contract.SelectContractUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ContractsUiState(
    val loading: Boolean = true,
    val contracts: List<Contract> = emptyList(),
    val selectedContractId: Int? = null,
)

sealed interface ContractsEvent {
    data object ContractSelected : ContractsEvent
}

@HiltViewModel
class ContractsViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    private val selectContract: SelectContractUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContractsUiState())
    val uiState: StateFlow<ContractsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ContractsEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<ContractsEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            observeSession().collect { state ->
                if (state is SessionState.LoggedIn) {
                    _uiState.value = ContractsUiState(
                        loading = false,
                        contracts = state.session.contracts,
                        selectedContractId = state.session.selectedContract?.id,
                    )
                }
            }
        }
    }

    fun select(contractId: Int) {
        viewModelScope.launch {
            selectContract(contractId)
            _events.emit(ContractsEvent.ContractSelected)
        }
    }
}
