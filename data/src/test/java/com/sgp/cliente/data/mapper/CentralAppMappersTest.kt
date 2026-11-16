package com.sgp.cliente.data.mapper

import com.sgp.cliente.data.remote.centralapp.CentralContractDto
import com.sgp.cliente.data.remote.centralapp.CentralContractServiceDto
import com.sgp.cliente.data.remote.centralapp.CentralInvoiceDto
import com.sgp.cliente.data.remote.centralapp.CentralMessageDto
import com.sgp.cliente.data.remote.centralapp.CentralPixResponseDto
import com.sgp.cliente.domain.model.InvoiceStatus
import java.time.LocalDate
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CentralAppMappersTest {

    @Test
    fun `contract dto maps fields and picks internet service`() {
        val dto = CentralContractDto(
            id = 566,
            description = "Contrato",
            customerName = "PEDRO",
            customerDoc = "00000000000",
            status = "Ativo",
            dueDay = 15,
            services = listOf(
                CentralContractServiceDto(id = 1, plan = "Voz", group = "telefonia", value = 10.0),
                CentralContractServiceDto(id = 591, plan = "Fibra 300", group = "internet", value = 119.9),
            ),
        )
        val domain = dto.toDomain()
        assertEquals(566, domain.id)
        assertEquals("Ativo", domain.status)
        assertEquals("PEDRO", domain.razaoSocial)
        assertEquals(15, domain.dueDay)
        assertEquals("Fibra 300", domain.internetPlan)
        assertEquals("591", domain.internetPlanId)
        assertTrue(domain.isActive)
    }

    @Test
    fun `invoice dto parses centralapp dates and string amounts`() {
        val dto = CentralInvoiceDto(
            id = 13658,
            status = "Em aberto",
            dueDate = "10/11/2025",
            amount = "99.90",
            totalAmount = "109.90",
            barcode = "23793.38128 60007.827136 95000.063305 9 89000000010990",
            allowsPix = true,
        )
        val domain = dto.toDomain()!!
        assertEquals(13658, domain.id)
        assertEquals(InvoiceStatus.OPEN, domain.status)
        assertEquals(LocalDate.of(2025, 11, 10), domain.dueDate)
        assertEquals(109.90, domain.displayAmount!!, 0.001)
        assertEquals("2025-11", domain.competenceYearMonth)
        assertTrue(domain.canGeneratePix)
    }

    @Test
    fun `situacao maps to SGP status ids`() {
        assertEquals(InvoiceStatus.PAID, CentralInvoiceDto(id = 1, status = "Pago").toDomain()!!.status)
        assertEquals(InvoiceStatus.PAID, CentralInvoiceDto(id = 1, status = "Liquidada").toDomain()!!.status)
        assertEquals(InvoiceStatus.CANCELLED, CentralInvoiceDto(id = 1, status = "Cancelado").toDomain()!!.status)
        assertEquals(InvoiceStatus.OPEN, CentralInvoiceDto(id = 1, status = "Em aberto").toDomain()!!.status)
        assertEquals(InvoiceStatus.OPEN, CentralInvoiceDto(id = 1, status = "Vencida").toDomain()!!.status)
    }

    @Test
    fun `paid invoice parses data_pagamento`() {
        val domain = CentralInvoiceDto(
            id = 2,
            status = "Pago",
            dueDate = "10/09/2025",
            paidAt = "08/09/2025",
        ).toDomain()!!
        assertEquals(LocalDate.of(2025, 9, 8), domain.paidAt)
    }

    @Test
    fun `overdue flags and link fallback`() {
        val dto = CentralInvoiceDto(
            id = 3,
            status = "Em aberto",
            dueDate = "01/10/2025",
            overdueDays = 20,
            boletoUrl = "/f/3.pdf",
        )
        assertTrue(dto.isOverdueOn(LocalDate.of(2025, 10, 21)))
        assertEquals("/f/3.pdf", dto.toDomain()!!.boletoPath)

        // chargeUrl is the public checkout page (HTML), never a PDF the
        // viewer can render — it maps to paymentUrl, not boletoPath.
        val chargeOnly = CentralInvoiceDto(id = 4, status = "Em aberto", chargeUrl = "https://pay/x")
        val mapped = chargeOnly.toDomain()!!
        assertNull(mapped.boletoPath)
        assertEquals("https://pay/x", mapped.paymentUrl)

        // `pagamento_boleto=false` does NOT mean "no boleto" — verified on
        // the live demo: the flag stays false even when `link` serves a
        // valid PDF, so boletoPath keeps the link and the viewer validates
        // the payload instead.
        val flagOff = CentralInvoiceDto(
            id = 5,
            status = "Em aberto",
            boletoUrl = "/f/5.pdf",
            allowsBoleto = false,
        )
        assertEquals("/f/5.pdf", flagOff.toDomain()!!.boletoPath)
    }

    @Test
    fun `pix response picks the copy-paste code`() {
        val dto = CentralPixResponseDto(
            status = 1,
            message = "ok",
            pix = buildJsonObject { put("codigo", "000201pix") },
        )
        val domain = dto.toDomain()
        assertTrue(domain.isSuccess)
        assertEquals("000201pix", domain.code)
    }

    @Test
    fun `pix response accepts plain string and top-level code`() {
        // Some gateways answer `"pix": "<emv>"` instead of an object.
        val stringPayload = CentralPixResponseDto(
            status = 1,
            pix = kotlinx.serialization.json.JsonPrimitive("000201emv"),
        )
        assertEquals("000201emv", stringPayload.copyPasteCode())

        // Others expose `codigo_pix` at the top level with no `pix` block.
        val topLevel = CentralPixResponseDto(status = 1, pixCode = "000201top")
        assertEquals("000201top", topLevel.copyPasteCode())
    }

    @Test
    fun `pix response rejects empty and status-zero payloads`() {
        // The demo server answers `{"msg":"PIX não disponível.","status":0,"pix":{}}`.
        val unavailable = CentralPixResponseDto(
            status = 0,
            message = "PIX não disponível.",
            pix = buildJsonObject { },
        )
        val domain = unavailable.toDomain()
        assertFalse(domain.isSuccess)
        assertNull(domain.code)
        assertEquals("PIX não disponível.", domain.message)

        // A `null` inside the object must not leak the literal "null".
        val nullCode = CentralPixResponseDto(
            status = 1,
            pix = buildJsonObject { put("codigo", kotlinx.serialization.json.JsonNull) },
        )
        assertNull(nullCode.copyPasteCode())
    }

    @Test
    fun `message dto maps promise and error text`() {
        val promise = CentralMessageDto(status = 1, protocol = "P-1", message = "ok").toPaymentPromise()
        assertTrue(promise.released)
        assertEquals("P-1", promise.protocol)

        val deniedDto = CentralMessageDto(status = 0, message = "limite atingido")
        val denied = deniedDto.toPaymentPromise()
        assertFalse(denied.released)
        assertNull(denied.protocol)
        assertEquals("limite atingido", deniedDto.toMessageOrError())
    }
}
