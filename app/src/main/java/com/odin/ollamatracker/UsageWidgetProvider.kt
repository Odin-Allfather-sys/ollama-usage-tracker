package com.odin.ollamatracker

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/**
 * Erich's Ollama Usage Widget.
 * Min size: 2 grid cells wide x 1 tall. Resizable up to full screen.
 * Modes: session only, weekly only, both.
 * Refresh button for manual refresh.
 */
class UsageWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) updateWidget(context, appWidgetManager, id)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_MANUAL_REFRESH) {
            UsageRepository.refreshNow(context)
            return
        }
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE ||
            intent.action == ACTION_USAGE_UPDATED) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = if (intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)) {
                intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
            } else mgr.getAppWidgetIds(javaClass.let { android.content.ComponentName(context, it) })
            ids?.forEach { updateWidget(context, mgr, it) }
        }
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: android.os.Bundle) {
        // Resize: re-render to adjust layout density for new size (2x1 up to fullscreen)
        updateWidget(context, appWidgetManager, appWidgetId)
    }

    private fun updateWidget(context: Context, mgr: AppWidgetManager, appWidgetId: Int) {
        val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        val mode = prefs.getInt("mode_$appWidgetId", MODE_BOTH) // 0=session 1=weekly 2=both
        val views = RemoteViews(context.packageName, R.layout.usage_widget).apply {
            val sessionPct = UsageRepository.cachedSessionPct(context)
            val weeklyPct = UsageRepository.cachedWeeklyPct(context)
            when (mode) {
                MODE_SESSION -> {
                    setViewVisibility(R.id.session_row, android.view.View.VISIBLE)
                    setViewVisibility(R.id.weekly_row, android.view.View.GONE)
                    setTextViewText(R.id.session_pct, "$sessionPct%")
                    setProgressBar(R.id.session_bar, 100, sessionPct, false)
                }
                MODE_WEEKLY -> {
                    setViewVisibility(R.id.session_row, android.view.View.GONE)
                    setViewVisibility(R.id.weekly_row, android.view.View.VISIBLE)
                    setTextViewText(R.id.weekly_pct, "$weeklyPct%")
                    setProgressBar(R.id.weekly_bar, 100, weeklyPct, false)
                }
                else -> {
                    setViewVisibility(R.id.session_row, android.view.View.VISIBLE)
                    setViewVisibility(R.id.weekly_row, android.view.View.VISIBLE)
                    setTextViewText(R.id.session_pct, "$sessionPct%")
                    setTextViewText(R.id.weekly_pct, "$weeklyPct%")
                    setProgressBar(R.id.session_bar, 100, sessionPct, false)
                    setProgressBar(R.id.weekly_bar, 100, weeklyPct, false)
                }
            }
            val ts = UsageRepository.cachedTimestamp(context)
            setTextViewText(R.id.updated_at, "Updated $ts")
            // refresh button
            setOnClickPendingIntent(R.id.refresh_btn,
                android.app.PendingIntent.getBroadcast(context, 0,
                    Intent(context, UsageWidgetProvider::class.java).apply { action = ACTION_MANUAL_REFRESH },
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE))
            // tap to toggle mode
            setOnClickPendingIntent(R.id.mode_btn,
                android.app.PendingIntent.getBroadcast(context, 1,
                    Intent(context, UsageWidgetProvider::class.java).apply {
                        action = ACTION_CYCLE_MODE
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    },
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE))
        }
        mgr.updateAppWidget(appWidgetId, views)
    }

    companion object {
        const val ACTION_MANUAL_REFRESH = "com.odin.ollamatracker.MANUAL_REFRESH"
        const val ACTION_USAGE_UPDATED = "com.odin.ollamatracker.USAGE_UPDATED"
        const val ACTION_CYCLE_MODE = "com.odin.ollamatracker.CYCLE_MODE"
        const val MODE_SESSION = 0
        const val MODE_WEEKLY = 1
        const val MODE_BOTH = 2
    }
}