package com.mine.remindaler.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.mine.remindaler.MainActivity
import com.mine.remindaler.data.Reminder

object AlarmScheduler {

    fun schedule(context: Context, reminder: Reminder): Boolean {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return false
        val operation = alarmPendingIntent(context, reminder.id)
        val showIntent = PendingIntent.getActivity(
            context,
            requestCode(reminder.id, 0),
            Intent(context, MainActivity::class.java).apply {
                action = "com.mine.remindaler.SHOW_${reminder.id}"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val exactAllowed =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        if (!exactAllowed) {
            return scheduleInexact(alarmManager, reminder.triggerAt, operation)
        }

        return try {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(reminder.triggerAt, showIntent),
                operation
            )
            true
        } catch (e: Exception) {
            scheduleInexact(alarmManager, reminder.triggerAt, operation)
        }
    }

    private fun scheduleInexact(
        alarmManager: AlarmManager,
        triggerAt: Long,
        operation: PendingIntent
    ): Boolean = runCatching {
        @Suppress("DEPRECATION")
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            operation
        )
        false
    }.getOrDefault(false)

    fun cancel(context: Context, reminderId: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        alarmManager.cancel(alarmPendingIntent(context, reminderId))
    }

    private fun alarmPendingIntent(context: Context, id: Long): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.mine.remindaler.ALARM_$id"
            putExtra(AlarmReceiver.EXTRA_ID, id)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode(id, 1),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun requestCode(id: Long, offset: Int): Int =
        ((id xor (id ushr 32)).toInt() and 0x7FFFFFFF) - offset
}
