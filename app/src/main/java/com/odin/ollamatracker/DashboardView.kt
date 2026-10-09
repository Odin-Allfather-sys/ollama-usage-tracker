package com.odin.ollamatracker

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView

/**
 * Brand-themed main dashboard, built in code (no per-brand XML).
 * Big session ring + weekly bar; settings behind gear.
 */
@SuppressLint("ViewConstructor")
class DashboardView(activity: Activity, private val theme: BrandTheme) : LinearLayout(activity) {

    val sessionRing: UsageRingView
    val weeklyBar: UsageBarView
    val sessionLabel: TextView
    val weeklyLabel: TextView
    val statusText: TextView
    val settingsBtn: TextView
    val refreshBtn: TextView
    val themeSpinner: Spinner
    val calibSession: EditText
    val calibWeekly: EditText
    val calibrateBtn: Button
    val sessionThresholdSeek: SeekBar
    val weeklyThresholdSeek: SeekBar
    val thresholdLabel: TextView
    val settingsPanel: LinearLayout
    lateinit var intervalEdit: EditText
    lateinit var keyEdit: EditText
    lateinit var applyBtn: Button

    init {
        val ctx = activity
        orientation = VERTICAL
        setBackgroundColor(theme.bg)
        val pad = (16 * resources.displayMetrics.density).toInt()
        setPadding(pad, pad, pad, pad)

        val h = ctx.resources.displayMetrics.heightPixels

        // Header row: title + gear
        val header = LinearLayout(ctx).apply { orientation = HORIZONTAL }
        val title = TextView(ctx).apply {
            text = "USAGE"
            textSize = theme.titleSizeSp
            setTextColor(theme.textPrimary)
            typeface = if (theme.monoNumbers) Typeface.MONOSPACE else Typeface.create(theme.font, Typeface.BOLD)
            letterSpacing = 0.04f
        }
        settingsBtn = TextView(ctx).apply {
            text = "⚙"
            textSize = 26f
            setTextColor(theme.textSecondary)
            gravity = Gravity.END
            setPadding(0, 0, 0, theme.cornerRadiusPx.toInt() / 4)
        }
        header.addView(title, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        header.addView(settingsBtn)
        addView(header)

        // Session ring, dominant
        sessionRing = UsageRingView(ctx, theme).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, (h * 0.34f).toInt())
        }
        addView(sessionRing)

        // Session label under ring
        sessionLabel = TextView(ctx).apply {
            textSize = 14f
            setTextColor(theme.textSecondary)
            gravity = Gravity.CENTER
            setPadding(0, (8 * resources.displayMetrics.density).toInt(), 0, 0)
        }
        addView(sessionLabel)

        // Weekly bar block
        weeklyLabel = TextView(ctx).apply {
            textSize = theme.hugeMetricSizeSp / 2.2f
            setTextColor(theme.textPrimary)
            typeface = if (theme.monoNumbers) Typeface.MONOSPACE else Typeface.create(theme.font, Typeface.BOLD)
            setPadding(0, (20 * resources.displayMetrics.density).toInt(), 0, (4 * resources.displayMetrics.density).toInt())
        }
        addView(weeklyLabel)

        weeklyBar = UsageBarView(ctx, theme).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, (24 * resources.displayMetrics.density).toInt()).also {
                (it as MarginLayoutParams).bottomMargin = (8 * resources.displayMetrics.density).toInt()
            }
        }
        addView(weeklyBar)
        val weeklyCaption = TextView(ctx).apply {
            text = "WEEKLY"
            textSize = 12f
            letterSpacing = 0.12f
            setTextColor(theme.textSecondary)
        }
        addView(weeklyCaption)

        // Status
        statusText = TextView(ctx).apply {
            textSize = 12f
            setTextColor(theme.textSecondary)
            setPadding(0, (12 * resources.displayMetrics.density).toInt(), 0, 0)
        }
        addView(statusText)

        // Refresh row
        refreshBtn = TextView(ctx).apply {
            text = "⟳  Refresh"
            textSize = 16f
            setTextColor(theme.accent)
            gravity = Gravity.CENTER
            setPadding(0, (16 * resources.displayMetrics.density).toInt(), 0, 0)
        }
        addView(refreshBtn)

        // ---- Settings panel (hidden by default) ----
        settingsPanel = LinearLayout(ctx).apply {
            orientation = VERTICAL
            visibility = View.GONE
            setPadding(0, (16 * resources.displayMetrics.density).toInt(), 0, 0)
        }
        settingsPanel.addView(label(ctx, "Theme pack"))
        themeSpinner = Spinner(ctx).apply {
            adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_dropdown_item,
                ThemePack.entries.map { it.label })
        }
        settingsPanel.addView(themeSpinner)

        settingsPanel.addView(label(ctx, "Refresh interval (1-60 minutes)"))
        val intervalEdit = EditText(ctx).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setTextColor(theme.textPrimary)
            background = GradientDrawable().apply { setColor(theme.surface); cornerRadius = theme.cornerRadiusPx }
            setPadding(24, 24, 24, 24)
        }
        settingsPanel.addView(intervalEdit)
        this.intervalEdit = intervalEdit

        settingsPanel.addView(label(ctx, "API key (ollama.com -> Keys)"))
        keyEdit = EditText(ctx).apply {
            hint = "Paste your API key"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setTextColor(theme.textPrimary)
            setHintTextColor(theme.textSecondary)
            background = GradientDrawable().apply { setColor(theme.surface); cornerRadius = theme.cornerRadiusPx }
            setPadding(24, 24, 24, 24)
        }
        settingsPanel.addView(keyEdit)
        val applyBtn = Button(ctx).apply {
            text = "APPLY SETTINGS"
            setBackgroundColor(theme.surfaceAlt)
            setTextColor(theme.textPrimary)
        }
        settingsPanel.addView(applyBtn)
        this.applyBtn = applyBtn

        settingsPanel.addView(label(ctx, "Session notify threshold (%)"))
        sessionThresholdSeek = SeekBar(ctx).apply { max = 100 }
        settingsPanel.addView(sessionThresholdSeek)

        settingsPanel.addView(label(ctx, "Weekly notify threshold (%)"))
        weeklyThresholdSeek = SeekBar(ctx).apply { max = 100 }
        settingsPanel.addView(weeklyThresholdSeek)

        thresholdLabel = label(ctx, "")

        settingsPanel.addView(label(ctx, "Calibrate - type the two % values your dashboard shows"))
        calibSession = TextField(ctx, "Session % (e.g. 52.3)")
        calibWeekly = TextField(ctx, "Weekly % (e.g. 43.6)")
        settingsPanel.addView(calibSession)
        settingsPanel.addView(calibWeekly)
        calibrateBtn = Button(ctx).apply {
            text = "CALIBRATE"
            setBackgroundColor(theme.surfaceAlt)
            setTextColor(theme.textPrimary)
        }
        settingsPanel.addView(calibrateBtn)
        addView(settingsPanel)
    }

    private fun label(ctx: android.content.Context, text: String) = TextView(ctx).apply {
        this.text = text
        textSize = 13f
        setTextColor(theme.textSecondary)
        setPadding(0, (14 * resources.displayMetrics.density).toInt(), 0, (4 * resources.displayMetrics.density).toInt())
    }

    private fun TextField(ctx: android.content.Context, hint: String) = EditText(ctx).apply {
        this.hint = hint
        inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        setTextColor(theme.textPrimary)
        setHintTextColor(theme.textSecondary)
        background = GradientDrawable().apply {
            setColor(theme.surface)
            cornerRadius = theme.cornerRadiusPx
        }
        setPadding(24, 24, 24, 24)
    }

    fun asView(): View = this
}