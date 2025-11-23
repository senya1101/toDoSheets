package com.example.todolistapp.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.todolistapp.models.Task
import kotlinx.coroutines.flow.Flow

// DAO — интерфейс для работы с таблицей tasks в базе Room
@Dao
interface TaskDao {

    // Вставить новую задачу в базу (асинхронно)
    @Insert
    suspend fun insertTask(task: Task)

    // Обновить существующую задачу
    @Update
    suspend fun updateTask(task: Task)

    // Удалить задачу
    @Delete
    suspend fun deleteTask(task: Task)

    // Очистить всю таблицу задач
    @Query("DELETE FROM tasks")
    suspend fun deleteAll()

    // Получить задачу по id (одиночный результат)
    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): Task?

    // Поток активных (не завершённых) задач, сортировка по id по убыванию (новые сверху)
    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY id DESC")
    fun getAllActiveTasks(): Flow<List<Task>>

    // Активные задачи, сортировка по названию (по возрастанию)
    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY title ASC")
    fun getActiveTasksSortedByTitle(): Flow<List<Task>>

    // Активные задачи, сортировка по дедлайну (по возрастанию)
    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY deadline ASC")
    fun getActiveTasksSortedByDate(): Flow<List<Task>>

    // Поток завершённых задач, сортировка по id (новые сверху)
    @Query("SELECT * FROM tasks WHERE isCompleted = 1 ORDER BY id DESC")
    fun getAllCompletedTasks(): Flow<List<Task>>

    // Завершённые задачи, сортировка по названию
    @Query("SELECT * FROM tasks WHERE isCompleted = 1 ORDER BY title ASC")
    fun getCompletedTasksSortedByTitle(): Flow<List<Task>>

    // Завершённые задачи, сортировка по дедлайну
    @Query("SELECT * FROM tasks WHERE isCompleted = 1 ORDER BY deadline ASC")
    fun getCompletedTasksSortedByDate(): Flow<List<Task>>

    // Поиск активных задач по названию с сортировкой по дедлайну
    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND title LIKE '%' || :query || '%' ORDER BY deadline ASC")
    fun searchActiveTasksByDate(query: String): Flow<List<Task>>

    // Поиск активных задач по названию с сортировкой по названию
    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND title LIKE '%' || :query || '%' ORDER BY title ASC")
    fun searchActiveTasksByTitle(query: String): Flow<List<Task>>

    // Поиск завершённых задач по названию с сортировкой по дедлайну
    @Query("SELECT * FROM tasks WHERE isCompleted = 1 AND title LIKE '%' || :query || '%' ORDER BY deadline ASC")
    fun searchCompletedTasksByDate(query: String): Flow<List<Task>>

    // Поиск завершённых задач по названию с сортировкой по названию
    @Query("SELECT * FROM tasks WHERE isCompleted = 1 AND title LIKE '%' || :query || '%' ORDER BY title ASC")
    fun searchCompletedTasksByTitle(query: String): Flow<List<Task>>

    // Получение задач начиная с заданной даты (deadline >= startDate)
    @Query("SELECT * FROM tasks WHERE deadline >= :startDate ORDER BY deadline ASC")
    fun getTasksByDate(startDate: Long): Flow<List<Task>>
}
