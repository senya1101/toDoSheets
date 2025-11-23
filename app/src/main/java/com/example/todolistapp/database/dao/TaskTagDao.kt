package com.example.todolistapp.database.dao

import androidx.room.*
import com.example.todolistapp.models.TaskTagCrossRef
import com.example.todolistapp.models.Tag
import kotlinx.coroutines.flow.Flow

// DAO для связи "многие ко многим" между задачами и тегами
@Dao
interface TaskTagDao {

    // Вставляем связь между задачей и тегом
    @Insert
    suspend fun insertTaskTag(taskTag: TaskTagCrossRef)

    // Удаляем связь задачи и тега
    @Delete
    suspend fun deleteTaskTag(taskTag: TaskTagCrossRef)

    // Получаем список тегов, связанных с конкретной задачей (через join)
    @Query("SELECT t.* FROM tags t INNER JOIN task_tag_cross_ref tt ON t.id = tt.tagId WHERE tt.taskId = :taskId")
    fun getTagsForTask(taskId: Long): Flow<List<Tag>>

    // Получаем связи (TaskTagCrossRef) для задачи (id задачи → id тегов)
    @Query("SELECT tt.* FROM task_tag_cross_ref tt WHERE tt.taskId = :taskId")
    fun getTaskTags(taskId: Long): Flow<List<TaskTagCrossRef>>

    // Удаляем все связи тегов для конкретной задачи (например, при обновлении тегов)
    @Query("DELETE FROM task_tag_cross_ref WHERE taskId = :taskId")
    suspend fun deleteAllTagsForTask(taskId: Long)
}
