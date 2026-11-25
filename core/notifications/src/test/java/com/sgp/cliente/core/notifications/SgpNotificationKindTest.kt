package com.sgp.cliente.core.notifications

import org.junit.Assert.assertEquals
import org.junit.Test

class SgpNotificationKindTest {

    @Test
    fun `invoice maps to invoices channel`() {
        val kind = SgpNotificationKind.of("invoice")
        assertEquals(SgpNotificationKind.INVOICE, kind)
        assertEquals(NotificationChannels.INVOICES, kind.channelId)
    }

    @Test
    fun `ticket maps to tickets channel`() {
        val kind = SgpNotificationKind.of("ticket")
        assertEquals(SgpNotificationKind.TICKET, kind)
        assertEquals(NotificationChannels.TICKETS, kind.channelId)
    }

    @Test
    fun `crm announcement aliases fall back to general channel`() {
        assertEquals(SgpNotificationKind.GENERAL, SgpNotificationKind.of("announcement"))
        assertEquals(SgpNotificationKind.GENERAL, SgpNotificationKind.of("maintenance"))
        assertEquals(
            NotificationChannels.GENERAL,
            SgpNotificationKind.of("announcement").channelId,
        )
    }

    @Test
    fun `unknown and null fall back to general channel`() {
        assertEquals(SgpNotificationKind.GENERAL, SgpNotificationKind.of(null))
        assertEquals(SgpNotificationKind.GENERAL, SgpNotificationKind.of("promo"))
        assertEquals(
            NotificationChannels.GENERAL,
            SgpNotificationKind.of(null).channelId,
        )
    }
}
