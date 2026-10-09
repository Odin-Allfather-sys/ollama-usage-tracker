package com.odin.ollamatracker

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class TrackerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Kick start first poll and schedule recurring ones
        UsageRepository.refreshNowAsync(this)
        UsagePollScheduler.reschedule(this)
    }
}