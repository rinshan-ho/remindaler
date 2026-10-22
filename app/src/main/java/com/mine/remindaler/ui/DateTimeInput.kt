package com.mine.remindaler.ui

import java.time.DateTimeException
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

data class ParsedDateTime(
    val dateTime: ZonedDateTime,
    val nextYear: Boolean,
    val display: String
)

sealed interface DateTimeParseResult {
    data object Empty : DateTimeParseResult
    data class Incomplete(val entered: Int) : DateTimeParseResult
    data class Invalid(val message: String) : DateTimeParseResult
    data class Valid(val value: ParsedDateTime) : DateTimeParseResult
}

/**
 * Parses the 8 digit dialer input: MMDDhhmm.
 * If the resulting date/time is in the past, the next valid year is used.
 */
object DateTimeInput {

    const val LENGTH = 8

    private val displayFormatter =
        DateTimeFormatter.ofPattern("M月d日(E) HH:mm", Locale.JAPAN)

    fun parse(digits: String, now: ZonedDateTime = ZonedDateTime.now()): DateTimeParseResult {
        if (digits.isEmpty()) return DateTimeParseResult.Empty
        if (digits.length < LENGTH) return DateTimeParseResult.Incomplete(digits.length)
        if (digits.length > LENGTH) return DateTimeParseResult.Invalid("8桁で入力してください")

        val month = digits.substring(0, 2).toIntOrNull()
            ?: return DateTimeParseResult.Invalid("数字のみ入力できます")
        val day = digits.substring(2, 4).toIntOrNull()
            ?: return DateTimeParseResult.Invalid("数字のみ入力できます")
        val hour = digits.substring(4, 6).toIntOrNull()
            ?: return DateTimeParseResult.Invalid("数字のみ入力できます")
        val minute = digits.substring(6, 8).toIntOrNull()
            ?: return DateTimeParseResult.Invalid("数字のみ入力できます")

        if (month !in 1..12) return DateTimeParseResult.Invalid("月は 01〜12 で入力してください")
        if (day !in 1..31) return DateTimeParseResult.Invalid("日は 01〜31 で入力してください")
        if (hour !in 0..23) return DateTimeParseResult.Invalid("時は 00〜23 で入力してください")
        if (minute !in 0..59) return DateTimeParseResult.Invalid("分は 00〜59 で入力してください")

        var year = now.year
        var candidate = buildOrNull(year, month, day, hour, minute, now)
        val currentMinute = now.truncatedTo(ChronoUnit.MINUTES)

        if (candidate == null || candidate.isBefore(currentMinute)) {
            var found: ZonedDateTime? = null
            var attempts = 0
            while (attempts < 9 && found == null) {
                year += 1
                attempts += 1
                found = buildOrNull(year, month, day, hour, minute, now)
            }
            candidate = found ?: return DateTimeParseResult.Invalid("存在しない日付です")
        }

        val nextYear = candidate.year != now.year
        val formatted = candidate.format(displayFormatter)
        val display = if (nextYear) "${candidate.year}年$formatted" else formatted
        return DateTimeParseResult.Valid(
            ParsedDateTime(
                dateTime = candidate,
                nextYear = nextYear,
                display = display
            )
        )
    }

    private fun buildOrNull(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
        now: ZonedDateTime
    ): ZonedDateTime? = try {
        LocalDateTime.of(year, month, day, hour, minute).atZone(now.zone)
    } catch (e: DateTimeException) {
        null
    }
}
