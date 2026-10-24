package com.kram1nctrl.eggtimer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Keeps the countdown alive while the app is in the background or the screen is off.
 * The remaining time is derived from [TimerEngine.endAtElapsed], so missed ticks
 * (doze, process jank) never make the timer drift.
 */
class TimerService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var tickJob: Job? = null
    private var lastShownSecond = -1L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(TimerEngine.state.value.remainingMs, finished = false),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            },
        )

        tickJob?.cancel()
        tickJob = scope.launch {
            while (isActive) {
                val remaining = (TimerEngine.endAtElapsed - SystemClock.elapsedRealtime())
                    .coerceAtLeast(0L)
                TimerEngine.tick(remaining)

                val second = remaining / 1000L
                if (second != lastShownSecond) {
                    lastShownSecond = second
                    notify(NOTIFICATION_ID, buildNotification(remaining, finished = false))
                }

                if (remaining == 0L) {
                    TimerEngine.finish(this@TimerService)
                    notify(NOTIFICATION_ID + 1, buildNotification(0L, finished = true))
                    stopForeground(STOP_FOREGROUND_DETACH)
                    stopSelf()
                    break
                }
                delay(200)
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        tickJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    private fun buildNotification(remainingMs: Long, finished: Boolean): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val title = if (finished) "Eggs are ready" else "Egg timer"
        val text = if (finished) "Your eggs are done." else "${formatRemaining(remainingMs)} left"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_egg)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setOngoing(!finished)
            .setAutoCancel(finished)
            .setSilent(!finished)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .build()
    }

    private fun formatRemaining(remainingMs: Long): String {
        val totalSeconds = (remainingMs + 999) / 1000
        return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.timer_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.timer_channel_description)
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    private fun notify(id: Int, notification: Notification) {
        getSystemService(NotificationManager::class.java)?.notify(id, notification)
    }

    companion object {
        private const val CHANNEL_ID = "egg-timer"
        private const val NOTIFICATION_ID = 1

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, TimerService::class.java),
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, TimerService::class.java))
        }
    }
}
