package com.sgp.cliente.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sgp.cliente.BuildConfig
import com.sgp.cliente.domain.model.InAppNotification
import com.sgp.cliente.domain.repository.InboxRepository
import com.sgp.cliente.update.UpdateInstaller
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado do sino no header: lista do inbox, contador de não lidas,
 * tick de animação a cada notificação nova e a fila de modais
 * (comunicados com `presentation=modal` e avisos de app-update).
 */
@HiltViewModel
class NotificationCenterViewModel @Inject constructor(
    private val inbox: InboxRepository,
    private val updateInstaller: UpdateInstaller,
) : ViewModel() {

    val notifications: StateFlow<List<InAppNotification>> = inbox.notifications
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val unreadCount: StateFlow<Int> = inbox.notifications
        .map { items -> items.count { !it.read } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    /** Incrementa a cada recebimento em foreground — dispara a animação do sino. */
    val arrivalTick = MutableStateFlow(0)

    private val _centerOpen = MutableStateFlow(false)
    val centerOpen: StateFlow<Boolean> = _centerOpen.asStateFlow()

    /** Modal exibido no momento (um por vez; próximo entra após dismiss). */
    private val _modal = MutableStateFlow<InAppNotification?>(null)
    val modal: StateFlow<InAppNotification?> = _modal.asStateFlow()

    init {
        viewModelScope.launch {
            inbox.arrivals.collect { item ->
                arrivalTick.update { it + 1 }
                if (shouldPresent(item)) _modal.value = item
            }
        }
        // Notificação que chegou com o app fechado: ao abrir, mostra o
        // modal uma única vez — depois nunca mais (modalDismissed).
        viewModelScope.launch {
            inbox.notifications.first()
                .firstOrNull { it.wantsModal && !it.modalDismissed && isVersionRelevant(it) }
                ?.let { _modal.value = it }
        }
    }

    fun openCenter() = _centerOpen.update { true }
    fun closeCenter() = _centerOpen.update { false }

    fun markRead(id: String) = viewModelScope.launch { inbox.markRead(id) }
    fun markAllRead() = viewModelScope.launch { inbox.markAllRead() }

    fun dismissModal() {
        val current = _modal.value ?: return
        viewModelScope.launch {
            inbox.dismissModal(current.id)
            // Próximo candidato da fila de modais (mais antigo primeiro).
            _modal.value = inbox.notifications.first()
                .lastOrNull { it.wantsModal && !it.modalDismissed && it.id != current.id && isVersionRelevant(it) }
        }
    }

    /** Ação do modal ("Atualizar agora"/"Abrir"): delega ao instalador
     *  (APK → download + install; outro link → navegador) e dispensa. */
    fun openNotificationAction(item: InAppNotification) {
        val url = item.actionUrl ?: return
        updateInstaller.openUpdate(url, item.appVersion)
        dismissModal()
    }

    /** Download concluído enquanto o app estava fora → retoma instalação. */
    fun installPendingUpdateIfReady() = updateInstaller.installIfReady()

    fun installFinishedUpdate(downloadId: Long) =
        updateInstaller.installIfReady(downloadId)

    /** Só abre modal quando é novidade de verdade para este aparelho:
     *  `app_update` de versão não superior à instalada é ignorada. */
    private fun shouldPresent(item: InAppNotification): Boolean =
        item.wantsModal && !item.modalDismissed && isVersionRelevant(item)

    private fun isVersionRelevant(item: InAppNotification): Boolean {
        if (!item.isAppUpdate) return true
        val offered = item.appVersion ?: return true
        return compareVersions(offered, BuildConfig.VERSION_NAME) > 0
    }

    private companion object {
        /** "0.1.5" > "0.1.4" → positivo; igual → 0; inferior → negativo. */
        fun compareVersions(a: String, b: String): Int {
            val pa = a.split('.', '-').mapNotNull { it.toIntOrNull() }
            val pb = b.split('.', '-').mapNotNull { it.toIntOrNull() }
            val size = maxOf(pa.size, pb.size)
            for (i in 0 until size) {
                val x = pa.getOrElse(i) { 0 }
                val y = pb.getOrElse(i) { 0 }
                if (x != y) return x.compareTo(y)
            }
            return 0
        }
    }
}
