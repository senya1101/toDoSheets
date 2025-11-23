package com.example.todolistapp.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.io.Serializable

// Сущность таблицы tasks в базе данных Room
@Entity(
    tableName = "tasks",
    // Связь с таблицей категорий: categoryId - внешний ключ
    foreignKeys = [
        ForeignKey(
            entity = Category::class,       // Ссылаемся на категорию
            parentColumns = ["id"],         // Колонка id в категории
            childColumns = ["categoryId"], // Колонка categoryId в этой таблице
            onDelete = ForeignKey.SET_NULL // Если категория удалена, ставим categoryId в null
        )
    ]
)
data class Task(
    @PrimaryKey(autoGenerate = true)    // id - первичный ключ, автоинкремент
    var id: Long = 0,

    var title: String = "",             // Название задачи

    var description: String = "",       // Описание

    var isCompleted: Boolean = false,   // Флаг, завершена задача или нет

    var categoryId: Long? = null,       // ID выбранной категории (или null)

    var timestamp: Long? = System.currentTimeMillis(), // Время создания задачи, по умолчанию текущее время

    var deadline: Long? = null          // Дедлайн в виде времени в миллисекундах, опционально

) : Serializable {                    // Serializable для передачи задачи через Intent

    @Ignore                           // Поле, не сохраняется в БД (пометка для Room)
    var tags: List<String> = emptyList() // Список названий тегов для удобства отображения
}
