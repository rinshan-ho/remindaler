package com.mine.remindaler.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt

private const val MAX_RECORD_MS = 60_000L

@Composable
fun RecordScreen(
    ui: MainUiState,
    onStop: () -> Unit,
    onCancel: () -> Unit,
    elapsedMs: () -> Long,
    amplitude: () -> Int
) {
    var elapsed by remember { mutableLongStateOf(0L) }
    var level by remember { mutableIntStateOf(0) }
    var stopping by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            elapsed = elapsedMs()
            level = amplitude()
            if (elapsed >= MAX_RECORD_MS && !stopping) {
                stopping = true
                onStop()
                break
            }
            delay(80L)
        }
    }

    val pulse = rememberInfiniteTransition(label = "record-pulse")
    val pulseScale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse-scale"
    )
    val pulseAlpha by pulse.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse-alpha"
    )

    val remaining = ((MAX_RECORD_MS - elapsed) / 1000L).coerceAtLeast(0L)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .safeDrawingPadding()
    ) {
        val compact = maxHeight < 640.dp || LocalDensity.current.fontScale >= 1.3f
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (compact) 16.dp else 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "録音",
                    color = AppText,
                    fontSize = if (compact) 16.sp else 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "残り ${remaining}秒",
                    color = if (remaining <= 10) AppDanger else AppMuted,
                    fontSize = 12.sp
                )
                IconButton(onClick = onCancel) {
                    Icon(Icons.Filled.Close, contentDescription = "破棄", tint = AppMuted)
                }
            }

            if (compact) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(8.dp))
                    RecordSummary(ui.pendingPreview, compact = true)
                    Spacer(Modifier.height(8.dp))
                    RecordVisual(pulseScale, pulseAlpha, compact = true)
                    Spacer(Modifier.height(8.dp))
                    RecordTimer(elapsed, compact = true)
                    Spacer(Modifier.height(8.dp))
                    Waveform(level = level, elapsed = elapsed, compact = true)
                    Spacer(Modifier.height(8.dp))
                }
                RecordStopButton(
                    onClick = {
                        if (!stopping) {
                            stopping = true
                            onStop()
                        }
                    },
                    compact = true
                )
                Spacer(Modifier.height(8.dp))
            } else {
                Spacer(Modifier.weight(1f))
                RecordSummary(ui.pendingPreview, compact = false)
                Spacer(Modifier.height(24.dp))
                RecordVisual(pulseScale, pulseAlpha, compact = false)
                Spacer(Modifier.height(20.dp))
                RecordTimer(elapsed, compact = false)
                Spacer(Modifier.height(26.dp))
                Waveform(level = level, elapsed = elapsed, compact = false)
                Spacer(Modifier.weight(1f))
                RecordStopButton(
                    onClick = {
                        if (!stopping) {
                            stopping = true
                            onStop()
                        }
                    },
                    compact = false
                )
                Spacer(Modifier.weight(0.5f))
            }
        }
    }
}

@Composable
private fun RecordSummary(preview: String, compact: Boolean) {
    Text(
        text = preview,
        color = AppPrimary,
        fontSize = if (compact) 12.sp else 14.sp,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(if (compact) 4.dp else 6.dp))
    Text(
        text = "リマインドしたい内容を話してください",
        color = AppText,
        fontSize = if (compact) 18.sp else 20.sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun RecordVisual(pulseScale: Float, pulseAlpha: Float, compact: Boolean) {
    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(if (compact) 118.dp else 168.dp)
                .scale(pulseScale)
                .alpha(pulseAlpha)
                .clip(CircleShape)
                .background(AppDanger)
        )
        Box(
            modifier = Modifier
                .size(if (compact) 92.dp else 132.dp)
                .clip(CircleShape)
                .background(Color(0xFF1B1114)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(if (compact) 42.dp else 58.dp)) {
                val w = size.width
                drawRoundRect(
                    color = AppDanger,
                    topLeft = Offset(w * 0.36f, w * 0.04f),
                    size = Size(w * 0.28f, w * 0.52f),
                    cornerRadius = CornerRadius(w * 0.14f)
                )
                drawArc(
                    color = AppDanger,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(w * 0.22f, w * 0.28f),
                    size = Size(w * 0.56f, w * 0.46f),
                    style = Stroke(width = w * 0.075f, cap = StrokeCap.Round)
                )
                drawLine(
                    color = AppDanger,
                    start = Offset(w * 0.5f, w * 0.78f),
                    end = Offset(w * 0.5f, w * 0.93f),
                    strokeWidth = w * 0.075f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = AppDanger,
                    start = Offset(w * 0.34f, w * 0.93f),
                    end = Offset(w * 0.66f, w * 0.93f),
                    strokeWidth = w * 0.075f,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun RecordTimer(elapsed: Long, compact: Boolean) {
    Text(
        text = formatElapsed(elapsed),
        color = AppText,
        fontSize = if (compact) 32.sp else 46.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Light
    )
    Text(
        text = "録音中",
        color = AppDanger,
        fontSize = 12.sp,
        letterSpacing = 2.sp
    )
}

@Composable
private fun RecordStopButton(onClick: () -> Unit, compact: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(if (compact) 60.dp else 82.dp)
                .clip(CircleShape)
                .background(Color(0xFFDC2626))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Call,
                contentDescription = "受話器を置く",
                tint = Color.White,
                modifier = Modifier
                    .size(if (compact) 28.dp else 38.dp)
                    .rotate(135f)
            )
        }
        Text(
            text = "受話器を置いて予約",
            color = AppText,
            fontSize = if (compact) 12.sp else 13.sp,
            modifier = Modifier.padding(top = 6.dp)
        )
        Text(
            text = "最長60秒・自動で停止します",
            color = AppMuted,
            fontSize = if (compact) 9.sp else 10.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun Waveform(level: Int, elapsed: Long, compact: Boolean) {
    val normalized = sqrt((level.coerceIn(0, 32767) / 32767f)).coerceIn(0f, 1f)
    Row(
        horizontalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(if (compact) 40.dp else 64.dp)
    ) {
        repeat(if (compact) 24 else 30) { index ->
            val wave = (0.25f + 0.75f * abs(sin(index * 0.55 + elapsed / 140.0))).toFloat()
            val barHeight = if (compact) {
                (4f + 32f * normalized * wave).dp
            } else {
                (6f + 52f * normalized * wave).dp
            }
            Box(
                modifier = Modifier
                    .width(if (compact) 3.dp else 4.dp)
                    .height(barHeight)
                    .clip(CircleShape)
                    .background(AppDanger.copy(alpha = 0.75f))
            )
        }
    }
}

private fun formatElapsed(ms: Long): String {
    val total = (ms / 1000L).coerceAtLeast(0L)
    return String.format(Locale.JAPAN, "%02d:%02d", total / 60L, total % 60L)
}
