package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_logs")
data class StudyLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subjectId: Long,
    val subjectName: String,
    val minutes: Int,
    val dateString: String, // e.g. "월", "화", "수", "목", "금", "토", "일" or "2026-09-22"
    val dayOfWeekIndex: Int, // 0 = Mon, 6 = Sun
    val topicDescription: String,
    val timestamp: Long = System.currentTimeMillis()
)
