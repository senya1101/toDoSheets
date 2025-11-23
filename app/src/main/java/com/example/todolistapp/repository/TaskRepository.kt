package com.example.todolistapp.repository

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import com.example.todolistapp.R
import com.example.todolistapp.database.dao.TaskDao
import com.example.todolistapp.database.dao.TaskTagDao
import com.example.todolistapp.database.dao.TagDao
import com.example.todolistapp.models.Task
import com.example.todolistapp.models.Tag
import com.example.todolistapp.models.TaskTagCrossRef
import com.example.todolistapp.widget.TaskWidgetProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

// Репозиторий — посредник между БД и UI/другими слоями
class TaskRepository(
    private val taskDao: TaskDao,
    private val tagDao: TagDao,
    private val taskTagDao: TaskTagDao
) {

    // Поток активных (не завершённых) задач для подписки в UI
    val allActiveTasks: Flow<List<Task>> = taskDao.getAllActiveTasks()

    // Поток завершённых задач для подписки в UI
    val allCompletedTasks: Flow<List<Task>> = taskDao.getAllCompletedTasks()

    // Вставляем новую задачу (асинхронно)
    suspend fun insert(task: Task) = taskDao.insertTask(task)

    // Обновляем задачу
    suspend fun update(task: Task) = taskDao.updateTask(task)

    // Удаляем задачу
    suspend fun delete(task: Task) = taskDao.deleteTask(task)

    // Удаляем все задачи (полная очистка)
    suspend fun deleteAll() = taskDao.deleteAll()

    // Получаем поток всех тегов
    fun getAllTags(): Flow<List<Tag>> = tagDao.getAllTags()

    // Вставляем новый тег, возвращаем его ID
    suspend fun insertTag(tag: Tag): Long = tagDao.insertTag(tag)

    // Добавляем связь задача-тег
    suspend fun insertTaskTag(taskTagCrossRef: TaskTagCrossRef) = taskTagDao.insertTaskTag(taskTagCrossRef)

    // Получаем текущие связи задачи с тегами (для редактирования)
    suspend fun getTaskTagsForEdit(taskId: Long): List<TaskTagCrossRef> = taskTagDao.getTaskTags(taskId).first()

    // Удаляем все связи тегов для задачи (например, перед обновлением связей)
    suspend fun deleteTaskTags(taskId: Long) = taskTagDao.deleteAllTagsForTask(taskId)

    // Получаем id последней добавленной активной задачи, или 0 если нет задач
    suspend fun getLastTaskId(): Long {
        val allTasks = taskDao.getAllActiveTasks().first()
        return allTasks.lastOrNull()?.id ?: 0L
    }

    // Получаем задачи начиная с выбранной даты
    fun getTasksByDate(selectedDate: Long): Flow<List<Task>> = taskDao.getTasksByDate(selectedDate)

    // Обновляем данные виджета задач на домашнем экране
    @Suppress("DEPRECATION") // suppressing warning on some deprecated usage here
    fun updateWidget(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, TaskWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
        // Сообщаем всем виджетам, что их данные в списке изменились (перерисовать)
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widgetListView)
    }

    // Получаем имя тега по его id
    suspend fun getTagNameById(tagId: Long): String? {
        return try {
            val allTags = tagDao.getAllTags().first()
            allTags.firstOrNull { it.id == tagId }?.name
        } catch (_: Exception) {
            null // Если что-то не так, просто возвращаем null
        }
    }
}
