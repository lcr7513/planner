package com.example.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.DailyStudygramReport

@Composable
fun StudygramShareDialog(
    report: DailyStudygramReport,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .testTag("studygram_share_dialog"),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Modal Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "📸", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "스터디그램 데일리 리포트",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "닫기")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // The Instagram / Social Share Card (Visual Preview)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF1E1B4B), // Deep Indigo
                                    Color(0xFF312E81),
                                    Color(0xFF4338CA)
                                )
                            )
                        )
                        .border(1.dp, Color(0xFF6366F1).copy(alpha = 0.4f), RoundedCornerShape(22.dp))
                        .padding(22.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Card Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "STUDY PLANNER REPORT",
                                    color = Color(0xFFA5B4FC),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.2.sp
                                )
                                Text(
                                    text = "${report.studentName}의 열공 기록",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // D-Day Chip
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFEF4444).copy(alpha = 0.9f)
                            ) {
                                Text(
                                    text = "${report.pinnedDDayTitle} ${report.pinnedDDayCount}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Big Total Study Time
                        val hours = report.totalStudyMinutes / 60
                        val mins = report.totalStudyMinutes % 60
                        val formattedTimeStr = String.format("%02d:%02d:00", hours, mins)

                        Text(
                            text = "TODAY'S STUDY TIME",
                            color = Color(0xFFC7D2FE),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = formattedTimeStr,
                            color = Color(0xFF38BDF8),
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Stats Summary Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.1f))
                                .padding(vertical = 10.dp, horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("To-Do 달성률", color = Color(0xFFE0E7FF), fontSize = 10.sp)
                                val todoPercent = if (report.totalTodoCount > 0) {
                                    (report.completedTodoCount * 100) / report.totalTodoCount
                                } else 100
                                Text(
                                    text = "$todoPercent% (${report.completedTodoCount}/${report.totalTodoCount})",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(24.dp)
                                    .background(Color.White.copy(alpha = 0.2f))
                            )

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("최대 집중 과목", color = Color(0xFFE0E7FF), fontSize = 10.sp)
                                Text(
                                    text = report.primarySubject,
                                    color = Color(0xFFFBBF24),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(24.dp)
                                    .background(Color.White.copy(alpha = 0.2f))
                            )

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("학습 날짜", color = Color(0xFFE0E7FF), fontSize = 10.sp)
                                Text(
                                    text = report.dateString,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Motivational Quote
                        Text(
                            text = "\"${report.motivationalQuote}\"",
                            color = Color(0xFFE0E7FF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // App watermark
                        Text(
                            text = "#학습매니저 #스터디플래너 #열공스타그램",
                            color = Color(0xFFA5B4FC).copy(alpha = 0.8f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Button(
                    onClick = {
                        shareStudyReport(context, report)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("share_to_sns_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "공유",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "인스타그램 / 카카오톡으로 공유하기",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

private fun shareStudyReport(context: Context, report: DailyStudygramReport) {
    val hours = report.totalStudyMinutes / 60
    val mins = report.totalStudyMinutes % 60
    val shareText = """
        🔥 [학습매니저] ${report.studentName}의 오늘 하루 열공 리포트!
        
        📅 날짜: ${report.dateString}
        ⏱️ 총 순공 시간: ${hours}시간 ${mins}분
        🎯 To-Do 달성률: ${report.completedTodoCount}/${report.totalTodoCount}개 완료
        🏆 목표 D-Day: ${report.pinnedDDayTitle} ${report.pinnedDDayCount}
        📚 주력 과목: ${report.primarySubject}
        
        💬 "${report.motivationalQuote}"
        
        #학습매니저 #스터디플래너 #수험생 #열공스타그램 #공스타그램
    """.trimIndent()

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "[학습매니저] 데일리 열공 리포트")
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    context.startActivity(Intent.createChooser(intent, "학습 리포트 공유하기"))
}
