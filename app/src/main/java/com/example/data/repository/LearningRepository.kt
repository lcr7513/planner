package com.example.data.repository

import com.example.data.dao.RecommendationDao
import com.example.data.dao.StudyLogDao
import com.example.data.dao.SubjectDao
import com.example.data.model.RecommendationEntity
import com.example.data.model.RecommendationType
import com.example.data.model.StudyLogEntity
import com.example.data.model.SubjectEntity
import kotlinx.coroutines.flow.Flow

class LearningRepository(
    private val subjectDao: SubjectDao,
    private val studyLogDao: StudyLogDao,
    private val recommendationDao: RecommendationDao
) {
    val allSubjects: Flow<List<SubjectEntity>> = subjectDao.getAllSubjects()
    val weakSubjects: Flow<List<SubjectEntity>> = subjectDao.getWeakSubjects()
    val allRecommendations: Flow<List<RecommendationEntity>> = recommendationDao.getAllRecommendations()
    val recentLogs: Flow<List<StudyLogEntity>> = studyLogDao.getRecentLogs()
    val allLogs: Flow<List<StudyLogEntity>> = studyLogDao.getAllLogs()
    val totalStudyMinutes: Flow<Int?> = studyLogDao.getTotalMinutes()

    suspend fun insertSubject(subject: SubjectEntity): Long = subjectDao.insertSubject(subject)

    suspend fun updateSubject(subject: SubjectEntity) = subjectDao.updateSubject(subject)

    suspend fun updateSubjectScore(subjectId: Long, newScore: Int) {
        subjectDao.updateScore(subjectId, newScore)
    }

    suspend fun updateProgress(subjectId: Long, completedUnits: Int, addedMinutes: Int) {
        subjectDao.updateProgressAndStudyTime(subjectId, completedUnits, addedMinutes)
    }

    suspend fun deleteSubject(id: Long) {
        subjectDao.deleteSubjectById(id)
        recommendationDao.deleteRecommendationsBySubjectId(id)
    }

    suspend fun deleteSubject(subject: SubjectEntity) {
        subjectDao.deleteSubject(subject)
        recommendationDao.deleteRecommendationsBySubjectId(subject.id)
    }

    suspend fun insertStudyLog(log: StudyLogEntity): Long = studyLogDao.insertLog(log)

    suspend fun setRecommendationCompleted(id: Long, completed: Boolean) {
        recommendationDao.setCompleted(id, completed)
    }

    suspend fun addRecommendation(rec: RecommendationEntity): Long {
        return recommendationDao.insertRecommendation(rec)
    }

    suspend fun deleteCompletedRecommendations() {
        recommendationDao.deleteCompletedRecommendations()
    }

    suspend fun resetAndRefreshSmartRecommendations(subjects: List<SubjectEntity>) {
        recommendationDao.deleteAllRecommendations()
        val weakList = subjects.filter { it.currentScore < 70 || it.isWeak }
        val targetList = if (weakList.isNotEmpty()) weakList else subjects

        val newRecs = mutableListOf<RecommendationEntity>()
        targetList.forEach { sub ->
            val topic = if (sub.weakTopic.isNotBlank()) sub.weakTopic else "단원 기초 개념"
            val weakName = sub.name

            // Prioritize: 1 clinic quiz and 1 concept or daily mission per weak subject
            newRecs.add(
                RecommendationEntity(
                    subjectId = sub.id,
                    subjectName = weakName,
                    type = RecommendationType.CLINIC_QUIZ,
                    title = "[$weakName] $topic 3분 핵심 퀴즈",
                    description = "진도 ${sub.completedUnits}/${sub.totalUnits}단원 기준 현재 실력(${sub.currentScore}점) 집중 보완 클리닉입니다.",
                    actionLabel = "보완 퀴즈 풀기",
                    targetTopic = topic,
                    estimatedMinutes = 8,
                    priority = 1
                )
            )
            newRecs.add(
                RecommendationEntity(
                    subjectId = sub.id,
                    subjectName = weakName,
                    type = RecommendationType.CONCEPT_REVIEW,
                    title = "[$weakName] $topic 10분 마인드맵 복습",
                    description = "취약 단원의 기본 공식 및 핵심 정의를 다시 한번 점검하세요.",
                    actionLabel = "개념 요약 보기",
                    targetTopic = topic,
                    estimatedMinutes = 10,
                    priority = 2
                )
            )
            newRecs.add(
                RecommendationEntity(
                    subjectId = sub.id,
                    subjectName = weakName,
                    type = RecommendationType.DAILY_MISSION,
                    title = "[$weakName] 오늘 $topic 20분 집중 학습 미션",
                    description = "목표 점수 ${sub.targetScore}점 달성을 위한 오늘 하루 목표입니다.",
                    actionLabel = "미션 완료 체크",
                    targetTopic = topic,
                    estimatedMinutes = 20,
                    priority = 2
                )
            )
        }
        recommendationDao.insertAll(newRecs)
    }

    /**
     * Intelligent recommendation generator:
     * Scans subjects, identifies weak topics or low scores (< 70),
     * and generates prescriptive clinic & practice items.
     */
    suspend fun generateRecommendationsForSubject(subject: SubjectEntity) {
        val weakName = subject.name
        val topic = if (subject.weakTopic.isNotBlank()) subject.weakTopic else "기초 핵심 개념"

        val generated = listOf(
            RecommendationEntity(
                subjectId = subject.id,
                subjectName = weakName,
                type = RecommendationType.CONCEPT_REVIEW,
                title = "[$weakName] $topic 10분 마인드맵 복습",
                description = "취약 단원의 기본 공식 및 개념 정의를 다시 한번 점검하고 노트를 정리해 보세요.",
                actionLabel = "개념 노트 열기",
                targetTopic = topic,
                estimatedMinutes = 10,
                priority = 1
            ),
            RecommendationEntity(
                subjectId = subject.id,
                subjectName = weakName,
                type = RecommendationType.CLINIC_QUIZ,
                title = "[$weakName] 취약 극복 단원별 3분 퀴즈",
                description = "현재 이해도(${subject.currentScore}점)를 보완하기 위한 필수 기출 유형 3문항입니다.",
                actionLabel = "보완 퀴즈 풀기",
                targetTopic = topic,
                estimatedMinutes = 8,
                priority = 1
            ),
            RecommendationEntity(
                subjectId = subject.id,
                subjectName = weakName,
                type = RecommendationType.DAILY_MISSION,
                title = "[$weakName] 오늘 $topic 20분 집중 학습 미션",
                description = "단기간 점수 향상을 위한 1일 추천 미션을 수행하고 진도를 완료하세요.",
                actionLabel = "학습 완료 체크",
                targetTopic = topic,
                estimatedMinutes = 20,
                priority = 2
            )
        )
        recommendationDao.insertAll(generated)
    }
}
