package com.sgp.cliente.core.notifications.cms

import com.sgp.cliente.core.common.error.AppError
import com.sgp.cliente.core.common.result.AppResult
import com.sgp.cliente.core.notifications.NotificationsConfig
import com.sgp.cliente.domain.model.Banner
import com.sgp.cliente.domain.repository.BannerRepository
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber

/**
 * Feed público de banners do CMS — `GET {crmApiUrl}/banners` devolve
 * só os publicados (`active`), já na ordem do carrossel.
 *
 * Cache quente em memória: [refresh] regrava [banners]; o push
 * silencioso `cms_refresh` chama o mesmo caminho — assim o card
 * atualiza sozinho quando o provedor publica/despublica no CMS.
 * Sem `crmApiUrl` configurado (flavors sem CRM) o feed fica vazio e
 * o carrossel simplesmente não aparece.
 */
@Singleton
class CmsBannerRepository @Inject constructor(
    private val config: NotificationsConfig,
) : BannerRepository {

    private val _banners = MutableStateFlow<List<Banner>>(emptyList())
    override val banners: StateFlow<List<Banner>> = _banners.asStateFlow()

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun refresh(): AppResult<Unit> = withContext(Dispatchers.IO) {
        val baseUrl = config.crmApiUrl?.trim()?.trimEnd('/')
        if (baseUrl.isNullOrEmpty()) {
            Timber.w("crmApiUrl não configurado — feed de banners desativado")
            return@withContext AppResult.success(Unit)
        }
        try {
            val request = Request.Builder().url("$baseUrl/banners").get()
                .apply {
                    // Escopo multi-tenant — feed do CMS desta empresa.
                    config.crmTenantSlug?.takeIf(String::isNotBlank)?.let {
                        header("X-Tenant", it.trim())
                    }
                }
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Timber.w("GET /banners → HTTP %d", response.code)
                    return@withContext AppResult.failure(AppError.Server)
                }
                val body = response.body?.string().orEmpty()
                val items = runCatching {
                    json.decodeFromString<List<BannerJson>>(body)
                }.getOrElse {
                    Timber.w(it, "GET /banners → payload inválido")
                    return@withContext AppResult.failure(AppError.InvalidResponse)
                }
                _banners.value = items.map { item ->
                    Banner(
                        id = item.id,
                        title = item.title,
                        // `/media/…` do upload do CMS é path relativo
                        // — resolve contra a origem da API pro Coil.
                        imageUrl = if (item.imageUrl.startsWith("/")) {
                            "$baseUrl${item.imageUrl}"
                        } else {
                            item.imageUrl
                        },
                        actionUrl = item.actionUrl,
                        position = item.position,
                    )
                }
                AppResult.success(Unit)
            }
        } catch (error: IOException) {
            Timber.w(error, "falha ao buscar banners do CMS")
            AppResult.failure(AppError.NetworkUnavailable)
        }
    }
}

@Serializable
private data class BannerJson(
    val id: String,
    val title: String,
    @SerialName("imageUrl") val imageUrl: String,
    @SerialName("actionUrl") val actionUrl: String? = null,
    val position: Int = 0,
)
