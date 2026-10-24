package com.kram1nctrl.eggtimer

import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class TimerStatus { IDLE, RUNNING, PAUSED, FINISHED }

enum class Preset(val label: String, val minutes: Int, val note: String) {
    SOFT("Soft", 4, "runny yolk"),
    MEDIUM("Medium", 6, "jammy yolk"),
    HARD("Hard", 9, "firm yolk"),
    CUSTOM("Custom", 5, "your call"),
}

data class TimerState(
    val preset: Preset = Preset.MEDIUM,
    val customMinutes: Int = 5,
    val totalMs: Long = Preset.MEDIUM.minutes * 60_000L,
    val remainingMs: Long = Preset.MEDIUM.minutes * 60_000L,
    val status: TimerStatus = TimerStatus.IDLE,
) {
    /** Fraction of the countdown still to go, 1f at the start and 0f when done. */
    val progress: Float
        get() = if (totalMs <= 0L) 0f else (remainingMs.toFloat() / totalMs).coerceIn(0f, 1f)

    val label: String get() = if (preset == Preset.CUSTOM) "$customMinutes min" else preset.label
}

/**
 * Single source of truth for the countdown. Both the Compose UI and [TimerService]
 * read and write this object, so the timer survives rotation and backgrounding.
 */
object TimerEngine {

    private val _state = MutableStateFlow(TimerState())
    val state: StateFlow<TimerState> = _state.asStateFlow()

    /** Wall-clock deadline in [SystemClock.elapsedRealtime] terms, so it is immune to clock changes. */
    var endAtElapsed: Long = 0L
        private set

    private var ringtone: Ringtone? = null

    fun selectPreset(preset: Preset) {
        _state.update { current ->
            if (current.status == TimerStatus.RUNNING) return@update current
            val minutes = if (preset == Preset.CUSTOM) current.customMinutes else preset.minutes
            current.copy(
                preset = preset,
                totalMs = minutes * 60_000L,
                remainingMs = minutes * 60_000L,
                status = TimerStatus.IDLE,
            )
        }
    }

    fun setCustomMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(1, 60)
        _state.update { current ->
            if (current.preset != Preset.CUSTOM || current.status == TimerStatus.RUNNING) {
                current.copy(customMinutes = clamped)
            } else {
                current.copy(
                    customMinutes = clamped,
                    totalMs = clamped * 60_000L,
                    remainingMs = clamped * 60_000L,
                )
            }
        }
    }

    fun start(context: Context) {
        val current = _state.value
        val from = if (current.status == TimerStatus.FINISHED) current.totalMs else current.remainingMs
        if (from <= 0L) return
        stopAlarm()
        endAtElapsed = SystemClock.elapsedRealtime() + from
        _state.update { it.copy(remainingMs = from, status = TimerStatus.RUNNING) }
        TimerService.start(context)
    }

    fun pause(context: Context) {
        if (_state.value.status != TimerStatus.RUNNING) return
        val left = (endAtElapsed - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
        _state.update { it.copy(remainingMs = left, status = TimerStatus.PAUSED) }
        TimerService.stop(context)
    }

    fun reset(context: Context) {
        stopAlarm()
        TimerService.stop(context)
        _state.update { it.copy(remainingMs = it.totalMs, status = TimerStatus.IDLE) }
    }

    /** Called by the service on every tick so the UI stays in sync. */
    fun tick(remainingMs: Long) {
        _state.update { it.copy(remainingMs = remainingMs) }
    }

    fun finish(context: Context) {
        _state.update { it.copy(remainingMs = 0L, status = TimerStatus.FINISHED) }
        playAlarm(context)
    }

    fun stopAlarm() {
        ringtone?.let { runCatching { if (it.isPlaying) it.stop() } }
        ringtone = null
    }

    private fun playAlarm(context: Context) {
        stopAlarm()
        val app = context.applicationContext
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        ringtone = RingtoneManager.getRingtone(app, uri)?.apply {
            audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            play()
        }
        vibrate(app)
    }

    private fun vibrate(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(VibratorManager::class.java)
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400, 200, 700), -1),
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 400, 200, 400, 200, 700), -1)
        }
    }
}
