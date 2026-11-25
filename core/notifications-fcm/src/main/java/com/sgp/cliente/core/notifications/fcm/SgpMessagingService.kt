package com.sgp.cliente.core.notifications.fcm

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.sgp.cliente.core.notifications.NotificationPoster
import com.sgp.cliente.core.notifications.PushTokenRegistrar
import com.sgp.cliente.domain.model.InAppNotification
import com.sgp.cliente.domain.repository.BannerRepository
import com.sgp.cliente.domain.repository.BrandingRepository
import com.sgp.cliente.domain.repository.InboxRepository
import com.sgp.cliente.domain.repository.OnboardingRepository
import com.sgp.cliente.domain.repository.SessionRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Ponte FCM → notificação local. Só entra no build quando
 * `app/google-services.json` existe (módulo condicional — ver
 * settings.gradle.kts); sem o arquivo, o Firebase não inicializa e
 * este service nunca é invocado.
 */
@AndroidEntryPoint
class SgpMessagingService : FirebaseMessagingService() {

    @Inject lateinit var poster: NotificationPoster
    @Inject lateinit var registrar: PushTokenRegistrar
    @Inject lateinit var sessionRepository: SessionRepository
    @Inject lateinit var inbox: InboxRepository
    @Inject lateinit var banners: BannerRepository
    @Inject lateinit var onboarding: OnboardingRepository
    @Inject lateinit var branding: BrandingRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Token novo/rotacionado → re-registra no CRM já com o
     * `subscriberId` da sessão ativa (null quando deslogado). */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Nunca logar o token — é credencial de endereçamento.
        Timber.d("novo token FCM — registrando no CRM")
        scope.launch {
            registrar.register(token, sessionRepository.credentials()?.login)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        // Sinal silencioso do CMS (data-only, sem bandeja): um feed de
        // conteúdo mudou — rebusca já. `feed` ausente (API antiga)
        // refresca todos por compatibilidade. Não vira inbox nem
        // notificação.
        if (message.data["type"] == "cms_refresh") {
            val feed = message.data["feed"]
            Timber.d("cms_refresh recebido — feed=%s", feed)
            scope.launch {
                if (feed == null || feed == "banners") banners.refresh()
                if (feed == null || feed == "onboarding") onboarding.refresh()
                if (feed == null || feed == "branding") branding.refresh()
            }
            return
        }
        val title = message.notification?.title ?: message.data["title"]
        val body = message.notification?.body ?: message.data["body"]
        if (title.isNullOrBlank() && body.isNullOrBlank()) {
            Timber.d("mensagem FCM sem conteúdo — ignorada")
            return
        }
        scope.launch {
            // Primeiro no inbox in-app (sino/modal), depois a notificação
            // do sistema — que depende de permissão/preferências.
            inbox.add(
                InAppNotification(
                    id = message.data["notificationId"]
                        ?: java.util.UUID.randomUUID().toString(),
                    type = message.data["type"].orEmpty(),
                    title = title.orEmpty(),
                    body = body.orEmpty(),
                    receivedAt = System.currentTimeMillis(),
                    presentation = message.data["presentation"].orEmpty(),
                    actionUrl = message.data["url"],
                    imageUrl = message.data["imageUrl"],
                    appVersion = message.data["appVersion"],
                    mandatory = message.data["mandatory"] == "true",
                )
            )
            poster.notify(
                kind = message.data["type"],
                title = title.orEmpty(),
                body = body.orEmpty(),
                data = message.data,
            )
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
