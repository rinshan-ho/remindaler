package com.mine.remindaler.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mine.remindaler.RemindalerApp
import com.mine.remindaler.data.ReminderState
import com.mine.remindaler.notify.NotificationHelper

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_ID, -1L)
        if (id <= 0L) return

        val repository = RemindalerApp.instance.repository
        val reminder = repository.get(id) ?: return
        if (reminder.state != ReminderState.SCHEDULED) return

        val shown = NotificationHelper.showIncoming(context, reminder)
        if (!shown) {
            repository.updateState(id, ReminderState.MISSED)
            NotificationHelper.showMissed(
                context,
                reminder.copy(state = ReminderState.MISSED)
            )
        }
    }

    companion object {
        const val EXTRA_ID = "reminder_id"
    }
}
