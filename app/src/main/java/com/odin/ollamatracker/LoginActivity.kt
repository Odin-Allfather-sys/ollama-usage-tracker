package com.odin.ollamatracker

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Login screen: user signs in inside the WebView, then presses DONE.
 * No auto-detection (SPAs + OAuth redirects broke it). DONE captures the
 * cookie from the jar and verifies it live against /api/usage.
 */
class LoginActivity : AppCompatActivity() {
    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        webView = findViewById(R.id.login_webview)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        CookieManager.getInstance().setAcceptCookie(true)

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: android.webkit.WebResourceRequest): Boolean {
                // block external app launches; everything renders in this WebView
                return false
            }
        }
        webView.loadUrl("https://ollama.com/signin")

        findViewById<Button>(R.id.skip_login).setOnClickListener { finish() }
        findViewById<Button>(R.id.done_login).setOnClickListener {
            val cookie = CookieManager.getInstance().getCookie("https://ollama.com")
            if (cookie.isNullOrEmpty()) {
                Toast.makeText(this, "No cookie found. Sign in first.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Toast.makeText(this, "Verifying...", Toast.LENGTH_SHORT).show()
            Thread {
                val ok = verifyCookie(cookie)
                runOnUiThread {
                    if (ok) {
                        getSharedPreferences("app_prefs", MODE_PRIVATE)
                            .edit().putString("ollama_session_cookie", cookie)
                            .putBoolean("needs_relogin", false).apply()
                        Toast.makeText(this, "Signed in.", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this, "Login not complete yet. Finish signing in, then press DONE again.", Toast.LENGTH_LONG).show()
                    }
                }
            }.start()
        }
    }

    private fun verifyCookie(cookie: String): Boolean = runCatching {
        val req = okhttp3.Request.Builder()
            .url("https://ollama.com/api/usage")
            .header("Cookie", cookie)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36")
            .build()
        okhttp3.OkHttpClient().newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return@use false
            val b = resp.body?.string() ?: return@use false
            !b.contains("invalid credentials") && b.trimStart().startsWith("{")
        }
    }.getOrDefault(false)

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }
}