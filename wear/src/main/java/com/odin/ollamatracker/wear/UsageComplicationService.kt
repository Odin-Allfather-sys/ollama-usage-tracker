package com.odin.ollamatracker.wear

import android.support.wearable.complications.ComplicationProviderService
import android.support.wearable.complications.ComplicationData
import android.support.wearable.complications.ComplicationText

/**
 * Watch complication: bar for session or weekly Ollama usage.
 * Galaxy Watch 7 (Wear OS 5): user picks this complication on their watch face,
 * TYPE_RANGED_VALUE renders as an arc/gauge bar.
 */
class UsageComplicationService : ComplicationProviderService() {

    override fun onComplicationUpdate(complicationId: Int, type: Int, callback: ComplicationData.ComplicationListener) {
        val prefs = getSharedPreferences("usage_cache", MODE_PRIVATE)
        val showWeekly = prefs.getBoolean("show_weekly", false)
        val pct = if (showWeekly) prefs.getInt("weekly_pct", 0) else prefs.getInt("session_pct", 0)
        val label = if (showWeekly) "Weekly" else "Session"
        callback.onComplicationData(
            ComplicationData.Builder(ComplicationData.TYPE_RANGED_VALUE)
                .setValue(pct.toFloat(), 0f, 100f)
                .setTitle(ComplicationText.plainText("$pct%"))
                .setContentDescription(ComplicationText.plainText("Ollama $label $pct%"))
                .build()
        )
    }
}