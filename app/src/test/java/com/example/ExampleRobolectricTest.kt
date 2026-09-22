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
}
