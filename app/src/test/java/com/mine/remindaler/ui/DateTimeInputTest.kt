package com.mine.remindaler.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class DateTimeInputTest {

    private val now = ZonedDateTime.of(2026, 10, 22, 9, 0, 0, 0, ZoneId.of("Asia/Tokyo"))

    @Test
    fun emptyInput() {
        assertEquals(DateTimeParseResult.Empty, DateTimeInput.parse("", now))
    }

    @Test
    fun incompleteInput() {
        val result = DateTimeInput.parse("1022", now)
        assertTrue(result is DateTimeParseResult.Incomplete)
        assertEquals(4, (result as DateTimeParseResult.Incomplete).entered)
    }

    @Test
    fun futureTimeInSameYear() {
        val result = DateTimeInput.parse("10221030", now)
        assertTrue(result is DateTimeParseResult.Valid)
        val value = (result as DateTimeParseResult.Valid).value
        assertEquals(2026, value.dateTime.year)
        assertEquals(10, value.dateTime.monthValue)
        assertEquals(22, value.dateTime.dayOfMonth)
        assertEquals(10, value.dateTime.hour)
        assertEquals(30, value.dateTime.minute)
        assertEquals(false, value.nextYear)
    }

    @Test
    fun pastTimeGoesToNextYear() {
        val result = DateTimeInput.parse("10220800", now)
        val value = (result as DateTimeParseResult.Valid).value
        assertEquals(2027, value.dateTime.year)
        assertTrue(value.nextYear)
    }

    @Test
    fun february29SkipsInvalidYears() {
        val leapDay = ZonedDateTime.of(2027, 1, 1, 0, 0, 0, 0, ZoneId.of("Asia/Tokyo"))
        val result = DateTimeInput.parse("02290800", leapDay)
        val value = (result as DateTimeParseResult.Valid).value
        assertEquals(2028, value.dateTime.year)
        assertEquals(2, value.dateTime.monthValue)
        assertEquals(29, value.dateTime.dayOfMonth)
    }

    @Test
    fun invalidMonthRejected() {
        assertTrue(DateTimeInput.parse("13220800", now) is DateTimeParseResult.Invalid)
    }

    @Test
    fun invalidDayRejected() {
        assertTrue(DateTimeInput.parse("02300800", now) is DateTimeParseResult.Invalid)
    }

    @Test
    fun april31Rejected() {
        assertTrue(DateTimeInput.parse("04310800", now) is DateTimeParseResult.Invalid)
    }

    @Test
    fun invalidHourRejected() {
        assertTrue(DateTimeInput.parse("10222500", now) is DateTimeParseResult.Invalid)
    }

    @Test
    fun invalidMinuteRejected() {
        assertTrue(DateTimeInput.parse("10220860", now) is DateTimeParseResult.Invalid)
    }

    @Test
    fun currentMinuteIsAccepted() {
        val result = DateTimeInput.parse("10220900", now)
        val value = (result as DateTimeParseResult.Valid).value
        assertEquals(2026, value.dateTime.year)
    }
}
