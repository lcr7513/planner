package com.example.data.service

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.RecommendationEntity
import com.example.data.model.RecommendationType
import com.example.data.model.StudyLogEntity
import com.example.data.model.SubjectEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AiLearningAnalysisResult(
    val summaryText: String,
    val weakSubjectNames: List<String>,
    val recommendations: List<RecommendationEntity>
)

class GeminiStudyAnalyzer {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    private fun getApiKey(): String {
        return try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun analyzeLearningData(
        studentName: String,
        gradeLevel: String,
        subjects: List<SubjectEntity>,
        recentLogs: List<StudyLogEntity>
    ): AiLearningAnalysisResult = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "DEFAULT_API_KEY" || apiKey == "MY_GEMINI_API_KEY") {
            // Intelligent fallback analysis using local heuristic
            return@withContext generateLocalAnalysis(subjects, recentLogs)
        }

        try {
            val subjectsData = subjects.joinToString("\n") { s ->
                "- 과목: ${s.name}, 현재 점수: ${s.currentScore}점/100점 (목표: ${s.targetScore}점), 진도: ${s.completedUnits}/${s.totalUnits}단원, 누적 학습: ${s.totalStudyMinutes}분, 취약 단원: ${s.weakTopic.ifBlank { "없음" }}, 취약 과목 여부: ${if (s.isWeak || s.currentScore < 70) "취약" else "양호"}"
            }

            val logsData = recentLogs.take(10).joinToString("\n") { l ->
                "- [${l.dateString}] ${l.subjectName} ${l.minutes}분 학습 (내용: ${l.topicDescription})"
            }

            val prompt = """
                당신은 학생 1:1 맞춤형 학습 진단 및 취약점 처방 전문 교육 AI입니다.
                다음 학생의 학습 진도, 성적, 최근 학습 기록을 심층 분석하여 보완이 시급한 과목과 맞춤형 추천 액션을 도출해 주세요.

                [학생 정보]
                - 이름: $studentName
                - 학년/목표: $gradeLevel

                [과목별 성적 및 진도 현황]
                $subjectsData

                [최근 학습 기록 (Room DB)]
                ${if (logsData.isNotBlank()) logsData else "- 최근 등록된 학습 기록이 없습니다."}

                반드시 아래 JSON 포맷만을 순수 JSON(마크다운 백틱 없이)으로 출력해 주세요:
                {
                  "summary": "학생의 현재 학습 상태에 대한 총평 및 격려 (2~3문장)",
                  "weakSubjects": ["취약과목1", "취약과목2"],
                  "recommendations": [
                    {
                      "subjectName": "과목명",
                      "type": "CLINIC_QUIZ 또는 CONCEPT_REVIEW 또는 DAILY_MISSION 또는 STUDY_STRATEGY",
                      "title": "추천 활동 제목",
                      "description": "구체적인 보완 이유와 공부 방법 가이드 (1~2문장)",
                      "actionLabel": "버튼 라벨 (예: 퀴즈 풀기, 개념 복습, 과제 확인)",
                      "targetTopic": "보완 대상 단원/개념",
                      "estimatedMinutes": 15,
                      "priority": 1
                    }
                  ]
                }
            """.trimIndent()

            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        }
                        put("parts", partsArray)
                    }
                    put(contentObj)
                }
                put("contents", contentsArray)

                val generationConfig = JSONObject().apply {
                    put("temperature", 0.4)
                    put("responseMimeType", "application/json")
                }
                put("generationConfig", generationConfig)
            }

            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            // gemini-3.5-flash as mandated for basic text tasks
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w("GeminiStudyAnalyzer", "Gemini API call failed: ${response.code}")
                return@withContext generateLocalAnalysis(subjects, recentLogs)
            }

            val responseBody = response.body?.string() ?: return@withContext generateLocalAnalysis(subjects, recentLogs)
            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates") ?: return@withContext generateLocalAnalysis(subjects, recentLogs)
            if (candidates.length() == 0) return@withContext generateLocalAnalysis(subjects, recentLogs)

            val text = candidates.getJSONObject(0)
                .optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text") ?: return@withContext generateLocalAnalysis(subjects, recentLogs)

            val cleanedText = text.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val parsedJson = JSONObject(cleanedText)
            val summary = parsedJson.optString("summary", "학습 데이터 분석이 완료되었습니다.")
            val weakSubjectsJson = parsedJson.optJSONArray("weakSubjects")
            val weakNames = mutableListOf<String>()
            if (weakSubjectsJson != null) {
                for (i in 0 until weakSubjectsJson.length()) {
                    weakNames.add(weakSubjectsJson.getString(i))
                }
            }

            val recsJson = parsedJson.optJSONArray("recommendations")
            val recList = mutableListOf<RecommendationEntity>()
            if (recsJson != null) {
                for (i in 0 until recsJson.length()) {
                    val rObj = recsJson.getJSONObject(i)
                    val sName = rObj.optString("subjectName", "수학")
                    val matchingSubject = subjects.find { it.name == sName }
                    val sId = matchingSubject?.id ?: 1L

                    val typeStr = rObj.optString("type", "CLINIC_QUIZ")
                    val recType = try {
                        RecommendationType.valueOf(typeStr)
                    } catch (e: Exception) {
                        when {
                            typeStr.contains("QUIZ", ignoreCase = true) -> RecommendationType.CLINIC_QUIZ
                            typeStr.contains("CONCEPT", ignoreCase = true) -> RecommendationType.CONCEPT_REVIEW
                            typeStr.contains("MISSION", ignoreCase = true) -> RecommendationType.DAILY_MISSION
                            else -> RecommendationType.STUDY_STRATEGY
                        }
                    }

                    recList.add(
                        RecommendationEntity(
                            subjectId = sId,
                            subjectName = sName,
                            type = recType,
                            title = rObj.optString("title", "[$sName] 취약점 집중 클리닉"),
                            description = rObj.optString("description", "현재 점수를 보완하기 위한 추천 활동입니다."),
                            actionLabel = rObj.optString("actionLabel", "실행하기"),
                            targetTopic = rObj.optString("targetTopic", matchingSubject?.weakTopic ?: ""),
                            estimatedMinutes = rObj.optInt("estimatedMinutes", 15),
                            priority = rObj.optInt("priority", 1),
                            isCompleted = false
                        )
                    )
                }
            }

            if (recList.isNotEmpty()) {
                AiLearningAnalysisResult(
                    summaryText = summary,
                    weakSubjectNames = if (weakNames.isNotEmpty()) weakNames else subjects.filter { it.currentScore < 70 || it.isWeak }.map { it.name },
                    recommendations = recList
                )
            } else {
                generateLocalAnalysis(subjects, recentLogs)
            }
        } catch (e: Exception) {
            Log.e("GeminiStudyAnalyzer", "Analysis error: ${e.message}", e)
            generateLocalAnalysis(subjects, recentLogs)
        }
    }

    private fun generateLocalAnalysis(
        subjects: List<SubjectEntity>,
        recentLogs: List<StudyLogEntity>
    ): AiLearningAnalysisResult {
        val weakList = subjects.filter { it.currentScore < 70 || it.isWeak }
        val weakNames = weakList.map { it.name }
        val totalMinutes = recentLogs.sumOf { it.minutes }

        val summary = if (weakList.isNotEmpty()) {
            val namesStr = weakNames.joinToString(", ")
            "학습 데이터 분석 결과, $namesStr 과목의 이해도 보완이 시급합니다. 최근 총 ${totalMinutes}분의 학습이 기록되었으며 취약 단원 중심의 퀴즈 및 개념 복습이 추천됩니다."
        } else {
            "모든 과목의 평균 점수와 진도가 안정적입니다! 현재 페이스를 유지하며 취약 단원 심화 문제를 학습해 보세요."
        }

        val recs = mutableListOf<RecommendationEntity>()
        val targetList = if (weakList.isNotEmpty()) weakList else subjects.take(2)

        targetList.forEachIndexed { index, sub ->
            val topic = if (sub.weakTopic.isNotBlank()) sub.weakTopic else "단원 핵심 원리"
            recs.add(
                RecommendationEntity(
                    subjectId = sub.id,
                    subjectName = sub.name,
                    type = RecommendationType.CLINIC_QUIZ,
                    title = "[${sub.name}] $topic 3분 클리닉 퀴즈",
                    description = "현재 ${sub.currentScore}점대 학생을 위한 필수 기출 유형 집중 보완 퀴즈입니다.",
                    actionLabel = "보완 퀴즈 풀기",
                    targetTopic = topic,
                    estimatedMinutes = 8,
                    priority = 1
                )
            )
            recs.add(
                RecommendationEntity(
                    subjectId = sub.id,
                    subjectName = sub.name,
                    type = RecommendationType.CONCEPT_REVIEW,
                    title = "[${sub.name}] $topic 개념 10분 마인드맵",
                    description = "취약 단원의 기본 정의 및 핵심 요약 노트를 점검해 보세요.",
                    actionLabel = "개념 요약 보기",
                    targetTopic = topic,
                    estimatedMinutes = 10,
                    priority = 2
                )
            )
            recs.add(
                RecommendationEntity(
                    subjectId = sub.id,
                    subjectName = sub.name,
                    type = RecommendationType.DAILY_MISSION,
                    title = "[${sub.name}] $topic 20분 집중 학습 미션",
                    description = "목표 점수 ${sub.targetScore}점 달성을 위해 오늘 하루 집중 복습을 진행하세요.",
                    actionLabel = "미션 완료 체크",
                    targetTopic = topic,
                    estimatedMinutes = 20,
                    priority = 2
                )
            )
        }

        return AiLearningAnalysisResult(
            summaryText = summary,
            weakSubjectNames = weakNames,
            recommendations = recs
        )
    }
}
