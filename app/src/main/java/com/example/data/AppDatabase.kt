package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.RecommendationDao
import com.example.data.dao.StudyLogDao
import com.example.data.dao.SubjectDao
import com.example.data.model.RecommendationEntity
import com.example.data.model.RecommendationType
import com.example.data.model.StudyLogEntity
import com.example.data.model.SubjectEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        SubjectEntity::class,
        StudyLogEntity::class,
        RecommendationEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun subjectDao(): SubjectDao
    abstract fun studyLogDao(): StudyLogDao
    abstract fun recommendationDao(): RecommendationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "student_learning_db"
                )
                    .addCallback(AppDatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class AppDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        prepopulateDatabase(database)
                    }
                }
            }

            suspend fun prepopulateDatabase(db: AppDatabase) {
                val subjectDao = db.subjectDao()
                val logDao = db.studyLogDao()
                val recDao = db.recommendationDao()

                // Initial Subjects
                val math = SubjectEntity(
                    name = "수학",
                    currentScore = 58,
                    targetScore = 85,
                    completedUnits = 5,
                    totalUnits = 12,
                    totalStudyMinutes = 340,
                    isWeak = true,
                    weakTopic = "이차방정식과 함수 활용",
                    colorHex = "#EF4444"
                )
                val english = SubjectEntity(
                    name = "영어",
                    currentScore = 64,
                    targetScore = 90,
                    completedUnits = 6,
                    totalUnits = 10,
                    totalStudyMinutes = 290,
                    isWeak = true,
                    weakTopic = "관계대명사 및 독해 어휘",
                    colorHex = "#F59E0B"
                )
                val korean = SubjectEntity(
                    name = "국어",
                    currentScore = 86,
                    targetScore = 95,
                    completedUnits = 9,
                    totalUnits = 11,
                    totalStudyMinutes = 410,
                    isWeak = false,
                    weakTopic = "고전 시가 어휘",
                    colorHex = "#3B82F6"
                )
                val science = SubjectEntity(
                    name = "과학",
                    currentScore = 74,
                    targetScore = 88,
                    completedUnits = 7,
                    totalUnits = 10,
                    totalStudyMinutes = 260,
                    isWeak = false,
                    weakTopic = "화학 반응식과 양적 관계",
                    colorHex = "#10B981"
                )
                val social = SubjectEntity(
                    name = "사회·역사",
                    currentScore = 92,
                    targetScore = 95,
                    completedUnits = 10,
                    totalUnits = 10,
                    totalStudyMinutes = 380,
                    isWeak = false,
                    weakTopic = "근현대사 연표 정리",
                    colorHex = "#8B5CF6"
                )

                val mathId = subjectDao.insertSubject(math)
                val engId = subjectDao.insertSubject(english)
                val korId = subjectDao.insertSubject(korean)
                val sciId = subjectDao.insertSubject(science)
                val socId = subjectDao.insertSubject(social)

                // Initial Weekly Study Logs (Mon - Sun)
                val sampleLogs = listOf(
                    StudyLogEntity(subjectId = mathId, subjectName = "수학", minutes = 65, dateString = "월", dayOfWeekIndex = 0, topicDescription = "이차방정식 개념 풀이"),
                    StudyLogEntity(subjectId = engId, subjectName = "영어", minutes = 45, dateString = "화", dayOfWeekIndex = 1, topicDescription = "수능 필수 영단어 50개"),
                    StudyLogEntity(subjectId = korId, subjectName = "국어", minutes = 50, dateString = "수", dayOfWeekIndex = 2, topicDescription = "비문학 독해 지문 2편"),
                    StudyLogEntity(subjectId = sciId, subjectName = "과학", minutes = 40, dateString = "목", dayOfWeekIndex = 3, topicDescription = "화학 반응 기초 개념"),
                    StudyLogEntity(subjectId = mathId, subjectName = "수학", minutes = 80, dateString = "금", dayOfWeekIndex = 4, topicDescription = "취약 단원 기출 10제 풀이"),
                    StudyLogEntity(subjectId = engId, subjectName = "영어", minutes = 60, dateString = "토", dayOfWeekIndex = 5, topicDescription = "관계사 문법 연습문제"),
                    StudyLogEntity(subjectId = socId, subjectName = "사회·역사", minutes = 75, dateString = "일", dayOfWeekIndex = 6, topicDescription = "근현대사 마인드맵 정리")
                )
                logDao.insertAll(sampleLogs)

                // Tailored Recommendations for weak subjects
                val recommendations = listOf(
                    RecommendationEntity(
                        subjectId = mathId,
                        subjectName = "수학",
                        type = RecommendationType.CONCEPT_REVIEW,
                        title = "이차방정식 근의 공식 3분 핵심 복습",
                        description = "판별식 D의 부호에 따른 실근 개수와 완전제곱식 변형 핵심 요약본을 점검하세요.",
                        actionLabel = "요약 정리 보기",
                        targetTopic = "이차방정식과 함수 활용",
                        estimatedMinutes = 15
                    ),
                    RecommendationEntity(
                        subjectId = mathId,
                        subjectName = "수학",
                        type = RecommendationType.CLINIC_QUIZ,
                        title = "취약 단원 3문항 맞춤 클리닉 퀴즈",
                        description = "최근 오답률이 높은 이차함수 꼭짓점 좌표와 판별식 핵심 문제를 직접 풀어보세요.",
                        actionLabel = "클리닉 퀴즈 풀기",
                        targetTopic = "이차함수와 그래프",
                        estimatedMinutes = 10
                    ),
                    RecommendationEntity(
                        subjectId = engId,
                        subjectName = "영어",
                        type = RecommendationType.CLINIC_QUIZ,
                        title = "관계대명사 which vs who 구분 퀴즈",
                        description = "선행사와 격(주격, 목적격, 소유격) 판단을 바로잡는 실전 3문항입니다.",
                        actionLabel = "영문법 퀴즈 풀기",
                        targetTopic = "관계대명사 및 독해 어휘",
                        estimatedMinutes = 8
                    ),
                    RecommendationEntity(
                        subjectId = engId,
                        subjectName = "영어",
                        type = RecommendationType.DAILY_MISSION,
                        title = "취약 어휘 20개 플래시 암기 미션",
                        description = "독해 지문에서 자주 놓치는 필수 연결사와 다의어 20개를 완벽 암기하세요.",
                        actionLabel = "단어 확인하기",
                        targetTopic = "필수 수능 어휘",
                        estimatedMinutes = 15
                    ),
                    RecommendationEntity(
                        subjectId = mathId,
                        subjectName = "수학",
                        type = RecommendationType.STUDY_STRATEGY,
                        title = "수학 오답노트 작성법 가이드",
                        description = "틀린 문제를 바로 해설 보지 않고, 3단계(개념 파악 -> 식 유도 -> 검산)로 정리하는 전략입니다.",
                        actionLabel = "학습 전략 확인",
                        targetTopic = "오답 정복 전략",
                        estimatedMinutes = 5
                    )
                )
                recDao.insertAll(recommendations)
            }
        }
    }
}
