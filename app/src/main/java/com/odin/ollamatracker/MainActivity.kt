package com.odin.ollamatracker

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

/**
 * Main settings screen.
 * - Sign in to ollama.com (WebView login, cookie captured automatically)
 * - Poll interval (1-60 min)
 * - Session/weekly threshold steps (0=off, 1=every 1%)
 * - Manual refresh
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)

        val loginBtn = findViewById<Button>(R.id.login_btn)
        val intervalEdit = findViewById<EditText>(R.id.interval_input)
        val sessionThresholdEdit = findViewById<EditText>(R.id.session_threshold_input)
        val weeklyThresholdEdit = findViewById<EditText>(R.id.weekly_threshold_input)
        val saveBtn = findViewById<Button>(R.id.save_btn)
        val refreshBtn = findViewById<Button>(R.id.refresh_btn)
        val statusText = findViewById<TextView>(R.id.status_text)
        val usageText = findViewById<TextView>(R.id.usage_text)

        updateLoginLabel(loginBtn, prefs)
        intervalEdit.setText(prefs.getInt("poll_interval_min", 15).toString())
        sessionThresholdEdit.setText(prefs.getInt("session_threshold_pct", 0).toString())
        weeklyThresholdEdit.setText(prefs.getInt("weekly_threshold_pct", 0).toString())

        loginBtn.setOnClickListener {
            startActivityForResult(Intent(this, LoginActivity::class.java), 1)
        }

        saveBtn.setOnClickListener {
            val interval = intervalEdit.text.toString().toIntOrNull() ?: 15
            val sessionT = sessionThresholdEdit.text.toString().toIntOrNull() ?: 0
            val weeklyT = weeklyThresholdEdit.text.toString().toIntOrNull() ?: 0
            prefs.edit()
                .putInt("session_threshold_pct", sessionT.coerceIn(0, 100))
                .putInt("weekly_threshold_pct", weeklyT.coerceIn(0, 100))
                .apply()
            UsagePollScheduler.setInterval(this, interval)
            Toast.makeText(this, "Saved. Polling every ${interval.coerceIn(1,60)} min", Toast.LENGTH_SHORT).show()
        }

        refreshBtn.setOnClickListener {
            statusText.text = "Refreshing..."
            Thread {
                UsageRepository.refreshNow(this)
                runOnUiThread {
                    showUsage(usageText, statusText, prefs)
                }
            }.start()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1) {
            updateLoginLabel(findViewById(R.id.login_btn), getSharedPreferences("app_prefs", MODE_PRIVATE))
            Thread { UsageRepository.refreshNow(this) }.start()
        }
    }

    private fun updateLoginLabel(btn: Button, prefs: android.content.SharedPreferences) {
        val signedIn = prefs.getString("ollama_session_cookie", null) != null
        btn.text = if (signedIn) "Signed in - tap to re-login" else "Sign in to ollama.com"
    }

    private fun showUsage(usageText: TextView, statusText: TextView, prefs: android.content.SharedPreferences) {
        val p = getSharedPreferences("usage_cache", MODE_PRIVATE)
        usageText.text = "Session: ${p.getInt("session_pct", 0)}%   Weekly: ${p.getInt("weekly_pct", 0)}%"
        statusText.text = "Updated " + UsageRepository.cachedTimestamp(this)
    }
}