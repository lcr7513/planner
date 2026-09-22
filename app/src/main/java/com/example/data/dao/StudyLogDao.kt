package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.StudyLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyLogDao {
    @Query("SELECT * FROM study_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<StudyLogEntity>>

    @Query("SELECT * FROM study_logs ORDER BY timestamp DESC LIMIT 7")
    fun getRecentLogs(): Flow<List<StudyLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: StudyLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<StudyLogEntity>)

    @Query("SELECT SUM(minutes) FROM study_logs")
    fun getTotalMinutes(): Flow<Int?>
}
