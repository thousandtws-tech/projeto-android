package com.sgp.cliente.core.datastore.inbox

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sgp.cliente.domain.model.InAppNotification
import com.sgp.cliente.domain.repository.InboxRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.inboxDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "sgp_inbox",
)

/**
 * Inbox in-app: guarda as push notifications recebidas como JSON em um
 * DataStore próprio, para o sino exibir lista + contador de não lidas
 * e modais já vistos não reaparecerem.
 */
@Singleton
class InboxManager @Inject constructor(
    @ApplicationContext context: Context,
) : InboxRepository {

    private val store = context.inboxDataStore
    private val json = Json { ignoreUnknownKeys = true }

    private val _arrivals =
        MutableSharedFlow<InAppNotification>(extraBufferCapacity = 8)

    override val notifications: Flow<List<InAppNotification>> =
        store.data.map { decode(it[ITEMS]) }

    override val arrivals: SharedFlow<InAppNotification> = _arrivals.asSharedFlow()

    override suspend fun add(item: InAppNotification) {
        store.edit { prefs ->
            val items = decode(prefs[ITEMS]).toMutableList()
            val existing = items.indexOfFirst { it.id == item.id }
            if (existing >= 0) {
                // Mesmo push recebido de novo (retry/duplicata): atualiza
                // o conteúdo, mas preserva o estado de lida/modal.
                items[existing] = item.copy(
                    read = items[existing].read,
                    modalDismissed = items[existing].modalDismissed,
                )
            } else {
                items.add(0, item)
            }
            prefs[ITEMS] = json.encodeToString(
                items.take(MAX_ITEMS).map(ItemJson::from),
            )
        }
        _arrivals.emit(item)
    }

    override suspend fun markRead(id: String) = update(id) { it.copy(read = true) }

    override suspend fun markAllRead() {
        store.edit { prefs ->
            val items = decode(prefs[ITEMS])
                .map { it.copy(read = true) }
            prefs[ITEMS] = json.encodeToString(items.map(ItemJson::from))
        }
    }

    override suspend fun dismissModal(id: String) =
        update(id) { it.copy(modalDismissed = true) }

    private suspend fun update(id: String, transform: (InAppNotification) -> InAppNotification) {
        store.edit { prefs ->
            val items = decode(prefs[ITEMS])
                .map { if (it.id == id) transform(it) else it }
            prefs[ITEMS] = json.encodeToString(items.map(ItemJson::from))
        }
    }

    private fun decode(raw: String?): List<InAppNotification> =
        raw?.takeIf(String::isNotBlank)
            ?.let { runCatching { json.decodeFromString<List<ItemJson>>(it) }.getOrNull() }
            ?.map(ItemJson::toDomain)
            ?: emptyList()

    /** Notificação nova no topo, com limite rígido para a store não crescer. */
    private companion object {
        const val MAX_ITEMS = 50
        val ITEMS = stringPreferencesKey("items_json")
    }
}

@Serializable
private data class ItemJson(
    val id: String,
    val type: String,
    val title: String,
    val body: String,
    val receivedAt: Long,
    val read: Boolean = false,
    val presentation: String = "",
    val modalDismissed: Boolean = false,
    val actionUrl: String? = null,
    val imageUrl: String? = null,
    val appVersion: String? = null,
    val mandatory: Boolean = false,
) {
    fun toDomain() = InAppNotification(
        id = id,
        type = type,
        title = title,
        body = body,
        receivedAt = receivedAt,
        read = read,
        presentation = presentation,
        modalDismissed = modalDismissed,
        actionUrl = actionUrl,
        imageUrl = imageUrl,
        appVersion = appVersion,
        mandatory = mandatory,
    )

    companion object {
        fun from(item: InAppNotification) = ItemJson(
            id = item.id,
            type = item.type,
            title = item.title,
            body = item.body,
            receivedAt = item.receivedAt,
            read = item.read,
            presentation = item.presentation,
            modalDismissed = item.modalDismissed,
            actionUrl = item.actionUrl,
            imageUrl = item.imageUrl,
            appVersion = item.appVersion,
            mandatory = item.mandatory,
        )
    }
}
