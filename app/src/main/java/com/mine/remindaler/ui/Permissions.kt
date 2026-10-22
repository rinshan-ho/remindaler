package com.mine.remindaler.ui

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mine.remindaler.notify.NotificationAvailability
import com.mine.remindaler.notify.NotificationHelper

data class PermissionStatus(
    val microphone: Boolean,
    val notifications: Boolean,
    val fullScreenIntent: Boolean,
    val exactAlarm: Boolean,
    val batteryExempt: Boolean
) {
    val blockingIssues: Boolean
        get() = !microphone || !notifications
}

fun readPermissionStatus(context: Context): PermissionStatus {
    val microphone = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    val notifications = NotificationAvailability.canUseChannel(
        context,
        NotificationHelper.CHANNEL_INCOMING
    )

    val fullScreenIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        context.getSystemService(NotificationManager::class.java)?.canUseFullScreenIntent() == true
    } else {
        true
    }

    val exactAlarm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() == true
    } else {
        true
    }

    val batteryExempt = context.getSystemService(PowerManager::class.java)
        ?.isIgnoringBatteryOptimizations(context.packageName) == true

    return PermissionStatus(
        microphone = microphone,
        notifications = notifications,
        fullScreenIntent = fullScreenIntent,
        exactAlarm = exactAlarm,
        batteryExempt = batteryExempt
    )
}

class PermissionsState(context: Context) {

    private val appContext = context.applicationContext

    var status by mutableStateOf(readPermissionStatus(appContext))
        private set

    fun refresh() {
        status = readPermissionStatus(appContext)
    }
}

@Composable
fun rememberPermissionsState(): PermissionsState {
    val context = LocalContext.current
    val state = remember { PermissionsState(context) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                state.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return state
}

fun openNotificationSettings(context: Context) {
    startFirstAvailable(
        context,
        notificationSettingsIntent(context),
        appDetailsIntent(context),
        Intent(Settings.ACTION_SETTINGS)
    )
}

fun openFullScreenIntentSettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        val opened = startFirstAvailable(
            context,
            Intent(
                Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                packageUri(context)
            ),
            appDetailsIntent(context),
            notificationSettingsIntent(context),
            Intent(Settings.ACTION_SETTINGS)
        )
        if (opened) return
    }
    openNotificationSettings(context)
}

fun openExactAlarmSettings(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        openAppSettings(context)
        return
    }
    startFirstAvailable(
        context,
        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, packageUri(context)),
        appDetailsIntent(context),
        Intent(Settings.ACTION_SETTINGS)
    )
}

fun requestBatteryExemption(context: Context) {
    startFirstAvailable(
        context,
        Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            packageUri(context)
        ),
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
        appDetailsIntent(context),
        Intent(Settings.ACTION_SETTINGS)
    )
}

fun openSoundSettings(context: Context) {
    startFirstAvailable(
        context,
        Intent(Settings.ACTION_SOUND_SETTINGS),
        appDetailsIntent(context),
        Intent(Settings.ACTION_SETTINGS)
    )
}

fun openAppSettings(context: Context) {
    startFirstAvailable(
        context,
        appDetailsIntent(context),
        notificationSettingsIntent(context),
        Intent(Settings.ACTION_SETTINGS)
    )
}

private fun appDetailsIntent(context: Context): Intent =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri(context))

private fun notificationSettingsIntent(context: Context): Intent =
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    }

private fun packageUri(context: Context): Uri = Uri.parse("package:${context.packageName}")

private fun startFirstAvailable(context: Context, vararg intents: Intent): Boolean {
    for (intent in intents) {
        val launched = runCatching {
            context.startActivity(
                Intent(intent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.isSuccess
        if (launched) return true
    }
    return false
}
