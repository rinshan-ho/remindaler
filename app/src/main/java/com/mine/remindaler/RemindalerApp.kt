package com.mine.remindaler

import android.app.Application
import com.mine.remindaler.data.ReminderRepository
import com.mine.remindaler.notify.NotificationHelper

class RemindalerApp : Application() {

    lateinit var repository: ReminderRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        repository = ReminderRepository(this)
        NotificationHelper.createChannels(this)
    }

    companion object {
        lateinit var instance: RemindalerApp
            private set
    }
}
