package com.mine.remindaler.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PermissionsScreen(
    permissions: PermissionsState,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val status = permissions.status

    var micRequested by remember { mutableStateOf(false) }
    var notificationRequested by remember { mutableStateOf(false) }

    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissions.refresh()
        if (!granted) micRequested = true
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissions.refresh()
        if (!granted) notificationRequested = true
    }

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
                text = "設定と権限",
                color = AppText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (status.blockingIssues) {
                    "リマインド着信に必要な権限が不足しています。"
                } else {
                    "リマインド着信の準備ができています。"
                },
                color = if (status.blockingIssues) AppWarning else AppPrimary,
                fontSize = 13.sp,
                modifier = Modifier.padding(4.dp)
            )

            val micNeedsSettings = status.microphone || micRequested
            PermissionCard(
                title = "マイク",
                description = "リマインドメッセージを録音するために使用します。",
                granted = status.microphone,
                actionLabel = if (micNeedsSettings) "アプリ設定" else "許可する",
                onAction = {
                    if (micNeedsSettings) {
                        openAppSettings(context)
                    } else {
                        micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            )

            val notificationNeedsSettings = status.notifications ||
                notificationRequested ||
                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
            PermissionCard(
                title = "通知",
                description = "指定時刻に着信を知らせるために必須です。",
                granted = status.notifications,
                actionLabel = if (notificationNeedsSettings) "通知設定" else "許可する",
                onAction = {
                    if (notificationNeedsSettings) {
                        openNotificationSettings(context)
                    } else {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            )

            PermissionCard(
                title = "全画面通知",
                description = "画面ロック中でも電話のような着信画面を表示します。許可されない場合は通知の「応答」から再生できます。",
                granted = status.fullScreenIntent,
                actionLabel = "設定を開く",
                onAction = { openFullScreenIntentSettings(context) },
                optional = true
            )

            PermissionCard(
                title = "正確なアラーム",
                description = "指定した時刻どおりに着信させるために使用します。未許可の場合は数分遅れることがあります。",
                granted = status.exactAlarm,
                actionLabel = "設定を開く",
                onAction = { openExactAlarmSettings(context) },
                optional = true
            )

            PermissionCard(
                title = "電池の最適化",
                description = "端末がスリープ中でも着信しやすくします。除外を推奨します（必須ではありません）。",
                granted = status.batteryExempt,
                actionLabel = if (status.batteryExempt) "設定を開く" else "除外する",
                onAction = {
                    if (status.batteryExempt) openAppSettings(context) else requestBatteryExemption(context)
                },
                optional = true
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(AppSurface)
                    .padding(16.dp)
            ) {
                Text(
                    text = "音量の確認",
                    color = AppText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "アラーム音量が0になっていると、着信画面は出ても音が聞こえません。端末の音量ボタンでアラーム音量を確認してください。",
                    color = AppMuted,
                    fontSize = 12.sp
                )
                TextButton(onClick = { openSoundSettings(context) }) {
                    Text("音量設定を開く", color = AppSecondary, fontSize = 13.sp)
                }
            }

            Text(
                text = "ヒント: 予約一覧の「テスト着信」を使うと、待たずに着信画面と再生を確認できます。",
                color = AppMuted,
                fontSize = 11.sp,
                modifier = Modifier.padding(4.dp)
            )

            Text(
                text = "メーカー独自の省電力設定や自動起動制限（Xiaomi / OPPO / vivo / HUAWEI / Samsung など）により、画面オフ時の着信が遅れたり、再起動後に予約が復元されない場合があります。設定アプリの「バッテリー」や「アプリの自動起動」も確認してください。",
                color = AppMuted,
                fontSize = 11.sp,
                modifier = Modifier.padding(4.dp)
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    granted: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
    optional: Boolean = false
) {
    val stateColor = when {
        granted -> AppPrimary
        optional -> AppWarning
        else -> AppDanger
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppSurface)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(stateColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (granted) Icons.Filled.Check else Icons.Filled.Close,
                    contentDescription = null,
                    tint = stateColor,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = title,
                color = AppText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = when {
                    granted -> "許可済み"
                    optional -> "推奨"
                    else -> "未許可"
                },
                color = stateColor,
                fontSize = 11.sp
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(text = description, color = AppMuted, fontSize = 12.sp)
        Spacer(Modifier.height(2.dp))
        TextButton(onClick = onAction, modifier = Modifier.align(Alignment.End)) {
            Text(actionLabel, color = if (granted) AppSecondary else AppPrimary, fontSize = 13.sp)
        }
    }
}
