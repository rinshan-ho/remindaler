package com.mine.remindaler

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.mine.remindaler.data.Reminder
import com.mine.remindaler.data.ReminderState
import com.mine.remindaler.notify.NotificationHelper
import com.mine.remindaler.ui.CallState
import com.mine.remindaler.ui.IncomingScreen
import com.mine.remindaler.ui.RemindalerTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class IncomingCallActivity : ComponentActivity() {

    private var reminder: Reminder? = null
    private var ringPlayer: MediaPlayer? = null
    private var fallbackRingtone: Ringtone? = null
    private var messagePlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var closing = false
    private var isTest = false
    private var keyguardRequestInFlight = false

    private val state = mutableStateOf(CallState.RINGING)
    private val progress = mutableFloatStateOf(0f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        val id = intent.getLongExtra(EXTRA_ID, -1L)
        val reminder = if (id > 0) {
            RemindalerApp.instance.repository.get(id)
        } else {
            null
        }

        if (reminder == null) {
            finish()
            return
        }

        this.reminder = reminder
        isTest = intent.getBooleanExtra(EXTRA_TEST, false)
        NotificationHelper.cancelIncoming(this, reminder.id)
        requestAlarmFocus()

        setContent {
            RemindalerTheme {
                IncomingScreen(
                    state = state.value,
                    progress = progress.floatValue,
                    reminder = reminder,
                    onAnswer = ::answer,
                    onDecline = ::decline,
                    onStopPlayback = ::stopPlayback,
                    onClose = ::closeError
                )
            }
        }

        onBackPressedDispatcher.addCallback(this) {
            when (state.value) {
                CallState.RINGING -> decline()
                CallState.PLAYING -> stopPlayback()
                CallState.ERROR -> closeError()
            }
        }

        if (intent.getBooleanExtra(EXTRA_ANSWER, false)) {
            answer()
        } else {
            startRinging()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_ANSWER, false) && state.value == CallState.RINGING) {
            answer()
        }
    }

    private fun startRinging() {
        startVibration()

        lifecycleScope.launch {
            delay(RING_TIMEOUT_MS)
            if (!closing && state.value == CallState.RINGING) {
                decline()
            }
        }

        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: return

        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val player = MediaPlayer()
        ringPlayer = runCatching {
            player.apply {
                setAudioAttributes(attributes)
                setWakeMode(this@IncomingCallActivity, PowerManager.PARTIAL_WAKE_LOCK)
                setDataSource(this@IncomingCallActivity, uri)
                isLooping = true
                prepare()
                start()
            }
        }.getOrElse {
            runCatching { player.release() }
            null
        }

        if (ringPlayer == null) {
            fallbackRingtone = runCatching {
                RingtoneManager.getRingtone(this, uri)?.apply {
                    audioAttributes = attributes
                    isLooping = true
                    play()
                }
            }.getOrNull()
        }
    }

    private fun answer() {
        if (closing || state.value != CallState.RINGING) return
        // Never start the recorded message while the lock screen is showing.
        // Notification actions request authentication on API 31+, but this
        // guard also covers API 29-30 and the in-app answer button.
        val keyguardManager = getSystemService(KeyguardManager::class.java)
        if (keyguardManager?.isKeyguardLocked == true) {
            requestKeyguardDismissThenPlay()
            return
        }
        startMessagePlayback()
    }

    private fun requestKeyguardDismissThenPlay() {
        if (keyguardRequestInFlight || closing || state.value != CallState.RINGING) return
        val keyguardManager = getSystemService(KeyguardManager::class.java)
        if (keyguardManager == null) {
            decline()
            return
        }

        keyguardRequestInFlight = true
        val callback = object : KeyguardManager.KeyguardDismissCallback() {
            override fun onDismissSucceeded() {
                keyguardRequestInFlight = false
                startMessagePlayback()
            }

            override fun onDismissCancelled() {
                keyguardRequestInFlight = false
                decline()
            }

            override fun onDismissError() {
                keyguardRequestInFlight = false
                decline()
            }
        }

        runCatching {
            keyguardManager.requestDismissKeyguard(this, callback)
        }.onFailure {
            keyguardRequestInFlight = false
            decline()
        }
    }

    private fun startMessagePlayback() {
        if (closing || state.value != CallState.RINGING) return
        stopRinging()
        val current = reminder ?: return
        val file = File(RemindalerApp.instance.repository.recordingsDir, current.audioFile)
        if (!file.exists()) {
            state.value = CallState.ERROR
            return
        }

        state.value = CallState.PLAYING
        val player = MediaPlayer()
        messagePlayer = runCatching {
            player.apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setWakeMode(this@IncomingCallActivity, PowerManager.PARTIAL_WAKE_LOCK)
                setDataSource(file.absolutePath)
                setOnCompletionListener {
                    this@IncomingCallActivity.window.decorView.post { finishAsPlayed() }
                }
                prepare()
                start()
            }
        }.getOrElse {
            runCatching { player.release() }
            null
        }

        if (messagePlayer == null) {
            state.value = CallState.ERROR
            return
        }

        lifecycleScope.launch {
            while (!closing && state.value == CallState.PLAYING) {
                val position = runCatching {
                    val player = messagePlayer ?: return@runCatching 0f
                    if (player.duration > 0) {
                        player.currentPosition.toFloat() / player.duration
                    } else {
                        0f
                    }
                }.getOrDefault(0f)
                progress.floatValue = position.coerceIn(0f, 1f)
                delay(100L)
            }
        }
    }

    private fun decline() {
        if (closing) return
        closing = true
        stopAllAudio()
        reminder?.let { current ->
            if (!isTest) {
                RemindalerApp.instance.repository.updateState(current.id, ReminderState.MISSED)
                NotificationHelper.cancelIncoming(this, current.id)
                NotificationHelper.showMissed(this, current.copy(state = ReminderState.MISSED))
            } else {
                NotificationHelper.cancelIncoming(this, current.id)
            }
        }
        finish()
    }

    private fun stopPlayback() {
        if (closing) return
        closing = true
        stopAllAudio()
        reminder?.let { current ->
            if (isTest) {
                NotificationHelper.cancelIncoming(this, current.id)
            } else {
                RemindalerApp.instance.repository.updateState(current.id, ReminderState.PLAYED)
                NotificationHelper.cancelAll(this, current.id)
            }
        }
        finish()
    }

    private fun finishAsPlayed() {
        if (closing) return
        closing = true
        stopAllAudio()
        reminder?.let { current ->
            if (isTest) {
                NotificationHelper.cancelIncoming(this, current.id)
            } else {
                RemindalerApp.instance.repository.updateState(current.id, ReminderState.PLAYED)
                NotificationHelper.cancelAll(this, current.id)
            }
        }
        finish()
    }

    private fun closeError() {
        if (closing) return
        closing = true
        stopAllAudio()
        reminder?.let { current ->
            if (!isTest) {
                RemindalerApp.instance.repository.updateState(current.id, ReminderState.MISSED)
                NotificationHelper.cancelIncoming(this, current.id)
                NotificationHelper.showMissed(this, current.copy(state = ReminderState.MISSED))
            } else {
                NotificationHelper.cancelIncoming(this, current.id)
            }
        }
        finish()
    }

    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Vibrator::class.java)
        }
        if (vibrator?.hasVibrator() == true) {
            vibrator?.vibrate(
                VibrationEffect.createWaveform(longArrayOf(0, 700, 400, 700, 900), 0)
            )
        }
    }

    private fun requestAlarmFocus() {
        val audioManager = getSystemService(AudioManager::class.java) ?: return
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setOnAudioFocusChangeListener { }
            .build()
        runCatching { audioManager.requestAudioFocus(request) }
        audioFocusRequest = request
    }

    private fun stopRinging() {
        runCatching { vibrator?.cancel() }
        vibrator = null

        fallbackRingtone?.let { ringtone ->
            runCatching { if (ringtone.isPlaying) ringtone.stop() }
        }
        fallbackRingtone = null

        val player = ringPlayer ?: return
        ringPlayer = null
        runCatching { player.stop() }
        runCatching { player.release() }
    }

    private fun stopAllAudio() {
        stopRinging()
        val player = messagePlayer
        messagePlayer = null
        if (player != null) {
            runCatching { player.stop() }
            runCatching { player.release() }
        }
        val focus = audioFocusRequest
        audioFocusRequest = null
        if (focus != null) {
            runCatching {
                getSystemService(AudioManager::class.java)?.abandonAudioFocusRequest(focus)
            }
        }
    }

    override fun onDestroy() {
        stopAllAudio()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_ID = "reminder_id"
        const val EXTRA_ANSWER = "answer_immediately"
        const val EXTRA_TEST = "test_mode"
        private const val RING_TIMEOUT_MS = 60_000L

        fun intent(
            context: Context,
            reminderId: Long,
            answer: Boolean = false,
            test: Boolean = false
        ): Intent =
            Intent(context, IncomingCallActivity::class.java).apply {
                putExtra(EXTRA_ID, reminderId)
                putExtra(EXTRA_ANSWER, answer)
                putExtra(EXTRA_TEST, test)
            }
    }
}
