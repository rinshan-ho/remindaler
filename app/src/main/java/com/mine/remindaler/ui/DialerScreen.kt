package com.mine.remindaler.ui

import android.Manifest
import android.os.Build
import android.view.SoundEffectConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mine.remindaler.R
import com.mine.remindaler.ui.DateTimeParseResult.Incomplete
import com.mine.remindaler.ui.DateTimeParseResult.Invalid
import com.mine.remindaler.ui.DateTimeParseResult.Valid

@Composable
fun DialerScreen(
    ui: MainUiState,
    permissions: PermissionsState,
    onKey: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onStart: (ParsedDateTime) -> Unit,
    onOpenReminders: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val status = permissions.status
    val prefs = remember(context) {
        context.getSharedPreferences("remindaler", android.content.Context.MODE_PRIVATE)
    }

    var pendingStart by remember { mutableStateOf<ParsedDateTime?>(null) }
    var permissionDenied by remember { mutableStateOf(false) }
    var askedNotification by remember {
        mutableStateOf(prefs.getBoolean("asked_notifications", false))
    }

    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissions.refresh()
        val pending = pendingStart
        pendingStart = null
        if (granted && pending != null) {
            onStart(pending)
        } else if (!granted) {
            permissionDenied = true
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { permissions.refresh() }

    LaunchedEffect(Unit) {
        if (!askedNotification && !status.notifications &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        ) {
            askedNotification = true
            prefs.edit().putBoolean("asked_notifications", true).apply()
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val parsed = DateTimeInput.parse(ui.digits)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .safeDrawingPadding()
    ) {
        val compact = maxHeight < 640.dp || LocalDensity.current.fontScale >= 1.3f
        val wideLandscape = maxWidth >= 600.dp && maxWidth > maxHeight
        val horizontalPadding = if (compact) 12.dp else 20.dp
        val keySize = dialerKeySize(maxWidth, maxHeight, compact, wideLandscape)
        val scrollState = rememberScrollState()
        val callEnabled = parsed is Valid
        val onCall: () -> Unit = {
            val current = DateTimeInput.parse(ui.digits)
            if (current is Valid) {
                permissionDenied = false
                if (permissions.status.microphone) {
                    onStart(current.value)
                } else {
                    pendingStart = current.value
                    micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "REMINDaler",
                    color = AppText,
                    fontSize = if (compact) 16.sp else 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onOpenReminders) {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = "予約一覧", tint = AppMuted)
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = "設定", tint = AppMuted)
                }
            }

            if (status.blockingIssues) {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF3A2A12))
                        .clickable { onOpenSettings() }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = null,
                        tint = AppWarning,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "通知・マイクの権限を確認してください",
                        color = AppWarning,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text("確認", color = AppWarning, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (wideLandscape) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        DialerInputSection(ui.digits, parsed, compact, onClear)
                        DialerMessage(ui.error, permissionDenied)
                        Spacer(Modifier.height(if (compact) 8.dp else 16.dp))
                        DialerCallSection(callEnabled, compact, onCall)
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        DialerKeypadSection(
                            keySize = keySize,
                            onKey = {
                                view.playSoundEffect(SoundEffectConstants.CLICK)
                                onKey(it)
                            },
                            onBackspace = {
                                view.playSoundEffect(SoundEffectConstants.CLICK)
                                onBackspace()
                            }
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .then(
                            if (compact) Modifier.verticalScroll(scrollState) else Modifier
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = if (compact) Arrangement.Top else Arrangement.Center
                ) {
                    DialerInputSection(ui.digits, parsed, compact, onClear)
                    DialerMessage(ui.error, permissionDenied)
                    Spacer(Modifier.height(if (compact) 4.dp else 10.dp))
                    DialerKeypadSection(
                        keySize = keySize,
                        onKey = {
                            view.playSoundEffect(SoundEffectConstants.CLICK)
                            onKey(it)
                        },
                        onBackspace = {
                            view.playSoundEffect(SoundEffectConstants.CLICK)
                            onBackspace()
                        }
                    )
                    if (compact) Spacer(Modifier.height(8.dp))
                }
                Spacer(Modifier.height(if (compact) 8.dp else 12.dp))
                DialerCallSection(callEnabled, compact, onCall)
                if (compact) Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (permissionDenied && status.microphone) {
        LaunchedEffect(Unit) { permissionDenied = false }
    }
}

@Composable
private fun DialerInputSection(
    digits: String,
    parsed: DateTimeParseResult,
    compact: Boolean,
    onClear: () -> Unit
) {
    Column(
        modifier = Modifier.widthIn(max = 480.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = formattedDigits(digits),
            color = AppText,
            fontSize = if (compact) 30.sp else 42.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Light,
            letterSpacing = 3.sp,
            maxLines = 1
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = previewText(parsed),
            color = when (parsed) {
                is Invalid -> AppDanger
                is Valid -> if (parsed.value.nextYear) AppWarning else AppPrimary
                else -> AppMuted
            },
            fontSize = if (compact) 14.sp else 15.sp,
            textAlign = TextAlign.Center
        )
        Text(
            text = "MMDDhhmm（例: 1022 0830）",
            color = AppMuted.copy(alpha = 0.7f),
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
        if (digits.isNotEmpty()) {
            TextButton(onClick = onClear) {
                Text("クリア", color = AppMuted, fontSize = 12.sp)
            }
        } else {
            Spacer(Modifier.height(if (compact) 32.dp else 41.dp))
        }
    }
}

@Composable
private fun DialerMessage(error: String?, permissionDenied: Boolean) {
    if (error != null || permissionDenied) {
        Text(
            text = error ?: "マイク権限がないと録音できません。設定から許可してください。",
            color = AppDanger,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
    } else {
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun DialerKeypadSection(
    keySize: Dp,
    onKey: (String) -> Unit,
    onBackspace: () -> Unit
) {
    Keypad(
        keySize = keySize,
        onKey = onKey,
        onBackspace = onBackspace
    )
    Text(
        text = "＊ 今日の月 → 日 → 時",
        color = AppMuted.copy(alpha = 0.65f),
        fontSize = 10.sp,
        modifier = Modifier.padding(top = 6.dp)
    )
}

@Composable
private fun DialerCallSection(
    callEnabled: Boolean,
    compact: Boolean,
    onCall: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(if (compact) 56.dp else 74.dp)
                .clip(CircleShape)
                .background(if (callEnabled) AppPrimary else AppSurfaceHigh)
                .clickable(enabled = callEnabled, onClick = onCall),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Phone,
                contentDescription = "発信",
                tint = if (callEnabled) Color(0xFF04120A) else AppMuted,
                modifier = Modifier.size(if (compact) 24.dp else 34.dp)
            )
        }
        Text(
            text = "発信",
            color = if (callEnabled) AppPrimary else AppMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

internal fun dialerKeySize(
    maxWidth: Dp,
    maxHeight: Dp,
    compact: Boolean,
    wideLandscape: Boolean
): Dp {
    val widthForKeys = if (wideLandscape) maxWidth / 2f else maxWidth
    val horizontalRoom = if (compact) 16.dp else 40.dp
    val byWidth = (widthForKeys - horizontalRoom - 32.dp) / 3f
    val reservedHeight = if (wideLandscape) 120.dp else 360.dp
    val byHeight = (maxHeight - reservedHeight) / 4.8f
    val maxKey = if (compact) 56.dp else 76.dp
    return minOf(byWidth, byHeight).coerceIn(48.dp, maxKey)
}

@Composable
private fun Keypad(
    keySize: Dp,
    onKey: (String) -> Unit,
    onBackspace: () -> Unit
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9")
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                row.forEach { label ->
                    KeypadKey(
                        label = label,
                        size = keySize,
                        onClick = { onKey(label) }
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            KeypadKey(
                label = "*",
                size = keySize,
                onClick = { onKey("*") }
            )
            KeypadKey(
                label = "0",
                size = keySize,
                onClick = { onKey("0") }
            )
            BackspaceKey(size = keySize, onClick = onBackspace)
        }
    }
}

@Composable
private fun KeypadKey(
    label: String,
    size: Dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(AppSurface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = AppText,
            fontSize = 26.sp,
            fontWeight = FontWeight.Light
        )
    }
}

@Composable
private fun BackspaceKey(size: Dp, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(AppSurface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_backspace),
            contentDescription = "1文字削除",
            tint = AppText,
            modifier = Modifier.size(size * 0.38f)
        )
    }
}

private fun formattedDigits(digits: String): String {
    if (digits.isEmpty()) return "＿＿＿＿ ＿＿＿＿"
    return if (digits.length <= 4) digits else "${digits.take(4)} ${digits.drop(4)}"
}

private fun previewText(parsed: DateTimeParseResult): String = when (parsed) {
    DateTimeParseResult.Empty -> "リマインドする日時を入力"
    is Incomplete -> "あと ${DateTimeInput.LENGTH - parsed.entered} 桁"
    is Invalid -> parsed.message
    is Valid -> parsed.value.display + if (parsed.value.nextYear) "（翌年）" else ""
}
