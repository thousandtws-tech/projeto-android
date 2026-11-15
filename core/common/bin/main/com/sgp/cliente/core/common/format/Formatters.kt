package com.sgp.cliente.core.common.format

import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val PT_BR: Locale = Locale.forLanguageTag("pt-BR")

/** Formats monetary values as `R$ 1.234,56`. */
object MoneyFormatter {

    private val format: NumberFormat =
        NumberFormat.getCurrencyInstance(PT_BR)

    fun brl(value: Double): String = format.format(value)

    fun brl(value: Double?): String = value?.let(::brl) ?: "—"
}

/** Formats raw CPF/CNPJ digits for display; passes through already-formatted input. */
object DocumentFormatter {

    fun display(value: String?): String {
        val digits = value?.filter(Char::isDigit).orEmpty()
        return when (digits.length) {
            11 -> "${digits.substring(0, 3)}.${digits.substring(3, 6)}.${digits.substring(6, 9)}-${digits.substring(9)}"
            14 -> "${digits.substring(0, 2)}.${digits.substring(2, 5)}.${digits.substring(5, 8)}/${digits.substring(8, 12)}-${digits.substring(12)}"
            else -> value.orEmpty()
        }
    }
}

/** Date helpers for the SGP API and pt-BR presentation. */
object DateFormatter {

    private val apiFormats: List<DateTimeFormatter> = listOf(
        DateTimeFormatter.ISO_LOCAL_DATE,
        // Central App payloads carry pt-BR dates ("05/04/2024").
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
    )
    private val displayFormat: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd/MM/yyyy")

    fun parseApi(value: String?): LocalDate? =
        value?.takeIf { it.isNotBlank() }?.let { raw ->
            apiFormats.firstNotNullOfOrNull { format ->
                runCatching { LocalDate.parse(raw, format) }.getOrNull()
            }
        }

    fun display(date: LocalDate?): String =
        date?.format(displayFormat) ?: "—"

    /**
     * Competence label for grouping bills, e.g. `janeiro/2026`.
     */
    fun competenceLabel(date: LocalDate): String {
        val month = date.month.getDisplayName(TextStyle.FULL, PT_BR)
        return "$month/${date.year}"
    }
}
