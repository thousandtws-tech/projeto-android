package com.sgp.cliente.feature.bills

import com.sgp.cliente.domain.model.Invoice
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Regression coverage for the LazyColumn crash
 * `Key "header_março/2025" was already used` — grouping must produce one
 * entry per competence label even when several invoices share the month.
 */
class BillsUiStateTest {

    private fun invoice(id: Int, dueDate: LocalDate?) = Invoice(
        id = id,
        documentNumber = null,
        statusId = 1,
        statusLabel = "Em aberto",
        dueDate = dueDate,
        effectiveDueDate = null,
        amount = 100.0,
        correctedAmount = null,
        paidAt = null,
        barcode = null,
        pixCode = null,
        canGeneratePix = false,
        boletoPath = null,
    )

    @Test
    fun `two invoices in the same month produce a single group`() {
        val state = BillsUiState(
            invoices = listOf(
                invoice(1, LocalDate.of(2025, 3, 5)),
                invoice(2, LocalDate.of(2025, 3, 15)),
            ),
        )

        val groups = state.grouped

        assertEquals(1, groups.size)
        assertEquals(2, groups[0].second.size)
        // LazyColumn keys are derived from these labels — they must be unique.
        assertEquals(groups.size, groups.map { it.first }.distinct().size)
    }

    @Test
    fun `distinct months produce distinct group labels`() {
        val state = BillsUiState(
            invoices = listOf(
                invoice(1, LocalDate.of(2025, 3, 5)),
                invoice(2, LocalDate.of(2025, 2, 10)),
            ),
        )

        val labels = state.grouped.map { it.first }

        assertEquals(2, labels.size)
        assertEquals(labels.size, labels.distinct().size)
    }

    @Test
    fun `groups are ordered newest competence first`() {
        val state = BillsUiState(
            invoices = listOf(
                invoice(1, LocalDate.of(2025, 2, 10)),
                invoice(2, LocalDate.of(2025, 4, 1)),
                invoice(3, LocalDate.of(2025, 3, 20)),
            ),
        )

        val firstDueDates = state.grouped.map { it.second.first().dueDate }

        assertEquals(
            listOf(
                LocalDate.of(2025, 4, 1),
                LocalDate.of(2025, 3, 20),
                LocalDate.of(2025, 2, 10),
            ),
            firstDueDates,
        )
    }

    @Test
    fun `invoices without due date fall into the no-due-date group`() {
        val state = BillsUiState(
            invoices = listOf(
                invoice(1, null),
                invoice(2, null),
                invoice(3, LocalDate.of(2025, 3, 5)),
            ),
        )

        val noDueDate = state.grouped.firstOrNull { it.first == "Sem vencimento" }

        assertEquals(2, noDueDate?.second?.size)
        assertEquals(
            state.grouped.size,
            state.grouped.map { it.first }.distinct().size,
        )
    }
}
