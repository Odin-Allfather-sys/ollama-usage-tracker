package com.odin.ollamatracker

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View

/** Big circular progress ring for session usage. */
class UsageRingView(ctx: Context, private val theme: BrandTheme) : View(ctx) {

    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = theme.accentTrack
        strokeCap = if (theme.pack == ThemePack.NOTHING) Paint.Cap.BUTT else Paint.Cap.ROUND
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = theme.accent
        strokeCap = if (theme.pack == ThemePack.NOTHING) Paint.Cap.BUTT else Paint.Cap.ROUND
    }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = theme.textPrimary
        textAlign = Paint.Align.CENTER
        isFakeBoldText = !theme.monoNumbers
        typeface = if (theme.monoNumbers) android.graphics.Typeface.MONOSPACE else android.graphics.Typeface.DEFAULT_BOLD
    }
    private val sub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = theme.textSecondary
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.1f
    }

    var pct: Int = 0
        set(v) { field = v; invalidate() }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val w = width.toFloat(); val h = height.toFloat()
        val stroke = w * 0.055f
        track.strokeWidth = stroke; fill.strokeWidth = stroke
        val inset = stroke / 2 + w * 0.04f
        val rect = RectF(inset, inset, w - inset, h - inset)

        // backdrop
        c.drawArc(rect, 0f, 360f, false, track)

        // sweep: Nothing OS goes clockwise flat; others start at top
        val start = if (theme.pack == ThemePack.NOTHING) -90f else -90f
        c.drawArc(rect, start, 360f * (pct / 100f), false, fill)

        // danger color when >= 90
        fill.color = if (pct >= 90) theme.danger else theme.accent

        text.textSize = w * 0.24f
        sub.textSize = w * 0.07f
        val cy = h / 2f
        c.drawText("$pct%", w / 2f, cy + text.textSize * 0.15f, text)
        c.drawText("SESSION  5H", w / 2f, cy + text.textSize * 0.55f, sub)
    }
}