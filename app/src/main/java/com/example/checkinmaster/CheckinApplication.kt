package com.alosir.task

import android.app.Application
import com.alosir.task.util.CrashLogger
import com.alosir.task.util.CheckinResetScheduler

class CheckinApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashLogger.init(this)
        CheckinResetScheduler.schedule(this)
    }
}
