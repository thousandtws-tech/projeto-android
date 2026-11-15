package com.sgp.cliente.core.common.result

import com.sgp.cliente.core.common.error.AppError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AppResultTest {

    @Test
    fun `success carries value`() {
        val result = AppResult.success(42)
        assertTrue(result.isSuccess)
        assertEquals(42, result.getOrNull())
        assertNull(result.errorOrNull())
    }

    @Test
    fun `failure carries typed error`() {
        val result: AppResult<Int> = AppResult.failure(AppError.Unauthorized)
        assertTrue(result.isFailure)
        assertNull(result.getOrNull())
        assertSame(AppError.Unauthorized, result.errorOrNull())
    }

    @Test
    fun `map transforms success`() {
        val mapped = AppResult.success(2).map { it * 10 }
        assertEquals(20, mapped.getOrNull())
    }

    @Test
    fun `map propagates failure`() {
        val failure: AppResult<Int> = AppResult.failure(AppError.Timeout)
        val mapped = failure.map { it * 10 }
        assertSame(AppError.Timeout, mapped.errorOrNull())
    }

    @Test
    fun `fold picks the matching branch`() {
        assertEquals("ok", AppResult.success(1).fold({ "ok" }, { "err" }))
        val failure: AppResult<Int> = AppResult.failure(AppError.Server)
        assertEquals("err", failure.fold({ "ok" }, { "err" }))
    }

    @Test
    fun `onSuccess and onFailure run only on their branch`() {
        var hit = ""
        AppResult.success(5).onSuccess { hit = "s" }.onFailure { hit = "f" }
        assertEquals("s", hit)
        val failure: AppResult<Int> = AppResult.failure(AppError.NotFound)
        failure.onSuccess { hit = "s2" }.onFailure { hit = "f" }
        assertEquals("f", hit)
    }
}
