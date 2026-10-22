package com.mine.remindaler.notify

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * Notification permission checks that also cover Android 10-12, where the user can
 * disable notifications from system settings without a runtime permission.
 */
object NotificationAvailability {

    fun canPost(context: Context): Boolean {
        val appContext = context.applicationContext
        if (!NotificationManagerCompat.from(appContext).areNotificationsEnabled()) return false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }
        return true
    }

    fun canUseChannel(context: Context, channelId: String): Boolean {
        if (!canPost(context)) return false
        val manager = context.applicationContext
            .getSystemService(NotificationManager::class.java)
            ?: return false
        val channel = manager.getNotificationChannel(channelId) ?: return true
        return channel.importance != NotificationManager.IMPORTANCE_NONE
    }
}
