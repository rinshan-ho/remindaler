package com.mine.remindaler.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mine.remindaler.RemindalerApp
import com.mine.remindaler.data.ReminderState
import com.mine.remindaler.notify.NotificationHelper

/**
 * Alarms are cleared by the OS on reboot, so all future reminders are scheduled
 * again. Reminders whose time passed while the device was off are marked as
 * missed.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in SUPPORTED_ACTIONS) return

        val repository = RemindalerApp.instance.repository
        val now = System.currentTimeMillis()

        repository.reminders.value
            .filter { it.state == ReminderState.SCHEDULED }
            .forEach { reminder ->
                if (reminder.triggerAt <= now) {
                    repository.updateState(reminder.id, ReminderState.MISSED)
                    NotificationHelper.showMissed(
                        context,
                        reminder.copy(state = ReminderState.MISSED)
                    )
                } else {
                    AlarmScheduler.schedule(context, reminder)
                }
            }
    }

    private companion object {
        val SUPPORTED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED
        )
    }
}
