package com.mine.remindaler.notify

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.mine.remindaler.IncomingCallActivity
import com.mine.remindaler.MainActivity
import com.mine.remindaler.R
import com.mine.remindaler.alarm.DeclineReceiver
import com.mine.remindaler.data.Reminder
import com.mine.remindaler.util.TimeFormat

object NotificationHelper {

    const val CHANNEL_INCOMING = "incoming_calls"
    const val CHANNEL_MISSED = "missed_calls"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val alarmSound = defaultAlarmSound()

        val alarmAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val incoming = NotificationChannel(
            CHANNEL_INCOMING,
            "リマインド着信",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "指定した時刻に電話のような着信を表示します"
            setSound(alarmSound, alarmAttributes)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 800, 500, 800)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }

        val missed = NotificationChannel(
            CHANNEL_MISSED,
            "不在着信",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "応答されなかったリマインドを知らせます"
            setSound(null, null)
            enableVibration(false)
        }

        manager.createNotificationChannels(listOf(incoming, missed))
    }

    @SuppressLint("MissingPermission")
    fun showIncoming(context: Context, reminder: Reminder, test: Boolean = false): Boolean {
        if (!NotificationAvailability.canUseChannel(context, CHANNEL_INCOMING)) return false

        val appContext = context.applicationContext
        val fullScreenIntent = PendingIntent.getActivity(
            appContext,
            requestCode(reminder.id, 11),
            IncomingCallActivity.intent(appContext, reminder.id, test = test).apply {
                action = "com.mine.remindaler.INCOMING_${reminder.id}"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val answerIntent = PendingIntent.getActivity(
            appContext,
            requestCode(reminder.id, 12),
            IncomingCallActivity.intent(appContext, reminder.id, answer = true, test = test).apply {
                action = "com.mine.remindaler.ANSWER_${reminder.id}"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val declineIntent = PendingIntent.getBroadcast(
            appContext,
            requestCode(reminder.id, 13),
            Intent(appContext, DeclineReceiver::class.java).apply {
                action = "com.mine.remindaler.DECLINE_${reminder.id}"
                putExtra(DeclineReceiver.EXTRA_ID, reminder.id)
                putExtra(DeclineReceiver.EXTRA_TEST, test)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(appContext, CHANNEL_INCOMING)
            .setSmallIcon(R.drawable.ic_stat_phone)
            .setColor(0xFF4ADE80.toInt())
            .setContentTitle("リマインド着信")
            .setContentText("${TimeFormat.full(reminder.triggerAt)} のリマインド")
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreenIntent, true)
            .setContentIntent(fullScreenIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setWhen(0)
            .setShowWhen(false)
            .addAction(
                // Playing the recorded message requires an unlocked device.
                NotificationCompat.Action.Builder(0, "応答", answerIntent)
                    .setAuthenticationRequired(true)
                    .build()
            )
            .addAction(
                NotificationCompat.Action.Builder(0, "拒否", declineIntent).build()
            )

        return try {
            NotificationManagerCompat.from(appContext)
                .notify(incomingNotificationId(reminder.id), builder.build())
            true
        } catch (e: SecurityException) {
            false
        }
    }

    @SuppressLint("MissingPermission")
    fun showMissed(context: Context, reminder: Reminder) {
        if (!NotificationAvailability.canPost(context)) return

        val appContext = context.applicationContext
        val openIntent = PendingIntent.getActivity(
            appContext,
            requestCode(reminder.id, 21),
            IncomingCallActivity.intent(appContext, reminder.id, answer = true).apply {
                action = "com.mine.remindaler.REPLAY_${reminder.id}"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val appIntent = PendingIntent.getActivity(
            appContext,
            requestCode(reminder.id, 22),
            Intent(appContext, MainActivity::class.java).apply {
                action = "com.mine.remindaler.OPEN_${reminder.id}"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(appContext, CHANNEL_MISSED)
            .setSmallIcon(R.drawable.ic_stat_phone)
            .setContentTitle("不在着信")
            .setContentText("${TimeFormat.full(reminder.triggerAt)} のリマインド")
            .setCategory(NotificationCompat.CATEGORY_MISSED_CALL)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            // Tapping the notification opens the app; it never starts playback directly.
            .setContentIntent(appIntent)
            .setAutoCancel(true)
            .addAction(
                // Replay also requires an unlocked device.
                NotificationCompat.Action.Builder(0, "再生", openIntent)
                    .setAuthenticationRequired(true)
                    .build()
            )
            .addAction(NotificationCompat.Action.Builder(0, "アプリを開く", appIntent).build())

        try {
            NotificationManagerCompat.from(appContext)
                .notify(missedNotificationId(reminder.id), builder.build())
        } catch (e: SecurityException) {
            // Notifications are not permitted.
        }
    }

    fun cancelIncoming(context: Context, reminderId: Long) {
        NotificationManagerCompat.from(context.applicationContext)
            .cancel(incomingNotificationId(reminderId))
    }

    fun cancelMissed(context: Context, reminderId: Long) {
        NotificationManagerCompat.from(context.applicationContext)
            .cancel(missedNotificationId(reminderId))
    }

    fun cancelAll(context: Context, reminderId: Long) {
        cancelIncoming(context, reminderId)
        cancelMissed(context, reminderId)
    }

    private fun defaultAlarmSound() =
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    private fun incomingNotificationId(id: Long): Int =
        10_000 + ((id xor (id ushr 32)).toInt() and 0x0FFFFFFF)

    private fun missedNotificationId(id: Long): Int =
        300_000_000 + ((id xor (id ushr 32)).toInt() and 0x0FFFFFFF)

    private fun requestCode(id: Long, offset: Int): Int =
        ((id xor (id ushr 32)).toInt() and 0x7FFFFFFF) - 100 * offset
}
