package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val currentScore: Int,      // e.g. 58 (out of 100)
    val targetScore: Int = 90,  // target score
    val completedUnits: Int,    // e.g. 6
    val totalUnits: Int,        // e.g. 15
    val totalStudyMinutes: Int, // total minutes studied
    val isWeak: Boolean = false,// whether flagged as weak subject
    val weakTopic: String = "", // e.g. "이차방정식과 함수", "문법 및 관계대명사"
    val colorHex: String = "#4F46E5",
    val lastStudiedDate: String = "오늘"
) {
    val progressPercent: Float
        get() = if (totalUnits > 0) (completedUnits.toFloat() / totalUnits.toFloat()).coerceIn(0f, 1f) else 0f
}
