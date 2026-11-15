package com.sgp.cliente.core.common.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CpfCnpjValidatorTest {

    @Test
    fun `valid CPF digits are accepted`() {
        assertTrue(CpfCnpjValidator.isValid("52998224725"))
    }

    @Test
    fun `valid CPF with mask is accepted`() {
        assertTrue(CpfCnpjValidator.isValid("529.982.247-25"))
    }

    @Test
    fun `CPF with wrong check digit is rejected`() {
        assertFalse(CpfCnpjValidator.isValid("52998224724"))
    }

    @Test
    fun `CPF with all equal digits is rejected`() {
        assertFalse(CpfCnpjValidator.isValid("11111111111"))
    }

    @Test
    fun `valid CNPJ is accepted`() {
        assertTrue(CpfCnpjValidator.isValid("11222333000181"))
        assertTrue(CpfCnpjValidator.isValid("11.222.333/0001-81"))
    }

    @Test
    fun `CNPJ with wrong check digit is rejected`() {
        assertFalse(CpfCnpjValidator.isValid("11222333000182"))
    }

    @Test
    fun `wrong length is rejected`() {
        assertFalse(CpfCnpjValidator.isValid("123"))
        assertFalse(CpfCnpjValidator.isValid(""))
        assertFalse(CpfCnpjValidator.isValid("123456789012"))
    }

    @Test
    fun `digits strips mask`() {
        assertEquals("52998224725", CpfCnpjValidator.digits("529.982.247-25"))
    }

    @Test
    fun `hasCompleteLength checks only the digit count`() {
        assertTrue(CpfCnpjValidator.hasCompleteLength("11111111111"))
        assertFalse(CpfCnpjValidator.hasCompleteLength("1111"))
    }
}
