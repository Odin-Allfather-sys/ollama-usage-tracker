package com.odin.ollamatracker

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Themed dashboard: big session ring + weekly bar, settings behind gear.
 */
class MainActivity : AppCompatActivity() {
    private lateinit var dash: DashboardView

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val pack = ThemePack.fromName(prefs.getString("theme_pack", null))
        val theme = Themes.resolve(pack, this)
        dash = DashboardView(this, theme)
        setContentView(dash.asView())

        dash.settingsBtn.setOnClickListener {
            dash.settingsPanel.visibility =
                if (dash.settingsPanel.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        dash.applyBtn.setOnClickListener {
            val key = dash.keyEdit.text.toString().trim()
            val interval = dash.intervalEdit.text.toString().toIntOrNull() ?: 15
            if (key.isNotEmpty()) prefs.edit().putString("api_key", key).apply()
            UsagePollScheduler.setInterval(this, interval.coerceIn(1, 60))
            Toast.makeText(this, "Saved. Polling every ${interval.coerceIn(1,60)} min", Toast.LENGTH_SHORT).show()
            Thread {
                UsageRepository.refreshNow(this)
                runOnUiThread { updateLabels() }
            }.start()
        }

        // fill settings fields with current values
        dash.keyEdit.setText(prefs.getString("api_key", "") ?: "")
        dash.intervalEdit.setText(prefs.getInt("poll_interval_min", 15).toString())

        dash.refreshBtn.setOnClickListener {
            dash.statusText.text = "Refreshing..."
            Thread {
                UsageRepository.refreshNow(this)
                runOnUiThread { updateLabels() }
            }.start()
        }

        // Theme pack selection
        val packs = ThemePack.entries
        val prefIdx = packs.indexOf(pack).takeIf { it >= 0 } ?: 0
        dash.themeSpinner.setSelection(prefIdx, false)
        dash.themeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                val sel = packs[pos]
                prefs.edit().putString("theme_pack", if (sel == ThemePack.AUTO) null else sel.name).apply()
                if (sel != pack) recreate()
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        // Thresholds via sliders
        val sessionT = prefs.getInt("session_threshold_pct", 0)
        val weeklyT = prefs.getInt("weekly_threshold_pct", 0)
        dash.sessionThresholdSeek.progress = sessionT
        dash.weeklyThresholdSeek.progress = weeklyT
        updateThresholdLabel(sessionT, weeklyT)
        dash.sessionThresholdSeek.setOnSeekBarChangeListener(seekListener { t ->
            prefs.edit().putInt("session_threshold_pct", t).apply()
            updateThresholdLabel(t, dash.weeklyThresholdSeek.progress)
        })
        dash.weeklyThresholdSeek.setOnSeekBarChangeListener(seekListener { t ->
            prefs.edit().putInt("weekly_threshold_pct", t).apply()
            updateThresholdLabel(dash.sessionThresholdSeek.progress, t)
        })

        dash.calibrateBtn.setOnClickListener {
            val s = dash.calibSession.text.toString().toDoubleOrNull()
            val w = dash.calibWeekly.text.toString().toDoubleOrNull()
            if (s == null || w == null || s <= 0 || w <= 0 || s > 100 || w > 100) {
                Toast.makeText(this, "Enter both dashboard % values (e.g. 52.3)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            dash.statusText.text = "Calibrating..."
            Thread {
                val r = UsageRepository.calibrate(this, s, w)
                runOnUiThread {
                    if (r == null) dash.statusText.text = "Calibration failed."
                    else {
                        Toast.makeText(this, "Calibrated.", Toast.LENGTH_SHORT).show()
                        updateLabels()
                    }
                }
            }.start()
        }

        if (prefs.getString("api_key", null) != null) {
            Thread {
                UsageRepository.refreshNow(this)
                runOnUiThread { updateLabels() }
            }.start()
        } else {
            dash.statusText.text = "API key needed: ollama.com -> Keys"
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateThresholdLabel(s: Int, w: Int) {
        dash.thresholdLabel.text = "Session notify: " + (if (s == 0) "off" else "$s%") +
                "   Weekly notify: " + (if (w == 0) "off" else "$w%")
    }

    @SuppressLint("SetTextI18n")
    private fun updateLabels() {
        val s = UsageRepository.cachedSessionPct(this)
        val w = UsageRepository.cachedWeeklyPct(this)
        dash.sessionRing.pct = s
        dash.weeklyBar.pct = w
        dash.weeklyLabel.text = "$w%"
        dash.statusText.text = "Updated " + UsageRepository.cachedTimestamp(this)
    }

    private fun seekListener(on: (Int) -> Unit) = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) { if (fromUser) on(p) }
        override fun onStartTrackingTouch(sb: SeekBar?) {}
        override fun onStopTrackingTouch(sb: SeekBar?) {}
    }
}