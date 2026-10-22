package com.mine.remindaler.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class ReminderRepository(context: Context) {

    private val appContext = context.applicationContext
    private val storeFile = File(appContext.filesDir, "reminders.json")
    private val lock = Any()

    val recordingsDir: File = File(appContext.filesDir, "recordings").apply { mkdirs() }

    private val _reminders = MutableStateFlow<List<Reminder>>(emptyList())
    val reminders: StateFlow<List<Reminder>> = _reminders.asStateFlow()

    init {
        _reminders.value = loadFromDisk().sortedBy { it.triggerAt }
    }

    fun get(id: Long): Reminder? = _reminders.value.firstOrNull { it.id == id }

    fun add(reminder: Reminder) {
        synchronized(lock) {
            _reminders.value = (_reminders.value + reminder).sortedBy { it.triggerAt }
            persist(_reminders.value)
        }
    }

    fun updateState(id: Long, state: ReminderState) {
        synchronized(lock) {
            val updated = _reminders.value.map { if (it.id == id) it.copy(state = state) else it }
            _reminders.value = updated
            persist(updated)
        }
    }

    fun remove(id: Long) {
        synchronized(lock) {
            val target = _reminders.value.firstOrNull { it.id == id }
            if (target != null) {
                File(recordingsDir, target.audioFile).delete()
            }
            val updated = _reminders.value.filterNot { it.id == id }
            _reminders.value = updated
            persist(updated)
        }
    }

    fun audioFile(reminder: Reminder): File = File(recordingsDir, reminder.audioFile)

    private fun loadFromDisk(): List<Reminder> {
        if (!storeFile.exists()) return emptyList()
        return runCatching {
            val array = JSONArray(storeFile.readText())
            (0 until array.length()).map { index ->
                val obj = array.getJSONObject(index)
                Reminder(
                    id = obj.getLong("id"),
                    triggerAt = obj.getLong("triggerAt"),
                    audioFile = obj.getString("audioFile"),
                    durationMs = obj.optLong("durationMs", 0L),
                    state = runCatching {
                        ReminderState.valueOf(obj.optString("state", ReminderState.SCHEDULED.name))
                    }.getOrDefault(ReminderState.SCHEDULED),
                    createdAt = obj.optLong("createdAt", 0L)
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun persist(list: List<Reminder>) {
        val array = JSONArray()
        list.forEach { reminder ->
            array.put(
                JSONObject().apply {
                    put("id", reminder.id)
                    put("triggerAt", reminder.triggerAt)
                    put("audioFile", reminder.audioFile)
                    put("durationMs", reminder.durationMs)
                    put("state", reminder.state.name)
                    put("createdAt", reminder.createdAt)
                }
            )
        }
        runCatching {
            val temp = File(storeFile.parentFile, "${storeFile.name}.tmp")
            temp.writeText(array.toString())
            if (!temp.renameTo(storeFile)) {
                storeFile.writeText(array.toString())
                temp.delete()
            }
        }
    }
}
