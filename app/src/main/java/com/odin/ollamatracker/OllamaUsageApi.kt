package com.odin.ollamatracker

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Fetches real session + weekly usage from Ollama's own /api/usage endpoint,
 * authenticating with the session cookie captured at in-app login.
 * Tolerant parser: accepts multiple known/likely response shapes.
 */
object DashboardScraper {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    data class UsageResult(val sessionPct: Int, val weeklyPct: Int)

    /**
     * Fetches real session + weekly usage from Ollama's own /api/usage endpoint,
     * authenticating with the session cookie captured at in-app login.
     * Tolerant parser: accepts multiple known/likely response shapes.
     * On auth expiry, triggers re-login notification.
     */
    fun fetchUsage(ctx: Context): UsageResult {
        val prefs = ctx.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val cookie = prefs.getString("ollama_session_cookie", null)
            ?: throw IllegalStateException("Not signed in. Open the app and sign in to ollama.com.")
        if (prefs.getBoolean("logged_out", false)) {
            // Session cookie invalidated server-side; user needs to re-login.
            prefs.edit().putBoolean("logged_out", false).apply()
        }

        val req = Request.Builder()
            .url("https://ollama.com/api/usage")
            .header("Cookie", cookie)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36")
            .build()

        client.newCall(req).execute().use { resp ->
            when (resp.code) {
                401, 403 -> {
                    prefs.edit().putBoolean("needs_relogin", true).apply()
                    ThresholdNotifier.notifyReauthRequired(ctx)
                    throw AuthExpired()
                }
            }
            val body = resp.body?.string() ?: throw IllegalStateException("Empty response")
            return parse(body) ?: throw IllegalStateException("Unrecognized usage response shape; report this to the app developer.")
        }
    }

    class AuthExpired : Exception()

    /** Tolerant parser across shapes. Percent fields anywhere in the JSON. */
    fun parse(body: String): UsageResult? {
        fun find(vararg keys: Pair<String, Int>): Int? {
            for ((k, idx) in keys) {
                val re = Regex("\"$k\"\\s*:\\s*\"?(\\d+(?:\\.\\d+)?)\"?")
                re.find(body)?.let { return it.groupValues[1].toDouble().toInt() }
            }
            return null
        }
        val session = find("sessionPercent" to 0, "session_percent" to 0, "session_pct" to 0, "sessionUsagePercent" to 0)
            ?: Regex("\"session\"\\s*:\\s*\\{[^}]*?(?:percent|pct)\"\\s*:\\s*(\\d+(?:\\.\\d+)?)", RegexOption.IGNORE_CASE).find(body)?.groupValues?.get(1)?.toDouble()?.toInt()
        val weekly = find("weeklyPercent" to 0, "weekly_percent" to 0, "weekly_pct" to 0, "monthlyPercent" to 0, "monthly_percent" to 0, "monthlyUsagePercent" to 0)
            ?: Regex("\"(weekly|monthly|month)\"\\s*:\\s*\\{[^}]*?(?:percent|pct)\"\\s*:\\s*(\\d+(?:\\.\\d+)?)", RegexOption.IGNORE_CASE).find(body)?.groupValues?.get(2)?.toDouble()?.toInt()
        return if (session != null && weekly != null) UsageResult(session, weekly) else null
    }
}

/** Fired when the session cookie expires server-side. */
class AuthExpiredException : Exception()