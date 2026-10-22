package com.mine.remindaler.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mine.remindaler.RemindalerApp
import com.mine.remindaler.data.ReminderState
import com.mine.remindaler.notify.NotificationHelper

/**
 * Handles the "拒否" action on the notification when the full screen activity
 * was not shown (full screen intent not permitted).
 */
class DeclineReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_ID, -1L)
        if (id <= 0L) return

        val repository = RemindalerApp.instance.repository
        val reminder = repository.get(id) ?: return

        NotificationHelper.cancelIncoming(context, id)
        if (intent.getBooleanExtra(EXTRA_TEST, false)) return

        repository.updateState(id, ReminderState.MISSED)
        NotificationHelper.showMissed(context, reminder.copy(state = ReminderState.MISSED))
    }

    companion object {
        const val EXTRA_ID = "reminder_id"
        const val EXTRA_TEST = "test_mode"
    }
}
