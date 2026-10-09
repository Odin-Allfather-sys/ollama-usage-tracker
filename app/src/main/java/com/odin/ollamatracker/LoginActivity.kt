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
 * Login screen: WebView at ollama.com/signin. User signs in with their own
 * credentials inside the WebView. When navigation lands on a signed-in page,
 * we capture the session cookie and store it. Ollama's system handles auth;
 * we only persist the resulting cookie for /api/usage polling.
 */
class LoginActivity : AppCompatActivity() {
    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webView = WebView(this)
        setContentView(webView)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                // Signed-in indicators: being on the home/dashboard and NOT /signin
                if (!url.contains("/signin") && !url.contains("/signup")) {
                    val cm = CookieManager.getInstance()
                    val cookie = cm.getCookie("https://ollama.com")
                    if (!cookie.isNullOrEmpty()) {
                        getSharedPreferences("app_prefs", MODE_PRIVATE)
                            .edit().putString("ollama_session_cookie", cookie).apply()
                        // immediate first fetch
                        Thread {
                            UsageRepository.refreshNow(applicationContext)
                            runOnUiThread {
                                Toast.makeText(applicationContext, "Signed in. Saving usage data.", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                        }.start()
                    }
                }
            }
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