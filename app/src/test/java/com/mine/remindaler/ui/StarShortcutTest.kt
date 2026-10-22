package com.mine.remindaler.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class StarShortcutTest {

    private val zone = ZoneId.of("Asia/Tokyo")
    private val now = ZonedDateTime.of(2026, 10, 22, 17, 35, 0, 0, zone)

    @Test
    fun firstTapInsertsMonthAndCapturesTime() {
        val result = StarShortcut.next("", StarShortcutState(), now)!!

        assertEquals("10", result.digits)
        assertEquals(1, result.state.tapCount)
        assertEquals(now, result.state.capturedAt)
    }

    @Test
    fun tapsInsertMonthDayHourInOrder() {
        val first = StarShortcut.next("", StarShortcutState(), now)!!
        val second = StarShortcut.next(first.digits, first.state, now.plusHours(1))!!
        val third = StarShortcut.next(second.digits, second.state, now.plusHours(2))!!

        assertEquals("1022", second.digits)
        assertEquals("102217", third.digits)
        assertEquals(3, third.state.tapCount)
    }

    @Test
    fun fourthTapDoesNothing() {
        val first = StarShortcut.next("", StarShortcutState(), now)!!
        val second = StarShortcut.next(first.digits, first.state, now)!!
        val third = StarShortcut.next(second.digits, second.state, now)!!

        assertNull(StarShortcut.next(third.digits, third.state, now))
    }

    @Test
    fun doesNotStartWhenDigitsAlreadyEntered() {
        assertNull(StarShortcut.next("1", StarShortcutState(), now))
        assertNull(StarShortcut.next("10", StarShortcutState(), now))
    }

    @Test
    fun padsMonthDayHourToTwoDigits() {
        val early = ZonedDateTime.of(2026, 1, 5, 3, 7, 0, 0, zone)
        val first = StarShortcut.next("", StarShortcutState(), early)!!
        val second = StarShortcut.next(first.digits, first.state, early)!!
        val third = StarShortcut.next(second.digits, second.state, early)!!

        assertEquals("01", first.digits)
        assertEquals("0105", second.digits)
        assertEquals("010503", third.digits)
    }

    @Test
    fun usesFirstTapTimeAcrossMidnight() {
        val justBeforeMidnight = ZonedDateTime.of(2026, 10, 31, 23, 59, 0, 0, zone)
        val first = StarShortcut.next("", StarShortcutState(), justBeforeMidnight)!!
        val second = StarShortcut.next(
            first.digits,
            first.state,
            justBeforeMidnight.plusMinutes(2)
        )!!
        val third = StarShortcut.next(second.digits, second.state, first.state.capturedAt!!.plusMinutes(3))!!

        assertEquals("1031", second.digits)
        assertEquals("103123", third.digits)
        assertEquals(justBeforeMidnight, third.state.capturedAt)
    }

    @Test
    fun rejectsStateWhenDigitsDoNotMatchSequence() {
        val first = StarShortcut.next("", StarShortcutState(), now)!!

        assertNull(StarShortcut.next("99", first.state, now))

        val second = StarShortcut.next(first.digits, first.state, now)!!
        assertNull(StarShortcut.next("1099", second.state, now))
    }
}
