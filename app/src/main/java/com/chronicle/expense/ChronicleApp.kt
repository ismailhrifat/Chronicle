package com.chronicle.expense

import android.app.Application
import com.chronicle.expense.notify.Notifier
import com.chronicle.expense.notify.ReminderWorker

class ChronicleApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifier.createChannels(this)
        ReminderWorker.schedule(this)
    }
}
