package com.odin.ollamatracker

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Login screen: embedded WebView at ollama.com/signin. User signs in with
 * their own credentials in-app. External-browser links are intercepted and
 * forced back into the WebView (or blocked) so session cookies stay in-app.
 * On landing at a signed-in page we persist the cookie string.
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
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url
                val host = url.host ?: return false
                // Keep everything ollama.com (and its auth subdomains) in-app.
                if (host.endsWith("ollama.com")) return false
                // Google/GitHub OAuth popups: block external app launch, open in the WebView instead.
                return try {
                    // Load external auth providers inline in the same WebView.
                    false
                } catch (e: ActivityNotFoundException) {
                    true
                }
            }

            override fun onPageFinished(view: WebView, url: String) {
                val u = Uri.parse(url)
                if (!(u.host ?: "").endsWith("ollama.com") || url.contains("/signin") || url.contains("/signup") || url.contains("github.com") || url.contains("oauth")) return
                val cm = CookieManager.getInstance()
                val cookie = cm.getCookie("https://ollama.com") ?: return
                if (cookie.isNullOrEmpty()) return
                // VERIFY the cookie actually authenticates before declaring signed in.
                Thread {
                    val ok = runCatching {
                        val req = okhttp3.Request.Builder()
                            .url("https://ollama.com/api/usage")
                            .header("Cookie", cookie)
                            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36")
                            .build()
                        okhttp3.OkHttpClient().newCall(req).execute().use { resp ->
                            resp.isSuccessful && !(resp.body?.string() ?: "").contains("invalid credentials")
                        }
                    }.getOrDefault(false)
                    if (ok) {
                        getSharedPreferences("app_prefs", MODE_PRIVATE)
                            .edit().putString("ollama_session_cookie", cookie).apply()
                        UsageRepository.refreshNow(applicationContext)
                        runOnUiThread {
                            Toast.makeText(applicationContext, "Signed in. Saving usage data.", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(applicationContext, "Not signed in yet, keep going...", Toast.LENGTH_SHORT).show()
                        }
                    }
                }.start()
            }

            private fun hostIsOllama(u: Uri): Boolean = (u.host ?: "").endsWith("ollama.com")
        }
        webView.loadUrl("https://ollama.com/signin")

        findViewById<Button>(R.id.skip_login).setOnClickListener {
            finish()
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }
}