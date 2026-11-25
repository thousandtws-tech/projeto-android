package com.sgp.cliente.core.notifications.cms

import com.sgp.cliente.core.common.error.AppError
import com.sgp.cliente.core.common.result.AppResult
import com.sgp.cliente.core.notifications.NotificationsConfig
import com.sgp.cliente.domain.model.TenantBranding
import com.sgp.cliente.domain.repository.BrandingRepository
import com.sgp.cliente.domain.repository.SettingsRepository
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber

/**
 * Branding white-label do tenant — `GET {crmApiUrl}/branding` é rota
 * pública escopada pelo `X-Tenant` do build (fallback platform).
 *
 * Cache em duas camadas: [branding] quente em memória + persistido em
 * [SettingsRepository] (DataStore) — o app abre vestido com a última
 * identidade conhecida antes da rede responder; [refresh] regrava os
 * dois. Empresa sem branding configurado (campos nulos no payload)
 * volta à identidade embutida — cache limpo, tema padrão.
 */
@Singleton
class CmsBrandingRepository @Inject constructor(
    private val config: NotificationsConfig,
    private val settings: SettingsRepository,
) : BrandingRepository {

    private val _branding = MutableStateFlow(TenantBranding())
    override val branding: StateFlow<TenantBranding> = _branding.asStateFlow()

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    private val json = Json { ignoreUnknownKeys = true }

    // Hidrata o cache quente com o último branding persistido — primeiro
    // frame já sai vestido; o refresh de rede corrige se mudou.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch {
            val cached = settings.branding.first()
            if (cached != TenantBranding()) _branding.value = cached
        }
    }

    override suspend fun refresh(): AppResult<Unit> = withContext(Dispatchers.IO) {
        val baseUrl = config.crmApiUrl?.trim()?.trimEnd('/')
        if (baseUrl.isNullOrEmpty()) {
            Timber.d("crmApiUrl não configurado — branding do build mantido")
            return@withContext AppResult.success(Unit)
        }
        try {
            val request = Request.Builder().url("$baseUrl/branding").get()
                .apply {
                    // Escopo multi-tenant — identidade desta empresa.
                    config.crmTenantSlug?.takeIf(String::isNotBlank)?.let {
                        header("X-Tenant", it.trim())
                    }
                }
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Timber.w("GET /branding → HTTP %d", response.code)
                    return@withContext AppResult.failure(AppError.Server)
                }
                val body = response.body?.string().orEmpty()
                val payload = runCatching {
                    json.decodeFromString<BrandingJson>(body)
                }.getOrElse {
                    Timber.w(it, "GET /branding → payload inválido")
                    return@withContext AppResult.failure(AppError.InvalidResponse)
                }
                val branding = TenantBranding(
                    name = payload.name?.takeIf(String::isNotBlank),
                    // `/media/…` do upload do CMS é path relativo — resolve
                    // contra a origem da API pro Coil (mesmo caso do banner).
                    logoUrl = payload.logoUrl?.takeIf(String::isNotBlank)?.let { url ->
                        if (url.startsWith("/")) "$baseUrl$url" else url
                    },
                    primaryColor = payload.primaryColor?.takeIf(String::isNotBlank),
                    accentColor = payload.accentColor?.takeIf(String::isNotBlank),
                )
                _branding.value = branding
                // Persiste para o próximo cold start já abrir vestido.
                runCatching { settings.setBranding(branding) }
                    .onFailure { Timber.w(it, "falha ao persistir branding") }
                AppResult.success(Unit)
            }
        } catch (error: IOException) {
            Timber.w(error, "falha ao buscar branding do tenant")
            AppResult.failure(AppError.NetworkUnavailable)
        }
    }
}

@Serializable
private data class BrandingJson(
    val name: String? = null,
    @SerialName("logoUrl") val logoUrl: String? = null,
    @SerialName("primaryColor") val primaryColor: String? = null,
    @SerialName("accentColor") val accentColor: String? = null,
)
