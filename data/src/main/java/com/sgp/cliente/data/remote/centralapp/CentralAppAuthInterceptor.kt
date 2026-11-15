package com.sgp.cliente.data.remote.centralapp

import com.sgp.cliente.domain.repository.SessionRepository
import com.sgp.cliente.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Adds the Central App credentials to `/api/centralapp/` requests:
 *  - `X-TOKEN`: the selected central's token (settings);
 *  - `Authorization: Bearer <jwt>`: the subscriber's session token.
 *
 * Bootstrap/public endpoints (`list/`, `config`, `captcha`, `login`,
 * `recuperarsenha`) get no Bearer, matching the official client. Calls that
 * carry an explicit `X-TOKEN` (e.g. `config/{id}` during discovery) keep it.
 *
 * `runBlocking` is safe here: OkHttp interceptors never run on the main
 * thread and the DataStore reads are small and cached.
 */
@Singleton
class CentralAppAuthInterceptor @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val sessionRepository: SessionRepository,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath
        if (!path.contains("/api/centralapp/") || request.header(HEADER_TOKEN) != null) {
            return chain.proceed(request)
        }

        val builder = request.newBuilder()
        val centralToken = runBlocking { settingsRepository.centralToken.first() }
        if (!centralToken.isNullOrBlank()) {
            builder.header(HEADER_TOKEN, centralToken)
        }
        if (!isAuthFree(path)) {
            runBlocking { sessionRepository.authToken() }
                ?.takeIf(String::isNotBlank)
                ?.let { builder.header(HEADER_AUTHORIZATION, "Bearer $it") }
        }
        return chain.proceed(builder.build())
    }

    private fun isAuthFree(path: String): Boolean =
        AUTH_FREE_SUFFIXES.any { path.endsWith(it) }

    private companion object {
        const val HEADER_TOKEN = "X-TOKEN"
        const val HEADER_AUTHORIZATION = "Authorization"
        val AUTH_FREE_SUFFIXES = listOf(
            "/centralapp/list/",
            "/login/",
            "/recuperarsenha/",
            "/captcha/challenge",
        )
    }
}
