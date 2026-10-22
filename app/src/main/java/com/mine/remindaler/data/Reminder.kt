package com.mine.remindaler.data

enum class ReminderState {
    SCHEDULED,
    PLAYED,
    MISSED
}

data class Reminder(
    val id: Long,
    val triggerAt: Long,
    val audioFile: String,
    val durationMs: Long,
    val state: ReminderState,
    val createdAt: Long
)
