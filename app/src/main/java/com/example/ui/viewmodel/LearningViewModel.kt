package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.model.RecommendationEntity
import com.example.data.model.StudyLogEntity
import com.example.data.model.SubjectEntity
import com.example.data.repository.LearningRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
    val selectedTab: Int = 0, // 0: 종합 홈, 1: 진도 시각화, 2: 취약 보완 추천
    val overallProgress: Float = 0f,
    val averageScore: Int = 0,
    val weeklyMinutes: List<Int> = listOf(65, 45, 50, 40, 80, 60, 75) // Mon-Sun
)

class LearningViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: LearningRepository

    init {
        val db = AppDatabase.getDatabase(application, viewModelScope)
        repository = LearningRepository(
            subjectDao = db.subjectDao(),
            studyLogDao = db.studyLogDao(),
            recommendationDao = db.recommendationDao()
        )
    }

    private val _selectedTab = MutableStateFlow(0)
    private val _streakDays = MutableStateFlow(7)
    private val _studentName = MutableStateFlow("김민수 학생")
    private val _hideCompletedRecs = MutableStateFlow(true)

    val uiState: StateFlow<LearningUiState> = combine(
        repository.allSubjects,
        repository.allRecommendations,
        repository.allLogs,
        _selectedTab,
        _hideCompletedRecs
    ) { subjects, recommendations, logs, tab, hideCompleted ->
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

        LearningUiState(
            studentName = _studentName.value,
            subjects = subjects,
            weakSubjects = weak,
            recommendations = recommendations,
            activeRecommendations = activeRecs,
            completedRecommendationsCount = completedCount,
            hideCompletedRecommendations = hideCompleted,
            recentLogs = logs.take(7),
            totalStudyMinutes = totalMins,
            streakDays = _streakDays.value,
            selectedTab = tab,
            overallProgress = overallProg,
            averageScore = avgScore,
            weeklyMinutes = weeklyList
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LearningUiState()
    )

    fun setSelectedTab(tab: Int) {
        _selectedTab.value = tab
    }

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
}
