package com.waxx.englishcard

import android.app.Application

class WaxxApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashLogger.install(this)
    }
}
