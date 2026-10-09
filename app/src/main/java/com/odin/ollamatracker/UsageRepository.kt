package com.odin.ollamatracker

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Central data holder. Real usage from OllamaUsageApi (API key auth),
 * caches it, detects threshold crossings, fires notifications, updates widgets.
 */
object UsageRepository {
    private var lastSessionPct: Int = -1
    private var lastWeeklyPct: Int = -1
    private var lastFetch: Long = 0
    private var lastError: String? = null

    fun cachedSessionPct(ctx: Context): Int {
        if (lastSessionPct < 0) {
            val p = ctx.getSharedPreferences("usage_cache", Context.MODE_PRIVATE)
            lastSessionPct = p.getInt("session_pct", 0)
        }
        return lastSessionPct
    }

    fun cachedWeeklyPct(ctx: Context): Int {
        if (lastWeeklyPct < 0) {
            val p = ctx.getSharedPreferences("usage_cache", Context.MODE_PRIVATE)
            lastWeeklyPct = p.getInt("weekly_pct", 0)
        }
        return lastWeeklyPct
    }

    fun cachedTimestamp(ctx: Context): String {
        val p = ctx.getSharedPreferences("usage_cache", Context.MODE_PRIVATE)
        return SimpleDateFormat("h:mm a", Locale.US).format(Date(p.getLong("ts", 0)))
    }

    fun refreshNowAsync(ctx: Context) = CoroutineScope(Dispatchers.IO).launch { refreshNow(ctx) }

    fun refreshNow(ctx: Context) {
        val result = runCatching { OllamaUsageApi.fetchUsage(ctx) }
        result.fold(
            onSuccess = { usage ->
                val p = ctx.getSharedPreferences("usage_cache", Context.MODE_PRIVATE)
                p.edit().putInt("session_pct", usage.sessionPct)
                    .putInt("weekly_pct", usage.weeklyPct)
                    .putLong("ts", System.currentTimeMillis()).apply()
                lastSessionPct = usage.sessionPct
                lastWeeklyPct = usage.weeklyPct
                lastFetch = System.currentTimeMillis()
                lastError = null
                ThresholdNotifier.onNewUsage(ctx, usage.sessionPct, usage.weeklyPct)
                WidgetRefresher.broadcast(ctx)
            },
            onFailure = { e ->
                lastError = e.message
                android.util.Log.w("UsageRepo", "scrape failed: ${e.message}")
            }
        )
    }

    /** Synchronous variant for contexts that need the value now (widget refresh press). */
    fun refreshBlocking(ctx: Context) = runBlocking {
        refreshNow(ctx)
        lastSessionPct to lastWeeklyPct
    }
}