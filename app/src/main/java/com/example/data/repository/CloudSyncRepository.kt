package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.DDayGoalEntity
import com.example.data.model.PlannerTodoEntity
import com.example.data.model.StudyLogEntity
import com.example.data.model.SubjectEntity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "CloudSyncRepository"

data class RestoreDataPayload(
    val subjects: List<SubjectEntity>,
    val logs: List<StudyLogEntity>,
    val todos: List<PlannerTodoEntity>,
    val ddays: List<DDayGoalEntity>,
    val backupTimestamp: Long
)

class CloudSyncRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    // Secondary constructor to safely resolve database ID from string resources
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        Firebase.auth
    )

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    fun observeAuthState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user ?: throw IllegalStateException("Firebase User is null after sign in")

            // Initialize or update user document in Firestore
            val userDoc = firestore.collection("users").document(user.uid)
            val userData = mapOf(
                "uid" to user.uid,
                "email" to (user.email ?: ""),
                "displayName" to (user.displayName ?: "학생"),
                "lastActive" to System.currentTimeMillis()
            )
            userDoc.set(userData, SetOptions.merge()).await()

            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Google sign-in error", e)
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
    }

    /**
     * 학생의 전체 학습 데이터(과목, 기록, To-Do, D-Day)를 클라우드 Firestore에 백업
     */
    suspend fun backupFullData(
        userId: String,
        subjects: List<SubjectEntity>,
        logs: List<StudyLogEntity>,
        todos: List<PlannerTodoEntity>,
        ddays: List<DDayGoalEntity>
    ): Result<String> {
        return try {
            val userRef = firestore.collection("users").document(userId)

            // Convert to JSON snapshot payload
            val rootObj = JSONObject()
            val subjectsArr = JSONArray()
            subjects.forEach { s ->
                val obj = JSONObject()
                obj.put("id", s.id)
                obj.put("name", s.name)
                obj.put("currentScore", s.currentScore)
                obj.put("targetScore", s.targetScore)
                obj.put("completedUnits", s.completedUnits)
                obj.put("totalUnits", s.totalUnits)
                obj.put("totalStudyMinutes", s.totalStudyMinutes)
                obj.put("isWeak", s.isWeak)
                obj.put("weakTopic", s.weakTopic)
                obj.put("colorHex", s.colorHex)
                subjectsArr.put(obj)
            }
            rootObj.put("subjects", subjectsArr)

            val todosArr = JSONArray()
            todos.forEach { t ->
                val obj = JSONObject()
                obj.put("id", t.id)
                obj.put("date", t.date)
                obj.put("subjectId", t.subjectId ?: -1L)
                obj.put("subjectName", t.subjectName)
                obj.put("title", t.title)
                obj.put("targetMinutes", t.targetMinutes)
                obj.put("isCompleted", t.isCompleted)
                todosArr.put(obj)
            }
            rootObj.put("todos", todosArr)

            val ddaysArr = JSONArray()
            ddays.forEach { d ->
                val obj = JSONObject()
                obj.put("id", d.id)
                obj.put("title", d.title)
                obj.put("targetDate", d.targetDate)
                obj.put("colorHex", d.colorHex)
                obj.put("isPinned", d.isPinned)
                ddaysArr.put(obj)
            }
            rootObj.put("ddays", ddaysArr)

            val logsArr = JSONArray()
            logs.take(50).forEach { l ->
                val obj = JSONObject()
                obj.put("id", l.id)
                obj.put("subjectId", l.subjectId)
                obj.put("subjectName", l.subjectName)
                obj.put("dateString", l.dateString)
                obj.put("minutes", l.minutes)
                obj.put("dayOfWeekIndex", l.dayOfWeekIndex)
                obj.put("topicDescription", l.topicDescription)
                obj.put("timestamp", l.timestamp)
                logsArr.put(obj)
            }
            rootObj.put("logs", logsArr)

            val backupId = "backup_latest"
            val backupDoc = userRef.collection("backups").document(backupId)
            val backupData = mapOf(
                "backupId" to backupId,
                "createdAt" to System.currentTimeMillis(),
                "payloadJson" to rootObj.toString(),
                "totalStudyMinutes" to subjects.sumOf { it.totalStudyMinutes },
                "completedTodosCount" to todos.count { it.isCompleted },
                "subjectCount" to subjects.size
            )
            backupDoc.set(backupData).await()

            // Update user's lastSyncedAt
            userRef.set(mapOf("lastSyncedAt" to System.currentTimeMillis()), SetOptions.merge()).await()

            Result.success("클라우드 백업이 완료되었습니다. (과목 ${subjects.size}개, 플래너 할 일 ${todos.size}개, D-Day ${ddays.size}개)")
        } catch (e: Exception) {
            Log.e(TAG, "Backup failed", e)
            Result.failure(e)
        }
    }

    /**
     * 클라우드 Firestore에서 최신 백업 데이터를 가져와 복원 가능한 객체로 변환
     */
    suspend fun restoreFullData(userId: String): Result<RestoreDataPayload> {
        return try {
            val userRef = firestore.collection("users").document(userId)
            val backupDoc = userRef.collection("backups").document("backup_latest").get().await()

            if (!backupDoc.exists()) {
                return Result.failure(IllegalStateException("클라우드에 저장된 백업 데이터가 없습니다."))
            }

            val payloadJson = backupDoc.getString("payloadJson") ?: ""
            val createdAt = backupDoc.getLong("createdAt") ?: System.currentTimeMillis()

            val rootObj = JSONObject(payloadJson)

            val subjects = mutableListOf<SubjectEntity>()
            val subjectsArr = rootObj.optJSONArray("subjects")
            if (subjectsArr != null) {
                for (i in 0 until subjectsArr.length()) {
                    val o = subjectsArr.getJSONObject(i)
                    subjects.add(
                        SubjectEntity(
                            id = o.optLong("id", 0L),
                            name = o.optString("name", "과목"),
                            currentScore = o.optInt("currentScore", 60),
                            targetScore = o.optInt("targetScore", 80),
                            completedUnits = o.optInt("completedUnits", 1),
                            totalUnits = o.optInt("totalUnits", 10),
                            totalStudyMinutes = o.optInt("totalStudyMinutes", 0),
                            isWeak = o.optBoolean("isWeak", false),
                            weakTopic = o.optString("weakTopic", ""),
                            colorHex = o.optString("colorHex", "#6366F1")
                        )
                    )
                }
            }

            val todos = mutableListOf<PlannerTodoEntity>()
            val todosArr = rootObj.optJSONArray("todos")
            if (todosArr != null) {
                for (i in 0 until todosArr.length()) {
                    val o = todosArr.getJSONObject(i)
                    val sId = o.optLong("subjectId", -1L)
                    todos.add(
                        PlannerTodoEntity(
                            id = o.optLong("id", 0L),
                            date = o.optString("date", ""),
                            subjectId = if (sId > 0) sId else null,
                            subjectName = o.optString("subjectName", ""),
                            title = o.optString("title", ""),
                            targetMinutes = o.optInt("targetMinutes", 45),
                            isCompleted = o.optBoolean("isCompleted", false)
                        )
                    )
                }
            }

            val ddays = mutableListOf<DDayGoalEntity>()
            val ddaysArr = rootObj.optJSONArray("ddays")
            if (ddaysArr != null) {
                for (i in 0 until ddaysArr.length()) {
                    val o = ddaysArr.getJSONObject(i)
                    ddays.add(
                        DDayGoalEntity(
                            id = o.optLong("id", 0L),
                            title = o.optString("title", ""),
                            targetDate = o.optString("targetDate", ""),
                            colorHex = o.optString("colorHex", "#6366F1"),
                            isPinned = o.optBoolean("isPinned", false)
                        )
                    )
                }
            }

            val logs = mutableListOf<StudyLogEntity>()
            val logsArr = rootObj.optJSONArray("logs")
            if (logsArr != null) {
                for (i in 0 until logsArr.length()) {
                    val o = logsArr.getJSONObject(i)
                    logs.add(
                        StudyLogEntity(
                            id = o.optLong("id", 0L),
                            subjectId = o.optLong("subjectId", 0L),
                            subjectName = o.optString("subjectName", ""),
                            dateString = o.optString("dateString", "오늘"),
                            minutes = o.optInt("minutes", 30),
                            dayOfWeekIndex = o.optInt("dayOfWeekIndex", 0),
                            topicDescription = o.optString("topicDescription", ""),
                            timestamp = o.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
            }

            Result.success(
                RestoreDataPayload(
                    subjects = subjects,
                    logs = logs,
                    todos = todos,
                    ddays = ddays,
                    backupTimestamp = createdAt
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Restore failed", e)
            Result.failure(e)
        }
    }
}
