package com.example.lotterychecker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TicketQrParserTest {
    @Test
    fun `extracts compact ticket number from QR content`() {
        assertEquals("PF 351208", TicketQrParser.extractTicketNumber("ticket=pf351208&draw=123"))
    }

    @Test
    fun `extracts ticket number with a separator`() {
        assertEquals("DZ 380334", TicketQrParser.extractTicketNumber("Lottery ticket: DZ-380334"))
    }

    @Test
    fun `does not accept QR content without a complete ticket number`() {
        assertNull(TicketQrParser.extractTicketNumber("https://example.com/ticket/351208"))
    }
}
