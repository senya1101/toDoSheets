package com.example.todolistapp

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.todolistapp.database.AppDatabase
import com.example.todolistapp.database.dao.TaskDao
import com.example.todolistapp.models.Task
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaskDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var taskDao: TaskDao

    // Настраиваем in-memory базу перед каждым тестом (чтобы не влиять на реальную)
    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries() // Позволяем запросы в главном потоке для тестов
            .build()
        taskDao = database.taskDao()
    }

    // Закрываем базу после каждого теста чтобы освобождать ресурсы
    @After
    fun teardown() {
        database.close()
    }

    // Тест вставки задачи и её корректного извлечения из БД
    @Test
    fun insertTaskAndRetrieveIt() = runBlocking {
        val task = Task(title = "Test Task", description = "Test Description")

        // Вставляем задачу
        taskDao.insertTask(task)
        // Получаем все активные задачи
        val tasks = taskDao.getAllActiveTasks().first()

        // Проверяем, что задача одна и её данные совпадают
        assertEquals(1, tasks.size)
        assertEquals("Test Task", tasks[0].title)
        assertEquals("Test Description", tasks[0].description)
    }

    // Тест удаления задачи из базы
    @Test
    fun deleteTaskRemovesIt() = runBlocking {
        val task = Task(title = "Test Task", description = "Test")

        taskDao.insertTask(task)
        val insertedTasks = taskDao.getAllActiveTasks().first()

        // Удаляем ранее вставленную задачу
        taskDao.deleteTask(insertedTasks[0])

        val remainingTasks = taskDao.getAllActiveTasks().first()
        // Проверяем, что задач теперь нет
        assertEquals(0, remainingTasks.size)
    }

    // Тест обновления свойств задачи
    @Test
    fun updateTaskChangesProperties() = runBlocking {
        val task = Task(title = "Old Title", description = "Test")

        taskDao.insertTask(task)
        val insertedTasks = taskDao.getAllActiveTasks().first()

        val updatedTask = insertedTasks[0].copy(title = "New Title")

        taskDao.updateTask(updatedTask)

        val finalTasks = taskDao.getAllActiveTasks().first()
        // Проверяем, что название обновилось
        assertEquals("New Title", finalTasks[0].title)
    }

    // Тест получения только завершённых задач
    @Test
    fun getCompletedTasksReturnsOnlyCompleted() = runBlocking {
        taskDao.insertTask(Task(title = "Task 1", description = "", isCompleted = true))
        taskDao.insertTask(Task(title = "Task 2", description = "", isCompleted = false))
        taskDao.insertTask(Task(title = "Task 3", description = "", isCompleted = true))

        val completedTasks = taskDao.getCompletedTasksSortedByDate().first()

        // Проверяем, что вернулись только две завершённые задачи
        assertEquals(2, completedTasks.size)
        assertTrue(completedTasks.all { it.isCompleted })
    }
}

