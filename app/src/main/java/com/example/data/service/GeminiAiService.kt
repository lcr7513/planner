package com.example.data.service

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.QuizBank
import com.example.data.model.QuizQuestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class MessageSender {
    USER, AI_TUTOR
}

class GeminiAiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    // Retrieve API key safely from BuildConfig
    private fun getApiKey(): String {
        return try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun generateClinicQuiz(
        subjectName: String,
        weakTopic: String,
        currentScore: Int
    ): List<QuizQuestion> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            // Smart local fallback if API key is not configured in Secrets yet
            return@withContext QuizBank.getDynamicQuestionsForTopic(subjectName, weakTopic)
        }

        try {
            val prompt = """
                당신은 중등 교육과정 평가 전문 AI입니다.
                다음 조건에 맞춰 학생의 취약점을 보완할 3문항의 객관식 퀴즈를 JSON 배열로 생성해 주세요.
                과목: $subjectName
                취약 단원/토픽: $weakTopic
                현재 학생 이해도 점수: ${currentScore}점 (점수에 맞춰 너무 어렵지 않고 핵심 개념을 확인할 수 있도록 난이도 조절)

                반드시 아래 JSON 형식만을 순수 텍스트(마크다운 백틱 없이)로 출력하세요:
                [
                  {
                    "question": "문제 내용 (수식은 알기 쉽게 기호 사용)",
                    "options": ["보기 1", "보기 2", "보기 3", "보기 4"],
                    "correctIndex": 0,
                    "explanation": "학생이 이해하기 쉬운 상세한 오답 및 정답 풀이 해설"
                  }
                ]
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
                    put("temperature", 0.3)
                    put("responseMimeType", "application/json")
                }
                put("generationConfig", generationConfig)
            }

            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w("GeminiAiService", "Gemini API call failed with code: ${response.code}")
                return@withContext QuizBank.getDynamicQuestionsForTopic(subjectName, weakTopic)
            }

            val responseBody = response.body?.string() ?: return@withContext QuizBank.getDynamicQuestionsForTopic(subjectName, weakTopic)
            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates") ?: return@withContext QuizBank.getDynamicQuestionsForTopic(subjectName, weakTopic)
            if (candidates.length() == 0) return@withContext QuizBank.getDynamicQuestionsForTopic(subjectName, weakTopic)

            val content = candidates.getJSONObject(0).optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text") ?: return@withContext QuizBank.getDynamicQuestionsForTopic(subjectName, weakTopic)

            val cleanedText = text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val questionsArray = JSONArray(cleanedText)
            val parsedList = mutableListOf<QuizQuestion>()

            for (i in 0 until questionsArray.length()) {
                val qObj = questionsArray.getJSONObject(i)
                val question = qObj.getString("question")
                val optionsJson = qObj.getJSONArray("options")
                val options = mutableListOf<String>()
                for (j in 0 until optionsJson.length()) {
                    options.add(optionsJson.getString(j))
                }
                val correctIndex = qObj.getInt("correctIndex")
                val explanation = qObj.getString("explanation")

                parsedList.add(
                    QuizQuestion(
                        id = i + 1,
                        question = question,
                        options = options,
                        correctIndex = correctIndex,
                        explanation = explanation
                    )
                )
            }

            if (parsedList.isNotEmpty()) parsedList else QuizBank.getDynamicQuestionsForTopic(subjectName, weakTopic)
        } catch (e: Exception) {
            Log.e("GeminiAiService", "Error generating quiz via Gemini: ${e.message}", e)
            QuizBank.getDynamicQuestionsForTopic(subjectName, weakTopic)
        }
    }

    suspend fun askTutor(
        userQuestion: String,
        subjectContext: String,
        recentHistory: List<ChatMessage>
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            // Intelligent local tutoring fallback
            return@withContext generateLocalTutorResponse(userQuestion, subjectContext)
        }

        try {
            val systemInstruction = """
                당신은 중·고등학생을 위한 1:1 학습 전문 AI 튜터 '학습매니저 AI'입니다.
                - 친절하고 격려하는 말투(이모지 활용)로 답변하세요.
                - 단순히 최종 정답만 알려주지 말고, 학생이 왜 그렇게 되는지 원리를 스스로 깨우칠 수 있도록 2~3단계로 나누어 설명해 주세요.
                - 과목 맥락: $subjectContext
            """.trimIndent()

            val contentsArray = JSONArray()

            // Include recent context
            recentHistory.takeLast(4).forEach { msg ->
                val role = if (msg.sender == MessageSender.USER) "user" else "model"
                contentsArray.put(JSONObject().apply {
                    put("role", role)
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", msg.text) })
                    })
                })
            }

            // Current prompt
            contentsArray.put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", userQuestion) })
                })
            })

            val requestJson = JSONObject().apply {
                put("contents", contentsArray)
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", systemInstruction) })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                })
            }

            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext generateLocalTutorResponse(userQuestion, subjectContext)
            }

            val responseBody = response.body?.string() ?: return@withContext generateLocalTutorResponse(userQuestion, subjectContext)
            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            val text = candidates?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text")

            text?.trim() ?: generateLocalTutorResponse(userQuestion, subjectContext)
        } catch (e: Exception) {
            Log.e("GeminiAiService", "Tutor call error: ${e.message}", e)
            generateLocalTutorResponse(userQuestion, subjectContext)
        }
    }

    private fun generateLocalTutorResponse(question: String, subjectContext: String): String {
        val q = question.lowercase()
        return when {
            q.contains("이차함수") || q.contains("꼭짓점") -> {
                "💡 **이차함수 꼭짓점 구하기 꿀팁!**\n\n" +
                        "1단계: 일반형 \$y = ax^2 + bx + c\$를 **완전제곱식**으로 변형하는 것이 핵심이에요.\n" +
                        "2단계: \$x\$의 계수(\$b\$)를 2로 나눈 뒤 제곱한 값을 더하고 빼줍니다.\n" +
                        "3단계: 변형된 식 \$y = a(x - p)^2 + q\$에서 꼭짓점의 좌표는 바로 **\$(p, q)\$**가 됩니다!\n\n" +
                        "공식을 억지로 외우기보다 완전제곱식을 한 번 직접 손으로 묶어보면 절대 잊어버리지 않아요. 궁금한 식이 있다면 예시를 적어줄래요? 😊"
            }
            q.contains("관계대명사") || q.contains("that") || q.contains("which") || q.contains("who") -> {
                "📖 **관계대명사 3초 구분 비법!**\n\n" +
                        "1. **선행사(꾸밈을 받는 앞 명사)**를 먼저 보세요:\n" +
                        "   - 사람이면 ➡️ `who`\n" +
                        "   - 사물이나 동물이면 ➡️ `which`\n" +
                        "   - 둘 다 만능으로 쓰고 싶다면 ➡️ `that`\n" +
                        "2. 관계대명사 뒤 문장을 보면 주어나 목적어가 하나 빠져서 **불완전한 문장**이 온다는 점을 꼭 기억하세요!\n\n" +
                        "헷갈리는 영어 예문이 있다면 편하게 적어주세요. 같이 분석해 봐요! ✍️"
            }
            q.contains("광합성") || q.contains("호흡") -> {
                "🔬 **광합성과 세포 호흡의 차이점!**\n\n" +
                        "• **광합성**: 엽록체에서 빛에너지를 이용해 물과 이산화탄소를 '포도당과 산소'로 만드는 과정 (에너지 저장 ☀️)\n" +
                        "• **세포 호흡**: 미토콘드리아에서 포도당과 산소를 분해해 우리가 활동할 수 있는 에너지를 얻는 과정 (에너지 방출 ⚡)\n\n" +
                        "둘은 서로 정반대 작용이라고 생각하면 아주 쉽게 이해할 수 있어요!"
            }
            else -> {
                "👋 안녕하세요! 질문해 주신 **[$question]**에 대해 설명해 드릴게요.\n\n" +
                        "공부할 때 가장 중요한 것은 **'핵심 원리 3단계'**를 차근차근 짚어보는 것입니다.\n" +
                        "1. 교과서의 기본 정의를 확인하기\n" +
                        "2. 공식이 유도되는 이유를 예제로 직접 확인하기\n" +
                        "3. 나만의 언어로 다른 사람에게 설명하듯 요약하기\n\n" +
                        "💡 *팁: 구체적인 문제나 헷갈리는 공식을 적어주시면 단계별로 쉽게 풀어드릴게요!*"
            }
        }
    }
}
