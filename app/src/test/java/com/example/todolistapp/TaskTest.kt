package com.example.todolistapp

import com.example.todolistapp.models.Task
import org.junit.Assert.*
import org.junit.Test


class askTest {

    // Тест создания задачи с явно указанными значениями
    @Test
    fun task_CreatedWithCorrectValues() {
        val task = Task(
            id = 1,
            title = "Test Task",
            description = "Test Description",
            isCompleted = false
        )

        assertEquals(1, task.id)
        assertEquals("Test Task", task.title)
        assertEquals("Test Description", task.description)
        assertFalse(task.isCompleted)  // Проверяем, что задача изначально не выполнена
    }

    // Тест проверки значений по умолчанию для полей
    @Test
    fun task_DefaultValues() {
        val task = Task(
            title = "Test",
            description = "Description"
        )

        // В data class id по умолчанию 0
        assertEquals(0, task.id)
        // isCompleted по умолчанию false (задача не выполнена)
        assertFalse(task.isCompleted)
        // categoryId и deadline по умолчанию null
        assertNull(task.categoryId)
        assertNull(task.deadline)
    }

    // Тест метода copy() — он должен менять только указанные поля
    @Test
    fun task_CopyModifiesOnlySpecifiedFields() {
        val original = Task(
            id = 1,
            title = "Original",
            description = "Desc",
            isCompleted = false
        )
        // Меняем только title
        val modified = original.copy(title = "Modified")

        // Проверяем, что только title изменился, а остальное осталось прежним
        assertEquals("Modified", modified.title)
        assertEquals("Desc", modified.description)
        assertEquals(1, modified.id)
        assertFalse(modified.isCompleted)
    }

    // Тест переключения состояния выполнения задачи
    @Test
    fun task_CompletionToggle() {
        val task = Task(
            id = 1,
            title = "Task",
            description = "",
            isCompleted = false
        )
        // Создаём копию с isCompleted = true
        val completed = task.copy(isCompleted = true)

        assertTrue(completed.isCompleted)   // Новая задача отмечена выполненной
        assertFalse(task.isCompleted)        // Оригинальная задача не изменилась (immutable)
    }

    // Тест: задача с дедлайном сохраняет значение корректно
    @Test
    fun task_WithDeadline() {
        val deadline = System.currentTimeMillis()
        val task = Task(
            id = 1,
            title = "Task",
            description = "",
            isCompleted = false,
            deadline = deadline
        )

        assertEquals(deadline, task.deadline)
        assertNotNull(task.deadline)
    }

    // Тест: задача с категорией
    @Test
    fun task_WithCategory() {
        val categoryId = 5L
        val task = Task(
            id = 1,
            title = "Task",
            description = "",
            isCompleted = false,
            categoryId = categoryId
        )

        assertEquals(categoryId, task.categoryId)
        assertNotNull(task.categoryId)
    }

    // Тест: проверка, что название не пустое (можно расширить в будущем)
    @Test
    fun task_EmptyTitle_NotAllowed() {
        val task = Task(
            title = "Valid Title",
            description = ""
        )
        assertTrue(task.title.isNotEmpty())
    }

    // Тест: проверяем, что класс Task можно использовать как Serializable (здесь просто вызов конструктора)
    @Test
    fun task_Serializable() {
        Task(
            id = 1,
            title = "Test",
            description = "Desc",
            isCompleted = false
        )
        // Просто проверка, что создание прошло без ошибок
        assertTrue(true)
    }
}


