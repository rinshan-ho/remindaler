package com.mine.remindaler.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mine.remindaler.IncomingCallActivity
import com.mine.remindaler.audio.PreviewPlayer
import com.mine.remindaler.data.Reminder
import com.mine.remindaler.data.ReminderState
import com.mine.remindaler.util.TimeFormat
import java.io.File

@Composable
fun RemindersScreen(
    reminders: List<Reminder>,
    recordingsDir: File,
    onBack: () -> Unit,
    onCancel: (Reminder) -> Unit
) {
    val context = LocalContext.current
    val player = remember { PreviewPlayer() }
    var playingId by remember { mutableStateOf<Long?>(null) }
    var cancelTarget by remember { mutableStateOf<Reminder?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            player.stop()
            playingId = null
        }
    }

    val now = System.currentTimeMillis()
    val upcoming = reminders
        .filter { it.state == ReminderState.SCHEDULED && it.triggerAt >= now }
        .sortedBy { it.triggerAt }
    val past = reminders
        .filter { it.state != ReminderState.SCHEDULED || it.triggerAt < now }
        .sortedByDescending { it.triggerAt }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .safeDrawingPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る", tint = AppText)
            }
            Text(
                text = "予約一覧",
                color = AppText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "${reminders.size} 件",
                color = AppMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(end = 16.dp)
            )
        }

        if (reminders.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(AppSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.List,
                        contentDescription = null,
                        tint = AppMuted,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text("予約はまだありません", color = AppText, fontSize = 16.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    "テンキーで日時を入力して発信すると、録音したメッセージを予約できます。",
                    color = AppMuted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 4.dp,
                    bottom = 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (upcoming.isNotEmpty()) {
                    item { SectionLabel("これからの予約") }
                    items(upcoming, key = { it.id }) { reminder ->
                        ReminderCard(
                            reminder = reminder,
                            recordingsDir = recordingsDir,
                            playing = playingId == reminder.id,
                            onPlay = {
                                if (playingId == reminder.id) {
                                    player.stop()
                                    playingId = null
                                } else {
                                    val file = File(recordingsDir, reminder.audioFile)
                                    if (!file.exists()) {
                                        Toast.makeText(
                                            context,
                                            "録音ファイルが見つかりません",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        val started = player.play(file) { playingId = null }
                                        playingId = if (started) reminder.id else null
                                    }
                                }
                            },
                            onTest = {
                                runCatching {
                                    context.startActivity(
                                        IncomingCallActivity
                                            .intent(context, reminder.id, test = true)
                                            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                }.onFailure {
                                    Toast.makeText(
                                        context,
                                        "テスト着信を開始できませんでした",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            onCancel = { cancelTarget = reminder }
                        )
                    }
                }

                if (past.isNotEmpty()) {
                    item { SectionLabel("過去のリマインド") }
                    items(past, key = { it.id }) { reminder ->
                        ReminderCard(
                            reminder = reminder,
                            recordingsDir = recordingsDir,
                            playing = playingId == reminder.id,
                            onPlay = {
                                if (playingId == reminder.id) {
                                    player.stop()
                                    playingId = null
                                } else {
                                    val file = File(recordingsDir, reminder.audioFile)
                                    if (!file.exists()) {
                                        Toast.makeText(
                                            context,
                                            "録音ファイルが見つかりません",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        val started = player.play(file) { playingId = null }
                                        playingId = if (started) reminder.id else null
                                    }
                                }
                            },
                            onTest = null,
                            onCancel = { cancelTarget = reminder }
                        )
                    }
                }
            }
        }
    }

    cancelTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { cancelTarget = null },
            title = { Text("この予約を削除しますか？") },
            text = {
                Text("${displayTime(target.triggerAt)} のリマインドと録音を削除します。")
            },
            confirmButton = {
                TextButton(onClick = {
                    player.stop()
                    playingId = null
                    cancelTarget = null
                    onCancel(target)
                }) {
                    Text("削除", color = AppDanger)
                }
            },
            dismissButton = {
                TextButton(onClick = { cancelTarget = null }) {
                    Text("キャンセル", color = AppMuted)
                }
            },
            containerColor = AppSurfaceHigh
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = AppMuted,
        fontSize = 11.sp,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp)
    )
}

@Composable
private fun ReminderCard(
    reminder: Reminder,
    recordingsDir: File,
    playing: Boolean,
    onPlay: () -> Unit,
    onTest: (() -> Unit)?,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppSurface)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusChip(reminder.state)
            Spacer(Modifier.weight(1f))
            Text(
                text = "録音 ${TimeFormat.duration(reminder.durationMs)}",
                color = AppMuted,
                fontSize = 11.sp
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = displayTime(reminder.triggerAt),
            color = AppText,
            fontSize = 22.sp,
            fontFamily = FontFamily.Monospace
        )
        Spacer(Modifier.height(4.dp))
        val file = File(recordingsDir, reminder.audioFile)
        Text(
            text = if (file.exists()) "録音あり" else "録音ファイルなし",
            color = if (file.exists()) AppMuted else AppDanger,
            fontSize = 11.sp
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onPlay, enabled = file.exists()) {
                Text(
                    text = if (playing) "停止" else "再生",
                    color = if (file.exists()) AppPrimary else AppMuted,
                    fontSize = 13.sp
                )
            }
            if (onTest != null) {
                TextButton(onClick = onTest) {
                    Text("テスト着信", color = AppSecondary, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onCancel) {
                Text(
                    text = if (reminder.state == ReminderState.SCHEDULED) "取消" else "削除",
                    color = AppDanger,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun StatusChip(state: ReminderState) {
    val (label, color) = when (state) {
        ReminderState.SCHEDULED -> "予約済み" to AppPrimary
        ReminderState.PLAYED -> "再生済み" to AppSecondary
        ReminderState.MISSED -> "不在" to AppWarning
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text = label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

private fun displayTime(triggerAt: Long): String = TimeFormat.full(triggerAt)
