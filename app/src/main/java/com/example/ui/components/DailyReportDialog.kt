package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.ui.theme.EduAmberWarning
import com.example.ui.theme.EduGreenSuccess
import com.example.ui.theme.EduIndigoPrimary
import com.example.ui.theme.EduRedAlert
import com.example.ui.viewmodel.LearningUiState
import com.example.util.NotificationHelper

@Composable
fun DailyReportDialog(
    uiState: LearningUiState,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var autoNotificationEnabled by remember { mutableStateOf(true) }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val sent = NotificationHelper.sendDailyReportNotification(context, uiState)
            if (sent) {
                Toast.makeText(context, "🔔 상단 알림창에 오늘 일간 리포트가 발송되었습니다!", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "알림 권한이 허용되지 않아 알림을 보낼 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    val todayMinutes = uiState.weeklyMinutes.lastOrNull() ?: 75
    val completedMissions = uiState.recommendations.count { it.isCompleted }
    val totalMissions = uiState.recommendations.size
    val weakCount = uiState.weakSubjects.size

    val grade = when {
        todayMinutes >= 90 && weakCount == 0 -> "S"
        todayMinutes >= 60 -> "A+"
        todayMinutes >= 30 -> "B+"
        else -> "C"
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("daily_report_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Summarize,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "오늘의 일간 학습 리포트",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "하루 성취도 및 활동 요약",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "닫기")
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Hero Grade Badge Banner
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "오늘의 성취 평가",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (grade.startsWith("A") || grade == "S") "목표 초과 달성! 훌륭해요" else "성실하게 학습을 이어갔어요",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "연속 ${uiState.streakDays}일째 학습 습관 유지 중",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = grade,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Grid stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ReportStatBox(
                        title = "오늘 학습 시간",
                        value = "${todayMinutes}분",
                        icon = Icons.Default.Schedule,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    ReportStatBox(
                        title = "미션 완료",
                        value = "${completedMissions}/${totalMissions}개",
                        icon = Icons.Default.TaskAlt,
                        tint = EduGreenSuccess,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ReportStatBox(
                        title = "취약 과목",
                        value = if (weakCount > 0) "${weakCount}과목 관리" else "취약점 없음",
                        icon = Icons.Default.WarningAmber,
                        tint = if (weakCount > 0) EduRedAlert else EduGreenSuccess,
                        modifier = Modifier.weight(1f)
                    )
                    ReportStatBox(
                        title = "평균 이해도",
                        value = "${uiState.averageScore}점",
                        icon = Icons.Default.TrendingUp,
                        tint = if (uiState.averageScore >= 80) EduGreenSuccess else EduAmberWarning,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // AI Feedback & Coaching Box
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "내일을 위한 맞춤 학습 처방",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (uiState.weakSubjects.isNotEmpty()) {
                                "취약 과목인 [${uiState.weakSubjects.first().name}]의 '${uiState.weakSubjects.first().weakTopic}' 단원 퀴즈를 10분 동안 집중 풀이하면 점수 취약점을 빠르게 극복할 수 있습니다."
                            } else {
                                "현재 전 과목이 고르게 우수한 진도를 보이고 있습니다. 내일은 심화 기출문제 풀이로 상위권 굳히기에 도전해 보세요!"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Auto reminder toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "매일 저녁 9시 일간 리포트 알림",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "학습 종료 시간에 맞춰 하루 요약 수신",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = autoNotificationEnabled,
                        onCheckedChange = { autoNotificationEnabled = it }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Button: Send notification now
                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                            if (!hasPermission) {
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                return@Button
                            }
                        }
                        val sent = NotificationHelper.sendDailyReportNotification(context, uiState)
                        if (sent) {
                            Toast.makeText(context, "🔔 스마트폰 상단에 일간 리포트 알림이 도착했습니다!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("send_notification_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("지금 휴대폰 상단 알림 받기", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ReportStatBox(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
