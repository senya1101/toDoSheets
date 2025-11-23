package com.example.todolistapp.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

// @Entity - промежуточная таблица для связи "многие ко многим" между Task и Tag
// Это классический паттерн БД для many-to-many отношений
@Entity(
    tableName = "task_tag_cross_ref",  // Имя таблицы в SQLite БД

    // primaryKeys - составной первичный ключ из двух полей
    // Комбинация (taskId, tagId) должна быть уникальной
    // Это предотвращает дублирование: одна задача не может иметь один тег дважды
    primaryKeys = ["taskId", "tagId"],

    // foreignKeys - массив внешних ключей (связи с двумя таблицами)
    foreignKeys = [
        // Первый ForeignKey - связь с таблицей tasks
        ForeignKey(
            entity = Task::class,        // Родительская таблица - tasks
            parentColumns = ["id"],       // Колонка в tasks (primary key)
            childColumns = ["taskId"],    // Колонка в task_tag_cross_ref
            // onDelete = CASCADE - при удалении задачи, все её связи с тегами тоже удаляются
            // Это логично: если задача удалена, её теги тоже должны исчезнуть
            onDelete = ForeignKey.CASCADE
        ),
        // Второй ForeignKey - связь с таблицей tags
        ForeignKey(
            entity = Tag::class,         // Родительская таблица - tags
            parentColumns = ["id"],       // Колонка в tags (primary key)
            childColumns = ["tagId"],     // Колонка в task_tag_cross_ref
            // onDelete = CASCADE - при удалении тега, все связи этого тега с задачами удаляются
            // Это логично: если тег "Срочно" удалён, он исчезает у всех задач
            onDelete = ForeignKey.CASCADE
        )
    ],

    // indices - индексы для ускорения поиска
    // Index("taskId") - ускоряет поиск "все теги для задачи X"
    // Index("tagId") - ускоряет поиск "все задачи с тегом Y"
    // Индексы критически важны для производительности JOIN запросов
    indices = [Index("taskId"), Index("tagId")]
)
// data class с двумя полями - минималистичная таблица связей
data class TaskTagCrossRef(
    // ID задачи из таблицы tasks
    val taskId: Long,
    // ID тега из таблицы tags
    val tagId: Long
)


