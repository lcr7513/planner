package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.DDayGoalEntity
import com.example.data.model.PlannerTodoEntity
import com.example.data.model.TimeBlockEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlannerDao {
    // --- To-Do ---
    @Query("SELECT * FROM planner_todos WHERE date = :date ORDER BY isCompleted ASC, id ASC")
    fun getTodosByDate(date: String): Flow<List<PlannerTodoEntity>>

    @Query("SELECT * FROM planner_todos ORDER BY date DESC, id ASC")
    fun getAllTodos(): Flow<List<PlannerTodoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodo(todo: PlannerTodoEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodos(todos: List<PlannerTodoEntity>)

    @Update
    suspend fun updateTodo(todo: PlannerTodoEntity)

    @Delete
    suspend fun deleteTodo(todo: PlannerTodoEntity)

    @Query("DELETE FROM planner_todos WHERE id = :id")
    suspend fun deleteTodoById(id: Long)

    @Query("DELETE FROM planner_todos")
    suspend fun clearAllTodos()

    // --- D-Day ---
    @Query("SELECT * FROM dday_goals ORDER BY isPinned DESC, targetDate ASC")
    fun getAllDDayGoals(): Flow<List<DDayGoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDDayGoal(goal: DDayGoalEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDDayGoals(goals: List<DDayGoalEntity>)

    @Update
    suspend fun updateDDayGoal(goal: DDayGoalEntity)

    @Delete
    suspend fun deleteDDayGoal(goal: DDayGoalEntity)

    @Query("DELETE FROM dday_goals WHERE id = :id")
    suspend fun deleteDDayGoalById(id: Long)

    @Query("DELETE FROM dday_goals")
    suspend fun clearAllDDayGoals()

    // --- 10-Minute Time Blocks ---
    @Query("SELECT * FROM time_blocks WHERE date = :date")
    fun getTimeBlocksByDate(date: String): Flow<List<TimeBlockEntity>>

    @Query("SELECT * FROM time_blocks")
    fun getAllTimeBlocks(): Flow<List<TimeBlockEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimeBlock(block: TimeBlockEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimeBlocks(blocks: List<TimeBlockEntity>)

    @Query("DELETE FROM time_blocks WHERE date = :date AND hour = :hour AND blockIndex = :blockIndex")
    suspend fun deleteTimeBlock(date: String, hour: Int, blockIndex: Int)

    @Query("DELETE FROM time_blocks")
    suspend fun clearAllTimeBlocks()
}
