package com.odin.ollamatracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.appwidget.AppWidgetManager

/** Broadcasts a widget refresh to all instances after a usage poll. */
object WidgetRefresher {
    fun broadcast(ctx: Context) {
        val i = Intent(ctx, UsageWidgetProvider::class.java).apply {
            action = UsageWidgetProvider.ACTION_USAGE_UPDATED
        }
        ctx.sendBroadcast(i)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        UsagePollScheduler.reschedule(context)
    }
}