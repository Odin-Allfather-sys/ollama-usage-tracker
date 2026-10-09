package com.odin.ollamatracker

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Real Ollama usage via the official API key auth.
 * GET api.ollama.com/api/usage?range=7d|24h with "Authorization: Bearer <key>".
 * Returns hourly/day buckets of request_count. Percent = requests / plan limit,
 * both limits configurable (defaults calibrated against the Pro legacy plan
 * from a real dashboard: weekly ~= 8,900 req, session(5h) ~= 1,950 req).
 * Session window = rolling 5 hours computed from hourly buckets.
 */
object OllamaUsageApi {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    const val DEFAULT_WEEKLY_LIMIT_REQ = 8900
    const val DEFAULT_SESSION_LIMIT_REQ = 1950

    data class UsageResult(
        val sessionPct: Int,
        val weeklyPct: Int,
        val sessionReq: Int,
        val weeklyReq: Int,
        val weeklyLimit: Int,
        val sessionLimit: Int,
    )

    class AuthExpired : Exception()

    fun fetchUsage(ctx: Context): UsageResult {
        val prefs = ctx.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val key = prefs.getString("api_key", null)?.trim()
            ?: throw IllegalStateException("No API key set. Paste your ollama.com API key in settings.")
        val weeklyLimit = prefs.getInt("weekly_limit_req", DEFAULT_WEEKLY_LIMIT_REQ)
        val sessionLimit = prefs.getInt("session_limit_req", DEFAULT_SESSION_LIMIT_REQ)

        val weeklyBody = get("https://api.ollama.com/api/usage?range=7d", key)
        val dayBody = get("https://api.ollama.com/api/usage?range=24h", key)
        if (weeklyBody.contains("invalid credentials") || dayBody.contains("invalid credentials")) {
            prefs.edit().putBoolean("needs_key", true).apply()
            ThresholdNotifier.notifyReauthRequired(ctx)
            throw AuthExpired()
        }
        val weeklyReq = intField(weeklyBody, "request_count", inTotals = true)
            ?: throw IllegalStateException("No request_count in usage response")

        // session = last 5 hours: sum hourly buckets whose from-time is within now-5h
        val sessionReq = computeLastHours(dayBody, 5)
            ?: (intField(dayBody, "request_count", inTotals = true) ?: 0)

        val sp = (sessionReq * 1000 / sessionLimit)   // x10, keeps decimal
        val wp = (weeklyReq * 1000 / weeklyLimit)
        return UsageResult(sp.coerceIn(0, 1000), wp.coerceIn(0, 1000),
            sessionReq, weeklyReq, weeklyLimit, sessionLimit)
    }

    private fun get(url: String, key: String): String {
        val req = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $key")
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14)")
            .build()
        client.newCall(req).execute().use { resp ->
            if (resp.code == 401 || resp.code == 403) throw AuthExpired()
            if (!resp.isSuccessful) throw IllegalStateException("Usage API responded ${resp.code}")
            return resp.body?.string() ?: throw IllegalStateException("Empty response")
        }
    }

    /** Pulls totals.request_count. */
    private fun intField(body: String, field: String, inTotals: Boolean): Int? {
        val re = if (inTotals)
            Regex("\"totals\"\\s*:\\s*\\{[^}]*?\"$field\"\\s*:\\s*(\\d+)")
        else
            Regex("\"$field\"\\s*:\\s*(\\d+)")
        return re.find(body)?.groupValues?.get(1)?.toInt()
    }

    /**
     * Sums hourly buckets from a range=24h response for the newest N hours,
     * using bucket "from" timestamps versus the response "until" time.
     * Handles the actual Ollama bucket order: from, until, request_count.
     */
    private fun computeLastHours(body: String, hours: Int): Int? {
        return runCatching {
            val untilM = Regex("\"until\"\\s*:\\s*\"([^\"]+)\"").findAll(body).lastOrNull()?.groupValues?.get(1) ?: return null
            val until = parseIso(untilM) ?: return null
            // buckets: {"from":"...","until":"...","request_count":N} in order
            val re = Regex("\"from\"\\s*:\\s*\"([^\"]+)\"[^{}]*?\"request_count\"\\s*:\\s*(\\d+)")
            var sum = 0
            var any = false
            for (m in re.findAll(body)) {
                val from = parseIso(m.groupValues[1]) ?: continue
                val count = m.groupValues[2].toInt()
                if (until - from <= hours * 3600.0 + 1800.0) {
                    sum += count; any = true
                }
            }
            if (any) sum else null
        }.getOrNull()
    }

    private fun parseIso(s: String): Double? = runCatching {
        java.time.Instant.parse(s.trim()).toEpochMilli() / 1000.0
    }.getOrNull()
}