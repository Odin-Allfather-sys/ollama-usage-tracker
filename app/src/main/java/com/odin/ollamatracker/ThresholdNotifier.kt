package com.odin.ollamatracker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

/**
 * Notifies when usage crosses a user-configured threshold step.
 * Per type (session/weekly) a step size in whole percent, from 1 (every 1%) up.
 * Crossings detected by comparing last-known percent to new percent.
 */
object ThresholdNotifier {
    const val CHANNEL_ID = "usage_thresholds"

    fun onNewUsage(ctx: Context, newSession: Int, newWeekly: Int) {
        val prefs = ctx.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val sessionThreshold = prefs.getInt("session_threshold_pct", 0)  // 0 = off
        val weeklyThreshold = prefs.getInt("weekly_threshold_pct", 0)
        val prevSession = prefs.getInt("prev_session_pct", -1)
        val prevWeekly = prefs.getInt("prev_weekly_pct", -1)

        if (sessionThreshold > 0 && prevSession >= 0) {
            val prevBucket = prevSession / sessionThreshold
            val newBucket = newSession / sessionThreshold
            if (newBucket > prevBucket) notify(ctx, "Ollama Session Usage",
                "Session usage crossed to ${newSession}%")
        }
        if (weeklyThreshold > 0 && prevWeekly >= 0) {
            val prevBucket = prevWeekly / weeklyThreshold
            val newBucket = newWeekly / weeklyThreshold
            if (newBucket > prevBucket) notify(ctx, "Ollama Weekly Usage",
                "Weekly usage crossed to ${newWeekly}%")
        }
        prefs.edit().putInt("prev_session_pct", newSession)
            .putInt("prev_weekly_pct", newWeekly).apply()
    }

    fun notifyReauthRequired(ctx: Context) {
        notify(ctx, "Ollama Tracker - Sign in again",
            "Your ollama.com session expired. Open the app and tap Sign in.")
    }

    private fun notify(ctx: Context, title: String, message: String) {
        val mgr = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mgr.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Usage Threshold Alerts", NotificationManager.IMPORTANCE_DEFAULT))
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return // permission not granted; user must enable in app
        }
        val intent = Intent(ctx, MainActivity::class.java)
        val pi = PendingIntent.getActivity(ctx, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        mgr.notify(title.hashCode(), n)
    }
}