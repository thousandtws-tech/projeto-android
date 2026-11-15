package com.sgp.cliente.core.common.validation

/**
 * Validates Brazilian CPF (11 digits) and CNPJ (14 digits) numbers,
 * including check-digit verification. Input may be masked or unmasked.
 */
object CpfCnpjValidator {

    private val CPF_LENGTH = 11
    private val CNPJ_LENGTH = 14

    /** Returns the document with non-digit characters removed. */
    fun digits(value: String): String = value.filter(Char::isDigit)

    /** True when the input is a structurally valid CPF or CNPJ. */
    fun isValid(value: String): Boolean {
        val d = digits(value)
        return when (d.length) {
            CPF_LENGTH -> isValidCpf(d)
            CNPJ_LENGTH -> isValidCnpj(d)
            else -> false
        }
    }

    /** True when the digit count matches a document (11 or 14) regardless of check digits. */
    fun hasCompleteLength(value: String): Boolean {
        val len = digits(value).length
        return len == CPF_LENGTH || len == CNPJ_LENGTH
    }

    private fun isValidCpf(digits: String): Boolean {
        if (digits.all { it == digits[0] }) return false
        val numbers = digits.map { it - '0' }
        val d1 = weightedDigit(numbers, 9, 10)
        val d2 = weightedDigit(numbers, 10, 11)
        return numbers[9] == d1 && numbers[10] == d2
    }

    private fun isValidCnpj(digits: String): Boolean {
        if (digits.all { it == digits[0] }) return false
        val numbers = digits.map { it - '0' }
        val d1 = weightedDigit(numbers, 12, intArrayOf(5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2))
        val d2 = weightedDigit(numbers, 13, intArrayOf(6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2))
        return numbers[12] == d1 && numbers[13] == d2
    }

    private fun weightedDigit(numbers: List<Int>, count: Int, startWeight: Int): Int {
        var sum = 0
        var weight = startWeight
        for (i in 0 until count) {
            sum += numbers[i] * weight
            weight = if (weight > 2) weight - 1 else 9
        }
        val mod = sum % 11
        return if (mod < 2) 0 else 11 - mod
    }

    private fun weightedDigit(numbers: List<Int>, count: Int, weights: IntArray): Int {
        var sum = 0
        for (i in 0 until count) sum += numbers[i] * weights[i]
        val mod = sum % 11
        return if (mod < 2) 0 else 11 - mod
    }
}
