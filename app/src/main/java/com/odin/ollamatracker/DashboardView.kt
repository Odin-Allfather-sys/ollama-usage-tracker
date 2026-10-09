package com.odin.ollamatracker

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView

/**
 * Brand-themed main dashboard, built in code.
 * Card-based: session ring card, weekly card, settings sheet behind gear.
 */
@SuppressLint("ViewConstructor")
class DashboardView(activity: Activity, private val theme: BrandTheme) : LinearLayout(activity) {

    val sessionRing: UsageRingView
    val weeklyBar: UsageBarView
    val weeklyValue: TextView
    val statusText: TextView
    val settingsBtn: TextView
    val refreshBtn: LinearLayout
    lateinit var refreshLabel: TextView
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

    private val d: Float = resources.displayMetrics.density
    private fun dp(v: Float) = (v * d).toInt()

    private fun card(ctx: android.content.Context) = LinearLayout(ctx).apply {
        orientation = VERTICAL
        background = GradientDrawable().apply {
            setColor(theme.surface)
            cornerRadius = theme.cornerRadiusPx
        }
        val p = dp(20f)
        setPadding(p, p, p, p)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).also {
            (it as MarginLayoutParams).bottomMargin = dp(14f)
        }
    }

    private fun label(ctx: android.content.Context, text: String, top: Float = 14f) = TextView(ctx).apply {
        this.text = text
        textSize = 12f
        letterSpacing = 0.08f
        setTextColor(theme.textSecondary)
        setPadding(0, dp(top), 0, dp(6f))
    }

    private fun input(ctx: android.content.Context, hint: String, password: Boolean = false) = EditText(ctx).apply {
        this.hint = hint
        inputType = if (password)
            android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        else
            android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        setTextColor(theme.textPrimary)
        setHintTextColor(theme.textSecondary)
        background = GradientDrawable().apply {
            setColor(theme.surfaceAlt)
            cornerRadius = theme.cornerRadiusPx * 0.6f
        }
        setPadding(dp(16f), dp(14f), dp(16f), dp(14f))
        textSize = 15f
    }

    private fun button(ctx: android.content.Context, text: String, accent: Boolean = false) = Button(ctx).apply {
        this.text = text
        textSize = 14f
        letterSpacing = 0.06f
        isAllCaps = true
        background = GradientDrawable().apply {
            setColor(if (accent) theme.accent else theme.surfaceAlt)
            cornerRadius = theme.cornerRadiusPx * 0.6f
        }
        setTextColor(if (accent) theme.bg else theme.textPrimary)
        stateListAnimator = null
        setPadding(0, dp(14f), 0, dp(14f))
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).also {
            (it as MarginLayoutParams).topMargin = dp(10f)
        }
    }

    init {
        val ctx = activity
        orientation = VERTICAL
        setBackgroundColor(theme.bg)
        val pad = dp(20f)
        setPadding(pad, dp(12f), pad, pad)

        // Header
        val header = LinearLayout(ctx).apply { 
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).also {
                (it as MarginLayoutParams).bottomMargin = dp(20f)
            }
        }
        header.addView(TextView(ctx).apply {
            text = "Usage"
            textSize = theme.titleSizeSp
            setTextColor(theme.textPrimary)
            typeface = Typeface.create(theme.font ?: "sans-serif", Typeface.BOLD)
        }, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        settingsBtn = TextView(ctx).apply {
            text = "Settings"
            textSize = 14f
            setTextColor(theme.accent)
            setPadding(dp(12f), dp(8f), 0, dp(8f))
        }
        header.addView(settingsBtn)
        addView(header)

        // Session ring card
        val ringCard = card(ctx)
        sessionRing = UsageRingView(ctx, theme).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(220f))
        }
        ringCard.addView(sessionRing)
        ringCard.addView(TextView(ctx).apply {
            text = "SESSION USAGE · RESETS EVERY 5 HOURS"
            gravity = Gravity.CENTER
        }.apply {
            textSize = 11f
            letterSpacing = 0.1f
            setTextColor(theme.textSecondary)
            setPadding(0, dp(10f), 0, 0)
        })
        addView(ringCard)

        // Weekly card
        val weekCard = card(ctx)
        weekCard.addView(TextView(ctx).apply {
            text = "WEEKLY"
            textSize = 11f
            letterSpacing = 0.1f
            setTextColor(theme.textSecondary)
        })
        weeklyValue = TextView(ctx).apply {
            textSize = 40f
            setTextColor(theme.textPrimary)
            typeface = if (theme.monoNumbers) Typeface.MONOSPACE else Typeface.create(theme.font, Typeface.BOLD)
            setPadding(0, dp(4f), 0, dp(10f))
        }
        weekCard.addView(weeklyValue)
        weeklyBar = UsageBarView(ctx, theme).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(14f))
        }
        weekCard.addView(weeklyBar)
        addView(weekCard)

        refreshBtn = LinearLayout(ctx).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(theme.surfaceAlt)
                cornerRadius = theme.cornerRadiusPx * 0.7f
            }
            setPadding(0, dp(16f), 0, dp(16f))
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).also {
                (it as MarginLayoutParams).bottomMargin = dp(10f)
            }
            refreshLabel = TextView(ctx).apply {
                text = "REFRESH NOW"
                textSize = 14f
                letterSpacing = 0.08f
                setTextColor(theme.accent)
            }
            addView(refreshLabel)
        }
        addView(refreshBtn)

        statusText = TextView(ctx).apply {
            textSize = 12f
            setTextColor(theme.textSecondary)
            gravity = Gravity.CENTER
            setPadding(0, dp(4f), 0, dp(8f))
        }
        addView(statusText)

        // ---- Settings panel ----
        settingsPanel = LinearLayout(ctx).apply {
            orientation = VERTICAL
            visibility = View.GONE
            background = GradientDrawable().apply {
                setColor(theme.surface)
                cornerRadius = theme.cornerRadiusPx
            }
            setPadding(dp(20f), dp(20f), dp(20f), dp(20f))
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).also {
                (it as MarginLayoutParams).topMargin = dp(6f)
            }
        }
        settingsPanel.addView(TextView(ctx).apply {
            text = "Settings"
            textSize = 20f
            setTextColor(theme.textPrimary)
            typeface = Typeface.create(theme.font ?: "sans-serif", Typeface.BOLD)
            setPadding(0, 0, 0, dp(14f))
        })

        settingsPanel.addView(label(ctx, "API KEY", 0f))
        keyEdit = input(ctx, "sk-...", password = true)
        settingsPanel.addView(keyEdit)

        settingsPanel.addView(label(ctx, "THEME PACK"))
        themeSpinner = Spinner(ctx).apply {
            adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_dropdown_item, ThemePack.entries.map { it.label })
            background = GradientDrawable().apply { setColor(theme.surfaceAlt); cornerRadius = theme.cornerRadiusPx * 0.6f }
            setPadding(dp(12f), dp(12f), dp(12f), dp(12f))
        }
        settingsPanel.addView(themeSpinner)

        settingsPanel.addView(label(ctx, "REFRESH INTERVAL (1-60 MIN)"))
        intervalEdit = input(ctx, "15")
        settingsPanel.addView(intervalEdit)

        applyBtn = button(ctx, "Apply Settings")
        settingsPanel.addView(applyBtn)

        settingsPanel.addView(label(ctx, "SESSION NOTIFY THRESHOLD - 0 = OFF"))
        sessionThresholdSeek = SeekBar(ctx).apply { max = 100 }
        settingsPanel.addView(sessionThresholdSeek)

        settingsPanel.addView(label(ctx, "WEEKLY NOTIFY THRESHOLD - 0 = OFF"))
        weeklyThresholdSeek = SeekBar(ctx).apply { max = 100 }
        settingsPanel.addView(weeklyThresholdSeek)

        thresholdLabel = TextView(ctx).apply {
            textSize = 12f
            setTextColor(theme.textSecondary)
            setPadding(0, dp(4f), 0, 0)
        }
        settingsPanel.addView(thresholdLabel)

        settingsPanel.addView(label(ctx, "CALIBRATE - DASHBOARD % VALUES", 20f))
        calibSession = input(ctx, "Session % (e.g. 52.3)")
        calibWeekly = input(ctx, "Weekly % (e.g. 43.6)")
        settingsPanel.addView(calibSession)
        settingsPanel.addView(calibWeekly)
        calibrateBtn = button(ctx, "Calibrate", accent = true)
        settingsPanel.addView(calibrateBtn)
        addView(settingsPanel)
    }

    fun asView(): View = this
}