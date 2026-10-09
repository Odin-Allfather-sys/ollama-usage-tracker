package com.odin.ollamatracker

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

/**
 * Main settings screen.
 * - Paste API key (from ollama.com -> Get API key)
 * - Poll interval (1-60 min)
 * - Session/weekly threshold steps (0=off, 1=every 1%)
 * - Plan limits (requests per 5h session / per week) for percent math
 * - Manual refresh
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)

        val keyEdit = findViewById<EditText>(R.id.api_key_input)
        val intervalEdit = findViewById<EditText>(R.id.interval_input)
        val sessionThresholdEdit = findViewById<EditText>(R.id.session_threshold_input)
        val weeklyThresholdEdit = findViewById<EditText>(R.id.weekly_threshold_input)
        val sessionLimitEdit = findViewById<EditText>(R.id.session_limit_input)
        val weeklyLimitEdit = findViewById<EditText>(R.id.weekly_limit_input)
        val saveBtn = findViewById<Button>(R.id.save_btn)
        val refreshBtn = findViewById<Button>(R.id.refresh_btn)
        val statusText = findViewById<TextView>(R.id.status_text)
        val usageText = findViewById<TextView>(R.id.usage_text)
        val calibSessionEdit = findViewById<EditText>(R.id.calib_session_input)
        val calibWeeklyEdit = findViewById<EditText>(R.id.calib_weekly_input)
        val calibrateBtn = findViewById<Button>(R.id.calibrate_btn)

        keyEdit.setText(prefs.getString("api_key", "")!!)
        intervalEdit.setText(prefs.getInt("poll_interval_min", 15).toString())
        sessionThresholdEdit.setText(prefs.getInt("session_threshold_pct", 0).toString())
        weeklyThresholdEdit.setText(prefs.getInt("weekly_threshold_pct", 0).toString())
        sessionLimitEdit.setText(prefs.getInt("session_limit_req", OllamaUsageApi.DEFAULT_SESSION_LIMIT_REQ).toString())
        weeklyLimitEdit.setText(prefs.getInt("weekly_limit_req", OllamaUsageApi.DEFAULT_WEEKLY_LIMIT_REQ).toString())

        saveBtn.setOnClickListener {
            val key = keyEdit.text.toString().trim()
            val interval = intervalEdit.text.toString().toIntOrNull() ?: 15
            val sessionT = sessionThresholdEdit.text.toString().toIntOrNull() ?: 0
            val weeklyT = weeklyThresholdEdit.text.toString().toIntOrNull() ?: 0
            val sl = sessionLimitEdit.text.toString().toIntOrNull() ?: OllamaUsageApi.DEFAULT_SESSION_LIMIT_REQ
            val wl = weeklyLimitEdit.text.toString().toIntOrNull() ?: OllamaUsageApi.DEFAULT_WEEKLY_LIMIT_REQ
            if (key.isEmpty()) {
                Toast.makeText(this, "API key required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            prefs.edit()
                .putString("api_key", key)
                .putInt("session_threshold_pct", sessionT.coerceIn(0, 100))
                .putInt("weekly_threshold_pct", weeklyT.coerceIn(0, 100))
                .putInt("session_limit_req", sl.coerceAtLeast(1))
                .putInt("weekly_limit_req", wl.coerceAtLeast(1))
                .apply()
            UsagePollScheduler.setInterval(this, interval)
            Toast.makeText(this, "Saved. Polling every ${interval.coerceIn(1,60)} min", Toast.LENGTH_SHORT).show()
        }

        refreshBtn.setOnClickListener {
            statusText.text = "Refreshing..."
            Thread {
                UsageRepository.refreshNow(this)
                runOnUiThread { showUsage(usageText, statusText) }
            }.start()
        }

        calibrateBtn.setOnClickListener {
            val s = calibSessionEdit.text.toString().toDoubleOrNull()
            val w = calibWeeklyEdit.text.toString().toDoubleOrNull()
            if (s == null || w == null || s <= 0 || w <= 0 || s > 100 || w > 100) {
                Toast.makeText(this, "Enter your dashboard % values (e.g. 52.3)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            statusText.text = "Calibrating..."
            Thread {
                val r = UsageRepository.calibrate(this, s, w)
                runOnUiThread {
                    if (r == null) {
                        statusText.text = "Calibration failed. Check % values and API key."
                    } else {
                        Toast.makeText(this, "Calibrated. Limits fit to your dashboard.", Toast.LENGTH_SHORT).show()
                        showUsage(usageText, statusText)
                    }
                }
            }.start()
        }

        if (prefs.getString("api_key", null) != null) {
            Thread {
                UsageRepository.refreshNow(this)
                runOnUiThread { showUsage(usageText, statusText) }
            }.start()
        }
    }

    private fun showUsage(usageText: TextView, statusText: TextView) {
        val s = UsageRepository.cachedSessionPct(this)
        val w = UsageRepository.cachedWeeklyPct(this)
        usageText.text = "Session: $s%   Weekly: $w%"
        statusText.text = "Updated " + UsageRepository.cachedTimestamp(this)
    }
}