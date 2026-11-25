package com.sgp.cliente.domain.repository

import com.sgp.cliente.domain.model.InAppNotification
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Inbox in-app: as notificações que chegam via push também ficam
 * guardadas aqui, para o sino no header exibir a lista e o contador
 * de não lidas. `arrivals` emite apenas em recebimento ao vivo (app aberto).
 */
interface InboxRepository {
    val notifications: Flow<List<InAppNotification>>
    val arrivals: SharedFlow<InAppNotification>

    /** Adiciona (ou substitui por `id`, preservando read/modalDismissed). */
    suspend fun add(item: InAppNotification)

    suspend fun markRead(id: String)
    suspend fun markAllRead()
    suspend fun dismissModal(id: String)
}
