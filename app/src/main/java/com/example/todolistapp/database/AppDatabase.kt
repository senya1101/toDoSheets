package com.example.todolistapp.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.todolistapp.models.Task
import com.example.todolistapp.models.Tag
import com.example.todolistapp.models.TaskTagCrossRef
import com.example.todolistapp.models.Category
import com.example.todolistapp.database.dao.TaskDao
import com.example.todolistapp.database.dao.TagDao
import com.example.todolistapp.database.dao.TaskTagDao

// Основной класс БД Room
// Содержит все сущности: задачи, теги, связи задач и тегов, категории
@Database(
    entities = [Task::class, Tag::class, TaskTagCrossRef::class, Category::class],
    version = 2,                 // Версия БД (нужно изменять при изменениях в схеме)
    exportSchema = false         // Не сохраняем схему БД в отдельный файл (для простоты)
)
abstract class AppDatabase : RoomDatabase() {
    // Абстрактные методы для получения DAO (Room генерирует их реализацию автоматически)
    abstract fun taskDao(): TaskDao
    abstract fun tagDao(): TagDao
    abstract fun taskTagDao(): TaskTagDao
    // ПРИМЕЧАНИЕ: categoryDao() здесь нет, возможно для простоты убрали или забыли добавить

    companion object {
        // INSTANCE - синглтон экземпляра БД, volatile для видимости между потоками
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Метод получения синглтона базы данных
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {   // Синхронизация для потокобезопасности
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"                 // Имя файла базы данных
                )
                    // Позволяет выполнять запросы в главном потоке (не рекомендуется для реальных проектов,
                    // но удобно для маленьких и простых приложений или разработки)
                    .allowMainThreadQueries()
                    // Не используем автоматическую миграцию, чтобы избежать потери данных (нужны вручную)
                    .fallbackToDestructiveMigration(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
