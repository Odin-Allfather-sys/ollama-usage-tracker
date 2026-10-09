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
 * Percents are stored as x10 ints (554 = 55.4%) so the UI shows one decimal
 * and matches the dashboard exactly.
 */
object UsageRepository {
    private var lastSessionPct10: Int = -1
    private var lastWeeklyPct10: Int = -1
    private var lastFetch: Long = 0
    private var lastError: String? = null

    /** 554 = 55.4% */
    fun cachedSessionPct10(ctx: Context): Int {
        if (lastSessionPct10 < 0) {
            val p = ctx.getSharedPreferences("usage_cache", Context.MODE_PRIVATE)
            lastSessionPct10 = p.getInt("session_pct_x10", p.getInt("session_pct", 0) * 10)
        }
        return lastSessionPct10
    }

    fun cachedWeeklyPct10(ctx: Context): Int {
        if (lastWeeklyPct10 < 0) {
            val p = ctx.getSharedPreferences("usage_cache", Context.MODE_PRIVATE)
            lastWeeklyPct10 = p.getInt("weekly_pct_x10", p.getInt("weekly_pct", 0) * 10)
        }
        return lastWeeklyPct10
    }

    /** 554 -> "55.4" */
    fun fmt(pct10: Int): String = "${pct10 / 10}.${pct10 % 10}"

    fun cachedTimestamp(ctx: Context): String {
        val p = ctx.getSharedPreferences("usage_cache", Context.MODE_PRIVATE)
        return SimpleDateFormat("h:mm a", Locale.US).format(Date(p.getLong("ts", 0)))
    }

    fun refreshNowAsync(ctx: Context) = CoroutineScope(Dispatchers.IO).launch { refreshNow(ctx) }

    fun refreshNow(ctx: Context) {
        val result = runCatching { OllamaUsageApi.fetchUsage(ctx) }
        result.fold(
            onSuccess = { usage ->
                val s10 = (usage.sessionPct * 10).toInt()
                val w10 = (usage.weeklyPct * 10).toInt()
                val p = ctx.getSharedPreferences("usage_cache", Context.MODE_PRIVATE)
                p.edit().putInt("session_pct_x10", s10)
                    .putInt("weekly_pct_x10", w10)
                    .putInt("session_pct", s10 / 10)   // legacy readers (widget/complication)
                    .putInt("weekly_pct", w10 / 10)
                    .putLong("ts", System.currentTimeMillis()).apply()
                lastSessionPct10 = s10
                lastWeeklyPct10 = w10
                lastFetch = System.currentTimeMillis()
                lastError = null
                ThresholdNotifier.onNewUsage(ctx, s10 / 10, w10 / 10)
                WidgetRefresher.broadcast(ctx)
            },
            onFailure = { e ->
                lastError = e.message
                android.util.Log.w("UsageRepo", "usage fetch failed: ${e.message}")
            }
        )
    }

    /** Synchronous variant for contexts that need the value now (widget refresh press). */
    fun refreshBlocking(ctx: Context) = runBlocking {
        refreshNow(ctx)
        lastSessionPct10 to lastWeeklyPct10
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
        refreshNow(ctx)  // recompute + cache with new limits
        return sessionLimit to weeklyLimit
    }
}