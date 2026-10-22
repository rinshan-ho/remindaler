package com.mine.remindaler

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import com.mine.remindaler.ui.AppSurfaceHigh
import com.mine.remindaler.ui.AppText
import com.mine.remindaler.ui.DialerScreen
import com.mine.remindaler.ui.MainViewModel
import com.mine.remindaler.ui.PermissionsScreen
import com.mine.remindaler.ui.RecordScreen
import com.mine.remindaler.ui.RemindersScreen
import com.mine.remindaler.ui.RemindalerTheme
import com.mine.remindaler.ui.Screen
import com.mine.remindaler.ui.rememberPermissionsState

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        setContent {
            RemindalerTheme {
                val ui = viewModel.ui
                val permissions = rememberPermissionsState()

                BackHandler(enabled = ui.screen != Screen.DIALER) {
                    when (ui.screen) {
                        Screen.RECORD -> viewModel.cancelRecording()
                        else -> viewModel.goToDialer()
                    }
                }

                when (ui.screen) {
                    Screen.DIALER -> DialerScreen(
                        ui = ui,
                        permissions = permissions,
                        onKey = viewModel::pressKey,
                        onBackspace = viewModel::backspace,
                        onClear = viewModel::clearDigits,
                        onStart = viewModel::startRecording,
                        onOpenReminders = { viewModel.goTo(Screen.REMINDERS) },
                        onOpenSettings = { viewModel.goTo(Screen.PERMISSIONS) }
                    )

                    Screen.RECORD -> RecordScreen(
                        ui = ui,
                        onStop = viewModel::stopAndSave,
                        onCancel = viewModel::cancelRecording,
                        elapsedMs = viewModel::elapsedMs,
                        amplitude = viewModel::amplitude
                    )

                    Screen.REMINDERS -> RemindersScreen(
                        reminders = ui.reminders,
                        recordingsDir = RemindalerApp.instance.repository.recordingsDir,
                        onBack = viewModel::goToDialer,
                        onCancel = viewModel::cancelReminder
                    )

                    Screen.PERMISSIONS -> PermissionsScreen(
                        permissions = permissions,
                        onBack = viewModel::goToDialer
                    )
                }

                ui.confirmation?.let { message ->
                    AlertDialog(
                        onDismissRequest = viewModel::dismissConfirmation,
                        title = { Text("予約しました", color = AppText) },
                        text = { Text(message, color = AppText) },
                        confirmButton = {
                            TextButton(onClick = viewModel::dismissConfirmation) {
                                Text("OK")
                            }
                        },
                        containerColor = AppSurfaceHigh
                    )
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (viewModel.isRecording()) {
            viewModel.cancelRecording()
        }
    }
}
