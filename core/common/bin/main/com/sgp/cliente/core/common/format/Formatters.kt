package com.sgp.cliente.core.common.format

import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
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

    /**
     * Due date with day countdown, e.g. `Vence em 10/12/2026 · em 12 dias`
     * or `Venceu em 10/11/2026 · há 8 dias`.
     */
    fun dueWithRelative(due: LocalDate?, today: LocalDate): String {
        if (due == null) return display(null)
        val days = ChronoUnit.DAYS.between(today, due)
        val rel = when {
            days < -1L -> "há ${-days} dias"
            days == -1L -> "há 1 dia"
            days == 0L -> "hoje"
            days == 1L -> "amanhã"
            else -> "em $days dias"
        }
        val verb = if (days < 0) "Venceu em" else "Vence em"
        return "$verb ${display(due)} · $rel"
    }

    /**
     * Idade amigável de um instante ("agora", "há 12 min", "há 3 h",
     * "ontem", "dd/MM") — usada no sino e nos cards de avisos.
     */
    fun relative(epochMillis: Long, now: LocalDateTime = LocalDateTime.now()): String {
        val then = Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault()).toLocalDateTime()
        val minutes = ChronoUnit.MINUTES.between(then, now)
        if (minutes < 1) return "agora"
        if (minutes < 60) return "há $minutes min"
        val hours = ChronoUnit.HOURS.between(then, now)
        if (hours < 24) return "há $hours h"
        val days = ChronoUnit.DAYS.between(then.toLocalDate(), now.toLocalDate())
        if (days == 1L) return "ontem"
        return then.format(DateTimeFormatter.ofPattern("dd/MM"))
    }
}
