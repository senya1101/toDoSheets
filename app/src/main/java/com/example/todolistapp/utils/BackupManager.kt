package com.example.todolistapp.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

// Объект для создания резервных копий базы данных и восстановления из них
object BackupManager {
    // Папка внутри internal storage для хранения бэкапов
    private const val BACKUP_DIR = "backups"
    // Имя файла базы данных (тот, что Room создает)
    private const val DB_NAME = "task_database"

    // Создаёт резервную копию текущей базы
    fun backupDatabase(context: Context, callback: (Boolean, String) -> Unit) {
        try {
            // Папка для бэкапов
            val backupDir = File(context.filesDir, BACKUP_DIR)
            if (!backupDir.exists()) backupDir.mkdirs() // Создаём если нет

            // Файл текущей БД
            val dbFile = context.getDatabasePath(DB_NAME)
            if (!dbFile.exists()) {
                callback(false, "❌ БД не найдена") // Если БД отсутствует — сообщаем
                return
            }

            // Имя файла бэкапа с временной меткой для уникальности
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
            val backupFile = File(backupDir, "${DB_NAME}_$timestamp.db")

            // Копируем файл по байтам
            val inChannel = FileInputStream(dbFile).channel
            val outChannel = FileOutputStream(backupFile).channel

            inChannel.transferTo(0, inChannel.size(), outChannel)

            inChannel.close()
            outChannel.close()

            // callback с успехом и именем бэкапа
            callback(true, "✅ Резервная копия сохранена:\n${backupFile.name}")
            Log.d("Backup", "Success: ${backupFile.absolutePath}")
        } catch (e: Exception) {
            // При ошибке сообщаем и пишем лог
            callback(false, "❌ Ошибка: ${e.message}")
            Log.e("Backup", "Error", e)
        }
    }

    // Восстанавливает базу из последней сохранённой резервной копии
    fun restoreDatabase(context: Context, callback: (Boolean, String) -> Unit) {
        try {
            val backupDir = File(context.filesDir, BACKUP_DIR)
            val backupFiles = backupDir.listFiles()?.sortedByDescending { it.lastModified() } // Сортируем по дате

            if (backupFiles.isNullOrEmpty()) {
                callback(false, "❌ Резервные копии не найдены") // Нет файлов — сообщаем
                return
            }

            // Берём самый свежий бэкап
            val latestBackup = backupFiles.first()
            val dbFile = context.getDatabasePath(DB_NAME)

            // Копируем бэкап в файл базы
            val inChannel = FileInputStream(latestBackup).channel
            val outChannel = FileOutputStream(dbFile).channel

            inChannel.transferTo(0, inChannel.size(), outChannel)

            inChannel.close()
            outChannel.close()

            // callback об успехе с именем восстановленного файла
            callback(true, "✅ БД восстановлена из:\n${latestBackup.name}")
            Log.d("Backup", "Restored from: ${latestBackup.absolutePath}")
        } catch (e: Exception) {
            // При ошибке сообщаем и пишем лог
            callback(false, "❌ Ошибка восстановления: ${e.message}")
            Log.e("Backup", "Restore error", e)
        }
    }
}
