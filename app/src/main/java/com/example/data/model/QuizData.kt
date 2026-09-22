package com.example.data.model

data class QuizQuestion(
    val id: Int,
    val subjectName: String = "",
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

object QuizBank {
    fun getDynamicQuestionsForTopic(subjectName: String, topic: String): List<QuizQuestion> {
        val baseList = getQuestionsForSubject(subjectName)
        return baseList.mapIndexed { index, q ->
            q.copy(
                id = index + 1,
                subjectName = subjectName,
                question = if (topic.isNotBlank()) "[$topic] ${q.question}" else q.question
            )
        }
    }

    val mathQuestions = listOf(
        QuizQuestion(
            id = 1,
            subjectName = "수학",
            question = "이차방정식 x² - 5x + 6 = 0 의 두 근은 무엇일까요?",
            options = listOf("x = 1 또는 x = 6", "x = 2 또는 x = 3", "x = -2 또는 x = -3", "x = 0 또는 x = 5"),
            correctIndex = 1,
            explanation = "(x - 2)(x - 3) = 0 으로 인수분해되므로 두 근은 2와 3입니다."
        ),
        QuizQuestion(
            id = 2,
            subjectName = "수학",
            question = "이차함수 y = (x - 2)² + 3 의 꼭짓점 좌표는?",
            options = listOf("(2, 3)", "(-2, 3)", "(2, -3)", "(3, 2)"),
            correctIndex = 0,
            explanation = "y = a(x - p)² + q 의 표준형에서 꼭짓점 좌표는 (p, q)이므로 (2, 3)입니다."
        ),
        QuizQuestion(
            id = 3,
            subjectName = "수학",
            question = "이차방정식 ax² + bx + c = 0 에서 판별식 D = b² - 4ac > 0 일 때 실근의 개수는?",
            options = listOf("0개 (서로 다른 두 허근)", "1개 (중근)", "서로 다른 두 실근 (2개)", "알 수 없음"),
            correctIndex = 2,
            explanation = "판별식 D > 0 이면 서로 다른 두 실근을 가집니다."
        )
    )

    val englishQuestions = listOf(
        QuizQuestion(
            id = 101,
            subjectName = "영어",
            question = "다음 빈칸에 알맞은 관계대명사는?\n\"This is the student ____ won the science award.\"",
            options = listOf("which", "who", "whom", "whose"),
            correctIndex = 1,
            explanation = "선행사가 사람('the student')이고 뒤에 주어가 없는 주격 관계대명사 자리이므로 'who'가 적절합니다."
        ),
        QuizQuestion(
            id = 102,
            subjectName = "영어",
            question = "빈칸에 알맞은 형태는?\n\"She insisted on ____ the homework before going out.\"",
            options = listOf("finish", "to finish", "finishing", "finished"),
            correctIndex = 2,
            explanation = "전치사 on의 목적어 자리이므로 동명사 형태인 finishing이 와야 합니다."
        ),
        QuizQuestion(
            id = 103,
            subjectName = "영어",
            question = "'reinforce'의 가장 적절한 한국어 뜻은 무엇일까요?",
            options = listOf("포기하다", "강화하다, 보완하다", "감소시키다", "관찰하다"),
            correctIndex = 1,
            explanation = "'reinforce'는 '강화하다', '보강하다', '보완하다'라는 의미입니다."
        )
    )

    val scienceQuestions = listOf(
        QuizQuestion(
            id = 201,
            subjectName = "과학",
            question = "화학 반응 전후에 물질의 총 질량은 변하지 않는다는 법칙은?",
            options = listOf("일정 성분비 법칙", "질량 보존 법칙", "기체 반응 법칙", "아보가드로 법칙"),
            correctIndex = 1,
            explanation = "화학 반응 시 원자의 종류와 개수가 보존되므로 반응 전후 총 질량이 보존됩니다."
        ),
        QuizQuestion(
            id = 202,
            subjectName = "과학",
            question = "빛의 합성과 관련된 3원색으로 올바른 조합은?",
            options = listOf("빨강, 초록, 파랑 (RGB)", "빨강, 노랑, 파랑", "청록, 자홍, 노랑", "검정, 흰색, 회색"),
            correctIndex = 0,
            explanation = "빛의 3원색은 빨강(Red), 초록(Green), 파랑(Blue)입니다."
        )
    )

    fun getQuestionsForSubject(subjectName: String): List<QuizQuestion> {
        return when {
            subjectName.contains("수학") -> mathQuestions
            subjectName.contains("영어") -> englishQuestions
            subjectName.contains("과학") -> scienceQuestions
            else -> mathQuestions
        }
    }
}
