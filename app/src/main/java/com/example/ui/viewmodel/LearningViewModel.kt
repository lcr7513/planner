package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.model.DDayGoalEntity
import com.example.data.model.DailyStudygramReport
import com.example.data.model.PlannerTodoEntity
import com.example.data.model.PomodoroMode
import com.example.data.model.PomodoroTimerState
import com.example.data.model.ProgressTrendPoint
import com.example.data.model.RecommendationEntity
import com.example.data.model.StudyAnalyticsSummary
import com.example.data.model.StudyLogEntity
import com.example.data.model.SubjectEntity
import com.example.data.model.SubjectStudyStat
import com.example.data.model.TimeBlockEntity
import com.example.data.repository.CloudSyncRepository
import com.example.data.repository.LearningRepository
import com.example.data.service.GeminiStudyAnalyzer
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class StudentProfile(
    val name: String = "김민수 학생",
    val gradeLevel: String = "중학 3학년 (목표: 고입/내신 대비)"
)

data class LearningUiState(
    val studentName: String = "김민수 학생",
    val gradeLevel: String = "중학 3학년 (목표: 고입/내신 대비)",
    val subjects: List<SubjectEntity> = emptyList(),
    val weakSubjects: List<SubjectEntity> = emptyList(),
    val recommendations: List<RecommendationEntity> = emptyList(),
    val activeRecommendations: List<RecommendationEntity> = emptyList(),
    val completedRecommendationsCount: Int = 0,
    val hideCompletedRecommendations: Boolean = true,
    val recentLogs: List<StudyLogEntity> = emptyList(),
    val totalStudyMinutes: Int = 0,
    val streakDays: Int = 7,
    val selectedTab: Int = 0, // 0: 종합 홈, 1: 플래너 & 10분 타임테이블, 2: 뽀모도로 & D-Day, 3: 진도 시각화, 4: 취약 보완 추천
    val overallProgress: Float = 0f,
    val averageScore: Int = 0,
    val weeklyMinutes: List<Int> = listOf(65, 45, 50, 40, 80, 60, 75), // Mon-Sun
    val isAnalyzingWithGemini: Boolean = false,
    val aiAnalysisSummary: String? = null,
    val analyticsSummary: StudyAnalyticsSummary = StudyAnalyticsSummary(),

    // --- 상용 플래너 고도화 기능 상태 ---
    val ddayGoals: List<DDayGoalEntity> = emptyList(),
    val plannerTodos: List<PlannerTodoEntity> = emptyList(),
    val timeBlocks: List<TimeBlockEntity> = emptyList(),
    val pomodoroState: PomodoroTimerState = PomodoroTimerState(),
    val firebaseUser: FirebaseUser? = null,
    val isSyncing: Boolean = false,
    val syncMessage: String? = null,
    val lastBackupTime: Long? = null,
    val selectedTimeBlockSubject: SubjectEntity? = null,
    val studygramReport: DailyStudygramReport? = null,
    val showStudygramDialog: Boolean = false,
    val showAddDDayDialog: Boolean = false,
    val showAddTodoDialog: Boolean = false,
    val showCloudAccountDialog: Boolean = false
)

class LearningViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: LearningRepository
    private val cloudSyncRepository: CloudSyncRepository
    private val geminiStudyAnalyzer = GeminiStudyAnalyzer()

    init {
        val db = AppDatabase.getDatabase(application, viewModelScope)
        repository = LearningRepository(
            subjectDao = db.subjectDao(),
            studyLogDao = db.studyLogDao(),
            recommendationDao = db.recommendationDao(),
            plannerDao = db.plannerDao()
        )
        cloudSyncRepository = CloudSyncRepository(application)
    }

    private val _selectedTab = MutableStateFlow(0)
    private val _streakDays = MutableStateFlow(7)
    private val _studentProfile = MutableStateFlow(StudentProfile())
    private val _hideCompletedRecs = MutableStateFlow(true)
    private val _isAnalyzing = MutableStateFlow(false)
    private val _aiAnalysisSummary = MutableStateFlow<String?>(null)

    // Planner, Pomodoro, and Cloud UI States
    private val _pomodoroState = MutableStateFlow(PomodoroTimerState())
    private var timerJob: Job? = null

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    private val _isSyncing = MutableStateFlow(false)
    private val _syncMessage = MutableStateFlow<String?>(null)
    private val _lastBackupTime = MutableStateFlow<Long?>(null)
    private val _selectedTimeBlockSubject = MutableStateFlow<SubjectEntity?>(null)
    private val _studygramReport = MutableStateFlow<DailyStudygramReport?>(null)
    private val _showStudygramDialog = MutableStateFlow(false)
    private val _showAddDDayDialog = MutableStateFlow(false)
    private val _showAddTodoDialog = MutableStateFlow(false)
    private val _showCloudAccountDialog = MutableStateFlow(false)

    init {
        // Observe Firebase Auth state
        viewModelScope.launch {
            cloudSyncRepository.observeAuthState().collect { user ->
                _currentUser.value = user
            }
        }
    }

    // Combine uiConfig
    data class UiExtraConfig(
        val tab: Int,
        val hideCompleted: Boolean,
        val profile: StudentProfile,
        val isAnalyzing: Boolean,
        val analysisSummary: String?
    )

    private val _uiConfig = combine(
        _selectedTab,
        _hideCompletedRecs,
        _studentProfile,
        _isAnalyzing,
        _aiAnalysisSummary
    ) { tab, hideCompleted, profile, isAnalyzing, summary ->
        UiExtraConfig(tab, hideCompleted, profile, isAnalyzing, summary)
    }

    // Combine Planner & Cloud dynamic state
    data class PlannerExtraConfig(
        val pomodoro: PomodoroTimerState,
        val user: FirebaseUser?,
        val syncing: Boolean,
        val syncMsg: String?,
        val backupTime: Long?,
        val timeBlockSub: SubjectEntity?,
        val report: DailyStudygramReport?,
        val showReport: Boolean,
        val showDDay: Boolean,
        val showTodo: Boolean,
        val showCloud: Boolean
    )

    private val _plannerConfig = combine(
        combine(_pomodoroState, _currentUser, _isSyncing, _syncMessage, _lastBackupTime) { pomodoro, user, syncing, syncMsg, backupTime ->
            Pair(Pair(pomodoro, user), Triple(syncing, syncMsg, backupTime))
        },
        combine(_selectedTimeBlockSubject, _studygramReport, _showStudygramDialog, _showAddDDayDialog, _showAddTodoDialog) { sub, report, showReport, showDDay, showTodo ->
            Triple(sub, report, Triple(showReport, showDDay, showTodo))
        },
        _showCloudAccountDialog
    ) { part1, part2, showCloud ->
        val pomodoro = part1.first.first
        val user = part1.first.second
        val syncing = part1.second.first
        val syncMsg = part1.second.second
        val backupTime = part1.second.third

        val timeBlockSub = part2.first
        val report = part2.second
        val showReport = part2.third.first
        val showDDay = part2.third.second
        val showTodo = part2.third.third

        PlannerExtraConfig(
            pomodoro = pomodoro,
            user = user,
            syncing = syncing,
            syncMsg = syncMsg,
            backupTime = backupTime,
            timeBlockSub = timeBlockSub,
            report = report,
            showReport = showReport,
            showDDay = showDDay,
            showTodo = showTodo,
            showCloud = showCloud
        )
    }

    private val todayDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(Date())

    val uiState: StateFlow<LearningUiState> = combine(
        combine(repository.allSubjects, repository.allRecommendations, repository.allLogs) { s, r, l -> Triple(s, r, l) },
        combine(repository.allDDayGoals, repository.getTodosByDate(todayDateStr), repository.getTimeBlocksByDate(todayDateStr)) { d, t, b -> Triple(d, t, b) },
        _uiConfig,
        _plannerConfig
    ) { baseData, plannerData, extraConfig, plannerConfig ->
        val subjects = baseData.first
        val recommendations = baseData.second
        val logs = baseData.third

        val ddayGoals = plannerData.first
        val plannerTodos = plannerData.second
        val timeBlocks = plannerData.third

        val weak = subjects.filter { it.currentScore < 70 || it.isWeak }
        val totalUnits = subjects.sumOf { it.totalUnits }
        val completedUnits = subjects.sumOf { it.completedUnits }
        val overallProg = if (totalUnits > 0) completedUnits.toFloat() / totalUnits.toFloat() else 0f
        val avgScore = if (subjects.isNotEmpty()) subjects.map { it.currentScore }.average().toInt() else 0

        val completedCount = recommendations.count { it.isCompleted }
        val activeRecs = recommendations.filter { !it.isCompleted }

        // Calculate weekly minutes from logs
        val dayMinutes = IntArray(7) { 0 }
        logs.forEach { log ->
            val idx = log.dayOfWeekIndex.coerceIn(0, 6)
            dayMinutes[idx] += log.minutes
        }
        val weeklyList = if (dayMinutes.all { it == 0 }) {
            listOf(65, 45, 50, 40, 80, 60, 75)
        } else {
            dayMinutes.toList()
        }

        val totalMins = subjects.sumOf { it.totalStudyMinutes }

        // --- Room DB 기반 과목별 학습 시간 계산 (Subject-wise Study Time Calculation) ---
        val subjectStats = subjects.map { subject ->
            val subjectLogs = logs.filter { it.subjectId == subject.id || it.subjectName == subject.name }
            val logsMinutes = subjectLogs.sumOf { it.minutes }
            val totalSubjectMinutes = maxOf(logsMinutes, subject.totalStudyMinutes)
            val sessionCount = maxOf(subjectLogs.size, if (totalSubjectMinutes > 0) 1 else 0)

            val progPercent = if (subject.totalUnits > 0) subject.completedUnits.toFloat() / subject.totalUnits.toFloat() else 0f
            SubjectStudyStat(
                subjectId = subject.id,
                subjectName = subject.name,
                colorHex = subject.colorHex,
                totalMinutes = totalSubjectMinutes,
                sessionCount = sessionCount,
                progressPercent = progPercent,
                completedUnits = subject.completedUnits,
                totalUnits = subject.totalUnits,
                currentScore = subject.currentScore,
                targetScore = subject.targetScore,
                percentOfTotalTime = 0f,
                isWeak = subject.isWeak || subject.currentScore < 70
            )
        }

        val grandTotalMinutes = subjectStats.sumOf { it.totalMinutes }.coerceAtLeast(1)
        val normalizedStats = subjectStats.map { stat ->
            stat.copy(percentOfTotalTime = stat.totalMinutes.toFloat() / grandTotalMinutes.toFloat())
        }

        // --- Room DB 기반 학습 진도 및 시간 변화 추이 계산 (Progress Trend & Cumulative Time) ---
        val daysLabels = listOf("월", "화", "수", "목", "금", "토", "일")
        var runningCumulative = 0
        val trendPoints = (0..6).map { dayIdx ->
            val dayLogs = logs.filter { it.dayOfWeekIndex == dayIdx }
            val dayM = if (dayLogs.isNotEmpty()) dayLogs.sumOf { it.minutes } else weeklyList.getOrElse(dayIdx) { 45 }
            runningCumulative += dayM

            val subjectMap = mutableMapOf<String, Int>()
            dayLogs.forEach { log ->
                subjectMap[log.subjectName] = (subjectMap[log.subjectName] ?: 0) + log.minutes
            }

            val estimatedProg = if (dayIdx == 6) overallProg else (overallProg * (0.35f + 0.65f * ((dayIdx + 1) / 7f))).coerceIn(0f, 1f)

            ProgressTrendPoint(
                label = daysLabels[dayIdx],
                fullDateString = "2026-10-14 (${daysLabels[dayIdx]})",
                timestamp = System.currentTimeMillis() - (6 - dayIdx) * 86400000L,
                dayMinutes = dayM,
                cumulativeMinutes = runningCumulative,
                progressPercent = estimatedProg,
                subjectMinutesMap = subjectMap,
                recentScore = avgScore
            )
        }

        val topSubject = normalizedStats.maxByOrNull { it.totalMinutes }
        val weakMinutes = normalizedStats.filter { it.isWeak }.sumOf { it.totalMinutes }
        val weakRatio = if (grandTotalMinutes > 0) weakMinutes.toFloat() / grandTotalMinutes.toFloat() else 0f
        val dailyAvg = if (trendPoints.isNotEmpty()) runningCumulative / trendPoints.size else 0

        val analyticsSummary = StudyAnalyticsSummary(
            subjectStats = normalizedStats,
            totalCalculatedMinutes = grandTotalMinutes,
            trendPoints = trendPoints,
            topSubject = topSubject,
            weakSubjectMinutes = weakMinutes,
            weakSubjectRatio = weakRatio,
            dailyAverageMinutes = dailyAvg
        )

        LearningUiState(
            studentName = extraConfig.profile.name,
            gradeLevel = extraConfig.profile.gradeLevel,
            subjects = subjects,
            weakSubjects = weak,
            recommendations = recommendations,
            activeRecommendations = activeRecs,
            completedRecommendationsCount = completedCount,
            hideCompletedRecommendations = extraConfig.hideCompleted,
            recentLogs = logs.take(7),
            totalStudyMinutes = totalMins,
            streakDays = _streakDays.value,
            selectedTab = extraConfig.tab,
            overallProgress = overallProg,
            averageScore = avgScore,
            weeklyMinutes = weeklyList,
            isAnalyzingWithGemini = extraConfig.isAnalyzing,
            aiAnalysisSummary = extraConfig.analysisSummary,
            analyticsSummary = analyticsSummary,
            // Planner & Cloud extensions
            ddayGoals = ddayGoals,
            plannerTodos = plannerTodos,
            timeBlocks = timeBlocks,
            pomodoroState = plannerConfig.pomodoro,
            firebaseUser = plannerConfig.user,
            isSyncing = plannerConfig.syncing,
            syncMessage = plannerConfig.syncMsg,
            lastBackupTime = plannerConfig.backupTime,
            selectedTimeBlockSubject = plannerConfig.timeBlockSub ?: subjects.firstOrNull(),
            studygramReport = plannerConfig.report,
            showStudygramDialog = plannerConfig.showReport,
            showAddDDayDialog = plannerConfig.showDDay,
            showAddTodoDialog = plannerConfig.showTodo,
            showCloudAccountDialog = plannerConfig.showCloud
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LearningUiState()
    )

    fun setSelectedTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun updateStudentProfile(name: String, grade: String) {
        val current = _studentProfile.value
        _studentProfile.value = current.copy(
            name = if (name.isNotBlank()) name else current.name,
            gradeLevel = if (grade.isNotBlank()) grade else current.gradeLevel
        )
    }

    // ==========================================
    // 1. 뽀모도로 타이머 로직 (Pomodoro Timer)
    // ==========================================
    fun togglePomodoroTimer() {
        val current = _pomodoroState.value
        if (current.isRunning) {
            timerJob?.cancel()
            _pomodoroState.value = current.copy(isRunning = false)
        } else {
            _pomodoroState.value = current.copy(isRunning = true)
            timerJob = viewModelScope.launch {
                while (_pomodoroState.value.isRunning && _pomodoroState.value.remainingSeconds > 0) {
                    delay(1000L)
                    val s = _pomodoroState.value
                    if (s.remainingSeconds > 1) {
                        _pomodoroState.value = s.copy(remainingSeconds = s.remainingSeconds - 1)
                    } else {
                        onPomodoroCompleted()
                    }
                }
            }
        }
    }

    private fun onPomodoroCompleted() {
        val s = _pomodoroState.value
        timerJob?.cancel()
        val isBreakNext = !s.isBreak
        val completedSessionCount = if (!s.isBreak) s.completedSessions + 1 else s.completedSessions
        val todayCount = if (!s.isBreak) s.todayPomodoroCount + 1 else s.todayPomodoroCount

        // 집중 세션 완료 시 Room DB 학습 기록에 자동 기록
        if (!s.isBreak) {
            viewModelScope.launch {
                val focusMins = s.selectedMode.focusMinutes
                val log = StudyLogEntity(
                    subjectId = s.selectedSubjectId ?: 1L,
                    subjectName = s.selectedSubjectName,
                    dateString = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(Date()),
                    minutes = focusMins,
                    dayOfWeekIndex = 2,
                    topicDescription = "뽀모도로 [${s.selectedMode.title}] 몰입 학습 완료"
                )
                repository.insertStudyLog(log)
                s.selectedSubjectId?.let { subId ->
                    repository.updateProgress(subId, 0, focusMins)
                }
            }
        }

        val nextTotalSecs = if (isBreakNext) s.selectedMode.breakMinutes * 60 else s.selectedMode.focusMinutes * 60
        _pomodoroState.value = s.copy(
            isRunning = false,
            isBreak = isBreakNext,
            totalSeconds = nextTotalSecs,
            remainingSeconds = nextTotalSecs,
            completedSessions = completedSessionCount,
            todayPomodoroCount = todayCount
        )
    }

    fun resetPomodoro() {
        timerJob?.cancel()
        val s = _pomodoroState.value
        val totalSecs = if (s.isBreak) s.selectedMode.breakMinutes * 60 else s.selectedMode.focusMinutes * 60
        _pomodoroState.value = s.copy(
            isRunning = false,
            totalSeconds = totalSecs,
            remainingSeconds = totalSecs
        )
    }

    fun setPomodoroMode(mode: PomodoroMode) {
        timerJob?.cancel()
        val s = _pomodoroState.value
        val totalSecs = if (s.isBreak) mode.breakMinutes * 60 else mode.focusMinutes * 60
        _pomodoroState.value = s.copy(
            selectedMode = mode,
            isRunning = false,
            totalSeconds = totalSecs,
            remainingSeconds = totalSecs
        )
    }

    fun setPomodoroSubject(subject: SubjectEntity) {
        val s = _pomodoroState.value
        _pomodoroState.value = s.copy(
            selectedSubjectId = subject.id,
            selectedSubjectName = subject.name
        )
    }

    // ==========================================
    // 2. D-Day 목표 관리 로직 (D-Day Goals)
    // ==========================================
    fun addDDayGoal(title: String, targetDate: String, colorHex: String) {
        viewModelScope.launch {
            repository.insertDDayGoal(
                DDayGoalEntity(
                    title = title,
                    targetDate = targetDate,
                    colorHex = colorHex,
                    isPinned = false
                )
            )
            _showAddDDayDialog.value = false
        }
    }

    fun deleteDDayGoal(goal: DDayGoalEntity) {
        viewModelScope.launch {
            repository.deleteDDayGoal(goal)
        }
    }

    fun setShowAddDDayDialog(show: Boolean) {
        _showAddDDayDialog.value = show
    }

    // ==========================================
    // 3. 플래너 To-Do 및 10분 플래너 로직
    // ==========================================
    fun addPlannerTodo(title: String, subjectName: String, subjectId: Long?, minutes: Int) {
        viewModelScope.launch {
            repository.insertTodo(
                PlannerTodoEntity(
                    date = todayDateStr,
                    subjectId = subjectId,
                    subjectName = subjectName,
                    title = title,
                    targetMinutes = minutes,
                    isCompleted = false
                )
            )
            _showAddTodoDialog.value = false
        }
    }

    fun togglePlannerTodo(todo: PlannerTodoEntity) {
        viewModelScope.launch {
            repository.updateTodo(todo.copy(isCompleted = !todo.isCompleted))
        }
    }

    fun deletePlannerTodo(todo: PlannerTodoEntity) {
        viewModelScope.launch {
            repository.deleteTodo(todo)
        }
    }

    fun setShowAddTodoDialog(show: Boolean) {
        _showAddTodoDialog.value = show
    }

    fun toggleTimeBlock(hour: Int, blockIndex: Int) {
        viewModelScope.launch {
            val sub = _selectedTimeBlockSubject.value ?: uiState.value.subjects.firstOrNull()
            val subName = sub?.name ?: "자율"
            val colorHex = sub?.colorHex ?: "#6366F1"
            repository.toggleTimeBlock(todayDateStr, hour, blockIndex, subName, colorHex)
        }
    }

    fun setSelectedTimeBlockSubject(subject: SubjectEntity) {
        _selectedTimeBlockSubject.value = subject
    }

    // ==========================================
    // 4. 클라우드 계정 & 백업 복원 (Firebase / Auth)
    // ==========================================
    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncMessage.value = "Google 계정 로그인 중..."
            val result = cloudSyncRepository.signInWithGoogle(idToken)
            _isSyncing.value = false
            if (result.isSuccess) {
                _syncMessage.value = "Google 계정 (${result.getOrNull()?.displayName}) 로그인 완료!"
            } else {
                _syncMessage.value = "로그인 실패: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun signOut() {
        cloudSyncRepository.signOut()
        _syncMessage.value = "로그아웃되었습니다."
    }

    fun backupDataToCloud() {
        val user = cloudSyncRepository.currentUser
        if (user == null) {
            _syncMessage.value = "클라우드 백업을 위해 먼저 Google 로그인을 진행해 주세요."
            return
        }
        viewModelScope.launch {
            _isSyncing.value = true
            _syncMessage.value = "클라우드에 학습 데이터를 안전하게 백업 중..."
            val currentUi = uiState.value
            val result = cloudSyncRepository.backupFullData(
                userId = user.uid,
                subjects = currentUi.subjects,
                logs = currentUi.recentLogs,
                todos = currentUi.plannerTodos,
                ddays = currentUi.ddayGoals
            )
            _isSyncing.value = false
            if (result.isSuccess) {
                _lastBackupTime.value = System.currentTimeMillis()
                _syncMessage.value = result.getOrNull()
            } else {
                _syncMessage.value = "백업 실패: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun restoreDataFromCloud() {
        val user = cloudSyncRepository.currentUser
        if (user == null) {
            _syncMessage.value = "데이터 복원을 위해 먼저 Google 로그인을 진행해 주세요."
            return
        }
        viewModelScope.launch {
            _isSyncing.value = true
            _syncMessage.value = "클라우드에서 최신 백업 데이터를 불러오는 중..."
            val result = cloudSyncRepository.restoreFullData(user.uid)
            _isSyncing.value = false
            if (result.isSuccess) {
                val payload = result.getOrNull()!!
                payload.subjects.forEach { repository.insertSubject(it) }
                payload.todos.forEach { repository.insertTodo(it) }
                payload.ddays.forEach { repository.insertDDayGoal(it) }
                payload.logs.forEach { repository.insertStudyLog(it) }
                _lastBackupTime.value = payload.backupTimestamp
                _syncMessage.value = "클라우드 데이터가 성공적으로 복원되었습니다!"
            } else {
                _syncMessage.value = "복원 실패: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun setShowCloudAccountDialog(show: Boolean) {
        _showCloudAccountDialog.value = show
    }

    // ==========================================
    // 5. 스터디그램 리포트 카드 (SNS 공유)
    // ==========================================
    fun openStudygramDialog() {
        val currentUi = uiState.value
        val todayStr = SimpleDateFormat("yyyy.MM.dd", Locale.KOREA).format(Date())
        val pinned = currentUi.ddayGoals.firstOrNull { it.isPinned } ?: currentUi.ddayGoals.firstOrNull()
        val quotes = listOf(
            "오늘 흘린 땀방울이 내일의 찬란한 꿈이 된다.",
            "노력은 결코 배신하지 않는다. 오늘도 완벽 집중!",
            "작은 성취가 모여 위대한 결과를 만든다.",
            "한 걸음씩 나아가면 어느새 목표 정상에 닿아 있다."
        )
        val quote = quotes.random()

        val report = DailyStudygramReport(
            dateString = todayStr,
            studentName = currentUi.studentName,
            totalStudyMinutes = currentUi.totalStudyMinutes.coerceAtLeast(180),
            completedTodoCount = currentUi.plannerTodos.count { it.isCompleted },
            totalTodoCount = currentUi.plannerTodos.size.coerceAtLeast(1),
            primarySubject = currentUi.analyticsSummary.topSubject?.subjectName ?: "수학",
            pinnedDDayTitle = pinned?.title ?: "목표 시험",
            pinnedDDayCount = pinned?.formattedDDayString() ?: "D-30",
            motivationalQuote = quote
        )
        _studygramReport.value = report
        _showStudygramDialog.value = true
    }

    fun closeStudygramDialog() {
        _showStudygramDialog.value = false
    }

    // ==========================================
    // 기존 추천 및 학습 기록 로직
    // ==========================================
    fun toggleHideCompletedRecommendations() {
        _hideCompletedRecs.value = !_hideCompletedRecs.value
    }

    fun clearCompletedRecommendations() {
        viewModelScope.launch {
            repository.deleteCompletedRecommendations()
        }
    }

    fun refreshSmartRecommendations() {
        viewModelScope.launch {
            val curSubjects = uiState.value.subjects
            repository.resetAndRefreshSmartRecommendations(curSubjects)
        }
    }

    fun toggleRecommendation(recId: Long, currentCompleted: Boolean) {
        viewModelScope.launch {
            repository.setRecommendationCompleted(recId, !currentCompleted)
        }
    }

    fun logStudySession(
        subjectId: Long,
        subjectName: String,
        minutes: Int,
        unitsCompleted: Int,
        newScore: Int?,
        topicDescription: String
    ) {
        viewModelScope.launch {
            if (newScore != null) {
                repository.updateSubjectScore(subjectId, newScore)
            }
            repository.updateProgress(subjectId, unitsCompleted, minutes)

            val dayOfWeek = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)
            val dayIndex = when (dayOfWeek) {
                java.util.Calendar.MONDAY -> 0
                java.util.Calendar.TUESDAY -> 1
                java.util.Calendar.WEDNESDAY -> 2
                java.util.Calendar.THURSDAY -> 3
                java.util.Calendar.FRIDAY -> 4
                java.util.Calendar.SATURDAY -> 5
                java.util.Calendar.SUNDAY -> 6
                else -> 0
            }
            val dayLabel = when (dayIndex) {
                0 -> "월"
                1 -> "화"
                2 -> "수"
                3 -> "목"
                4 -> "금"
                5 -> "토"
                else -> "일"
            }

            repository.insertStudyLog(
                StudyLogEntity(
                    subjectId = subjectId,
                    subjectName = subjectName,
                    minutes = minutes,
                    dateString = dayLabel,
                    dayOfWeekIndex = dayIndex,
                    topicDescription = topicDescription
                )
            )
        }
    }

    fun submitClinicQuizResult(subjectId: Long, subjectName: String, scoreIncrease: Int) {
        viewModelScope.launch {
            val subject = uiState.value.subjects.find { it.id == subjectId }
            val current = subject?.currentScore ?: 60
            val updated = (current + scoreIncrease).coerceAtMost(100)
            repository.updateSubjectScore(subjectId, updated)
            repository.updateProgress(subjectId, subject?.completedUnits ?: 0, 15)

            repository.insertStudyLog(
                StudyLogEntity(
                    subjectId = subjectId,
                    subjectName = subjectName,
                    minutes = 15,
                    dateString = "오늘",
                    dayOfWeekIndex = 4,
                    topicDescription = "취약 단원 보완 클리닉 퀴즈 완료 (+${scoreIncrease}점 상승)"
                )
            )
        }
    }

    fun addSubject(
        name: String,
        currentScore: Int,
        targetScore: Int,
        totalUnits: Int,
        weakTopic: String
    ) {
        viewModelScope.launch {
            val isWeak = currentScore < 70
            val colorHex = when (name) {
                "수학" -> "#EF4444"
                "영어" -> "#F59E0B"
                "국어" -> "#3B82F6"
                "과학" -> "#10B981"
                "사회", "역사" -> "#8B5CF6"
                else -> "#06B6D4"
            }
            val newSub = SubjectEntity(
                name = name,
                currentScore = currentScore,
                targetScore = targetScore,
                completedUnits = 0,
                totalUnits = totalUnits,
                totalStudyMinutes = 0,
                isWeak = isWeak,
                weakTopic = weakTopic,
                colorHex = colorHex
            )
            val subId = repository.insertSubject(newSub)
            if (isWeak) {
                repository.generateRecommendationsForSubject(newSub.copy(id = subId))
            }
        }
    }

    fun deleteSubject(id: Long) {
        viewModelScope.launch {
            repository.deleteSubject(id)
        }
    }

    fun deleteSubject(subject: SubjectEntity) {
        viewModelScope.launch {
            repository.deleteSubject(subject)
        }
    }

    fun requestAIGeneratedClinic(subject: SubjectEntity) {
        viewModelScope.launch {
            repository.generateRecommendationsForSubject(subject)
        }
    }

    fun analyzeWithGeminiAndRecommend(onCompleted: (() -> Unit)? = null) {
        viewModelScope.launch {
            _isAnalyzing.value = true
            try {
                val current = uiState.value
                val analysisResult = geminiStudyAnalyzer.analyzeLearningData(
                    studentName = current.studentName,
                    gradeLevel = current.gradeLevel,
                    subjects = current.subjects,
                    recentLogs = current.recentLogs
                )

                _aiAnalysisSummary.value = analysisResult.summaryText

                if (analysisResult.recommendations.isNotEmpty()) {
                    repository.replaceRecommendations(analysisResult.recommendations)
                }

                analysisResult.weakSubjectNames.forEach { weakName ->
                    val matched = current.subjects.find { it.name.contains(weakName, ignoreCase = true) }
                    if (matched != null && !matched.isWeak) {
                        repository.updateSubject(matched.copy(isWeak = true))
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("LearningViewModel", "AI analysis error", e)
            } finally {
                _isAnalyzing.value = false
                onCompleted?.invoke()
            }
        }
    }
}
