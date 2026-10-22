package com.mine.remindaler.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionStatusTest {

    @Test
    fun microphoneAndNotificationsAreBlocking() {
        assertTrue(status(microphone = false, notifications = false).blockingIssues)
        assertTrue(status(microphone = false, notifications = true).blockingIssues)
        assertTrue(status(microphone = true, notifications = false).blockingIssues)
    }

    @Test
    fun optionalPermissionsDoNotBlockDialer() {
        val status = PermissionStatus(
            microphone = true,
            notifications = true,
            fullScreenIntent = false,
            exactAlarm = false,
            batteryExempt = false
        )

        assertFalse(status.blockingIssues)
    }

    private fun status(
        microphone: Boolean,
        notifications: Boolean
    ): PermissionStatus = PermissionStatus(
        microphone = microphone,
        notifications = notifications,
        fullScreenIntent = true,
        exactAlarm = true,
        batteryExempt = true
    )
}
