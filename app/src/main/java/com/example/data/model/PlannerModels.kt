package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * 일일 학습 플래너 할 일(To-Do) 엔티티
 */
@Entity(tableName = "planner_todos")
data class PlannerTodoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String, // YYYY-MM-DD
    val subjectId: Long?,
    val subjectName: String,
    val title: String,
    val targetMinutes: Int = 45,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * D-Day 목표 엔티티 (수능, 모의고사, 기말고사 등)
 */
@Entity(tableName = "dday_goals")
data class DDayGoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val targetDate: String, // YYYY-MM-DD
    val colorHex: String = "#6366F1",
    val isPinned: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * 오늘 기준 남은 일수 계산
     * D-Day: 0, D-3: 3, D+2: -2
     */
    fun calculateDaysLeft(): Long {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)
            val todayStr = sdf.format(Date())
            val today = sdf.parse(todayStr)?.time ?: System.currentTimeMillis()
            val target = sdf.parse(targetDate)?.time ?: System.currentTimeMillis()
            val diffMillis = target - today
            TimeUnit.DAYS.convert(diffMillis, TimeUnit.MILLISECONDS)
        } catch (e: Exception) {
            0L
        }
    }

    fun formattedDDayString(): String {
        val days = calculateDaysLeft()
        return when {
            days > 0 -> "D-$days"
            days == 0L -> "D-Day"
            else -> "D+${-days}"
        }
    }
}

/**
 * 10분 플래너 타임테이블 엔티티
 * 06시부터 02시까지 10분 단위(한 시간에 6블록)로 어떤 과목을 공부했는지 기록
 */
@Entity(tableName = "time_blocks")
data class TimeBlockEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String, // YYYY-MM-DD
    val hour: Int, // 6..26 (24=00시, 25=01시, 26=02시)
    val blockIndex: Int, // 0..5 (0: 00-10분, 1: 10-20분, 2: 20-30분, 3: 30-40분, 4: 40-50분, 5: 50-60분)
    val subjectName: String,
    val colorHex: String = "#6366F1"
)

/**
 * 뽀모도로 타이머 모드
 */
enum class PomodoroMode(val title: String, val focusMinutes: Int, val breakMinutes: Int) {
    STANDARD_25("25분 집중 / 5분 휴식", 25, 5),
    EXTENDED_50("50분 집중 / 10분 휴식", 50, 10),
    EXAM_PRACTICE("80분 실전 모의고사", 80, 15),
    CUSTOM("자율 설정", 30, 5)
}

/**
 * 뽀모도로 타이머 상태
 */
data class PomodoroTimerState(
    val isRunning: Boolean = false,
    val isBreak: Boolean = false,
    val selectedMode: PomodoroMode = PomodoroMode.STANDARD_25,
    val selectedSubjectId: Long? = null,
    val selectedSubjectName: String = "수학",
    val totalSeconds: Int = 25 * 60,
    val remainingSeconds: Int = 25 * 60,
    val completedSessions: Int = 0,
    val todayPomodoroCount: Int = 4
) {
    val progress: Float
        get() = if (totalSeconds > 0) (totalSeconds - remainingSeconds).toFloat() / totalSeconds.toFloat() else 0f

    val formattedTime: String
        get() {
            val minutes = remainingSeconds / 60
            val seconds = remainingSeconds % 60
            return String.format(Locale.KOREA, "%02d:%02d", minutes, seconds)
        }
}

/**
 * 일일 학습 리포트 및 SNS 공유용 데이터 모델
 */
data class DailyStudygramReport(
    val dateString: String,
    val studentName: String,
    val totalStudyMinutes: Int,
    val completedTodoCount: Int,
    val totalTodoCount: Int,
    val primarySubject: String,
    val pinnedDDayTitle: String,
    val pinnedDDayCount: String,
    val motivationalQuote: String,
    val subjectTimes: List<Pair<String, Int>> = emptyList()
)
