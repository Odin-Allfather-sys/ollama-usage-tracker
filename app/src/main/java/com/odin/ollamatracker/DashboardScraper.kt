package com.odin.ollamatracker

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Fetches session + weekly percentages from the Asgard tracker endpoint
 * (token tally service running on the always-on host, tailnet-only).
 * No cookies, no scraping, no Ollama breakage risk.
 */
object DashboardScraper {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    data class UsageResult(val sessionPct: Int, val weeklyPct: Int)

    fun fetchUsage(ctx: Context): UsageResult {
        val prefs = ctx.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val url = prefs.getString("tracker_url", "http://100.94.103.108:8443/usage")
            ?: throw IllegalStateException("No tracker URL configured.")
        val req = Request.Builder().url(url).build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IllegalStateException("Tracker responded ${resp.code}")
            val body = resp.body?.string() ?: throw IllegalStateException("Empty tracker response")
            val s = Regex("\"session_pct\"\\s*:\\s*(\\d+)").find(body)?.groupValues?.get(1)?.toInt()
                ?: throw IllegalStateException("Tracker response missing session_pct")
            val w = Regex("\"weekly_pct\"\\s*:\\s*(\\d+)").find(body)?.groupValues?.get(1)?.toInt()
                ?: throw IllegalStateException("Tracker response missing weekly_pct")
            return UsageResult(s, w)
        }
    }
}