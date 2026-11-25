package com.sgp.cliente.core.notifications.cms

import com.sgp.cliente.core.common.error.AppError
import com.sgp.cliente.core.common.result.AppResult
import com.sgp.cliente.core.notifications.NotificationsConfig
import com.sgp.cliente.domain.model.OnboardingSlide
import com.sgp.cliente.domain.repository.OnboardingRepository
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
 * Feed público de slides de onboarding do CMS — `GET {crmApiUrl}/onboarding`
 * devolve só os publicados (`active`), já na ordem da paginação.
 *
 * Cache quente em memória: [refresh] regrava [slides]; o push
 * silencioso `cms_refresh` com `feed=onboarding` chama o mesmo caminho
 * — assim a primeira abertura já mostra os slides que o provedor
 * publicou. Sem `crmApiUrl` configurado (flavors sem CRM) o feed fica
 * vazio e a tela cai nas páginas embutidas.
 */
@Singleton
class CmsOnboardingRepository @Inject constructor(
    private val config: NotificationsConfig,
) : OnboardingRepository {

    private val _slides = MutableStateFlow<List<OnboardingSlide>>(emptyList())
    override val slides: StateFlow<List<OnboardingSlide>> = _slides.asStateFlow()

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun refresh(): AppResult<Unit> = withContext(Dispatchers.IO) {
        val baseUrl = config.crmApiUrl?.trim()?.trimEnd('/')
        if (baseUrl.isNullOrEmpty()) {
            Timber.w("crmApiUrl não configurado — feed de onboarding desativado")
            return@withContext AppResult.success(Unit)
        }
        try {
            val request = Request.Builder().url("$baseUrl/onboarding").get()
                .apply {
                    // Escopo multi-tenant — slides do CMS desta empresa.
                    config.crmTenantSlug?.takeIf(String::isNotBlank)?.let {
                        header("X-Tenant", it.trim())
                    }
                }
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Timber.w("GET /onboarding → HTTP %d", response.code)
                    return@withContext AppResult.failure(AppError.Server)
                }
                val body = response.body?.string().orEmpty()
                val items = runCatching {
                    json.decodeFromString<List<OnboardingSlideJson>>(body)
                }.getOrElse {
                    Timber.w(it, "GET /onboarding → payload inválido")
                    return@withContext AppResult.failure(AppError.InvalidResponse)
                }
                _slides.value = items.map { item ->
                    OnboardingSlide(
                        id = item.id,
                        title = item.title,
                        subtitle = item.subtitle.orEmpty(),
                        // `/media/…` do upload do CMS é path relativo
                        // — resolve contra a origem da API pro
                        // Coil/ExoPlayer.
                        imageUrl = item.imageUrl.resolveAgainst(baseUrl),
                        videoUrl = item.videoUrl.resolveAgainst(baseUrl),
                        position = item.position,
                    )
                }
                AppResult.success(Unit)
            }
        } catch (error: IOException) {
            Timber.w(error, "falha ao buscar slides de onboarding do CMS")
            AppResult.failure(AppError.NetworkUnavailable)
        }
    }
}

@Serializable
private data class OnboardingSlideJson(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    @SerialName("imageUrl") val imageUrl: String? = null,
    @SerialName("videoUrl") val videoUrl: String? = null,
    val position: Int = 0,
)

private fun String?.resolveAgainst(baseUrl: String): String? =
    this?.takeIf(String::isNotBlank)?.let { url ->
        if (url.startsWith("/")) "$baseUrl$url" else url
    }
