package com.odin.ollamatracker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Auto-refresh scheduler. Custom interval per user, min 1 min, max 60 min.
 * Uses AlarmManager setExactAndAllowWhileIdle for battery-friendly accuracy.
 */
object UsagePollScheduler {
    private const val REQUEST_CODE = 4242

    fun intervalMinutes(ctx: Context): Int {
        val prefs = ctx.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        // 1 = 1 minute, 60 = 1 hour
        return prefs.getInt("poll_interval_min", 15).coerceIn(1, 60)
    }

    fun setInterval(ctx: Context, minutes: Int) {
        ctx.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .edit().putInt("poll_interval_min", minutes.coerceIn(1, 60)).apply()
        reschedule(ctx)
    }

    fun reschedule(ctx: Context) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(ctx, REQUEST_CODE,
            Intent(ctx, PollReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        am.cancel(pi)
        val triggerAt = System.currentTimeMillis() + intervalMinutes(ctx) * 60_000L
        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    fun cancel(ctx: Context) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(ctx, REQUEST_CODE,
            Intent(ctx, PollReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        am.cancel(pi)
    }
}

class PollReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        UsageRepository.refreshNowAsync(context)
        UsagePollScheduler.reschedule(context) // chain next poll
    }
}