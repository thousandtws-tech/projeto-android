package com.sgp.cliente.core.notifications.fcm

import com.google.firebase.messaging.FirebaseMessaging
import com.sgp.cliente.core.notifications.PushTokenRegistrar
import com.sgp.cliente.core.notifications.PushTokenSync
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * `PushTokenSync` do Firebase: resolve o token FCM atual e entrega ao
 * `PushTokenRegistrar` (POST /devices no CRM). Rodado pelo
 * `PushRegistrationManager` a cada login — o CRM sempre tem o token
 * fresco amarrado ao `subscriberId` da sessão.
 */
@Singleton
class FcmPushTokenSync @Inject constructor(
    private val registrar: PushTokenRegistrar,
) : PushTokenSync {

    override suspend fun syncNow(subscriberId: String?) {
        val token = try {
            fetchToken()
        } catch (error: Exception) {
            Timber.w(error, "falha ao obter token FCM")
            return
        }
        registrar.register(token, subscriberId)
    }

    /** `FirebaseMessaging.token` é Task-based — await manual para não
     * arrastar kotlinx-coroutines-play-services por uma chamada só. */
    private suspend fun fetchToken(): String = suspendCancellableCoroutine { continuation ->
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token -> continuation.resume(token) }
            .addOnFailureListener { error -> continuation.resumeWithException(error) }
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface FcmNotificationsModule {
    @Binds
    @IntoSet
    fun bindPushTokenSync(sync: FcmPushTokenSync): PushTokenSync
}
