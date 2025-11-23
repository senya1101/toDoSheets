package com.example.todolistapp.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.example.todolistapp.models.Task
import com.example.todolistapp.models.Category
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// data class для хранения данных бэкапа
// Структура JSON файла: задачи + категории + дата экспорта
data class BackupData(
    val tasks: List<Task>,        // Список всех задач
    val categories: List<Category>, // Список всех категорий
    val exportDate: String          // Дата создания бэкапа в формате "2024-12-25 15:30:00"
)

// object - Singleton для экспорта/импорта данных в JSON
// Используется для создания резервных копий и восстановления данных
object DataExporter {
    // Gson - библиотека Google для сериализации/десериализации JSON
    // GsonBuilder().setPrettyPrinting() - красивое форматирование JSON с отступами
    // create() - создаёт экземпляр Gson с настройками
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    // Экспортирует задачи и категории в JSON файл
    // Возвращает Result.Success с путём к файлу или Result.Error с сообщением об ошибке
    fun exportToJson(
        context: Context,        // Контекст для доступа к файловой системе
        tasks: List<Task>,       // Список задач для экспорта
        categories: List<Category> // Список категорий для экспорта
    ): Result {
        return try {
            // Создаём объект BackupData с данными и текущей датой
            val backupData = BackupData(
                tasks = tasks,
                categories = categories,
                // SimpleDateFormat форматирует Date в строку "2024-12-25 15:30:00"
                exportDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            )

            // Имя файла с timestamp для уникальности
            // Пример: tasks_backup_1735134600000.json
            val fileName = "tasks_backup_${System.currentTimeMillis()}.json"
            // Папка backups в внутреннем хранилище приложения (/data/data/com.example.todolistapp/files/backups)
            val backupDir = File(context.filesDir, "backups")

            // Проверяем существует ли папка backups
            if (!backupDir.exists()) {
                // Если нет - создаём (mkdirs создаёт всю иерархию папок)
                backupDir.mkdirs()
            }

            // Создаём File объект для нового бэкапа
            val file = File(backupDir, fileName)
            // gson.toJson() - конвертирует BackupData в JSON строку
            // writeText() - записывает строку в файл (перезаписывает если файл существует)
            file.writeText(gson.toJson(backupData))

            Result.Success(file.absolutePath)
        } catch (e: Exception) {
            Result.Error("Ошибка экспорта: ${e.message}")
        }
    }

    fun importFromJson(filePath: String): Result {
        return try {
            val file = File(filePath)
            if (!file.exists()) {
                return Result.Error("Файл не найден")
            }
            val jsonString = file.readText()

            val backupData = gson.fromJson(jsonString, BackupData::class.java)
            Result.Success(backupData)
        } catch (e: Exception) {
            Result.Error("Ошибка импорта: ${e.message}")
        }
    }

    fun getBackupsList(context: Context): List<File> {
        val backupDir = File(context.filesDir, "backups")
        return backupDir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()
    }
}

