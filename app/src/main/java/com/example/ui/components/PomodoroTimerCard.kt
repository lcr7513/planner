package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PomodoroMode
import com.example.data.model.PomodoroTimerState
import com.example.data.model.SubjectEntity

@Composable
fun PomodoroTimerCard(
    timerState: PomodoroTimerState,
    subjects: List<SubjectEntity>,
    onToggleStartPause: () -> Unit,
    onResetTimer: () -> Unit,
    onSelectMode: (PomodoroMode) -> Unit,
    onSelectSubject: (SubjectEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSubjectSelector by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("pomodoro_timer_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Title & Sessions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (timerState.isBreak) Color(0xFF10B981).copy(alpha = 0.15f)
                                else Color(0xFFEF4444).copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (timerState.isBreak) "☕" else "🍅",
                            fontSize = 20.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (timerState.isBreak) "달콤한 휴식 시간" else "몰입 집중 뽀모도로",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (timerState.isBreak) "다음 세션을 위해 머리를 식히세요" else "과목: ${timerState.selectedSubjectName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Completed Pomodoro badge
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFEF4444).copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🍅", fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "오늘 ${timerState.todayPomodoroCount}세션",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDC2626)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mode Selector Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PomodoroMode.entries.take(3).forEach { mode ->
                    val isSelected = timerState.selectedMode == mode
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectMode(mode) },
                        label = {
                            Text(
                                text = if (mode == PomodoroMode.STANDARD_25) "25분 표준" else if (mode == PomodoroMode.EXTENDED_50) "50분 집중" else "80분 실전",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Circular Timer Display
            val animatedProgress by animateFloatAsState(
                targetValue = timerState.progress,
                animationSpec = tween(300),
                label = "progress"
            )
            val strokeColor by animateColorAsState(
                targetValue = if (timerState.isBreak) Color(0xFF10B981) else Color(0xFF6366F1),
                label = "strokeColor"
            )

            Box(
                modifier = Modifier
                    .size(200.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 14.dp.toPx()
                    // Track background
                    drawCircle(
                        color = Color(0xFFE2E8F0),
                        radius = (size.minDimension - strokeWidth) / 2,
                        style = Stroke(width = strokeWidth)
                    )
                    // Animated Arc
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(
                                strokeColor,
                                strokeColor.copy(alpha = 0.7f),
                                strokeColor
                            )
                        ),
                        startAngle = -90f,
                        sweepAngle = animatedProgress * 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = timerState.formattedTime,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (timerState.isRunning) (if (timerState.isBreak) "휴식 진행 중" else "초집중 타이머 가동 중")
                               else "일시 정지됨",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = if (timerState.isRunning) strokeColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Subject Selector Pill (when not on break)
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showSubjectSelector = !showSubjectSelector }
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = "과목",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "학습 과목: ${timerState.selectedSubjectName}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Subject Quick Dropdown / Chips
            AnimatedVisibility(visible = showSubjectSelector) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    subjects.forEach { sub ->
                        FilterChip(
                            selected = timerState.selectedSubjectName == sub.name,
                            onClick = {
                                onSelectSubject(sub)
                                showSubjectSelector = false
                            },
                            label = { Text(sub.name, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons (Play / Pause & Reset)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reset Button
                FilledTonalIconButton(
                    onClick = onResetTimer,
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("pomodoro_reset_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "리셋",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(20.dp))

                // Start/Pause Button (Primary Big Action)
                Button(
                    onClick = onToggleStartPause,
                    modifier = Modifier
                        .height(56.dp)
                        .widthIn(min = 160.dp)
                        .testTag("pomodoro_toggle_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (timerState.isRunning) Color(0xFFEA580C) else strokeColor
                    ),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(
                        imageVector = if (timerState.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (timerState.isRunning) "일시정지" else "시작",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (timerState.isRunning) "잠깐 멈춤" else "집중 시작",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
