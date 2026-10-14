package com.example.data.model

/**
 * 과목별 학습 시간 및 통계 데이터 모델
 * Room 데이터베이스의 study_logs 및 subjects 테이블을 기반으로 계산됩니다.
 */
data class SubjectStudyStat(
    val subjectId: Long,
    val subjectName: String,
    val colorHex: String,
    val totalMinutes: Int,
    val sessionCount: Int,
    val progressPercent: Float,
    val completedUnits: Int,
    val totalUnits: Int,
    val currentScore: Int,
    val targetScore: Int,
    val percentOfTotalTime: Float, // 0.0 ~ 1.0 (전체 학습 시간 중 해당 과목 비중)
    val isWeak: Boolean
)

/**
 * 학습 진도 및 시간 변화 추이를 위한 데이터 포인트
 * Recharts 스타일의 Monotone Line/Area 차트와 Bar 차트에서 시각화됩니다.
 */
data class ProgressTrendPoint(
    val label: String,                // e.g. "월", "화" 또는 "10/12"
    val fullDateString: String,       // e.g. "2026-10-12 (월)"
    val timestamp: Long,
    val dayMinutes: Int,              // 해당 일자 총 학습 분
    val cumulativeMinutes: Int,       // 해당 일자까지의 누적 학습 분
    val progressPercent: Float,       // 누적 진도율 (0.0 ~ 1.0)
    val subjectMinutesMap: Map<String, Int> = emptyMap(), // 과목별 학습 시간 맵
    val recentScore: Int = 0          // 당시 평가 점수
)

/**
 * Room 데이터베이스로부터 집계된 종합 학습 분석 통계
 */
data class StudyAnalyticsSummary(
    val subjectStats: List<SubjectStudyStat> = emptyList(),
    val totalCalculatedMinutes: Int = 0,
    val trendPoints: List<ProgressTrendPoint> = emptyList(),
    val topSubject: SubjectStudyStat? = null,
    val weakSubjectMinutes: Int = 0,
    val weakSubjectRatio: Float = 0f,
    val dailyAverageMinutes: Int = 0
)
