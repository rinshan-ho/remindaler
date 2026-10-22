package com.mine.remindaler.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mine.remindaler.data.Reminder
import com.mine.remindaler.util.TimeFormat
import kotlin.math.abs
import kotlin.math.sin

enum class CallState {
    RINGING,
    PLAYING,
    ERROR
}

@Composable
fun IncomingScreen(
    state: CallState,
    progress: Float,
    reminder: Reminder,
    onAnswer: () -> Unit,
    onDecline: () -> Unit,
    onStopPlayback: () -> Unit,
    onClose: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF06110C), Color(0xFF0A1A12), Color(0xFF04080C))
                )
            )
            .safeDrawingPadding()
    ) {
        val compact = maxHeight < 600.dp || LocalDensity.current.fontScale >= 1.3f
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = if (compact) 16.dp else 24.dp,
                    vertical = if (compact) 8.dp else 16.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "REMINDaler",
                color = AppMuted,
                fontSize = if (compact) 10.sp else 12.sp,
                letterSpacing = 3.sp
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .then(
                        if (compact) Modifier.verticalScroll(rememberScrollState()) else Modifier
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = if (compact) Arrangement.Top else Arrangement.Center
            ) {
                if (compact) Spacer(Modifier.height(8.dp))

                if (state == CallState.RINGING) {
                    PulsingAvatar(compact = compact)
                } else {
                    Avatar(active = state == CallState.PLAYING, compact = compact)
                }

                Spacer(Modifier.height(if (compact) 10.dp else 28.dp))

                Text(
                    text = "リマインド",
                    color = AppText,
                    fontSize = if (compact) 20.sp else 26.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(if (compact) 4.dp else 8.dp))
                Text(
                    text = TimeFormat.full(reminder.triggerAt),
                    color = AppPrimary,
                    fontSize = if (compact) 13.sp else 15.sp
                )
                Spacer(Modifier.height(if (compact) 6.dp else 10.dp))

                when (state) {
                    CallState.RINGING -> {
                        Text(
                            text = "着信中…",
                            color = AppMuted,
                            fontSize = if (compact) 12.sp else 14.sp,
                            letterSpacing = 2.sp
                        )
                    }

                    CallState.PLAYING -> {
                        Text(
                            text = "メッセージを再生しています",
                            color = AppMuted,
                            fontSize = if (compact) 12.sp else 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(if (compact) 10.dp else 20.dp))
                        PlaybackBars(compact = compact)
                        Spacer(Modifier.height(if (compact) 10.dp else 20.dp))
                        Box(
                            modifier = Modifier
                                .width(if (compact) 180.dp else 240.dp)
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color.White.copy(alpha = 0.12f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                                    .background(AppPrimary)
                            )
                        }
                    }

                    CallState.ERROR -> {
                        Text(
                            text = "録音ファイルを再生できませんでした",
                            color = AppDanger,
                            fontSize = if (compact) 12.sp else 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                if (compact) Spacer(Modifier.height(8.dp))
            }

            when (state) {
                CallState.RINGING -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(if (compact) 32.dp else 64.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CallButton(
                            background = Color(0xFFDC2626),
                            icon = Icons.Filled.Close,
                            label = "拒否",
                            onClick = onDecline,
                            compact = compact
                        )
                        CallButton(
                            background = AppPrimary,
                            icon = Icons.Filled.Call,
                            label = "応答",
                            iconTint = Color(0xFF04120A),
                            onClick = onAnswer,
                            compact = compact
                        )
                    }
                }

                CallState.PLAYING -> {
                    CallButton(
                        background = Color(0xFFDC2626),
                        icon = Icons.Filled.Close,
                        label = "終了",
                        onClick = onStopPlayback,
                        compact = compact
                    )
                }

                CallState.ERROR -> {
                    CallButton(
                        background = AppSurfaceHigh,
                        icon = Icons.Filled.Close,
                        label = "閉じる",
                        onClick = onClose,
                        compact = compact
                    )
                }
            }

            Spacer(Modifier.height(if (compact) 8.dp else 20.dp))
        }
    }
}

@Composable
private fun PulsingAvatar(compact: Boolean) {
    val transition = rememberInfiniteTransition(label = "call-pulse")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100),
            repeatMode = RepeatMode.Reverse
        ),
        label = "call-scale"
    )
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100),
            repeatMode = RepeatMode.Reverse
        ),
        label = "call-alpha"
    )

    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(if (compact) 128.dp else 176.dp)
                .scale(scale)
                .alpha(alpha)
                .clip(CircleShape)
                .background(AppPrimary)
        )
        Avatar(active = true, compact = compact)
    }
}

@Composable
private fun Avatar(active: Boolean, compact: Boolean) {
    Box(
        modifier = Modifier
            .size(if (compact) 92.dp else 124.dp)
            .clip(CircleShape)
            .background(if (active) AppPrimary.copy(alpha = 0.14f) else AppSurfaceHigh),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Call,
            contentDescription = null,
            tint = if (active) AppPrimary else AppMuted,
            modifier = Modifier.size(if (compact) 36.dp else 52.dp)
        )
    }
}

@Composable
private fun PlaybackBars(compact: Boolean) {
    val transition = rememberInfiniteTransition(label = "playback")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(if (compact) 34.dp else 46.dp)
    ) {
        repeat(24) { index ->
            val height = if (compact) {
                (7f + 24f * abs(sin(index * 0.5 + phase))).dp
            } else {
                (10f + 34f * abs(sin(index * 0.5 + phase))).dp
            }
            Box(
                modifier = Modifier
                    .width(if (compact) 3.dp else 4.dp)
                    .height(height)
                    .clip(CircleShape)
                    .background(AppPrimary.copy(alpha = 0.8f))
            )
        }
    }
}

@Composable
private fun CallButton(
    background: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    iconTint: Color = Color.White,
    onClick: () -> Unit,
    compact: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(if (compact) 60.dp else 76.dp)
                .clip(CircleShape)
                .background(background)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(if (compact) 26.dp else 34.dp)
            )
        }
        Text(
            text = label,
            color = AppText,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}
