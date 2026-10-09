package com.odin.ollamatracker

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

/**
 * Main settings screen. Everything in one place, no tabs needed.
 * - Paste session cookie
 * - Set poll interval (1-60 min)
 * - Set session threshold step (0=off, 1=every 1%)
 * - Set weekly threshold step
 * - Manual refresh
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)

        val cookieEdit = findViewById<EditText>(R.id.cookie_input)
        val intervalEdit = findViewById<EditText>(R.id.interval_input)
        val sessionThresholdEdit = findViewById<EditText>(R.id.session_threshold_input)
        val weeklyThresholdEdit = findViewById<EditText>(R.id.weekly_threshold_input)
        val saveBtn = findViewById<Button>(R.id.save_btn)
        val refreshBtn = findViewById<Button>(R.id.refresh_btn)
        val statusText = findViewById<TextView>(R.id.status_text)
        val usageText = findViewById<TextView>(R.id.usage_text)

        cookieEdit.setText(prefs.getString("tracker_url", "http://100.94.103.108:8443/usage"))
        intervalEdit.setText(prefs.getInt("poll_interval_min", 15).toString())
        sessionThresholdEdit.setText(prefs.getInt("session_threshold_pct", 0).toString())
        weeklyThresholdEdit.setText(prefs.getInt("weekly_threshold_pct", 0).toString())

        saveBtn.setOnClickListener {
            val cookie = cookieEdit.text.toString().trim()
            val interval = intervalEdit.text.toString().toIntOrNull() ?: 15
            val sessionT = sessionThresholdEdit.text.toString().toIntOrNull() ?: 0
            val weeklyT = weeklyThresholdEdit.text.toString().toIntOrNull() ?: 0
            if (cookie.isEmpty()) {
                Toast.makeText(this, "Tracker URL required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            prefs.edit()
                .putString("tracker_url", cookie)
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
                    val s = UsageRepository.cachedSessionPct(this)
                    val w = UsageRepository.cachedWeeklyPct(this)
                    usageText.text = "Session: $s%   Weekly: $w%"
                    statusText.text = "Updated " + UsageRepository.cachedTimestamp(this)
                }
            }.start()
        }
    }
}