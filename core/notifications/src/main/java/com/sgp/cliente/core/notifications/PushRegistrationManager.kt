package com.sgp.cliente.core.notifications

import com.sgp.cliente.domain.model.SessionState
import com.sgp.cliente.domain.repository.SessionRepository
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Mantém o token push registrado no CRM amarrado ao assinante logado.
 *
 * Sem isto o token só seria enviado na rotação do FCM (raro) e sem
 * `subscriberId` — o CRM não saberia pra quem cada dispositivo aponta.
 * `start()` é chamado uma vez pela MainActivity; a coleta reage a
 * login/logout e re-registra com o login da sessão.
 */
@Singleton
class PushRegistrationManager @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val syncs: Set<@JvmSuppressWildcards PushTokenSync>,
) {

    private val started = AtomicBoolean(false)

    fun start(scope: CoroutineScope) {
        if (!started.compareAndSet(false, true)) return
        scope.launch {
            sessionRepository.state
                .distinctUntilChanged()
                .collectLatest { state ->
                    when (state) {
                        is SessionState.LoggedIn -> syncAll(state.session.credentials.login)
                        else -> Unit
                    }
                }
        }
    }

    private suspend fun syncAll(subscriberId: String) {
        if (syncs.isEmpty()) {
            Timber.d("sem provider de push no build — registro ignorado")
            return
        }
        syncs.forEach { sync ->
            try {
                sync.syncNow(subscriberId)
            } catch (error: Exception) {
                Timber.w(error, "falha ao sincronizar token push")
            }
        }
    }
}
