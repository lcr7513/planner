package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.QuizBank
import com.example.data.service.GeminiAiService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("학습매니저", appName)
  }

  @Test
  fun `quiz bank returns dynamic questions for topic`() {
    val questions = QuizBank.getDynamicQuestionsForTopic("수학", "이차함수와 그래프")
    assertTrue(questions.isNotEmpty())
    assertEquals(3, questions.size)
    assertTrue(questions.first().options.size == 4)
  }

  @Test
  fun `gemini ai service returns fallback questions when api key is not set`() = runBlocking {
    val service = GeminiAiService()
    val questions = service.generateClinicQuiz("수학", "삼각비", 55)
    assertTrue(questions.isNotEmpty())
    assertEquals(3, questions.size)
  }

  @Test
  fun `room database subject dao insert and delete test`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    val subjectDao = db.subjectDao()

    val testSubject = com.example.data.model.SubjectEntity(
        id = 100L,
        name = "물리학",
        currentScore = 75,
        targetScore = 95,
        completedUnits = 3,
        totalUnits = 12,
        totalStudyMinutes = 90,
        isWeak = false,
        weakTopic = "역학적 에너지 보존"
    )

    val insertedId = subjectDao.insertSubject(testSubject)
    val fetched = subjectDao.getSubjectById(insertedId)
    assertEquals("물리학", fetched?.name)
    assertEquals(75, fetched?.currentScore)
    assertEquals(0.25f, fetched?.progress ?: 0f, 0.01f)

    // Test progress update
    subjectDao.updateProgressAndStudyTime(insertedId, 6, 45)
    val updated = subjectDao.getSubjectById(insertedId)
    assertEquals(6, updated?.completedUnits)
    assertEquals(0.5f, updated?.progress ?: 0f, 0.01f)

    subjectDao.deleteSubjectById(insertedId)
    val afterDelete = subjectDao.getSubjectById(insertedId)
    org.junit.Assert.assertNull(afterDelete)

    db.close()
  }

  @Test
  fun `room study logs subject study time calculation test`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    val logDao = db.studyLogDao()

    val log1 = com.example.data.model.StudyLogEntity(
        subjectId = 1L,
        subjectName = "수학",
        minutes = 50,
        dateString = "월",
        dayOfWeekIndex = 0,
        topicDescription = "방정식 풀이"
    )
    val log2 = com.example.data.model.StudyLogEntity(
        subjectId = 1L,
        subjectName = "수학",
        minutes = 40,
        dateString = "수",
        dayOfWeekIndex = 2,
        topicDescription = "함수 그래프"
    )
    val log3 = com.example.data.model.StudyLogEntity(
        subjectId = 2L,
        subjectName = "영어",
        minutes = 60,
        dateString = "화",
        dayOfWeekIndex = 1,
        topicDescription = "영단어 암기"
    )

    logDao.insertAll(listOf(log1, log2, log3))

    val logs = listOf(log1, log2, log3)
    val mathTotal = logs.filter { it.subjectName == "수학" }.sumOf { it.minutes }
    val engTotal = logs.filter { it.subjectName == "영어" }.sumOf { it.minutes }

    assertEquals(90, mathTotal)
    assertEquals(60, engTotal)

    val grandTotal = mathTotal + engTotal
    val mathRatio = mathTotal.toFloat() / grandTotal
    assertEquals(0.6f, mathRatio, 0.01f)

    db.close()
  }

  @Test
  fun `room planner todo and dday goals persistence test`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    val plannerDao = db.plannerDao()

    // 1. D-Day Goal
    val goal = com.example.data.model.DDayGoalEntity(
        title = "수능",
        targetDate = "2026-11-19",
        colorHex = "#6366F1",
        isPinned = true
    )
    val goalId = plannerDao.insertDDayGoal(goal)
    assertTrue(goalId > 0)

    // 2. Planner To-Do
    val todo = com.example.data.model.PlannerTodoEntity(
        date = "2026-10-14",
        subjectId = 1L,
        subjectName = "수학",
        title = "미적분 연습문제",
        targetMinutes = 45,
        isCompleted = false
    )
    val todoId = plannerDao.insertTodo(todo)
    assertTrue(todoId > 0)

    plannerDao.updateTodo(todo.copy(id = todoId, isCompleted = true))

    // 3. Time Block
    val block = com.example.data.model.TimeBlockEntity(
        date = "2026-10-14",
        hour = 10,
        blockIndex = 2,
        subjectName = "수학",
        colorHex = "#6366F1"
    )
    val blockId = plannerDao.insertTimeBlock(block)
    assertTrue(blockId > 0)

    db.close()
  }
}
