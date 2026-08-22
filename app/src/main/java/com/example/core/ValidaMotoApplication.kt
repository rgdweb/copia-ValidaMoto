package com.example.core
import android.app.Application
import com.example.core.notifications.SyncAlarmsOnStartup
class ValidaMotoApplication : Application() {
    override fun onCreate() { super.onCreate(); SyncAlarmsOnStartup(this).execute() }
}
