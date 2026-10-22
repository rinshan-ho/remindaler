package com.mine.remindaler.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object TimeFormat {

    private val fullFormatter =
        DateTimeFormatter.ofPattern("M月d日(E) HH:mm", Locale.JAPAN)

    private val shortFormatter =
        DateTimeFormatter.ofPattern("M/d(E) HH:mm", Locale.JAPAN)

    fun full(triggerAt: Long): String {
        val dateTime = Instant.ofEpochMilli(triggerAt).atZone(ZoneId.systemDefault())
        val base = dateTime.format(fullFormatter)
        val currentYear = ZoneId.systemDefault().let { java.time.ZonedDateTime.now(it).year }
        return if (dateTime.year == currentYear) base else "${dateTime.year}年$base"
    }

    fun short(triggerAt: Long): String =
        Instant.ofEpochMilli(triggerAt).atZone(ZoneId.systemDefault()).format(shortFormatter)

    fun duration(durationMs: Long): String {
        val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return String.format(Locale.JAPAN, "%d:%02d", minutes, seconds)
    }
}
