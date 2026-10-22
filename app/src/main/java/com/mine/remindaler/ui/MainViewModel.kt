package com.mine.remindaler.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mine.remindaler.RemindalerApp
import com.mine.remindaler.alarm.AlarmScheduler
import com.mine.remindaler.audio.AudioRecorder
import com.mine.remindaler.data.Reminder
import com.mine.remindaler.data.ReminderState
import com.mine.remindaler.notify.NotificationAvailability
import com.mine.remindaler.notify.NotificationHelper
import kotlinx.coroutines.launch
import java.io.File
import java.time.ZonedDateTime

enum class Screen {
    DIALER,
    RECORD,
    REMINDERS,
    PERMISSIONS
}

data class MainUiState(
    val screen: Screen = Screen.DIALER,
    val digits: String = "",
    val error: String? = null,
    val recording: Boolean = false,
    val pendingPreview: String = "",
    val confirmation: String? = null,
    val reminders: List<Reminder> = emptyList()
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RemindalerApp.instance.repository
    private val recorder = AudioRecorder(application)

    var ui by mutableStateOf(MainUiState(reminders = repository.reminders.value))
        private set

    private var pendingTriggerAt = 0L
    private var starState = StarShortcutState()

    init {
        viewModelScope.launch {
            repository.reminders.collect { reminders ->
                ui = ui.copy(reminders = reminders)
            }
        }
    }

    fun pressKey(key: String) {
        if (key == "*") {
            pressStarShortcut()
            return
        }
        if (key.length != 1 || !key[0].isDigit()) return
        starState = StarShortcutState()
        if (ui.digits.length >= DateTimeInput.LENGTH) return
        val next = ui.digits + key
        ui = ui.copy(digits = next, error = null)
    }

    fun backspace() {
        starState = StarShortcutState()
        if (ui.digits.isEmpty()) return
        val next = ui.digits.dropLast(1)
        ui = ui.copy(digits = next, error = null)
    }

    fun clearDigits() {
        starState = StarShortcutState()
        ui = ui.copy(digits = "", error = null)
    }

    private fun pressStarShortcut() {
        // A completed or abandoned sequence should not block a fresh start after the
        // input was cleared by saving a reminder or by another screen action.
        if (starState.tapCount > 0 && ui.digits.length != starState.tapCount * 2) {
            starState = StarShortcutState()
        }

        val result = StarShortcut.next(ui.digits, starState, ZonedDateTime.now())
        if (result == null) {
            if (starState.tapCount in 1..2) starState = StarShortcutState()
            return
        }
        starState = result.state
        ui = ui.copy(digits = result.digits, error = null)
    }

    fun startRecording(parsed: ParsedDateTime) {
        val file = recorder.start()
        if (file == null) {
            ui = ui.copy(error = "録音を開始できませんでした。マイク権限を確認してください。")
            return
        }
        pendingTriggerAt = parsed.dateTime.toInstant().toEpochMilli()
        ui = ui.copy(
            screen = Screen.RECORD,
            recording = true,
            pendingPreview = parsed.display,
            error = null
        )
    }

    fun stopAndSave() {
        if (!ui.recording) return
        val result = recorder.stop()
        ui = ui.copy(recording = false)

        if (result == null) {
            ui = ui.copy(screen = Screen.DIALER, error = "録音を保存できませんでした")
            return
        }

        if (result.durationMs < 700L) {
            result.file.delete()
            ui = ui.copy(
                screen = Screen.DIALER,
                error = "録音が短すぎます。1秒以上話してから受話器を置いてください。"
            )
            return
        }

        if (pendingTriggerAt <= 0L) {
            result.file.delete()
            ui = ui.copy(screen = Screen.DIALER, error = "日時を解釈できませんでした")
            return
        }

        val id = System.currentTimeMillis()
        val destination = File(repository.recordingsDir, "r_$id.m4a")
        if (!result.file.renameTo(destination)) {
            runCatching {
                result.file.copyTo(destination, overwrite = true)
                result.file.delete()
            }.onFailure {
                result.file.delete()
                ui = ui.copy(screen = Screen.DIALER, error = "録音ファイルを保存できませんでした")
                return
            }
        }

        val reminder = Reminder(
            id = id,
            triggerAt = pendingTriggerAt,
            audioFile = destination.name,
            durationMs = result.durationMs,
            state = ReminderState.SCHEDULED,
            createdAt = System.currentTimeMillis()
        )
        repository.add(reminder)
        val exact = AlarmScheduler.schedule(getApplication(), reminder)

        val preview = ui.pendingPreview
        val warnings = buildList {
            if (!exact) {
                add("正確なアラームが許可されていないため、数分遅れる場合があります。")
            }
            if (!canPostNotifications()) {
                add("通知が許可されていないため、時刻になっても着信が表示されません。")
            }
        }
        val message = buildString {
            append(preview)
            append(" に着信します。")
            if (warnings.isNotEmpty()) {
                append("\n\n")
                warnings.forEach { append("・").append(it).append('\n') }
            }
        }

        starState = StarShortcutState()
        ui = ui.copy(
            screen = Screen.DIALER,
            digits = "",
            error = null,
            pendingPreview = "",
            confirmation = message.trim()
        )
        pendingTriggerAt = 0L
    }

    fun cancelRecording() {
        recorder.cancel()
        ui = ui.copy(screen = Screen.DIALER, recording = false, pendingPreview = "")
        pendingTriggerAt = 0L
    }

    fun cancelReminder(reminder: Reminder) {
        AlarmScheduler.cancel(getApplication(), reminder.id)
        NotificationHelper.cancelAll(getApplication(), reminder.id)
        repository.remove(reminder.id)
    }

    fun goTo(screen: Screen) {
        ui = ui.copy(screen = screen, error = null)
    }

    fun goToDialer() {
        ui = ui.copy(screen = Screen.DIALER, error = null)
    }

    fun dismissConfirmation() {
        ui = ui.copy(confirmation = null)
    }

    fun dismissError() {
        ui = ui.copy(error = null)
    }

    fun elapsedMs(): Long = recorder.elapsedMs()

    fun amplitude(): Int = recorder.amplitude()

    fun isRecording(): Boolean = recorder.isRecording

    private fun canPostNotifications(): Boolean =
        NotificationAvailability.canUseChannel(
            getApplication(),
            NotificationHelper.CHANNEL_INCOMING
        )

    override fun onCleared() {
        if (recorder.isRecording) {
            recorder.cancel()
        }
        super.onCleared()
    }
}
