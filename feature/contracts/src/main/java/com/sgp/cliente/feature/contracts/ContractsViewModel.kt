package com.sgp.cliente.feature.contracts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sgp.cliente.domain.model.Contract
import com.sgp.cliente.domain.model.SessionState
import com.sgp.cliente.domain.usecase.auth.ObserveSessionUseCase
import com.sgp.cliente.domain.usecase.contract.GetContractOpenBillCountsUseCase
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
    /** Per-contract open-boleto count; `null` = probe failed (kept visible). */
    val openBillCounts: Map<Int, Int?> = emptyMap(),
    val probing: Boolean = false,
) {
    /** Only payable contracts are listed — active AND with an open/generated
     * boleto (the client's ask: inactive or nothing-to-pay contracts just
     * confuse subscribers, and a test account can carry a dozen of them).
     * Not-yet-probed contracts (`?: 1`) stay visible until the count lands. */
    val visibleContracts: List<Contract>
        get() = contracts.filter { it.isActive && (openBillCounts[it.id] ?: 1) > 0 }
}

sealed interface ContractsEvent {
    data object ContractSelected : ContractsEvent
}

@HiltViewModel
class ContractsViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    private val selectContract: SelectContractUseCase,
    private val getOpenBillCounts: GetContractOpenBillCountsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContractsUiState())
    val uiState: StateFlow<ContractsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ContractsEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<ContractsEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            observeSession().collect { state ->
                if (state is SessionState.LoggedIn) {
                    val contracts = state.session.contracts
                    val active = contracts.filter(Contract::isActive)
                    val counts = _uiState.value.openBillCounts
                    _uiState.value = ContractsUiState(
                        loading = false,
                        contracts = contracts,
                        selectedContractId = state.session.selectedContract?.id,
                        openBillCounts = counts,
                        probing = _uiState.value.probing,
                    )
                    if (active.size > 1 && !counts.keys.containsAll(active.map(Contract::id))) {
                        probe(active)
                    }
                }
            }
        }
    }

    private fun probe(contracts: List<Contract>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(probing = true)
            val counts = getOpenBillCounts(contracts)
            _uiState.value = _uiState.value.copy(probing = false, openBillCounts = counts)
        }
    }

    fun select(contractId: Int) {
        viewModelScope.launch {
            selectContract(contractId)
            _events.emit(ContractsEvent.ContractSelected)
        }
    }
}
