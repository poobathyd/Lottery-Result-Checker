package com.example.lotterychecker.data

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import org.jsoup.Jsoup

enum class ResultStatus { WIN, NO_PRIZE, INVALID, NOT_FOUND, ERROR }

data class LotteryCheckResult(
    val message: String,
    val completeResults: String? = null,
    val status: ResultStatus = ResultStatus.NO_PRIZE
)

class LotteryResultRepository {
    fun getResultSummary(ticketNumber: String, drawDate: String): LotteryCheckResult {
        val ticket = TicketNumber.from(ticketNumber)
            ?: return LotteryCheckResult(
                "Enter a valid ticket number, for example DZ 380334.",
                status = ResultStatus.INVALID
            )

        return try {
            val resultUrl = findResultUrl(drawDate)
                ?: return LotteryCheckResult(
                    "No published result was found for $drawDate.",
                    status = ResultStatus.NOT_FOUND
                )
            val parsedResult = LotteryResultParser.parse(downloadResultText(resultUrl))
            val completeResults = parsedResult.completeResults()

            when {
                parsedResult.validSeries.isNotEmpty() && ticket.series !in parsedResult.validSeries -> {
                    LotteryCheckResult(
                        "$drawDate: ${ticket.series} is not a valid series for this draw. " +
                            "Valid series: ${parsedResult.validSeries.joinToString(", ")}.",
                        completeResults,
                        ResultStatus.INVALID
                    )
                }
                else -> parsedResult.findPrize(ticket)
                    ?.let {
                        LotteryCheckResult(
                            "$drawDate: Your ticket won $it. Verify the ticket with the official Gazette before claiming.",
                            completeResults,
                            ResultStatus.WIN
                        )
                    }
                    ?: LotteryCheckResult(
                        "$drawDate: No prize was found for ${ticket.displayValue}.",
                        completeResults,
                        ResultStatus.NO_PRIZE
                    )
            }
        } catch (_: Exception) {
            LotteryCheckResult(
                "Could not check the result. Please check your internet connection and try again.",
                status = ResultStatus.ERROR
            )
        }
    }

    private fun findResultUrl(drawDate: String): String? {
        val archive = Jsoup.connect(RESULT_ARCHIVE_URL)
            .userAgent(USER_AGENT)
            .timeout(NETWORK_TIMEOUT_MS)
            .get()

        return archive.select("tr")
            .firstOrNull { row -> row.text().contains(drawDate) }
            ?.select("a[href]")
            ?.firstOrNull()
            ?.absUrl("href")
            ?.replaceFirst("http://", "https://")
            ?.takeIf { it.isNotBlank() }
    }

    private fun downloadResultText(url: String): String {
        val pdfBytes = Jsoup.connect(url)
            .userAgent(USER_AGENT)
            .timeout(NETWORK_TIMEOUT_MS)
            .ignoreContentType(true)
            .execute()
            .bodyAsBytes()

        PDDocument.load(pdfBytes).use { document ->
            return PDFTextStripper().getText(document)
        }
    }

    private companion object {
        const val RESULT_ARCHIVE_URL = "https://www.statelottery.kerala.gov.in/English/index.php/lottery-result-view"
        const val USER_AGENT = "Mozilla/5.0 (Android) LotteryChecker/1.0"
        const val NETWORK_TIMEOUT_MS = 20_000
    }
}

internal data class TicketNumber(val series: String, val number: String) {
    val displayValue get() = "$series $number"

    companion object {
        private val ticketPattern = Regex("^([A-Z]{2})\\s*([0-9]{6})$")

        fun from(value: String): TicketNumber? {
            val match = ticketPattern.matchEntire(value.trim().uppercase()) ?: return null
            return TicketNumber(match.groupValues[1], match.groupValues[2])
        }
    }
}

/** Parses only prize sections, so prize amounts can never be mistaken for winning numbers. */
internal object LotteryResultParser {
    // Supports both "1st Prize Rs :10000000/-" and "1st Prize-Rs.: 10000000/-".
    private val prizeHeading = Regex(
        "(?im)^\\s*((?:\\d+(?:st|nd|rd|th)|Cons(?:olation)?)\\s+Prize\\s*-?\\s*Rs\\.?\\s*:?\\s*[0-9,]+/-?)"
    )
    private val fullTicket = Regex("\\b([A-Z]{2})\\s*([0-9]{6})\\b")
    private val claimNoticeEnd = Regex("(?is)\\bwithin\\s+90\\s+days\\.?")

    fun parse(resultText: String): ParsedLotteryResult {
        val headings = prizeHeading.findAll(resultText).toList()
        val sections = headings.mapIndexed { index, heading ->
            val contentStart = heading.range.last + 1
            val contentEnd = headings.getOrNull(index + 1)?.range?.first ?: resultText.length
            PrizeSection(
                heading = heading.groupValues[1].replace(Regex("\\s+"), " ").trim(),
                content = resultText.substring(contentStart, contentEnd).trim()
            )
        }
        return ParsedLotteryResult(sections)
    }

    internal data class ParsedLotteryResult(private val sections: List<PrizeSection>) {
        val validSeries: List<String> by lazy {
            sections.asSequence()
                .filter { it.requiresFullTicket }
                .flatMap { section -> fullTicket.findAll(section.content).map { it.groupValues[1] } }
                .distinct()
                .sorted()
                .toList()
        }

        fun findPrize(ticket: TicketNumber): String? = sections.firstOrNull { section ->
            if (section.requiresFullTicket) {
                fullTicket.findAll(section.content).any { match ->
                    match.groupValues[1] == ticket.series && match.groupValues[2] == ticket.number
                }
            } else {
                Regex("(?<![0-9])${ticket.number.takeLast(4)}(?![0-9])").containsMatchIn(section.content)
            }
        }?.heading

        fun completeResults(): String? {
            if (sections.isEmpty()) return null
            val results = sections.joinToString("\n\n") { section ->
                val entries = section.content
                    .lineSequence()
                    .filterNot { it.trim().matches(Regex("Page\\s+\\d+", RegexOption.IGNORE_CASE)) }
                    .filterNot { it.contains("Modernization & IT Software Division", ignoreCase = true) }
                    .joinToString("\n") { it.trim() }
                    .trim()
                if (entries.isEmpty()) section.heading else "${section.heading}\n$entries"
            }.let { results ->
                // The official PDF appends signer and next-draw details after this notice.
                claimNoticeEnd.find(results)?.let { notice -> results.take(notice.range.last + 1) } ?: results
            }
            return buildString {
                if (validSeries.isNotEmpty()) {
                    append("Valid series: ")
                    append(validSeries.joinToString(", "))
                    append("\n\n")
                }
                append(results)
            }
        }
    }

    internal data class PrizeSection(val heading: String, val content: String) {
        val requiresFullTicket = heading.startsWith("1st", ignoreCase = true) ||
            heading.startsWith("2nd", ignoreCase = true) ||
            heading.startsWith("3rd", ignoreCase = true) ||
            heading.startsWith("Cons", ignoreCase = true)
    }
}
