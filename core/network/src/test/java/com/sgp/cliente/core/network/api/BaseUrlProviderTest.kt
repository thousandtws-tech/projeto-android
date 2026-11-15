package com.sgp.cliente.core.network.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for [BaseUrlProvider]: input normalization rules and the
 * "keep last valid value" semantics of [BaseUrlProvider.update].
 */
class BaseUrlProviderTest {

    @Test
    fun `normalize returns null for null empty and blank input`() {
        assertNull(BaseUrlProvider.normalize(null))
        assertNull(BaseUrlProvider.normalize(""))
        assertNull(BaseUrlProvider.normalize("   "))
    }

    @Test
    fun `normalize adds https scheme when missing`() {
        val url = BaseUrlProvider.normalize("sgp.exemplo.com.br")

        assertEquals("https", url!!.scheme)
        assertEquals("sgp.exemplo.com.br", url.host)
        assertEquals("/", url.encodedPath)
    }

    @Test
    fun `normalize keeps explicit http scheme`() {
        val url = BaseUrlProvider.normalize("http://10.0.0.1")

        assertEquals("http", url!!.scheme)
        assertEquals("10.0.0.1", url.host)
    }

    @Test
    fun `normalize keeps explicit port`() {
        val url = BaseUrlProvider.normalize("http://10.0.0.1:8080")

        assertEquals(8080, url!!.port)
    }

    @Test
    fun `normalize appends trailing slash`() {
        assertEquals(
            "https://sgp.exemplo.com.br/",
            BaseUrlProvider.normalize("https://sgp.exemplo.com.br").toString(),
        )
    }

    @Test
    fun `normalize keeps a path prefix`() {
        assertEquals(
            "https://sgp.exemplo.com.br/sgp/",
            BaseUrlProvider.normalize("https://sgp.exemplo.com.br/sgp").toString(),
        )
    }

    @Test
    fun `normalize trims surrounding whitespace`() {
        assertEquals(
            "https://sgp.exemplo.com.br/",
            BaseUrlProvider.normalize("  https://sgp.exemplo.com.br  ").toString(),
        )
    }

    @Test
    fun `normalize returns null for malformed urls`() {
        assertNull(BaseUrlProvider.normalize("https://host com espaco"))
        assertNull(BaseUrlProvider.normalize("http://"))
    }

    @Test
    fun `provider starts on the invalid placeholder`() {
        val provider = BaseUrlProvider()

        assertEquals("invalid.local", provider.current().host)
    }

    @Test
    fun `update stores the normalized url`() {
        val provider = BaseUrlProvider()

        provider.update("sgp.exemplo.com.br")

        assertEquals("https://sgp.exemplo.com.br/", provider.current().toString())
    }

    @Test
    fun `update with invalid input keeps the previous value`() {
        val provider = BaseUrlProvider()
        provider.update("https://valida.exemplo.com.br")

        provider.update("url com espaco")
        provider.update(null)
        provider.update("")

        assertEquals("https://valida.exemplo.com.br/", provider.current().toString())
    }
}
