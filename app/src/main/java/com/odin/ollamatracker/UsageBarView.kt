package com.odin.ollamatracker

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View

/** Horizontal usage bar for weekly usage. */
class UsageBarView(ctx: Context, private val theme: BrandTheme) : View(ctx) {

    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = theme.accentTrack
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = theme.accent
    }

    var pct10: Int = 0
        set(v) { field = v; invalidate() }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val r = theme.cornerRadiusPx
        val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
        c.drawRoundRect(rect, r, r, track)
        if (pct10 > 0) {
            val w = width * (pct10 / 1000f)
            fill.color = if (pct10 / 10 >= 90) theme.danger else theme.accent
            if (theme.pack == ThemePack.NOTHING && w < r * 2) {
                c.drawRect(0f, 0f, w, height.toFloat(), fill)
            } else {
                c.drawRoundRect(RectF(0f, 0f, w, height.toFloat()), r, r, fill)
            }
        }
    }
}