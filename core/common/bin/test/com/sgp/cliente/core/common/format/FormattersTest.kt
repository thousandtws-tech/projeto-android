package com.sgp.cliente.core.common.format

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FormattersTest {

    @Test
    fun `money formats in pt-BR`() {
        val formatted = MoneyFormatter.brl(1234.5)
        assertTrue(formatted.contains("1.234"))
        assertTrue(formatted.contains("50"))
    }

    @Test
    fun `money null renders placeholder`() {
        assertEquals("—", MoneyFormatter.brl(null))
    }

    @Test
    fun `api date parses ISO`() {
        assertEquals(LocalDate.of(2026, 2, 15), DateFormatter.parseApi("2026-02-15"))
        assertNull(DateFormatter.parseApi(null))
        assertNull(DateFormatter.parseApi(""))
        assertNull(DateFormatter.parseApi("15-02-2026"))
    }

    @Test
    fun `api date parses centralapp pt-BR`() {
        // Central App payloads carry "dd/MM/yyyy" dates.
        assertEquals(LocalDate.of(2026, 2, 15), DateFormatter.parseApi("15/02/2026"))
        assertEquals(LocalDate.of(2025, 12, 31), DateFormatter.parseApi("31/12/2025"))
        assertNull(DateFormatter.parseApi("32/13/2026"))
    }

    @Test
    fun `display renders dd MM yyyy`() {
        assertEquals("15/02/2026", DateFormatter.display(LocalDate.of(2026, 2, 15)))
        assertEquals("—", DateFormatter.display(null))
    }

    @Test
    fun `competence label uses month name and year`() {
        val label = DateFormatter.competenceLabel(LocalDate.of(2026, 1, 10))
        assertTrue(label.endsWith("/2026"))
    }

    @Test
    fun `document formats raw CPF digits`() {
        assertEquals("141.046.132-74", DocumentFormatter.display("14104613274"))
    }

    @Test
    fun `document formats raw CNPJ digits`() {
        assertEquals("07.387.503/0001-00", DocumentFormatter.display("07387503000100"))
    }

    @Test
    fun `document passes through formatted and unknown input`() {
        assertEquals("141.046.132-74", DocumentFormatter.display("141.046.132-74"))
        assertEquals("6zta35f0", DocumentFormatter.display("6zta35f0"))
        assertEquals("", DocumentFormatter.display(null))
    }
}
