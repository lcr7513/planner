package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SubjectEntity
import com.example.ui.theme.EduAmberWarning
import com.example.ui.theme.EduGreenSuccess
import com.example.ui.theme.EduIndigoPrimary
import com.example.ui.theme.EduRedAlert

enum class ProgressChartDisplayMode {
    CIRCULAR, // 원형 차트 모드
    BAR_LIST  // 프로그레스 바 목록 모드
}

/**
 * 학습 진행률을 보여주는 종합 대시보드 UI 컴포넌트
 * 원형 다이얼 차트와 과목별 프로그레스 바를 탭을 통해 상호 전환하여 확인할 수 있습니다.
 */
@Composable
fun LearningProgressDashboardCard(
    overallProgress: Float,
    averageScore: Int,
    streakDays: Int,
    totalMinutes: Int,
    weakCount: Int,
    subjects: List<SubjectEntity>,
    modifier: Modifier = Modifier
) {
    var displayMode by remember { mutableStateOf(ProgressChartDisplayMode.CIRCULAR) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("learning_progress_dashboard_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with Title & Mode Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "학습 진행률 대시보드",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "실시간 이수 현황 및 목표 달성률",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // View Mode Switcher
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(modifier = Modifier.padding(3.dp)) {
                        IconButton(
                            onClick = { displayMode = ProgressChartDisplayMode.CIRCULAR },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (displayMode == ProgressChartDisplayMode.CIRCULAR)
                                        MaterialTheme.colorScheme.primary
                                    else Color.Transparent
                                )
                                .testTag("toggle_circular_chart")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DonutLarge,
                                contentDescription = "원형 차트 뷰",
                                tint = if (displayMode == ProgressChartDisplayMode.CIRCULAR)
                                    MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { displayMode = ProgressChartDisplayMode.BAR_LIST },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (displayMode == ProgressChartDisplayMode.BAR_LIST)
                                        MaterialTheme.colorScheme.primary
                                    else Color.Transparent
                                )
                                .testTag("toggle_bar_chart")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatListBulleted,
                                contentDescription = "프로그레스 바 뷰",
                                tint = if (displayMode == ProgressChartDisplayMode.BAR_LIST)
                                    MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Animated content between Circular Gauge and Bar List
            AnimatedContent(
                targetState = displayMode,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                label = "progress_display_mode"
            ) { mode ->
                when (mode) {
                    ProgressChartDisplayMode.CIRCULAR -> {
                        CircularProgressChartContent(
                            overallProgress = overallProgress,
                            subjects = subjects
                        )
                    }
                    ProgressChartDisplayMode.BAR_LIST -> {
                        SubjectProgressBarListContent(subjects = subjects)
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Bottom Metric Badges Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatItem(
                    label = "연속 학습",
                    value = "${streakDays}일차",
                    icon = Icons.Default.LocalFireDepartment,
                    color = Color(0xFFD97706),
                    modifier = Modifier.weight(1f)
                )
                VerticalDivider(modifier = Modifier.height(26.dp))
                StatItem(
                    label = "평균 점수",
                    value = "${averageScore}점",
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    color = if (averageScore >= 80) EduGreenSuccess else EduAmberWarning,
                    modifier = Modifier.weight(1f)
                )
                VerticalDivider(modifier = Modifier.height(26.dp))
                StatItem(
                    label = "누적 학습",
                    value = "${totalMinutes / 60}h ${totalMinutes % 60}m",
                    icon = Icons.Default.Schedule,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                VerticalDivider(modifier = Modifier.height(26.dp))
                StatItem(
                    label = "보완 대상",
                    value = "${weakCount}과목",
                    icon = Icons.Default.WarningAmber,
                    color = if (weakCount > 0) EduRedAlert else EduGreenSuccess,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * 1. 원형 다이얼 차트 뷰
 * 부드러운 애니메이션 곡선과 중앙 진행률, 하단 과목별 인디케이터를 포함합니다.
 */
@Composable
fun CircularProgressChartContent(
    overallProgress: Float,
    subjects: List<SubjectEntity>
) {
    val animatedProgress by animateFloatAsState(
        targetValue = overallProgress,
        animationSpec = tween(durationMillis = 1000),
        label = "animated_overall_progress"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(170.dp)
        ) {
            val primaryColor = MaterialTheme.colorScheme.primary
            val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            val successColor = EduGreenSuccess

            Canvas(modifier = Modifier.size(150.dp)) {
                val strokeWidth = 15.dp.toPx()
                val diameter = size.minDimension - strokeWidth
                val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                val arcSize = Size(diameter, diameter)

                // Outer Background Track Arc (270 degrees)
                drawArc(
                    color = trackColor,
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Foreground Animated Progress Arc with Linear Gradient
                drawArc(
                    brush = Brush.horizontalGradient(
                        listOf(primaryColor, Color(0xFF6366F1), successColor)
                    ),
                    startAngle = 135f,
                    sweepAngle = 270f * animatedProgress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${(animatedProgress * 100).toInt()}%",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (animatedProgress >= 0.7f) "순항 중 🚀" else "진도 진행 중 📖",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 과목별 진도 칩 목록 (원형 차트 하단 레전드)
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            subjects.chunked(3).forEach { rowSubjects ->
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    rowSubjects.forEach { subject ->
                        val color = when {
                            subject.isWeak || subject.currentScore < 70 -> EduRedAlert
                            subject.currentScore >= 85 -> EduGreenSuccess
                            else -> EduIndigoPrimary
                        }
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "${subject.name} ${(subject.progressPercent * 100).toInt()}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 2. 과목별 프로그레스 바 목록 뷰
 * 각 과목의 상세 단원 완료율을 막대 바와 퍼센티지로 시각화합니다.
 */
@Composable
fun SubjectProgressBarListContent(
    subjects: List<SubjectEntity>
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        subjects.forEach { subject ->
            val animatedSubProgress by animateFloatAsState(
                targetValue = subject.progressPercent,
                animationSpec = tween(durationMillis = 800),
                label = "sub_prog_${subject.id}"
            )

            val barColor = when {
                subject.isWeak || subject.currentScore < 70 -> EduRedAlert
                subject.currentScore >= 85 -> EduGreenSuccess
                else -> EduIndigoPrimary
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = subject.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${subject.completedUnits}/${subject.totalUnits} 단원",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (subject.isWeak || subject.currentScore < 70) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = EduRedAlert.copy(alpha = 0.12f),
                                modifier = Modifier.padding(end = 6.dp)
                            ) {
                                Text(
                                    text = "취약",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EduRedAlert,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = "${(animatedSubProgress * 100).toInt()}%",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = barColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Custom animated progress bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedSubProgress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(barColor)
                    )
                }
            }
        }
    }
}

@Composable
fun StatItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(horizontal = 2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

@Composable
fun WeeklyStudyBarChart(
    weeklyMinutes: List<Int>,
    modifier: Modifier = Modifier
) {
    val days = listOf("월", "화", "수", "목", "금", "토", "일")
    val maxMinutes = (weeklyMinutes.maxOrNull() ?: 80).coerceAtLeast(60).toFloat()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("weekly_chart_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "주간 학습 시간 분석",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "매일 꾸준한 학습 루틴 형성",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val totalWeekly = weeklyMinutes.sum()
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "이번 주 총 ${totalWeekly / 60}h ${totalWeekly % 60}m",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Bar chart rows
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                weeklyMinutes.forEachIndexed { index, minutes ->
                    val heightRatio = (minutes.toFloat() / maxMinutes).coerceIn(0.1f, 1f)
                    val isPeak = minutes == weeklyMinutes.maxOrNull()
                    val day = days.getOrElse(index) { "" }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "${minutes}m",
                            fontSize = 10.sp,
                            fontWeight = if (isPeak) FontWeight.Bold else FontWeight.Normal,
                            color = if (isPeak) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(18.dp)
                                .height((80 * heightRatio).dp)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(
                                    if (isPeak) Brush.verticalGradient(
                                        listOf(EduIndigoPrimary, Color(0xFF818CF8))
                                    ) else Brush.verticalGradient(
                                        listOf(Color(0xFFCBD5E1), Color(0xFF94A3B8))
                                    )
                                )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = day,
                            fontSize = 12.sp,
                            fontWeight = if (isPeak) FontWeight.Bold else FontWeight.Normal,
                            color = if (isPeak) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SubjectProgressCard(
    subject: SubjectEntity,
    onAddStudyClick: () -> Unit,
    onClinicClick: () -> Unit,
    onDeleteClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val progressColor = when {
        subject.isWeak || subject.currentScore < 70 -> EduRedAlert
        subject.currentScore >= 85 -> EduGreenSuccess
        else -> EduIndigoPrimary
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("subject_card_${subject.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Subject Name, Weak indicator, Score
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(progressColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = subject.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (subject.isWeak || subject.currentScore < 70) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = EduRedAlert.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "취약 보완 필요",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = EduRedAlert,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${subject.currentScore}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = progressColor
                    )
                    Text(
                        text = " / ${subject.targetScore}점 목표",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar and Unit numbers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "학습 완료도",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(${subject.completedUnits}/${subject.totalUnits} 단원)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${(subject.progress * 100).toInt()}% 완료",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = progressColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { subject.progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .testTag("progress_bar_${subject.id}"),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )

            if (subject.weakTopic.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = if (subject.isWeak) EduAmberWarning else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "집중 보충 단원: ${subject.weakTopic}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onDeleteClick != null) {
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("delete_subject_${subject.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "${subject.name} 과목 삭제",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onAddStudyClick,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "학습 기록", fontSize = 12.sp)
                    }

                    if (subject.isWeak || subject.currentScore < 70) {
                        Button(
                            onClick = onClinicClick,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EduAmberWarning,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "취약점 클리닉", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
