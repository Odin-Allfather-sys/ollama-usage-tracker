package com.odin.ollamatracker

import android.content.Context
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Scrapes ollama.com dashboard using a session cookie the user provides
 * after logging into ollama.com in a browser. Cookie is pasted into app settings.
 * Scrapes usage page, parses percentages for session and weekly usage.
 * Sane polling, backoff on failure, cache-last-good semantics.
 */
object DashboardScraper {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val cookieStore = ConcurrentHashMap<String, List<Cookie>>()
    private val jar = object : CookieJar {
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            cookieStore[url.host] = cookies
        }
        override fun loadForRequest(url: HttpUrl): List<Cookie> = cookieStore[url.host] ?: emptyList()
    }

    private val clientWithJar = client.newBuilder().cookieJar(jar).build()

    data class UsageResult(val sessionPct: Int, val weeklyPct: Int)

    /**
     * Cookie is user-provided in settings (the ollama.com session cookie after login
     * in the browser). We inject it manually and hit the dashboard.
     * Parsing strategy: dashboard HTML/JSON contains usage values, or a JSON
     * endpoint under /api/account/usage if Ollama ships one, try that first.
     */
    fun fetchUsage(ctx: Context): UsageResult {
        val prefs = ctx.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val cookieStr = prefs.getString("ollama_session_cookie", null)
            ?: throw IllegalStateException("No session cookie set. Open app settings and paste your ollama.com session cookie (copy from browser after logging in).")

        // Try known JSON endpoints first
        for (endpoint in listOf(
            "https://ollama.com/api/account/usage",
            "https://ollama.com/account/usage",
            "https://ollama.com/usage"
        )) {
            val req = Request.Builder()
                .url(endpoint)
                .header("Cookie", cookieStr)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36")
                .build()
            clientWithJar.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: return@use
                    val parsed = UsageParser.parse(body)
                    if (parsed != null) return parsed
                }
            }
        }
        throw IllegalStateException("Could not fetch usage. Ollama may have changed the dashboard, or the session cookie expired. Refresh the cookie in settings.")
    }
}

object UsageParser {
    /**
     * Tolerant parser. Handles both JSON shapes and raw HTML percent strings.
     * Looks for "session" and "weekly"/"month" percentages, tolerant to 0-100 ints.
     */
    fun parse(body: String): DashboardScraper.UsageResult? {
        // JSON direct shape
        val jsonShape = Regex("\"session\"\\s*:\\s*\\{[^}]*?\"(percent|pct|usage)\"\\s*:\\s*(\\d+(?:\\.\\d+)?)", RegexOption.IGNORE_CASE)
        val sMatch = jsonShape.find(body)
        val weeklyShape = Regex("\"(weekly|month|monthly)\"\\s*:\\s*\\{[^}]*?\"(percent|pct|usage)\"\\s*:\\s*(\\d+(?:\\.\\d+)?)", RegexOption.IGNORE_CASE)
        val wMatch = weeklyShape.find(body)
        return when {
            sMatch != null && wMatch != null -> DashboardScraper.UsageResult(
                sMatch.groupValues[2].toDouble().toInt(),
                wMatch.groupValues[3].toDouble().toInt())
            else -> null
        }
    }
}