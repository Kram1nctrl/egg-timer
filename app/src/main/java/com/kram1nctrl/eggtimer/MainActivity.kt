package com.kram1nctrl.eggtimer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kram1nctrl.eggtimer.ui.theme.EggTimerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // A timer you can't see is useless, so keep the display awake while cooking.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            EggTimerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    TimerScreen()
                }
            }
        }
    }
}

@Composable
private fun TimerScreen() {
    val state by TimerEngine.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { /* The timer still runs if notifications are denied. */ }

    fun startTimer() {
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        TimerEngine.start(context)
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Egg Timer",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = statusLine(state),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(28.dp))

            CountdownRing(state)

            Spacer(Modifier.height(28.dp))

            PresetPicker(
                selected = state.preset,
                enabled = state.status != TimerStatus.RUNNING,
                onSelect = TimerEngine::selectPreset,
            )

            if (state.preset == Preset.CUSTOM && state.status != TimerStatus.RUNNING) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "${state.customMinutes} minutes",
                    style = MaterialTheme.typography.titleSmall,
                )
                Slider(
                    value = state.customMinutes.toFloat(),
                    onValueChange = { TimerEngine.setCustomMinutes(it.toInt()) },
                    valueRange = 1f..60f,
                    steps = 58,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    when (state.status) {
                        TimerStatus.RUNNING -> TimerEngine.pause(context)
                        TimerStatus.FINISHED -> {
                            TimerEngine.stopAlarm()
                            TimerEngine.reset(context)
                        }
                        else -> startTimer()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Text(primaryActionLabel(state.status), fontSize = 18.sp)
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = { TimerEngine.reset(context) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text("Reset")
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = "Lock your screen if you like — the countdown keeps running in the background.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun CountdownRing(state: TimerState) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    val accent = if (state.status == TimerStatus.FINISHED) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = Modifier.size(268.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 20.dp.toPx()
            val inset = strokeWidth / 2f
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(inset, inset)

            drawArc(
                color = track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            drawArc(
                color = accent,
                startAngle = -90f,
                sweepAngle = 360f * state.progress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = formatTime(state.remainingMs),
                fontSize = 54.sp,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = if (state.status == TimerStatus.FINISHED) "done" else state.label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PresetPicker(
    selected: Preset,
    enabled: Boolean,
    onSelect: (Preset) -> Unit,
) {
    val rows = listOf(
        listOf(Preset.SOFT, Preset.MEDIUM),
        listOf(Preset.HARD, Preset.CUSTOM),
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { preset ->
                    FilterChip(
                        selected = selected == preset,
                        onClick = { onSelect(preset) },
                        enabled = enabled,
                        label = { Text(preset.label) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

private fun statusLine(state: TimerState): String = when (state.status) {
    TimerStatus.IDLE -> "${state.label} · ${state.preset.note}"
    TimerStatus.RUNNING -> "Cooking…"
    TimerStatus.PAUSED -> "Paused"
    TimerStatus.FINISHED -> "Your eggs are ready"
}

private fun primaryActionLabel(status: TimerStatus): String = when (status) {
    TimerStatus.IDLE -> "Start"
    TimerStatus.RUNNING -> "Pause"
    TimerStatus.PAUSED -> "Resume"
    TimerStatus.FINISHED -> "Stop"
}

private fun formatTime(remainingMs: Long): String {
    val totalSeconds = (remainingMs + 999) / 1000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
