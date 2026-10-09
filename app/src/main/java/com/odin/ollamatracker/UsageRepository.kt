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

    /**
     * Calibration: user reads session % and weekly % off the real dashboard,
     * we solve the limits so app % matches. limit = requests * 100 / pct.
     * Returns null if a pct is 0 (can't solve from nothing).
     */
    fun calibrate(ctx: Context, dashSessionPct: Double, dashWeeklyPct: Double): Pair<Int, Int>? {
        if (dashSessionPct <= 0 || dashWeeklyPct <= 0) return null
        val result = runCatching { OllamaUsageApi.fetchUsage(ctx) }.getOrNull() ?: return null
        if (result.sessionReq <= 0 || result.weeklyReq <= 0) return null
        val sessionLimit = Math.round(result.sessionReq * 100.0 / dashSessionPct).toInt().coerceAtLeast(1)
        val weeklyLimit = Math.round(result.weeklyReq * 100.0 / dashWeeklyPct).toInt().coerceAtLeast(1)
        ctx.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).edit()
            .putInt("session_limit_req", sessionLimit)
            .putInt("weekly_limit_req", weeklyLimit)
            .apply()
        // recompute cached percents with new limits right away
        val sp = (result.sessionReq * 100 / sessionLimit).coerceIn(0, 100)
        val wp = (result.weeklyReq * 100 / weeklyLimit).coerceIn(0, 100)
        ctx.getSharedPreferences("usage_cache", Context.MODE_PRIVATE).edit()
            .putInt("session_pct", sp).putInt("weekly_pct", wp).apply()
        lastSessionPct = sp; lastWeeklyPct = wp
        return sessionLimit to weeklyLimit
    }
}