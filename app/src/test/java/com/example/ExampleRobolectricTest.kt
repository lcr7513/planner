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
}
