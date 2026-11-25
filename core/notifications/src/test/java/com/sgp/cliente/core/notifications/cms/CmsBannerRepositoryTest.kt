package com.sgp.cliente.core.notifications.cms

import com.sgp.cliente.core.common.result.fold
import com.sgp.cliente.core.notifications.NotificationsConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cobre o caminho real: HTTP + desserialização kotlinx do payload de
 * produção. Sem o plugin `kotlin-serialization` aplicado ao módulo,
 * `@Serializable` vira anotação morta e o decode falha em runtime —
 * feed silenciosamente vazio, carrossel invisível no app.
 */
class CmsBannerRepositoryTest {

    private val server = MockWebServer()

    @After
    fun tearDown() = server.close()

    private fun repository(
        crmApiUrl: String?,
        crmTenantSlug: String? = null,
    ): CmsBannerRepository =
        CmsBannerRepository(object : NotificationsConfig {
            override val crmApiUrl: String? = crmApiUrl
            override val crmTenantSlug: String? = crmTenantSlug
        })

    private fun baseUrl(): String {
        server.start()
        return server.url("/").toString().trimEnd('/')
    }

    @Test
    fun `refresh popula o feed e resolve imageUrl relativa contra a API`() = runTest {
        server.enqueue(
            MockResponse.Builder().body(
                """
                [{"id":"b1","title":"Fibra 600","imageUrl":"/media/abc.png",
                  "actionUrl":"https://loja.jdfibra.br","position":1,"active":true,
                  "createdAt":"2026-11-20T22:46:56Z"}]
                """.trimIndent(),
            ).build(),
        )
        val base = baseUrl()
        val repo = repository(base)

        repo.refresh().fold(
            onSuccess = {},
            onFailure = { error -> throw AssertionError("refresh falhou: $error") },
        )

        val banners = repo.banners.first()
        assertEquals(1, banners.size)
        val banner = banners.single()
        assertEquals("b1", banner.id)
        assertEquals("Fibra 600", banner.title)
        assertEquals("$base/media/abc.png", banner.imageUrl)
        assertEquals("https://loja.jdfibra.br", banner.actionUrl)
        assertEquals(1, banner.position)
    }

    @Test
    fun `refresh mantém imageUrl absoluta intacta`() = runTest {
        server.enqueue(
            MockResponse.Builder().body(
                """[{"id":"b2","title":"Externo","imageUrl":"https://cdn.exemplo.br/x.png",
                    "actionUrl":null,"position":2,"active":true,"createdAt":"x"}]""",
            ).build(),
        )
        val repo = repository(baseUrl())

        repo.refresh()

        assertEquals("https://cdn.exemplo.br/x.png", repo.banners.first().single().imageUrl)
    }

    @Test
    fun `HTTP de erro mantém feed vazio sem lançar`() = runTest {
        server.enqueue(MockResponse.Builder().code(500).build())
        val repo = repository(baseUrl())

        repo.refresh().fold(
            onSuccess = { throw AssertionError("500 não deveria ser sucesso") },
            onFailure = {},
        )
        assertTrue(repo.banners.first().isEmpty())
    }

    @Test
    fun `payload malformado mantém feed vazio sem lançar`() = runTest {
        server.enqueue(MockResponse.Builder().body("não é json").build())
        val repo = repository(baseUrl())

        repo.refresh()

        assertTrue(repo.banners.first().isEmpty())
    }

    @Test
    fun `sem crmApiUrl o refresh é no-op e o feed fica vazio`() = runTest {
        val repo = repository(null)
        repo.refresh()
        assertTrue(repo.banners.first().isEmpty())
    }

    @Test
    fun `slug configurado viaja no header X-Tenant`() = runTest {
        server.enqueue(MockResponse.Builder().body("[]").build())
        val repo = repository(baseUrl(), crmTenantSlug = "jd-fibra")

        repo.refresh()

        val recorded = server.takeRequest()
        assertEquals("jd-fibra", recorded.headers["X-Tenant"])
    }

    @Test
    fun `sem slug o header X-Tenant não sai — fallback platform`() = runTest {
        server.enqueue(MockResponse.Builder().body("[]").build())
        val repo = repository(baseUrl())

        repo.refresh()

        val recorded = server.takeRequest()
        assertEquals(null, recorded.headers["X-Tenant"])
    }
}
