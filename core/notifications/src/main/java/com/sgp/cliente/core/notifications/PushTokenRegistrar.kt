package com.sgp.cliente.core.notifications

import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber

/**
 * Registra o token FCM no CRM (`POST /devices`) para o backend poder
 * endereçar push a este aparelho. No-op quando `crmApiUrl` não está
 * configurado no flavor — o app funciona normalmente sem CRM.
 */
@Singleton
class PushTokenRegistrar @Inject constructor(
    private val config: NotificationsConfig,
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    /**
     * @param serviceOnline último `disponivel` do `rede/status` do SGP
     *   observado pelo app — `null` = heartbeat puro (o CRM mantém o
     *   último status conhecido).
     * @return true quando o backend confirmou o registro.
     */
    suspend fun register(
        token: String,
        subscriberId: String? = null,
        serviceOnline: Boolean? = null,
    ): Boolean =
        withContext(Dispatchers.IO) {
            val baseUrl = config.crmApiUrl?.trim()?.trimEnd('/')
            if (baseUrl.isNullOrEmpty()) {
                Timber.d("crmApiUrl não configurado — registro de token ignorado")
                return@withContext false
            }
            val body = buildJsonObject {
                put("token", token)
                put("platform", "android")
                subscriberId?.takeIf(String::isNotBlank)?.let { put("subscriberId", it) }
                serviceOnline?.let { put("serviceOnline", it) }
            }.toString().toRequestBody(JSON_MEDIA_TYPE)

            try {
                val request = Request.Builder().url("$baseUrl/devices").post(body)
                    .apply {
                        // Escopo multi-tenant do CRM — o device é gravado
                        // no tenant desta build (broadcast certo depois).
                        config.crmTenantSlug?.takeIf(String::isNotBlank)?.let {
                            header("X-Tenant", it.trim())
                        }
                    }
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Timber.w("POST /devices → HTTP %d", response.code)
                    }
                    response.isSuccessful
                }
            } catch (error: IOException) {
                Timber.w(error, "falha ao registrar token no CRM")
                false
            }
        }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}
