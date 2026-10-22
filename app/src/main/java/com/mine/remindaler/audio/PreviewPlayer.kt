package com.mine.remindaler.audio

import android.media.AudioAttributes
import android.media.MediaPlayer
import java.io.File

/**
 * Small helper used to preview recorded messages from the reminder list.
 */
class PreviewPlayer {

    private var player: MediaPlayer? = null

    fun play(file: File, onFinished: () -> Unit): Boolean {
        stop()
        if (!file.exists()) return false

        return try {
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(file.absolutePath)
                setOnCompletionListener {
                    stop()
                    onFinished()
                }
                prepare()
                start()
            }
            true
        } catch (e: Exception) {
            stop()
            false
        }
    }

    fun stop() {
        val current = player ?: return
        player = null
        runCatching { if (current.isPlaying) current.stop() }
        runCatching { current.release() }
    }
}
