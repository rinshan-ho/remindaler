package com.mine.remindaler.ui

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveLayoutTest {

    @Test
    fun tallPhoneUsesCappedKeySize() {
        val size = dialerKeySize(
            maxWidth = 360.dp,
            maxHeight = 800.dp,
            compact = false,
            wideLandscape = false
        )

        assertEquals(76f, size.value, 0.01f)
    }

    @Test
    fun shortScreenKeepsMinimumTouchSize() {
        val size = dialerKeySize(
            maxWidth = 480.dp,
            maxHeight = 320.dp,
            compact = true,
            wideLandscape = false
        )

        assertTrue(size.value >= 48f)
    }

    @Test
    fun wideLandscapeFitsInsideHalfWidth() {
        val size = dialerKeySize(
            maxWidth = 640.dp,
            maxHeight = 360.dp,
            compact = true,
            wideLandscape = true
        )

        assertTrue(size.value >= 48f)
        assertTrue(3f * size.value + 32f <= 320f)
    }

    @Test
    fun compactScreenCapsKeySize() {
        val size = dialerKeySize(
            maxWidth = 800.dp,
            maxHeight = 1280.dp,
            compact = true,
            wideLandscape = false
        )

        assertTrue(size.value <= 56f)
    }
}
