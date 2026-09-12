package com.example.lotterychecker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LotteryResultRepositoryTest {
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
    fun `last four digit winner is not rejected as an invalid series`() {
        val parsed = LotteryResultParser.parse(sampleResult)
        assertFalse("PF" in parsed.validSeries)

        val result = evaluateTicket(TicketNumber("PF", "351208"), parsed, "05/08/2026")

        assertEquals(ResultStatus.WIN, result.status)
        assertTrue(result.message.contains("5th Prize"))
    }
}
