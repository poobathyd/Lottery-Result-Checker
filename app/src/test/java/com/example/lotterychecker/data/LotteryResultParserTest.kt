package com.example.lotterychecker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LotteryResultParserTest {
    private val sampleResult = """
        1st Prize Rs :10000000/-
        1) PZ 902530 (KASARAGOD)
        Cons Prize-Rs :5000/-
        PN 902530 PO 902530 PP 902530
        2nd Prize Rs :3000000/-
        1) PV 358141 (PALAKKAD)
        3rd Prize Rs :500000/-
        1) PV 339420 (ADOOR)
        FOR THE TICKETS ENDING WITH THE FOLLOWING NUMBERS
        4th Prize-Rs :5000/-
        0663
        0782
        5th Prize-Rs :100/-
        1208
        9th Prize-Rs :100/-
        0208
        Prize winners should claim their prize within 90 days.
        SAJU KUMAR.P
        Deputy Director
        Next DHANALEKSHMI Draw will be held on 05/08/2026
    """.trimIndent()

    @Test
    fun `matches a lower prize by its final four digits`() {
        val result = LotteryResultParser.parse(sampleResult)

        assertEquals("5th Prize-Rs :100/-", result.findPrize(TicketNumber("PF", "351208")))
    }

    @Test
    fun `does not mistake a prize amount for a winning number`() {
        val result = LotteryResultParser.parse(sampleResult)

        assertFalse(result.findPrize(TicketNumber("PF", "123100")) != null)
    }

    @Test
    fun `reads the valid two letter series from the draw`() {
        val result = LotteryResultParser.parse(sampleResult)

        assertTrue("PZ" in result.validSeries)
        assertTrue("PN" in result.validSeries)
        assertTrue("PF" !in result.validSeries)
    }

    @Test
    fun `builds a complete list with every prize section`() {
        val completeResults = LotteryResultParser.parse(sampleResult).completeResults().orEmpty()

        assertTrue(completeResults.contains("1st Prize Rs :10000000/-"))
        assertTrue(completeResults.contains("Cons Prize-Rs :5000/-"))
        assertTrue(completeResults.contains("9th Prize-Rs :100/-"))
        assertTrue(completeResults.endsWith("within 90 days."))
        assertFalse(completeResults.contains("SAJU KUMAR"))
    }
}
