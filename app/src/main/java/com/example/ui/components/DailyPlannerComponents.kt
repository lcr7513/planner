package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PlannerTodoEntity
import com.example.data.model.SubjectEntity
import com.example.data.model.TimeBlockEntity

@Composable
fun DailyTodoListCard(
    todos: List<PlannerTodoEntity>,
    onToggleTodo: (PlannerTodoEntity) -> Unit,
    onDeleteTodo: (PlannerTodoEntity) -> Unit,
    onAddTodoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val completedCount = todos.count { it.isCompleted }
    val totalCount = todos.size
    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_todo_list_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header with completion gauge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF3B82F6).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Checklist,
                            contentDescription = "플래너 To-Do",
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "오늘의 플래너 To-Do",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "달성률: $completedCount/$totalCount (${(progress * 100).toInt()}%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = onAddTodoClick,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "추가",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "할 일 추가",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = Color(0xFF2563EB),
                trackColor = Color(0xFFE2E8F0)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Todo Items List
            if (todos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "오늘 등록된 플래너 할 일이 없습니다.\n'+ 할 일 추가' 버튼을 눌러 목표를 세워보세요!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    todos.forEach { todo ->
                        TodoItemRow(
                            todo = todo,
                            onToggle = { onToggleTodo(todo) },
                            onDelete = { onDeleteTodo(todo) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TodoItemRow(
    todo: PlannerTodoEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(14.dp),
        color = if (todo.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = todo.isCompleted,
                onCheckedChange = { onToggle() },
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Subject badge
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ) {
                Text(
                    text = todo.subjectName,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = todo.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (todo.isCompleted) FontWeight.Normal else FontWeight.SemiBold,
                    textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (todo.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "목표 ${todo.targetMinutes}분",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "삭제",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * 10분 플래너 타임테이블 (Ten-Minute Study Timetable Grid)
 * 06:00부터 24:00 (익일 02:00까지 총 21시간)
 * 시간당 6개의 10분 셀
 */
@Composable
fun TenMinutePlannerCard(
    timeBlocks: List<TimeBlockEntity>,
    subjects: List<SubjectEntity>,
    selectedSubject: SubjectEntity?,
    onSelectSubject: (SubjectEntity) -> Unit,
    onCellClick: (hour: Int, blockIndex: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalFilledBlocks = timeBlocks.size
    val totalStudyMinutes = totalFilledBlocks * 10
    val studyHours = totalStudyMinutes / 60
    val studyRemainingMinutes = totalStudyMinutes % 60

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ten_minute_planner_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF8B5CF6).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "10분 타임테이블",
                            tint = Color(0xFF7C3AED),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "10분 플래너 타임테이블",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "기록된 순공 시간: ${studyHours}시간 ${studyRemainingMinutes}분",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF8B5CF6).copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "${totalFilledBlocks}칸 달성",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6D28D9),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Subject Color Selector (Palette)
            Text(
                text = "칠할 과목 색상 선택:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                subjects.forEach { sub ->
                    val isSelected = selectedSubject?.id == sub.id
                    val subColor = try {
                        Color(android.graphics.Color.parseColor(sub.colorHex))
                    } catch (e: Exception) {
                        Color(0xFF6366F1)
                    }

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelectSubject(sub) }
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) subColor else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(10.dp)
                            ),
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) subColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(subColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = sub.name,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Grid column: Hour on left, 6 ten-minute blocks on right
            // Hours 6 to 24
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Header row with 10m tick marks
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "시간",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(42.dp)
                    )
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        listOf(":00", ":10", ":20", ":30", ":40", ":50").forEach { tick ->
                            Text(
                                text = tick,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Grid rows: Hours 6 to 24 (6 AM to 12 AM)
                (6..24).forEach { hour ->
                    val hourLabel = when {
                        hour < 10 -> "0$hour:00"
                        hour == 24 -> "24:00"
                        else -> "$hour:00"
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = hourLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.width(42.dp)
                        )

                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            (0..5).forEach { blockIdx ->
                                val filledBlock = timeBlocks.find { it.hour == hour && it.blockIndex == blockIdx }
                                val isFilled = filledBlock != null

                                val blockColor = if (isFilled) {
                                    try {
                                        Color(android.graphics.Color.parseColor(filledBlock?.colorHex ?: "#6366F1"))
                                    } catch (e: Exception) {
                                        Color(0xFF6366F1)
                                    }
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(20.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(blockColor)
                                        .clickable { onCellClick(hour, blockIdx) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddPlannerTodoDialog(
    subjects: List<SubjectEntity>,
    onDismiss: () -> Unit,
    onConfirm: (title: String, subjectName: String, subjectId: Long?, minutes: Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf(subjects.firstOrNull()) }
    var targetMinutes by remember { mutableIntStateOf(45) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("오늘의 플래너 할 일 추가", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("할 일 내용 (예: 기출문제 5개 풀이)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))
                Text("과목 선택", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    subjects.forEach { sub ->
                        FilterChip(
                            selected = selectedSubject?.id == sub.id,
                            onClick = { selectedSubject = sub },
                            label = { Text(sub.name) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text("목표 공부 시간: ${targetMinutes}분", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(25, 45, 60, 90).forEach { mins ->
                        FilterChip(
                            selected = targetMinutes == mins,
                            onClick = { targetMinutes = mins },
                            label = { Text("${mins}분") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(
                            title,
                            selectedSubject?.name ?: "자율",
                            selectedSubject?.id,
                            targetMinutes
                        )
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("추가")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        }
    )
}
