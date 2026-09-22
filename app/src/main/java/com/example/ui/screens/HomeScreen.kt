package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RecommendationEntity
import com.example.data.model.RecommendationType
import com.example.data.model.SubjectEntity
import com.example.ui.components.*
import com.example.ui.theme.EduAmberWarning
import com.example.ui.theme.EduGreenSuccess
import com.example.ui.theme.EduIndigoPrimary
import com.example.ui.theme.EduRedAlert
import com.example.ui.viewmodel.LearningUiState
import com.example.ui.viewmodel.LearningViewModel

data class QuizLaunchConfig(
    val subject: SubjectEntity,
    val isAiGenerated: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: LearningViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    var showAddStudyDialog by remember { mutableStateOf(false) }
    var selectedSubjectForStudy by remember { mutableStateOf<Long?>(null) }
    var showAddSubjectDialog by remember { mutableStateOf(false) }

    var quizLaunchConfig by remember { mutableStateOf<QuizLaunchConfig?>(null) }
    var conceptTargetRec by remember { mutableStateOf<RecommendationEntity?>(null) }
    var showDailyReportDialog by remember { mutableStateOf(false) }
    var showAiTutorDialog by remember { mutableStateOf(false) }
    var aiTutorSubject by remember { mutableStateOf("수학") }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen_root"),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "학습매니저",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "${uiState.studentName} · ${uiState.gradeLevel}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAiTutorDialog = true },
                        modifier = Modifier.testTag("open_ai_tutor_appbar_button")
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "AI 튜터",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "AI 튜터",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = { showDailyReportDialog = true },
                        modifier = Modifier.testTag("open_daily_report_button")
                    ) {
                        BadgedBox(
                            badge = {
                                Badge { Text("리포트") }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "일간 리포트 알림",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(
                        onClick = { showAddSubjectDialog = true },
                        modifier = Modifier.testTag("add_subject_appbar_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircleOutline,
                            contentDescription = "과목 추가",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                windowInsets = WindowInsets.navigationBars
            ) {
                NavigationBarItem(
                    selected = uiState.selectedTab == 0,
                    onClick = { viewModel.setSelectedTab(0) },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "대시보드") },
                    label = { Text("대시보드") },
                    modifier = Modifier.testTag("nav_tab_dashboard")
                )
                NavigationBarItem(
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.setSelectedTab(1) },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = "진도 현황") },
                    label = { Text("진도 현황") },
                    modifier = Modifier.testTag("nav_tab_progress")
                )
                NavigationBarItem(
                    selected = uiState.selectedTab == 2,
                    onClick = { viewModel.setSelectedTab(2) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (uiState.weakSubjects.isNotEmpty()) {
                                    Badge { Text("${uiState.weakSubjects.size}") }
                                }
                            }
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "취약점 추천")
                        }
                    },
                    label = { Text("취약점 추천") },
                    modifier = Modifier.testTag("nav_tab_recommendations")
                )
                NavigationBarItem(
                    selected = uiState.selectedTab == 3,
                    onClick = { viewModel.setSelectedTab(3) },
                    icon = { Icon(Icons.Default.School, contentDescription = "AI 튜터") },
                    label = { Text("AI 튜터") },
                    modifier = Modifier.testTag("nav_tab_ai_tutor")
                )
            }
        },
        floatingActionButton = {
            if (uiState.selectedTab != 3) {
                FloatingActionButton(
                    onClick = {
                        selectedSubjectForStudy = null
                        showAddStudyDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("fab_add_study")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.EditNote, contentDescription = "학습 기록")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "학습 기록", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (uiState.selectedTab) {
                0 -> DashboardTabContent(
                    uiState = uiState,
                    onSubjectAddStudy = { subjectId ->
                        selectedSubjectForStudy = subjectId
                        showAddStudyDialog = true
                    },
                    onSubjectClinic = { subject ->
                        quizLaunchConfig = QuizLaunchConfig(subject, isAiGenerated = false)
                    },
                    onToggleRecommendation = { recId, isCompleted ->
                        viewModel.toggleRecommendation(recId, isCompleted)
                    },
                    onRecommendationAction = { rec ->
                        if (rec.type == RecommendationType.CLINIC_QUIZ) {
                            val targetSub = uiState.subjects.find { it.id == rec.subjectId }
                                ?: uiState.subjects.find { it.name == rec.subjectName }
                                ?: SubjectEntity(name = rec.subjectName, currentScore = 60, totalUnits = 10, completedUnits = 5, totalStudyMinutes = 100)
                            quizLaunchConfig = QuizLaunchConfig(targetSub, isAiGenerated = false)
                        } else {
                            conceptTargetRec = rec
                        }
                    },
                    onGoToWeakTab = { viewModel.setSelectedTab(2) },
                    onOpenDailyReport = { showDailyReportDialog = true }
                )

                1 -> ProgressTabContent(
                    uiState = uiState,
                    onSubjectAddStudy = { subjectId ->
                        selectedSubjectForStudy = subjectId
                        showAddStudyDialog = true
                    },
                    onSubjectClinic = { subject ->
                        quizLaunchConfig = QuizLaunchConfig(subject, isAiGenerated = false)
                    },
                    onAddNewSubject = { showAddSubjectDialog = true }
                )

                2 -> RecommendationTabContent(
                    uiState = uiState,
                    onToggleRecommendation = { recId, isCompleted ->
                        viewModel.toggleRecommendation(recId, isCompleted)
                    },
                    onRecommendationAction = { rec ->
                        if (rec.type == RecommendationType.CLINIC_QUIZ) {
                            val targetSub = uiState.subjects.find { it.id == rec.subjectId }
                                ?: uiState.subjects.find { it.name == rec.subjectName }
                                ?: SubjectEntity(name = rec.subjectName, currentScore = 60, totalUnits = 10, completedUnits = 5, totalStudyMinutes = 100)
                            quizLaunchConfig = QuizLaunchConfig(targetSub, isAiGenerated = false)
                        } else {
                            conceptTargetRec = rec
                        }
                    },
                    onGenerateSmartPlan = {
                        val firstWeak = uiState.weakSubjects.firstOrNull()
                        if (firstWeak != null) {
                            viewModel.requestAIGeneratedClinic(firstWeak)
                        }
                    },
                    onStartQuiz = { subject ->
                        quizLaunchConfig = QuizLaunchConfig(subject, isAiGenerated = false)
                    },
                    onStartAiQuiz = { subject ->
                        quizLaunchConfig = QuizLaunchConfig(subject, isAiGenerated = true)
                    },
                    onOpenAiTutor = { subjectName ->
                        aiTutorSubject = subjectName
                        showAiTutorDialog = true
                    }
                )

                3 -> AiTutorScreenContent(
                    initialSubject = aiTutorSubject
                )
            }
        }
    }

    // Dialogs
    if (showAddStudyDialog && uiState.subjects.isNotEmpty()) {
        AddStudyDialog(
            subjects = uiState.subjects,
            initialSubjectId = selectedSubjectForStudy,
            onDismiss = { showAddStudyDialog = false },
            onSave = { subId, subName, mins, newUnits, newScore, note ->
                viewModel.logStudySession(subId, subName, mins, newUnits, newScore, note)
            }
        )
    }

    if (showAddSubjectDialog) {
        AddSubjectDialog(
            onDismiss = { showAddSubjectDialog = false },
            onSave = { name, curScore, targetScore, totalUnits, weakTopic ->
                viewModel.addSubject(name, curScore, targetScore, totalUnits, weakTopic)
            }
        )
    }

    quizLaunchConfig?.let { config ->
        DiagnosticQuizDialog(
            subjectName = config.subject.name,
            subjectId = config.subject.id,
            weakTopic = config.subject.weakTopic,
            currentScore = config.subject.currentScore,
            isAiGenerated = config.isAiGenerated,
            onDismiss = { quizLaunchConfig = null },
            onQuizComplete = { subId, subName, bonus ->
                viewModel.submitClinicQuizResult(subId, subName, bonus)
            }
        )
    }

    conceptTargetRec?.let { targetRec ->
        ConceptSummaryDialog(
            recommendation = targetRec,
            onDismiss = { conceptTargetRec = null }
        )
    }

    if (showDailyReportDialog) {
        DailyReportDialog(
            uiState = uiState,
            onDismiss = { showDailyReportDialog = false }
        )
    }

    if (showAiTutorDialog) {
        AiTutorDialog(
            initialSubject = aiTutorSubject,
            onDismiss = { showAiTutorDialog = false }
        )
    }
}

@Composable
fun DashboardTabContent(
    uiState: LearningUiState,
    onSubjectAddStudy: (Long) -> Unit,
    onSubjectClinic: (SubjectEntity) -> Unit,
    onToggleRecommendation: (Long, Boolean) -> Unit,
    onRecommendationAction: (RecommendationEntity) -> Unit,
    onGoToWeakTab: () -> Unit,
    onOpenDailyReport: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Daily Report Quick Banner Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenDailyReport() }
                    .testTag("daily_report_banner_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Summarize,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "오늘의 일간 성취 리포트",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    Text(
                                        text = "알림",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = "오늘 학습 시간 및 성취 요약 푸시 알림 받기",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    FilledTonalButton(
                        onClick = onOpenDailyReport,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("확인", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Interactive Learning Progress Dashboard (Circular Chart & Multi-Progress Bar Switcher)
        item {
            LearningProgressDashboardCard(
                overallProgress = uiState.overallProgress,
                averageScore = uiState.averageScore,
                streakDays = uiState.streakDays,
                totalMinutes = uiState.totalStudyMinutes,
                weakCount = uiState.weakSubjects.size,
                subjects = uiState.subjects
            )
        }

        // Weak subject diagnosis alert card (if any)
        if (uiState.weakSubjects.isNotEmpty()) {
            item {
                WeakSubjectDiagnosisBanner(
                    weakSubjects = uiState.weakSubjects,
                    onGeneratePlan = onGoToWeakTab
                )
            }
        }

        // Weekly study bar chart
        item {
            WeeklyStudyBarChart(weeklyMinutes = uiState.weeklyMinutes)
        }

        // Today's personalized recommendation missions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🎯 오늘의 취약 과목 맞춤 추천",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onGoToWeakTab) {
                    Text("전체보기")
                }
            }
        }

        items(uiState.recommendations.take(3), key = { it.id }) { rec ->
            RecommendationCard(
                recommendation = rec,
                onToggleComplete = { onToggleRecommendation(rec.id, rec.isCompleted) },
                onActionClick = { onRecommendationAction(rec) }
            )
        }

        // Recent study logs header
        if (uiState.recentLogs.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "📝 최근 학습 기록",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(uiState.recentLogs.take(4), key = { it.id }) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = log.dateString,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = log.subjectName,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = log.topicDescription,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = "${log.minutes}분",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProgressTabContent(
    uiState: LearningUiState,
    onSubjectAddStudy: (Long) -> Unit,
    onSubjectClinic: (SubjectEntity) -> Unit,
    onAddNewSubject: () -> Unit
) {
    var filterType by remember { mutableIntStateOf(0) } // 0: 전체, 1: 취약 과목, 2: 양호/우수

    val filteredSubjects = when (filterType) {
        1 -> uiState.subjects.filter { it.isWeak || it.currentScore < 70 }
        2 -> uiState.subjects.filter { it.currentScore >= 70 }
        else -> uiState.subjects
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "과목별 상세 학습 진도",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "단원 이수율과 목표 점수 달성 현황을 한눈에 파악하세요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Filter chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterType == 0,
                    onClick = { filterType = 0 },
                    label = { Text("전체 (${uiState.subjects.size})") },
                    shape = RoundedCornerShape(10.dp)
                )
                FilterChip(
                    selected = filterType == 1,
                    onClick = { filterType = 1 },
                    label = { Text("취약 과목 (${uiState.weakSubjects.size})") },
                    shape = RoundedCornerShape(10.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EduRedAlert.copy(alpha = 0.15f),
                        selectedLabelColor = EduRedAlert
                    )
                )
                FilterChip(
                    selected = filterType == 2,
                    onClick = { filterType = 2 },
                    label = { Text("양호·우수 (${uiState.subjects.size - uiState.weakSubjects.size})") },
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        items(filteredSubjects, key = { it.id }) { subject ->
            SubjectProgressCard(
                subject = subject,
                onAddStudyClick = { onSubjectAddStudy(subject.id) },
                onClinicClick = { onSubjectClinic(subject) }
            )
        }

        item {
            OutlinedButton(
                onClick = onAddNewSubject,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("새로운 과목 및 목표 추가하기", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun RecommendationTabContent(
    uiState: LearningUiState,
    onToggleRecommendation: (Long, Boolean) -> Unit,
    onRecommendationAction: (RecommendationEntity) -> Unit,
    onGenerateSmartPlan: () -> Unit,
    onStartQuiz: (SubjectEntity) -> Unit,
    onStartAiQuiz: (SubjectEntity) -> Unit = onStartQuiz,
    onOpenAiTutor: (String) -> Unit = {}
) {
    var selectedCategory by remember { mutableStateOf<RecommendationType?>(null) }

    val filteredList = if (selectedCategory == null) {
        uiState.recommendations
    } else {
        uiState.recommendations.filter { it.type == selectedCategory }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "취약 과목 보완 추천 시스템",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "학습 데이터와 오답 분석을 바탕으로 처방된 맞춤 솔루션입니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Diagnosis banner
        item {
            WeakSubjectDiagnosisBanner(
                weakSubjects = uiState.weakSubjects,
                onGeneratePlan = onGenerateSmartPlan
            )
        }

        // Gemini AI Smart Tools Showcase
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Gemini AI 실시간 스마트 학습",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "실시간 생성",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "AI가 취약점 데이터를 분석하여 즉석에서 새로운 클리닉 퀴즈를 생성하고, 1:1 질문 답변 튜터링을 제공합니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val target = uiState.weakSubjects.firstOrNull() ?: uiState.subjects.firstOrNull()
                                if (target != null) {
                                    onStartAiQuiz(target)
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("AI 퀴즈 생성", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                val subName = uiState.weakSubjects.firstOrNull()?.name ?: "수학"
                                onOpenAiTutor(subName)
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.School, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("1:1 AI 튜터", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Quick clinic start cards for each weak subject
        if (uiState.weakSubjects.isNotEmpty()) {
            item {
                Text(
                    text = "⚡ 즉시 취약점 극복 퀴즈 클리닉",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(uiState.weakSubjects, key = { "weak_${it.id}" }) { weakSub ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = EduRedAlert.copy(alpha = 0.15f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Quiz,
                                            contentDescription = null,
                                            tint = EduRedAlert,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "${weakSub.name} 맞춤 클리닉",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = "취약 단원: ${weakSub.weakTopic} · 현재 ${weakSub.currentScore}점",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onStartAiQuiz(weakSub) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("AI 실시간 생성", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { onStartQuiz(weakSub) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(0.9f),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Text("기본 문제", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }

                            IconButton(
                                onClick = { onOpenAiTutor(weakSub.name) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Chat,
                                    contentDescription = "AI 튜터 질문",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Category filter chips
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text("전체") },
                    shape = RoundedCornerShape(8.dp)
                )
                FilterChip(
                    selected = selectedCategory == RecommendationType.CLINIC_QUIZ,
                    onClick = { selectedCategory = RecommendationType.CLINIC_QUIZ },
                    label = { Text("클리닉 퀴즈") },
                    shape = RoundedCornerShape(8.dp)
                )
                FilterChip(
                    selected = selectedCategory == RecommendationType.CONCEPT_REVIEW,
                    onClick = { selectedCategory = RecommendationType.CONCEPT_REVIEW },
                    label = { Text("개념 복습") },
                    shape = RoundedCornerShape(8.dp)
                )
                FilterChip(
                    selected = selectedCategory == RecommendationType.DAILY_MISSION,
                    onClick = { selectedCategory = RecommendationType.DAILY_MISSION },
                    label = { Text("1일 과제") },
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        // Recommendation List
        items(filteredList, key = { it.id }) { rec ->
            RecommendationCard(
                recommendation = rec,
                onToggleComplete = { onToggleRecommendation(rec.id, rec.isCompleted) },
                onActionClick = { onRecommendationAction(rec) }
            )
        }
    }
}
