package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class RecommendationType {
    CONCEPT_REVIEW,   // 개념 복습 처방
    CLINIC_QUIZ,      // 취약 단원 진단/보완 퀴즈
    DAILY_MISSION,    // 오늘의 맞춤 과제
    STUDY_STRATEGY    // 오답 분석 및 공부법
}

@Entity(tableName = "recommendations")
data class RecommendationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subjectId: Long,
    val subjectName: String,
    val type: RecommendationType,
    val title: String,
    val description: String,
    val actionLabel: String = "실행하기",
    val isCompleted: Boolean = false,
    val targetTopic: String = "",
    val estimatedMinutes: Int = 15,
    val priority: Int = 1, // 1: high, 2: medium, 3: low
    val createdAt: Long = System.currentTimeMillis()
)
