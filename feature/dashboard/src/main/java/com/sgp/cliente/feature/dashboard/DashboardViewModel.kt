package com.sgp.cliente.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sgp.cliente.core.common.error.AppError
import com.sgp.cliente.core.common.result.fold
import com.sgp.cliente.core.common.result.onSuccess
import com.sgp.cliente.core.notifications.PushRegistrationManager
import com.sgp.cliente.domain.model.Banner
import com.sgp.cliente.domain.model.BoletoContent
import com.sgp.cliente.domain.model.Contract
import com.sgp.cliente.domain.model.CustomerSession
import com.sgp.cliente.domain.model.Invoice
import com.sgp.cliente.domain.model.SendChannel
import com.sgp.cliente.domain.model.ServiceStatus
import com.sgp.cliente.domain.model.SessionState
import com.sgp.cliente.domain.usecase.auth.LogoutUseCase
import com.sgp.cliente.domain.usecase.auth.ObserveSessionUseCase
import com.sgp.cliente.domain.usecase.banner.ObserveBannersUseCase
import com.sgp.cliente.domain.usecase.banner.RefreshBannersUseCase
import com.sgp.cliente.domain.usecase.contract.CheckServiceStatusUseCase
import com.sgp.cliente.domain.usecase.invoice.GetBoletoUseCase
import com.sgp.cliente.domain.usecase.invoice.GetOpenInvoicesUseCase
import com.sgp.cliente.domain.usecase.invoice.OpenInvoices
import com.sgp.cliente.domain.usecase.invoice.SendInvoiceCopyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val session: CustomerSession? = null,
    val openInvoices: OpenInvoices = OpenInvoices(emptyList(), emptyList()),
    val serviceStatus: ServiceStatus? = null,
    val error: AppError? = null,
    val segundaViaOpen: Boolean = false,
    val boleto: BoletoContent? = null,
    val boletoOpen: Boolean = false,
    val boletoLoading: Boolean = false,
    val boletoError: AppError? = null,
    val sendingCopy: Boolean = false,
    val sentCopyMessage: String? = null,
    val actionError: AppError? = null,
    val banners: List<Banner> = emptyList(),
) {
    val contract: Contract? get() = session?.selectedContract
    val all: List<Invoice> get() = openInvoices.all
    val hasMultipleContracts: Boolean get() = (session?.contracts?.size ?: 0) > 1

    /** Most urgent open invoice — overdue first, then nearest due date. */
    val currentInvoice: Invoice?
        get() = openInvoices.overdue.firstOrNull() ?: openInvoices.upcoming.firstOrNull()
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    private val getOpenInvoices: GetOpenInvoicesUseCase,
    private val checkServiceStatus: CheckServiceStatusUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val getBoleto: GetBoletoUseCase,
    private val sendCopy: SendInvoiceCopyUseCase,
    private val observeBanners: ObserveBannersUseCase,
    private val refreshBanners: RefreshBannersUseCase,
    private val pushRegistration: PushRegistrationManager,
    @Named("sendInvoiceCopyEnabled") val copySendEnabled: Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Feed quente do CMS — emite de novo quando o push silencioso
            // `cms_refresh` rebusca a lista (publish/unpublish/edit no CRM).
            observeBanners().collect { banners ->
                _uiState.update { it.copy(banners = banners) }
            }
        }
        viewModelScope.launch {
            var lastContractId: Int? = null
            observeSession().collect { state ->
                val session = (state as? SessionState.LoggedIn)?.session
                _uiState.update { it.copy(session = session) }
                // A contract switch elsewhere leaves the loaded invoices
                // pointing at the old contract — reload them right away.
                val contractId = session?.selectedContract?.id
                if (contractId != null && lastContractId != null && contractId != lastContractId) {
                    load(forceRefresh = true)
                    loadServiceStatus()
                }
                lastContractId = contractId ?: lastContractId
            }
        }
        load(forceRefresh = false)
        loadServiceStatus()
        loadBanners()
    }

    fun refresh() {
        load(forceRefresh = true)
        loadServiceStatus()
        loadBanners()
    }

    fun logout() {
        viewModelScope.launch { logoutUseCase() }
    }

    /** Banners do CMS — best-effort como o status pill: falha mantém o
     * cache atual do feed, sem poluir a dashboard com erro de marketing. */
    private fun loadBanners() {
        viewModelScope.launch { refreshBanners() }
    }

    private fun load(forceRefresh: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(refreshing = true, error = null) }
            getOpenInvoices(forceRefresh).fold(
                onSuccess = { open ->
                    _uiState.update {
                        it.copy(loading = false, refreshing = false, openInvoices = open)
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(loading = false, refreshing = false, error = error)
                    }
                },
            )
        }
    }

    /** Best-effort health pill — silent on failure, status stays null. */
    private fun loadServiceStatus() {
        viewModelScope.launch {
            checkServiceStatus()
                .onSuccess { status ->
                    _uiState.update { it.copy(serviceStatus = status) }
                    reportServiceStatus(status)
                }
        }
    }

    /**
     * Espelha o status observado no CRM — o POST /devices carrega o
     * `serviceOnline` e de quebra atualiza o `last_seen` (heartbeat)
     * do aparelho no painel. Best-effort: falha não polui a dashboard.
     */
    private suspend fun reportServiceStatus(status: ServiceStatus) {
        val login = _uiState.value.session?.credentials?.login ?: return
        pushRegistration.reportServiceStatus(login, status.isOnline)
    }

    // -- Segunda via (modal do card) ------------------------------------------

    fun openSegundaVia() = _uiState.update { it.copy(segundaViaOpen = true) }

    fun closeSegundaVia() = _uiState.update {
        it.copy(segundaViaOpen = false, sendingCopy = false, actionError = null)
    }

    /** Downloads the boleto PDF of the current invoice and opens the viewer. */
    fun openBoleto() {
        val invoice = _uiState.value.currentInvoice ?: return
        if (invoice.boletoPath == null) return
        if (_uiState.value.boletoLoading) return
        if (_uiState.value.boletoOpen && _uiState.value.boleto != null) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(boletoOpen = true, boletoLoading = true, boletoError = null)
            }
            getBoleto(invoice).fold(
                onSuccess = { content ->
                    _uiState.update {
                        it.copy(boleto = content, boletoLoading = false)
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(boletoLoading = false, boletoError = error)
                    }
                },
            )
        }
    }

    fun closeBoleto() = _uiState.update {
        it.copy(
            boletoOpen = false,
            boleto = null,
            boletoError = null,
            boletoLoading = false,
        )
    }

    /**
     * Envia a via por e-mail — preso na flag `sendInvoiceCopyEnabled`: em
     * prod o endpoint `titulo/{id}/send/` fica desligado porque o SGP abre
     * uma ordem de serviço a cada envio.
     */
    fun sendCopyByEmail() {
        val invoice = _uiState.value.currentInvoice ?: return
        if (!copySendEnabled) return
        viewModelScope.launch {
            _uiState.update { it.copy(sendingCopy = true, actionError = null) }
            sendCopy(invoice.id, SendChannel.EMAIL).fold(
                onSuccess = { message ->
                    _uiState.update {
                        it.copy(sendingCopy = false, sentCopyMessage = message)
                    }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(sendingCopy = false, actionError = error) }
                },
            )
        }
    }

    fun consumeSentCopyMessage() = _uiState.update { it.copy(sentCopyMessage = null) }
    fun consumeActionError() = _uiState.update { it.copy(actionError = null) }
}
