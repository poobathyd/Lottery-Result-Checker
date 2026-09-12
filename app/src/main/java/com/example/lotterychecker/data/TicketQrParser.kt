package com.example.lotterychecker.data

/** Extracts a Kerala lottery series and ticket number from the decoded QR text. */
internal object TicketQrParser {
    private val ticketPattern = Regex("(?i)(?<![A-Z0-9])([A-Z]{2})\\s*[-:/]?\\s*([0-9]{6})(?![0-9])")

    fun extractTicketNumber(rawValue: String): String? = ticketPattern.find(rawValue)?.let { match ->
        "${match.groupValues[1].uppercase()} ${match.groupValues[2]}"
    }
}
