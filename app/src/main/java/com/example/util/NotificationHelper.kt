package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.ui.viewmodel.LearningUiState

object NotificationHelper {
    const val CHANNEL_ID = "daily_learning_report_channel"
    private const val CHANNEL_NAME = "일간 학습 리포트 알림"
    private const val NOTIFICATION_ID = 2026

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "매일 사용자의 학습 활동을 요약하여 전달하는 일간 리포트 알림입니다."
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun sendDailyReportNotification(context: Context, uiState: LearningUiState): Boolean {
        createNotificationChannel(context)

        // Check POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionCheck = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }

        // Summary calculations
        val todayMinutes = uiState.weeklyMinutes.lastOrNull() ?: 75
        val completedUnits = uiState.subjects.sumOf { it.completedUnits }
        val weakCount = uiState.weakSubjects.size
        val completedMissions = uiState.recommendations.count { it.isCompleted }

        val title = "📊 [일간 학습 리포트] ${uiState.studentName}의 오늘 성취 요약"
        val summaryLine = "총 학습 ${todayMinutes / 60}시간 ${todayMinutes % 60}분 · 미션 ${completedMissions}개 완료"

        val bigText = StringBuilder().apply {
            append("✨ 오늘의 학습 성취도 요약:\n")
            append("• 오늘 순 공부 시간: ${todayMinutes}분\n")
            append("• 완료한 교과 단원: 누적 ${completedUnits}단원\n")
            if (weakCount > 0) {
                append("• 취약 과목 관리: ${weakCount}개 과목 집중 보완 중\n")
            } else {
                append("• 취약 과목: 모두 우수 등급 달성!\n")
            }
            append("• 연속 학습: 🔥 ${uiState.streakDays}일 연속 달성 중!\n\n")
            append("💡 AI 코칭: 내일은 취약 단원 퀴즈 1세트를 풀면 목표 점수에 한 걸음 더 가까워집니다. 오늘도 정말 수고 많으셨어요!")
        }.toString()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(summaryLine)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            return true
        } catch (e: SecurityException) {
            return false
        }
    }
}
