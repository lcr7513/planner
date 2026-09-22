package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.SubjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects ORDER BY currentScore ASC")
    fun getAllSubjects(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE isWeak = 1 ORDER BY currentScore ASC")
    fun getWeakSubjects(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE id = :id LIMIT 1")
    suspend fun getSubjectById(id: Long): SubjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: SubjectEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(subjects: List<SubjectEntity>)

    @Update
    suspend fun updateSubject(subject: SubjectEntity)

    @Query("UPDATE subjects SET currentScore = :newScore, isWeak = CASE WHEN :newScore < 70 THEN 1 ELSE 0 END WHERE id = :subjectId")
    suspend fun updateScore(subjectId: Long, newScore: Int)

    @Query("UPDATE subjects SET completedUnits = :units, totalStudyMinutes = totalStudyMinutes + :addMinutes WHERE id = :subjectId")
    suspend fun updateProgressAndStudyTime(subjectId: Long, units: Int, addMinutes: Int)

    @Query("DELETE FROM subjects WHERE id = :id")
    suspend fun deleteSubject(id: Long)
}
