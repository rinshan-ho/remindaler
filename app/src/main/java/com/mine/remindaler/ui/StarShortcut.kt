package com.mine.remindaler.ui

import java.time.ZonedDateTime

/**
 * State for the `*` shortcut: the number of taps and the time captured on the first tap.
 */
data class StarShortcutState(
    val tapCount: Int = 0,
    val capturedAt: ZonedDateTime? = null
)

data class StarShortcutResult(
    val digits: String,
    val state: StarShortcutState
)

/**
 * Fills the dialer with the current month, day, and hour, two digits at a time.
 * The first tap captures the time so a sequence that crosses midnight stays consistent.
 */
object StarShortcut {

    fun next(
        digits: String,
        state: StarShortcutState,
        now: ZonedDateTime
    ): StarShortcutResult? {
        if (state.tapCount == 0) {
            if (digits.isNotEmpty()) return null
            return StarShortcutResult(
                digits = twoDigits(now.monthValue),
                state = StarShortcutState(tapCount = 1, capturedAt = now)
            )
        }

        if (state.tapCount !in 1..2) return null
        val capturedAt = state.capturedAt ?: return null
        if (digits != expectedPrefix(capturedAt, state.tapCount)) return null
        if (digits.length + 2 > DateTimeInput.LENGTH) return null

        val value = when (state.tapCount) {
            1 -> capturedAt.dayOfMonth
            else -> capturedAt.hour
        }
        return StarShortcutResult(
            digits = digits + twoDigits(value),
            state = StarShortcutState(
                tapCount = state.tapCount + 1,
                capturedAt = capturedAt
            )
        )
    }

    private fun expectedPrefix(capturedAt: ZonedDateTime, tapCount: Int): String = buildString {
        append(twoDigits(capturedAt.monthValue))
        if (tapCount >= 2) append(twoDigits(capturedAt.dayOfMonth))
    }

    private fun twoDigits(value: Int): String = value.toString().padStart(2, '0')
}
