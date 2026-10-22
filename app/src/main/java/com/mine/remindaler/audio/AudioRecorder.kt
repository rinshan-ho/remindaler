package com.mine.remindaler.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import java.io.File

data class RecordingResult(val file: File, val durationMs: Long)

private data class RecordingProfile(val samplingRate: Int, val bitRate: Int)

class AudioRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startedAt = 0L

    val isRecording: Boolean
        get() = recorder != null

    fun start(): File? {
        if (recorder != null) return outputFile

        val dir = File(context.filesDir, "recordings").apply { mkdirs() }
        val file = File(dir, "pending_${System.currentTimeMillis()}.m4a")

        for (profile in PROFILES) {
            val mediaRecorder = runCatching { createRecorder() }.getOrNull() ?: continue
            try {
                mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
                mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                mediaRecorder.setAudioChannels(1)
                if (profile.samplingRate > 0) {
                    mediaRecorder.setAudioSamplingRate(profile.samplingRate)
                }
                if (profile.bitRate > 0) {
                    mediaRecorder.setAudioEncodingBitRate(profile.bitRate)
                }
                mediaRecorder.setOutputFile(file.absolutePath)
                mediaRecorder.prepare()
                mediaRecorder.start()

                recorder = mediaRecorder
                outputFile = file
                startedAt = SystemClock.elapsedRealtime()
                return file
            } catch (e: Exception) {
                runCatching { mediaRecorder.release() }
                file.delete()
            }
        }
        return null
    }

    private fun createRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

    fun elapsedMs(): Long =
        if (recorder == null) 0L else SystemClock.elapsedRealtime() - startedAt

    fun amplitude(): Int = runCatching { recorder?.maxAmplitude ?: 0 }.getOrDefault(0)

    fun stop(): RecordingResult? {
        val mediaRecorder = recorder ?: return null
        val file = outputFile
        val duration = elapsedMs()
        recorder = null
        outputFile = null

        return try {
            mediaRecorder.stop()
            mediaRecorder.release()
            if (file != null) RecordingResult(file, duration) else null
        } catch (e: RuntimeException) {
            runCatching { mediaRecorder.release() }
            file?.delete()
            null
        }
    }

    fun cancel() {
        val mediaRecorder = recorder ?: return
        recorder = null
        runCatching { mediaRecorder.stop() }
        runCatching { mediaRecorder.release() }
        outputFile?.delete()
        outputFile = null
    }

    private companion object {
        val PROFILES = listOf(
            RecordingProfile(44_100, 96_000),
            RecordingProfile(48_000, 96_000),
            RecordingProfile(16_000, 32_000),
            RecordingProfile(0, 0)
        )
    }
}
