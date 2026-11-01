package de.fabiobaensch.glitchy

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.time.Duration
import java.time.LocalDateTime
import java.util.Locale
import java.util.concurrent.TimeUnit

class ReminderWorker(private val ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel("daily", "Daily Challenge", NotificationManager.IMPORTANCE_DEFAULT))
        val de = Locale.getDefault().language == "de"
        val pi = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = NotificationCompat.Builder(ctx, "daily")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(if (de) "Deine Daily Challenge wartet 👾" else "Your daily challenge is waiting 👾")
            .setContentText(if (de) "Eine Aufgabe, ein Tag mehr auf dem Leaderboard." else "One task, one more day on the leaderboard.")
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        runCatching { nm.notify(1, n) } // schlägt still fehl, wenn keine Benachrichtigungs-Erlaubnis vorliegt
        return Result.success()
    }
}

object Reminder {
    /** Täglich zur gewählten Uhrzeit (WorkManager ist nicht sekundengenau, ±ein paar Minuten). */
    fun schedule(ctx: Context, hour: Int, minute: Int) {
        val now = LocalDateTime.now()
        var target = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!target.isAfter(now)) target = target.plusDays(1)
        val req = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(Duration.between(now, target))
            .build()
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork("daily", ExistingPeriodicWorkPolicy.UPDATE, req)
    }

    fun cancel(ctx: Context) = WorkManager.getInstance(ctx).cancelUniqueWork("daily")
}
