package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProgressTrendPoint
import com.example.data.model.StudyAnalyticsSummary
import com.example.data.model.SubjectEntity
import com.example.data.model.SubjectStudyStat
import com.example.ui.theme.*
import kotlin.math.roundToInt

enum class RechartsChartType {
    PROGRESS_AREA,   // Recharts Area/Monotone Spline: 학습 진도율 변화 추이
    SUBJECT_TIME_BAR,// Recharts Bar: 과목별 학습 시간 계산 및 비중 분석
    CUMULATIVE_TIME  // Recharts Line: 누적 학습 시간 증가 추세
}

/**
 * Recharts 스타일의 학습 진도 및 과목별 시간 변화 시각화 카드 컴포넌트
 * Room 데이터베이스의 study_logs 및 subjects 테이블을 기반으로 계산된 데이터를
 * CartesianGrid, Monotone Curve, Interactive Tooltip, Legend 필터링 기능과 함께 제공합니다.
 */
@Composable
fun RechartsLearningProgressChart(
    analyticsSummary: StudyAnalyticsSummary,
    subjects: List<SubjectEntity>,
    modifier: Modifier = Modifier,
    onSubjectClick: ((Long) -> Unit)? = null
) {
    var selectedChartType by remember { mutableStateOf(RechartsChartType.PROGRESS_AREA) }
    var highlightedSubjectId by remember { mutableStateOf<Long?>(null) }
    var activeScrubIndex by remember { mutableStateOf<Int?>(null) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("recharts_progress_chart_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header with Recharts Badge & Chart Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Text(
                                text = "Recharts Engine",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "학습 진도 & 시간 분석",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Room DB 학습 기록 기반 실시간 통계",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Quick Reset highlight button if active
                if (highlightedSubjectId != null) {
                    TextButton(
                        onClick = { highlightedSubjectId = null },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("전체 보기", fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Recharts Key Metric Summary Cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(vertical = 10.dp, horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val topSub = analyticsSummary.topSubject
                RechartsKpiItem(
                    label = "최다 학습 과목",
                    value = topSub?.let { "${it.subjectName} (${it.totalMinutes}m)" } ?: "-",
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    color = EduIndigoPrimary,
                    modifier = Modifier.weight(1f)
                )
                VerticalDivider(modifier = Modifier.height(24.dp))
                RechartsKpiItem(
                    label = "취약 과목 비중",
                    value = "${(analyticsSummary.weakSubjectRatio * 100).roundToInt()}%",
                    icon = Icons.Default.WarningAmber,
                    color = if (analyticsSummary.weakSubjectRatio >= 0.3f) EduGreenSuccess else EduAmberWarning,
                    modifier = Modifier.weight(1f)
                )
                VerticalDivider(modifier = Modifier.height(24.dp))
                RechartsKpiItem(
                    label = "일평균 학습",
                    value = "${analyticsSummary.dailyAverageMinutes}분",
                    icon = Icons.Default.Schedule,
                    color = EduTealSecondary,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Segmented Chart Switcher (Recharts Controls)
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("recharts_tab_row")
            ) {
                SegmentedButton(
                    selected = selectedChartType == RechartsChartType.PROGRESS_AREA,
                    onClick = {
                        selectedChartType = RechartsChartType.PROGRESS_AREA
                        activeScrubIndex = null
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ShowChart,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("진도 곡선", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                SegmentedButton(
                    selected = selectedChartType == RechartsChartType.SUBJECT_TIME_BAR,
                    onClick = {
                        selectedChartType = RechartsChartType.SUBJECT_TIME_BAR
                        activeScrubIndex = null
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("과목별 시간", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                SegmentedButton(
                    selected = selectedChartType == RechartsChartType.CUMULATIVE_TIME,
                    onClick = {
                        selectedChartType = RechartsChartType.CUMULATIVE_TIME
                        activeScrubIndex = null
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timeline,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("누적 추이", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Animated Canvas Chart Content
            AnimatedContent(
                targetState = selectedChartType,
                transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) },
                label = "recharts_type_transition"
            ) { chartType ->
                when (chartType) {
                    RechartsChartType.PROGRESS_AREA -> {
                        RechartsMonotoneAreaChart(
                            points = analyticsSummary.trendPoints,
                            activeScrubIndex = activeScrubIndex,
                            onScrubChanged = { activeScrubIndex = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                        )
                    }

                    RechartsChartType.SUBJECT_TIME_BAR -> {
                        RechartsSubjectBarChart(
                            subjectStats = analyticsSummary.subjectStats,
                            highlightedSubjectId = highlightedSubjectId,
                            onSelectSubject = { highlightedSubjectId = if (highlightedSubjectId == it) null else it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                        )
                    }

                    RechartsChartType.CUMULATIVE_TIME -> {
                        RechartsCumulativeLineChart(
                            points = analyticsSummary.trendPoints,
                            activeScrubIndex = activeScrubIndex,
                            onScrubChanged = { activeScrubIndex = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Interactive Tooltip Card Display (when user drags/touches)
            if (activeScrubIndex != null && activeScrubIndex in analyticsSummary.trendPoints.indices) {
                val point = analyticsSummary.trendPoints[activeScrubIndex!!]
                RechartsTooltipCard(point = point)
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Recharts Legend with Clickable Highlight Filter
            RechartsInteractiveLegend(
                subjectStats = analyticsSummary.subjectStats,
                highlightedSubjectId = highlightedSubjectId,
                onSubjectClick = { subId ->
                    highlightedSubjectId = if (highlightedSubjectId == subId) null else subId
                    onSubjectClick?.invoke(subId)
                }
            )
        }
    }
}

/**
 * 1. Recharts Monotone Spline / Area Chart
 * 부드러운 3차 베지어 곡선(Cubic Monotone Curve)과 그라데이션 면적 채우기,
 * 가상 Cartesian Grid 및 터치 스크러버를 제공합니다.
 */
@Composable
fun RechartsMonotoneAreaChart(
    points: List<ProgressTrendPoint>,
    activeScrubIndex: Int?,
    onScrubChanged: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("학습 진도 기록이 아직 충분하지 않습니다.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = EduTealSecondary
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    val textLabelColor = MaterialTheme.colorScheme.onSurfaceVariant

    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 800),
        label = "area_anim"
    )

    Box(
        modifier = modifier
            .testTag("recharts_area_chart_canvas")
            .pointerInput(points) {
                detectTapGestures(
                    onPress = { offset ->
                        val w = size.width - 64.dp.toPx()
                        val paddingStart = 40.dp.toPx()
                        val step = if (points.size > 1) w / (points.size - 1) else w
                        val touchedIdx = ((offset.x - paddingStart + step / 2) / step).toInt()
                        if (touchedIdx in points.indices) {
                            onScrubChanged(touchedIdx)
                        } else {
                            onScrubChanged(null)
                        }
                    }
                )
            }
            .pointerInput(points) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val w = size.width - 64.dp.toPx()
                        val paddingStart = 40.dp.toPx()
                        val step = if (points.size > 1) w / (points.size - 1) else w
                        val touchedIdx = ((offset.x - paddingStart + step / 2) / step).toInt()
                        if (touchedIdx in points.indices) {
                            onScrubChanged(touchedIdx)
                        }
                    },
                    onDragEnd = { /* keep active or timeout */ },
                    onDragCancel = { },
                    onDrag = { change, _ ->
                        val w = size.width - 64.dp.toPx()
                        val paddingStart = 40.dp.toPx()
                        val step = if (points.size > 1) w / (points.size - 1) else w
                        val touchedIdx = ((change.position.x - paddingStart + step / 2) / step).toInt()
                        if (touchedIdx in points.indices) {
                            onScrubChanged(touchedIdx)
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val paddingLeft = 38.dp.toPx()
            val paddingRight = 16.dp.toPx()
            val paddingTop = 16.dp.toPx()
            val paddingBottom = 28.dp.toPx()

            val chartWidth = size.width - paddingLeft - paddingRight
            val chartHeight = size.height - paddingTop - paddingBottom

            // 1. Cartesian Grid Horizontal Lines (0%, 25%, 50%, 75%, 100%)
            val gridLevels = listOf(0.0f, 0.25f, 0.5f, 0.75f, 1.0f)
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

            gridLevels.forEach { level ->
                val y = paddingTop + chartHeight * (1f - level)
                drawLine(
                    color = gridColor,
                    start = Offset(paddingLeft, y),
                    end = Offset(size.width - paddingRight, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dashEffect
                )
            }

            // 2. Compute Data Points Coordinates
            val stepX = if (points.size > 1) chartWidth / (points.size - 1) else chartWidth
            val coords = points.mapIndexed { idx, pt ->
                val x = paddingLeft + idx * stepX
                val prog = (pt.progressPercent * animProgress).coerceIn(0f, 1f)
                val y = paddingTop + chartHeight * (1f - prog)
                Offset(x, y)
            }

            // 3. Build Smooth Cubic Bezier Spline Path (Monotone Area)
            if (coords.isNotEmpty()) {
                val linePath = Path()
                val areaPath = Path()

                linePath.moveTo(coords.first().x, coords.first().y)
                areaPath.moveTo(coords.first().x, paddingTop + chartHeight)
                areaPath.lineTo(coords.first().x, coords.first().y)

                for (i in 0 until coords.size - 1) {
                    val p0 = coords[i]
                    val p1 = coords[i + 1]
                    val controlX1 = p0.x + (p1.x - p0.x) / 2f
                    val controlY1 = p0.y
                    val controlX2 = p0.x + (p1.x - p0.x) / 2f
                    val controlY2 = p1.y

                    linePath.cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
                    areaPath.cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
                }

                areaPath.lineTo(coords.last().x, paddingTop + chartHeight)
                areaPath.close()

                // Draw Gradient Area (fades down)
                val areaBrush = Brush.verticalGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.35f),
                        primaryColor.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    startY = paddingTop,
                    endY = paddingTop + chartHeight
                )
                drawPath(path = areaPath, brush = areaBrush, style = Fill)

                // Draw Monotone Curve Stroke
                val lineBrush = Brush.horizontalGradient(
                    listOf(primaryColor, Color(0xFF6366F1), secondaryColor)
                )
                drawPath(
                    path = linePath,
                    brush = lineBrush,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // 4. Draw Data Point Dots
                coords.forEachIndexed { index, offset ->
                    val isHovered = activeScrubIndex == index
                    val radius = if (isHovered) 6.dp.toPx() else 4.dp.toPx()

                    // Glow or outer circle when active
                    if (isHovered) {
                        drawCircle(
                            color = primaryColor.copy(alpha = 0.25f),
                            radius = 12.dp.toPx(),
                            center = offset
                        )
                    }

                    // White outer border ring
                    drawCircle(
                        color = Color.White,
                        radius = radius + 2.dp.toPx(),
                        center = offset
                    )
                    // Colored Center Dot
                    drawCircle(
                        color = if (isHovered) EduAmberWarning else primaryColor,
                        radius = radius,
                        center = offset
                    )
                }

                // 5. Vertical Cursor Hairline for Active Touch Scrubber
                if (activeScrubIndex != null && activeScrubIndex in coords.indices) {
                    val activeX = coords[activeScrubIndex].x
                    drawLine(
                        color = primaryColor.copy(alpha = 0.8f),
                        start = Offset(activeX, paddingTop),
                        end = Offset(activeX, paddingTop + chartHeight),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = dashEffect
                    )
                }
            }
        }

        // Y-Axis Labels overlay
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("100%", "75%", "50%", "25%", "0%").forEach { label ->
                Text(
                    text = label,
                    fontSize = 10.sp,
                    color = textLabelColor,
                    modifier = Modifier.padding(bottom = 18.dp)
                )
            }
        }

        // X-Axis Labels row overlay
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 38.dp, end = 16.dp)
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            points.forEachIndexed { index, pt ->
                val isSelected = activeScrubIndex == index
                Text(
                    text = pt.label,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) primaryColor else textLabelColor,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * 2. Recharts Bar Chart: 과목별 학습 시간 계산 및 비중 비교
 * Room 데이터베이스의 study_logs에서 합산된 과목별 총 학습 분(minutes)을 시각화합니다.
 */
@Composable
fun RechartsSubjectBarChart(
    subjectStats: List<SubjectStudyStat>,
    highlightedSubjectId: Long?,
    onSelectSubject: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (subjectStats.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("과목별 학습 데이터가 없습니다.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val maxMinutes = (subjectStats.maxOfOrNull { it.totalMinutes } ?: 100).coerceAtLeast(60).toFloat()
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)

    Column(modifier = modifier) {
        // Bar canvas area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("recharts_bar_chart_canvas")
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val paddingLeft = 32.dp.toPx()
                val paddingRight = 16.dp.toPx()
                val paddingTop = 12.dp.toPx()
                val paddingBottom = 20.dp.toPx()

                val chartWidth = size.width - paddingLeft - paddingRight
                val chartHeight = size.height - paddingTop - paddingBottom

                // Grid lines (3 horizontal intervals)
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                for (i in 0..3) {
                    val y = paddingTop + chartHeight * (i / 3f)
                    drawLine(
                        color = gridColor,
                        start = Offset(paddingLeft, y),
                        end = Offset(size.width - paddingRight, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = dashEffect
                    )
                }

                // Render Bars
                val barCount = subjectStats.size
                val slotWidth = chartWidth / barCount
                val barWidth = (slotWidth * 0.48f).coerceIn(16.dp.toPx(), 36.dp.toPx())

                subjectStats.forEachIndexed { idx, stat ->
                    val isHighlighted = highlightedSubjectId == null || highlightedSubjectId == stat.subjectId
                    val barHeightRatio = (stat.totalMinutes / maxMinutes).coerceIn(0.08f, 1f)
                    val barH = chartHeight * barHeightRatio

                    val x = paddingLeft + idx * slotWidth + (slotWidth - barWidth) / 2f
                    val y = paddingTop + chartHeight - barH

                    val color = parseSubjectColor(stat.colorHex, stat.isWeak)
                    val effectiveColor = if (isHighlighted) color else color.copy(alpha = 0.3f)

                    // Rounded top bar
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            listOf(effectiveColor, effectiveColor.copy(alpha = 0.8f)),
                            startY = y,
                            endY = y + barH
                        ),
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barH),
                        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                    )
                }
            }
        }

        // Subject Bar Labels & Minutes Below
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 28.dp, end = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            subjectStats.forEach { stat ->
                val isHighlighted = highlightedSubjectId == null || highlightedSubjectId == stat.subjectId
                val color = parseSubjectColor(stat.colorHex, stat.isWeak)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSelectSubject(stat.subjectId) }
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "${stat.totalMinutes}분",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isHighlighted) color else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stat.subjectName,
                        fontSize = 12.sp,
                        fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
                        color = if (isHighlighted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1
                    )
                    Text(
                        text = "${(stat.percentOfTotalTime * 100).roundToInt()}%",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 3. Recharts Cumulative Line Chart: 일자별 누적 학습 시간 증가 곡선
 */
@Composable
fun RechartsCumulativeLineChart(
    points: List<ProgressTrendPoint>,
    activeScrubIndex: Int?,
    onScrubChanged: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("누적 학습 데이터가 없습니다.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val primaryColor = Color(0xFF6366F1)
    val maxMins = (points.maxOfOrNull { it.cumulativeMinutes } ?: 300).coerceAtLeast(120).toFloat()
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    val textLabelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .testTag("recharts_cumulative_chart_canvas")
            .pointerInput(points) {
                detectTapGestures { offset ->
                    val w = size.width - 56.dp.toPx()
                    val paddingStart = 38.dp.toPx()
                    val step = if (points.size > 1) w / (points.size - 1) else w
                    val idx = ((offset.x - paddingStart + step / 2) / step).toInt()
                    if (idx in points.indices) onScrubChanged(idx) else onScrubChanged(null)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val paddingLeft = 38.dp.toPx()
            val paddingRight = 16.dp.toPx()
            val paddingTop = 16.dp.toPx()
            val paddingBottom = 28.dp.toPx()

            val chartWidth = size.width - paddingLeft - paddingRight
            val chartHeight = size.height - paddingTop - paddingBottom

            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
            for (i in 0..3) {
                val y = paddingTop + chartHeight * (i / 3f)
                drawLine(
                    color = gridColor,
                    start = Offset(paddingLeft, y),
                    end = Offset(size.width - paddingRight, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dashEffect
                )
            }

            val stepX = if (points.size > 1) chartWidth / (points.size - 1) else chartWidth
            val coords = points.mapIndexed { idx, pt ->
                val x = paddingLeft + idx * stepX
                val ratio = (pt.cumulativeMinutes / maxMins).coerceIn(0.05f, 1f)
                val y = paddingTop + chartHeight * (1f - ratio)
                Offset(x, y)
            }

            if (coords.isNotEmpty()) {
                val path = Path()
                path.moveTo(coords.first().x, coords.first().y)
                for (i in 1 until coords.size) {
                    path.lineTo(coords[i].x, coords[i].y)
                }

                drawPath(
                    path = path,
                    color = primaryColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                coords.forEachIndexed { index, offset ->
                    val isHovered = activeScrubIndex == index
                    drawCircle(color = Color.White, radius = 5.dp.toPx(), center = offset)
                    drawCircle(color = if (isHovered) EduAmberWarning else primaryColor, radius = 3.5.dp.toPx(), center = offset)
                }
            }
        }

        // X-Axis
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 38.dp, end = 16.dp)
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            points.forEachIndexed { index, pt ->
                val isSelected = activeScrubIndex == index
                Text(
                    text = pt.label,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) primaryColor else textLabelColor
                )
            }
        }
    }
}

/**
 * Recharts Tooltip Card: 차트 터치 시 노출되는 인터랙티브 상세 정보 팝업
 */
@Composable
fun RechartsTooltipCard(point: ProgressTrendPoint) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = CardDefaults.outlinedCardBorder(),
        shadowElevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("recharts_tooltip_card")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${point.label}요일 세션 (${point.fullDateString})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
                Text(
                    text = "당일 총 ${point.dayMinutes}분 학습",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "누적 진도율: ${(point.progressPercent * 100).roundToInt()}%",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "누적 학습: ${point.cumulativeMinutes / 60}시간 ${point.cumulativeMinutes % 60}분",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (point.subjectMinutesMap.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    point.subjectMinutesMap.forEach { (subject, mins) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(EduIndigoPrimary)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$subject: ${mins}분",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Recharts Interactive Legend (클릭 시 해당 과목 강조/필터링 지원)
 */
@Composable
fun RechartsInteractiveLegend(
    subjectStats: List<SubjectStudyStat>,
    highlightedSubjectId: Long?,
    onSubjectClick: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "과목별 범례 (탭하여 특정 과목 강조)",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            subjectStats.forEach { stat ->
                val isSelected = highlightedSubjectId == stat.subjectId
                val color = parseSubjectColor(stat.colorHex, stat.isWeak)

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) color.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, color) else null,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSubjectClick(stat.subjectId) }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stat.subjectName,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) color else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RechartsKpiItem(
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
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
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

private fun parseSubjectColor(hex: String, isWeak: Boolean): Color {
    return try {
        if (isWeak) EduRedAlert else Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        if (isWeak) EduRedAlert else EduIndigoPrimary
    }
}
