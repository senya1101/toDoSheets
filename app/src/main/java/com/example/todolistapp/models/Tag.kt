package com.example.todolistapp.models

import androidx.room.Entity
import androidx.room.PrimaryKey

// @Entity - аннотация Room, объявляет класс как таблицу в БД
// tableName = "tags" - явное указание имени таблицы (по умолчанию было бы "Tag")
// Эта таблица хранит все доступные теги для задач
@Entity(tableName = "tags")
// data class - класс для хранения данных с автогенерацией equals(), hashCode(), toString()
data class Tag(
    // @PrimaryKey - первичный ключ (уникальный идентификатор тега)
    // autoGenerate = true - Room автоматически генерирует id при insert
    // id=1 для первого тега, id=2 для второго, и т.д.
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,  // = 0 это дефолтное значение (при создании нового тега)

    // Название тега (например: "Срочно", "Важно", "Потом", "На работе")
    // String не nullable - тег ВСЕГДА должен иметь название
    val name: String
)

// Теги используются для дополнительной классификации задач
// Отличие от категорий:
// - Категория: задача может иметь ТОЛЬКО ОДНУ (например: "Работа")
// - Теги: задача может иметь НЕСКОЛЬКО (например: "Срочно" + "Важно" + "На работе")
//
// Пример использования:
// val urgentTag = Tag(name = "Срочно")
// tagRepository.insert(urgentTag)
//
// Связь с задачей через TaskTagCrossRef (many-to-many):
// val task = Task(title = "Написать отчёт", ...)
// taskRepository.insert(task)
// taskTagRepository.insertTaskTag(TaskTagCrossRef(taskId = task.id, tagId = urgentTag.id))
