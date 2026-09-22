package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.SubjectEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStudyDialog(
    subjects: List<SubjectEntity>,
    initialSubjectId: Long? = null,
    onDismiss: () -> Unit,
    onSave: (subjectId: Long, subjectName: String, minutes: Int, newUnits: Int, newScore: Int?, note: String) -> Unit
) {
    var selectedSubject by remember {
        mutableStateOf(subjects.find { it.id == initialSubjectId } ?: subjects.firstOrNull())
    }
    var expandedDropdown by remember { mutableStateOf(false) }

    var studyMinutes by remember { mutableIntStateOf(30) }
    var completedUnits by remember {
        mutableIntStateOf(selectedSubject?.completedUnits ?: 0)
    }
    var updatedScoreText by remember { mutableStateOf(selectedSubject?.currentScore?.toString() ?: "") }
    var memoText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("add_study_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "오늘의 학습 기록하기",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "닫기")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Subject Selector
                Text(text = "학습 과목 선택", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(6.dp))

                ExposedDropdownMenuBox(
                    expanded = expandedDropdown,
                    onExpandedChange = { expandedDropdown = !expandedDropdown }
                ) {
                    OutlinedTextField(
                        value = selectedSubject?.name ?: "과목 선택",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false }
                    ) {
                        subjects.forEach { sub ->
                            DropdownMenuItem(
                                text = { Text(text = "${sub.name} (현재 ${sub.currentScore}점)") },
                                onClick = {
                                    selectedSubject = sub
                                    completedUnits = sub.completedUnits
                                    updatedScoreText = sub.currentScore.toString()
                                    expandedDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Study Minutes quick buttons
                Text(text = "학습 시간 (분): $studyMinutes 분", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(15, 30, 45, 60, 90).forEach { mins ->
                        val isSelected = studyMinutes == mins
                        FilterChip(
                            selected = isSelected,
                            onClick = { studyMinutes = mins },
                            label = { Text("${mins}분") },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Completed Units
                val maxUnits = selectedSubject?.totalUnits ?: 10
                Text(
                    text = "완료한 단원 진도: $completedUnits / $maxUnits 단원",
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = { if (completedUnits > 0) completedUnits-- },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("-1 단원")
                    }

                    Text(
                        text = "$completedUnits 단원",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )

                    Button(
                        onClick = { if (completedUnits < maxUnits) completedUnits++ },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("+1 단원 완료")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Score Update
                Text(text = "최근 테스트 / 이해도 점수 (선택)", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = updatedScoreText,
                    onValueChange = { updatedScoreText = it.filter { char -> char.isDigit() }.take(3) },
                    placeholder = { Text("예: 85") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Notes
                Text(text = "학습 내용 및 메모", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = memoText,
                    onValueChange = { memoText = it },
                    placeholder = { Text("예: 이차함수 표준형 변환 연습문제 15개 풀이") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val sub = selectedSubject ?: return@Button
                        val parsedScore = updatedScoreText.toIntOrNull()?.coerceIn(0, 100)
                        val note = memoText.ifBlank { "${sub.name} $studyMinutes 분 집중 학습" }
                        onSave(sub.id, sub.name, studyMinutes, completedUnits, parsedScore, note)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("학습 기록 저장하기", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AddSubjectDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, currentScore: Int, targetScore: Int, totalUnits: Int, weakTopic: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var currentScoreText by remember { mutableStateOf("60") }
    var targetScoreText by remember { mutableStateOf("90") }
    var totalUnitsText by remember { mutableStateOf("10") }
    var weakTopic by remember { mutableStateOf("") }

    val presetNames = listOf("수학", "영어", "국어", "과학", "사회·역사", "정보·코딩")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("add_subject_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "새 과목 및 학습 목표 등록",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "닫기")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(text = "과목명", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("과목명 입력 (예: 수학, 화학)") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetNames.take(4).forEach { preset ->
                        AssistChip(
                            onClick = { name = preset },
                            label = { Text(preset, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "현재 점수 (0-100)", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = currentScoreText,
                            onValueChange = { currentScoreText = it.filter { c -> c.isDigit() }.take(3) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "목표 점수", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = targetScoreText,
                            onValueChange = { targetScoreText = it.filter { c -> c.isDigit() }.take(3) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(text = "총 교과 단원 수", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = totalUnitsText,
                    onValueChange = { totalUnitsText = it.filter { c -> c.isDigit() }.take(2) },
                    placeholder = { Text("예: 10") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(text = "집중 보완할 취약 단원/토픽", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = weakTopic,
                    onValueChange = { weakTopic = it },
                    placeholder = { Text("예: 함수와 그래프, 고난도 어휘") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            val cur = currentScoreText.toIntOrNull()?.coerceIn(0, 100) ?: 60
                            val target = targetScoreText.toIntOrNull()?.coerceIn(0, 100) ?: 90
                            val units = totalUnitsText.toIntOrNull()?.coerceIn(1, 50) ?: 10
                            onSave(name, cur, target, units, weakTopic)
                            onDismiss()
                        }
                    },
                    enabled = name.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("과목 등록하기", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
