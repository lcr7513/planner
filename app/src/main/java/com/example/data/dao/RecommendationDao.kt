package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.RecommendationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecommendationDao {
    @Query("SELECT * FROM recommendations ORDER BY isCompleted ASC, id ASC")
    fun getAllRecommendations(): Flow<List<RecommendationEntity>>

    @Query("SELECT * FROM recommendations WHERE subjectId = :subjectId ORDER BY isCompleted ASC")
    fun getRecommendationsForSubject(subjectId: Long): Flow<List<RecommendationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecommendation(item: RecommendationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<RecommendationEntity>)

    @Update
    suspend fun updateRecommendation(item: RecommendationEntity)

    @Query("UPDATE recommendations SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun setCompleted(id: Long, isCompleted: Boolean)

    @Query("DELETE FROM recommendations WHERE id = :id")
    suspend fun deleteRecommendation(id: Long)
}
